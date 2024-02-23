package com.ibm.wala.cast.python.ipa.callgraph;

import static com.ibm.wala.cast.python.types.Util.getFilename;
import static java.util.Objects.requireNonNull;

import com.ibm.wala.cast.python.loader.PythonLoader.DynamicMethodBody;
import com.ibm.wala.cast.python.loader.PythonLoader.PythonClass;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.client.AbstractAnalysisEngine.EntrypointBuilder;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.Entrypoint;
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
public class PytestEntrypointBuilder implements EntrypointBuilder {

  private static final Logger logger = Logger.getLogger(PytestEntrypointBuilder.class.getName());

  /**
   * Construct pytest entrypoints for all the pytest test functions in the given scope.
   *
   * @throws NullPointerException If the given {@link IClassHierarchy} is null.
   */
  @Override
  public Iterable<Entrypoint> createEntrypoints(IClassHierarchy cha) {
    requireNonNull(cha);

    final HashSet<Entrypoint> result = HashSetFactory.make();

    for (IClass klass : cha) {
      // if the class is a pytest test case,
      if (isPytestCase(klass)) {
        logger.fine(() -> "Pytest case: " + klass + ".");

        MethodReference methodReference =
            MethodReference.findOrCreate(klass.getReference(), AstMethodReference.fnSelector);

        result.add(new PytesttEntrypoint(methodReference, cha));

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

      // In Ariadne, a script is an invokable entity like a function.
      final boolean script = className.toString().endsWith(".py");

      if (!script // it's not an invokable script.
          && (fileName.startsWith("test_")
              || fileName.endsWith("_test")) // we're inside of a "test" file,
          && !(klass instanceof PythonClass)) { // classes aren't entrypoints.
        if (klass instanceof DynamicMethodBody) {
          // It's a method. In Ariadne, functions are also classes.
          DynamicMethodBody dmb = (DynamicMethodBody) klass;
          IClass container = dmb.getContainer();
          String containerName = container.getReference().getName().getClassName().toString();

          if (containerName.startsWith("Test") && container instanceof PythonClass) {
            // It's a test class.
            PythonClass containerClass = (PythonClass) container;

            final boolean hasCtor =
                containerClass.getMethodReferences().stream()
                    .anyMatch(
                        mr -> {
                          return mr.getName().toString().equals("__init__");
                        });

            // Test classes can't have constructors.
            if (!hasCtor) {
              // In Ariadne, methods are modeled as classes. Thus, a class name in this case is the
              // method name.
              String methodName = className.toString();

              // If the method starts with "test."
              if (methodName.startsWith("test")) return true;
            }
          }
        } else if (className.toString().startsWith("test")) return true; // It's a function.
      }
    }

    return false;
  }
}
