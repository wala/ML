package com.ibm.wala.cast.python.types;

import static com.ibm.wala.cast.python.util.Util.PYTHON_FILE_EXTENSION;

import com.ibm.wala.cast.types.AstTypeReference;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import java.util.Arrays;

public class Util {

  private static final String GLOBAL_KEYWORD = "global";

  /**
   * Returns the relative filename portion of the given {@link TypeName} representing a Python type.
   *
   * @param typeName A {@link TypeName} of a Python type.
   * @return The relative filename portion of the given {@link TypeName}.
   * @apiNote Python types include a file in their {@link TypeName}s in Ariadne.
   */
  public static String getRelativeFilename(final TypeName typeName) {
    String typeNameString = typeName.toString();

    // Remove the script prefix.
    typeNameString = typeNameString.substring("Lscript ".length());

    // Extract the filename.
    String[] segments = typeNameString.split("/");

    String filename =
        Arrays.stream(segments)
            .filter(s -> s.endsWith("." + PYTHON_FILE_EXTENSION))
            .findFirst()
            .orElseThrow();

    assert filename.endsWith("." + PYTHON_FILE_EXTENSION)
        : "Python files must have a \"" + PYTHON_FILE_EXTENSION + "\" extension.";

    return filename;
  }

  /**
   * creates a reference to a global named globalName. the declaring type and type of the global are
   * both the root type.
   */
  public static FieldReference makeGlobalRef(IClassLoader loader, String globalName) {
    TypeReference rootTypeRef =
        TypeReference.findOrCreate(loader.getReference(), AstTypeReference.rootTypeName);
    return FieldReference.findOrCreate(
        rootTypeRef, Atom.findOrCreateUnicodeAtom(GLOBAL_KEYWORD + " " + globalName), rootTypeRef);
  }

  /**
   * Returns the {@link TypeReference} of the given {@link TypeReference}'s declaring class.
   *
   * @param reference The {@link TypeReference} for which to extract the {@link TypeReference} of
   *     the declaring class.
   * @return The {@link TypeReference} of the given {@link TypeReference}'s declaring class.
   */
  public static TypeReference getDeclaringClassTypeReference(TypeReference reference) {
    TypeName name = reference.getName();
    Atom packageName = name.getPackage();
    name = TypeName.findOrCreate("L" + packageName.toString());
    return TypeReference.findOrCreate(reference.getClassLoader(), name);
  }

  /**
   * Returns the global name of the given {@link MethodReference}'s declaring class.
   *
   * @param methodReference The {@link MethodReference} for which to extract the global script name.
   * @return The global name of the given {@link MethodReference}'s declaring class.
   */
  public static String getGlobalName(MethodReference methodReference) {
    return getGlobalName(methodReference.getDeclaringClass());
  }

  /**
   * Returns the global name of the given {@link TypeReference}.
   *
   * @param typeReference The {@link TypeReference} for which to extract the global script name.
   * @return The global name of the given {@link TypeReference}.
   */
  public static String getGlobalName(TypeReference typeReference) {
    return typeReference.getName().getPackage().toString();
  }

  private Util() {}
}
