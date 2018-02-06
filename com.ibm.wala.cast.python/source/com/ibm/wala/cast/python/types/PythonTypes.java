package com.ibm.wala.cast.python.types;

import com.ibm.wala.cast.types.AstTypeReference;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

public class PythonTypes extends AstTypeReference {

	  public static final String pythonNameStr = "Python";

	  public static final String pythonLoaderNameStr = "PythonLoader";

	  public static final Atom pythonName = Atom.findOrCreateUnicodeAtom(pythonNameStr);

	  public static final Atom pythonLoaderName = Atom.findOrCreateUnicodeAtom(pythonLoaderNameStr);

	  public static final ClassLoaderReference pythonLoader = new ClassLoaderReference(pythonLoaderName, pythonName, null);

	  public static final TypeReference Root = TypeReference.findOrCreate(pythonLoader, rootTypeName);

	  public static final TypeReference Exception = TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("LException"));

	  public static final TypeReference CodeBody = TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("LCodeBody"));

	  public static final TypeReference object = TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Lobject"));

	  public static final TypeReference list = TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Llist"));

}
