package com.ibm.wala.cast.python.util;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.io.Files.getNameWithoutExtension;
import static com.ibm.wala.cast.python.types.PythonTypes.CAST_DYNAMIC_ANNOTATION;
import static com.ibm.wala.cast.python.types.PythonTypes.CLASS_METHOD;
import static com.ibm.wala.types.annotations.Annotation.make;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys.ScopeMappingInstanceKey;
import com.ibm.wala.cast.python.ipa.callgraph.PytestEntrypointBuilder;
import com.ibm.wala.cast.python.ipa.summaries.PythonInstanceMethodTrampoline;
import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Util {

  private static final Logger LOGGER = Logger.getLogger(Util.class.getName());

  /** Key used to map annotations (decorators) to names. */
  public static final String DYNAMIC_ANNOTATION_KEY = "dynamicAnnotation";

  /** Name of the annotation (decorator) that marks methods as static. */
  public static final String STATIC_METHOD_ANNOTATION_NAME = "staticmethod";

  /** Name of the annotation (decorator) that marks methods as a class method. */
  public static final String CLASS_METHOD_ANNOTATION_NAME = "classmethod";

  /** All Python files must have this extension. */
  public static final String PYTHON_FILE_EXTENSION = "py";

  /**
   * The Python standard module initialization file. These <a href="https://bit.ly/4as7ELJ">files
   * are required to make Python treat directories containing the file as packages (unless using a
   * namespace package, a relatively advanced feature)</a>.
   */
  public static final String MODULE_INITIALIZATION_FILENAME = "__init__." + PYTHON_FILE_EXTENSION;

  /** Name of the Python initialization file without the extension. */
  public static final String MODULE_INITIALIZATION_ENTITY_NAME =
      getNameWithoutExtension(MODULE_INITIALIZATION_FILENAME);

  public static final String IMPORT_WILDCARD_CHARACTER = "*";

  /**
   * Add Pytest entrypoints to the given {@link PropagationCallGraphBuilder}.
   *
   * @param callGraphBuilder The {@link PropagationCallGraphBuilder} for which to add Pytest
   *     entrypoints.
   */
  public static void addPytestEntrypoints(PropagationCallGraphBuilder callGraphBuilder) {
    Iterable<? extends Entrypoint> defaultEntrypoints =
        callGraphBuilder.getOptions().getEntrypoints();

    Iterable<Entrypoint> pytestEntrypoints =
        new PytestEntrypointBuilder().createEntrypoints(callGraphBuilder.getClassHierarchy());

    Iterable<Entrypoint> entrypoints = concat(defaultEntrypoints, pytestEntrypoints);

    callGraphBuilder.getOptions().setEntrypoints(entrypoints);

    for (Entrypoint ep : callGraphBuilder.getOptions().getEntrypoints())
      LOGGER.info(() -> "Using entrypoint: " + ep.getMethod().getDeclaringClass().getName() + ".");
  }

  /**
   * Get a {@link List} of {@link File}s corresponding to the sequence of paths represented by the
   * given {@link String}. The paths are separated by a colon.
   *
   * @param pathSequence A colon-seperated list of paths.
   * @return A {@link List} of {@link File}s constructed from the given path sequence.
   */
  public static List<File> getPathFiles(String pathSequence) {
    if (pathSequence == null || pathSequence.isEmpty() || pathSequence.isBlank())
      return emptyList();
    return Arrays.asList(pathSequence.split(":")).stream().map(File::new).collect(toList());
  }

  public static String removeFileProtocolFromPath(String path) {
    return path.replaceFirst("file:.*!/", "");
  }

  /**
   * Returns a {@link Collection} of annotation (decorator) names as {@link String}s from the given
   * {@link Collection} of {@link CAstAnnotation}s. The decorator names may be dot (.)-separated.
   *
   * @param annotations A {@link Collection} of {@link CAstAnnotation} for which to stream
   *     annotation (decorator) names.
   * @return A {@link Collection} of names as {@link String}s corresponding to the given annotations
   *     (decorators).
   * @implNote The decorator names may not be fully-qualified. They are returned here as they are
   *     presented in the CAst.
   */
  public static Collection<String> getNames(Collection<CAstAnnotation> annotations) {
    if (annotations == null) return emptyList();
    return annotations.stream().map(Util::getName).flatMap(Optional::stream).toList();
  }

  public static Optional<String> getName(CAstAnnotation annotation) {
    if (annotation.getType().equals(CAST_DYNAMIC_ANNOTATION)) {
      CAstNode node = (CAstNode) annotation.getArguments().get(DYNAMIC_ANNOTATION_KEY);
      List<String> decoratorSegments = getDecoratorSegments(node.getChild(0));
      String decoratorName = decoratorSegments.stream().collect(Collectors.joining("."));
      return Optional.of(decoratorName);
    }

    return Optional.empty();
  }

  private static List<String> getDecoratorSegments(CAstNode node) {
    List<String> ret = new ArrayList<>();

    if (node != null) {
      List<CAstNode> children = node.getChildren();

      for (CAstNode child : children) {
        List<String> childDecoratorSegments = getDecoratorSegments(child);
        ret.addAll(childDecoratorSegments);
      }

      Object value = node.getValue();

      if (value != null && value instanceof String) ret.add((String) value);
    }

    return ret;
  }

  /**
   * Returns true iff the given {@link IClass} represents a Python <a
   * href="https://docs.python.org/3/library/functions.html#classmethod">class method</a>.
   *
   * @param method The {@link IClass} in question.
   * @return True iff the given {@link IClass} represents a Python <a
   *     href="https://docs.python.org/3/library/functions.html#classmethod">class method</a>.
   * @apiNote Python methods and functions are represented using {@link IClass}.
   * @implNote This method will log whether the given {@link IClass} is a class method or not.
   */
  public static boolean isClassMethod(IClass method) {
    // If it's a trampoline.
    if (method instanceof PythonInstanceMethodTrampoline)
      // Use the "real class."
      method = ((PythonInstanceMethodTrampoline) method).getRealClass();

    boolean ret = method.getAnnotations().contains(make(CLASS_METHOD));

    LOGGER.fine(
        ret ? "Found class method: " + method : "Method: " + method + " is not a class method.");

    return ret;
  }

  private Util() {}

  /**
   * If the given {@link ConstantKey}'s value is <code>null</code>, then issue a warning and return
   * <code>null</code>. Otherwise, issue a warning stating that an {@link AllocationSiteInNode}
   * cannot be extracted from the given {@link ConstantKey}. A value of <code>
   * null</code> most likely indicates that a receiver can potentially be <code>null</code>.
   *
   * @param constantKey The {@link ConstantKey} from which to extract the corresponding {@link
   *     AllocationSiteInNode}.
   * @return <code>null</code>.
   */
  private static AllocationSiteInNode getAllocationSiteInNode(ConstantKey<?> constantKey) {
    Object value = constantKey.getValue();

    if (value == null)
      LOGGER.warning("Can't extract AllocationSiteInNode from: " + constantKey + ".");
    else
      LOGGER.warning(
          "Can't extract AllocationSiteInNode from: "
              + constantKey
              + ". Not expecting value of: "
              + value
              + " from ConstantKey.");

    return null;
  }

  /**
   * Extracts the {@link AllocationSiteInNode} from the given {@link InstanceKey}. If the given
   * {@link InstanceKey} is an instance of {@link AllocationSiteInNode}, then it itself is returned.
   * If the given {@link InstanceKey} is a {@link ScopeMappingInstanceKey}, then it's base {@link
   * InstanceKey} is returned if it is an instance {@link AllocationSiteInNode}.
   *
   * @param instanceKey The {@link InstanceKey} in question.
   * @return The {@link AllocationSiteInNode} corresponding to the given {@link InstanceKey}
   *     according to the above scheme.
   */
  public static AllocationSiteInNode getAllocationSiteInNode(InstanceKey instanceKey) {
    if (instanceKey instanceof AllocationSiteInNode) return (AllocationSiteInNode) instanceKey;
    else if (instanceKey instanceof ScopeMappingInstanceKey) {
      ScopeMappingInstanceKey smik = (ScopeMappingInstanceKey) instanceKey;
      InstanceKey baseInstanceKey = smik.getBase();

      if (baseInstanceKey instanceof AllocationSiteInNode)
        return (AllocationSiteInNode) baseInstanceKey;
      else if (baseInstanceKey instanceof ConstantKey) {
        return getAllocationSiteInNode((ConstantKey<?>) baseInstanceKey);
      } else
        throw new IllegalArgumentException(
            "Can't extract AllocationSiteInNode from: "
                + baseInstanceKey
                + ". Not expecting: "
                + baseInstanceKey.getClass()
                + ".");
    } else if (instanceKey instanceof ConstantKey) {
      return getAllocationSiteInNode((ConstantKey<?>) instanceKey);
    } else
      throw new IllegalArgumentException(
          "Can't extract AllocationSiteInNode from: "
              + instanceKey
              + ". Not expecting: "
              + instanceKey.getClass()
              + ".");
  }
}
