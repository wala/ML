package com.ibm.wala.cast.python.ipa.callgraph;

import static java.util.Objects.requireNonNull;

import com.ibm.wala.cast.python.loader.PythonLoader.DynamicMethodBody;
import com.ibm.wala.cast.python.loader.PythonLoader.PythonClass;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.HashSetFactory;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * This class represents entry points ({@link Entrypoint})s of Pytest test functions. Pytest test
 * functions are those invoked by the pytest framework reflectively. The entry points can be used to
 * specify entry points of a call graph.
 */
public class PytestEntryPoints {

  private static final Logger logger = Logger.getLogger(PytestEntryPoints.class.getName());

  /**
   * Construct pytest entrypoints for all the pytest test functions in the given scope.
   *
   * @throws NullPointerException If the given {@link IClassHierarchy} is null.
   */
  public static Iterable<Entrypoint> make(IClassHierarchy cha) {
    requireNonNull(cha);

    final HashSet<Entrypoint> result = HashSetFactory.make();

    for (IClass klass : cha) {
      // if the class is a pytest test case,
      if (isPytestCase(klass)) {
        logger.fine(() -> "Pytest case: " + klass + ".");

        MethodReference methodReference =
            MethodReference.findOrCreate(klass.getReference(), AstMethodReference.fnSelector);

        result.add(new DefaultEntrypoint(methodReference, cha));
        logger.fine(() -> "Adding test method as entry point: " + methodReference.getName() + ".");
      }
    }
    return result::iterator;
  }

  /**
   * Check if the given class is a Pytest test class according to: https://bit.ly/3wj8nPY.
   *
   * @throws NullPointerException If the given {@link IClass} is null.
   * @see https://bit.ly/3wj8nPY.
   */
  public static boolean isPytestCase(IClass klass) {
    requireNonNull(klass);

    final TypeName typeName = klass.getReference().getName();

    if (typeName.toString().startsWith("Lscript ")) {
      final String fileName = getFilename(typeName);
      final Atom className = typeName.getClassName();

      final boolean script = className.toString().endsWith(".py");

      if (!script) { // A script isn't a pytest entry point according to https://bit.ly/3wj8nPY.
        if (fileName.startsWith("test_") || fileName.endsWith("_test")) {
          // we're inside of a "test" file.
          if (!(klass instanceof PythonClass)) { // classes aren't entrypoints.
            if (klass instanceof DynamicMethodBody) {
              // It's a method.
              DynamicMethodBody dmb = (DynamicMethodBody) klass;
              IClass container = dmb.getContainer();
              String containerName = container.getReference().getName().getClassName().toString();

              if (containerName.startsWith("Test")) {
                if (container instanceof PythonClass) {
                  PythonClass containerClass = (PythonClass) container;

                  final boolean hasCtor =
                      containerClass.getMethodReferences().stream()
                          .anyMatch(
                              mr -> {
                                return mr.getName().toString().equals("__init__");
                              });

                  if (!hasCtor) {
                    String methodName = className.toString();

                    if (methodName.startsWith("test")) return true;
                  }
                }
              }

            } else {
              // It's a function.
              if (className.toString().startsWith("test")) return true;
            }
          }
        }
      }
    }

    return false;
  }

  private static String getFilename(final TypeName typeName) {
    String ret = typeName.toString();
    ret = ret.substring("Lscript ".length());

    if (ret.indexOf('/') != -1) ret = ret.substring(0, ret.indexOf('/'));

    return ret;
  }
}
