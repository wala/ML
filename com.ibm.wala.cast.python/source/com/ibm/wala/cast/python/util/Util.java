package com.ibm.wala.cast.python.util;

import static com.google.common.collect.Iterables.concat;
import static com.ibm.wala.cast.python.types.PythonTypes.CAST_DYNAMIC_ANNOTATION;

import com.ibm.wala.cast.python.ipa.callgraph.PytestEntrypointBuilder;
import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Util {

  private static final Logger LOGGER = Logger.getLogger(Util.class.getName());

  /** Key used to map annotations (decorators) to names. */
  public static final String DYNAMIC_ANNOTATION_KEY = "dynamicAnnotation";

  /** Name of the annotation (decorator) that marks methods as static. */
  public static final String STATIC_METHOD_ANNOTATION_NAME = "staticmethod";

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
   * Returns a {@link Stream} of annotation (decorator) names as {@link String}s from the given
   * {@link Collection} of {@link CAstAnnotation}s.
   *
   * @param annotations A {@link Collection} of {@link CAstAnnotation} for which to stream
   *     annotation (decorator) names.
   * @return A {@link Stream} of names as {@link String}s corresponding to the given annotations
   *     (decorators).
   */
  public static Stream<String> getNameStream(Collection<CAstAnnotation> annotations) {
    return annotations.stream()
        .filter(a -> a.getType().equals(CAST_DYNAMIC_ANNOTATION))
        .map(a -> a.getArguments().get(DYNAMIC_ANNOTATION_KEY))
        .filter(Objects::nonNull)
        .map(CAstNode.class::cast)
        .map(n -> n.getChild(0))
        .map(n -> n.getChild(0))
        .map(CAstNode::getValue)
        .filter(v -> v instanceof String)
        .map(String.class::cast);
  }

  private Util() {}
}
