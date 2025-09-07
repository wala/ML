/******************************************************************************
 * Copyright (c) 2018 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.python.types;

import static com.ibm.wala.cast.python.util.Util.CLASS_METHOD_ANNOTATION_NAME;
import static com.ibm.wala.cast.python.util.Util.STATIC_METHOD_ANNOTATION_NAME;

import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.types.AstTypeReference;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;
import java.util.HashSet;

public class PythonTypes extends AstTypeReference {

  public static final String pythonNameStr = "Python";

  public static final String pythonLoaderNameStr = "PythonLoader";

  /** The name of the type used for CAst dynamic annotations (decorators). */
  private static final String DYNAMIC_ANNOTATION_TYPE_NAME = "DYNAMIC_ANNOTATION";

  public static final Atom pythonName = Atom.findOrCreateUnicodeAtom(pythonNameStr);

  public static final Atom pythonLoaderName = Atom.findOrCreateUnicodeAtom(pythonLoaderNameStr);

  public static final ClassLoaderReference pythonLoader =
      new ClassLoaderReference(pythonLoaderName, pythonName, null);

  public static final TypeReference Root = TypeReference.findOrCreate(pythonLoader, rootTypeName);

  public static final TypeReference BaseException =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("LBaseException"));

  public static final TypeReference Exception =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("LException"));

  public static final TypeReference CodeBody =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("LCodeBody"));

  public static final TypeReference Coroutine =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Lcoroutine"));

  public static final TypeReference AsyncCodeBody =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("LAsyncCodeBody"));

  public static final TypeReference MethodBody =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("LMethodBody"));

  public static final TypeReference AsyncMethodBody =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("LAsyncMethodBody"));

  public static final TypeReference lambda =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Llambda"));

  public static final TypeReference LambdaMethod =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("LlambdaMethod"));

  public static final TypeReference filter =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Lfilter"));

  public static final TypeReference comprehension =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Lcomprehension"));

  public static final TypeReference object =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Lobject"));

  public static final TypeReference list =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Llist"));

  public static final TypeReference set =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Lset"));

  public static final TypeReference dict =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Ldict"));

  public static final TypeReference string =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Lstring"));

  public static final TypeReference tuple =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Ltuple"));

  public static final TypeReference enumerate =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Lenumerate"));

  public static final TypeReference trampoline =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Ltrampoline"));

  public static final TypeReference superfun =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Lsuperfun"));

  public static final TypeReference coroutine =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Lcoroutine"));

  public static final TypeReference sequence =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Lsequence"));

  /** https://docs.python.org/3/library/stdtypes.html#typeiter. */
  public static final TypeReference iterator =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Literator"));

  /** https://docs.python.org/3/library/functions.html#staticmethod. */
  public static final TypeReference STATIC_METHOD =
      TypeReference.findOrCreate(
          pythonLoader, TypeName.findOrCreate("L" + STATIC_METHOD_ANNOTATION_NAME));

  /** https://docs.python.org/3/library/functions.html#classmethod. */
  public static final TypeReference CLASS_METHOD =
      TypeReference.findOrCreate(
          pythonLoader, TypeName.findOrCreate("L" + CLASS_METHOD_ANNOTATION_NAME));

  /** A {@link CAstType} representing a dynamic annotation (decorator). */
  public static final CAstType CAST_DYNAMIC_ANNOTATION =
      new CAstType() {
        @Override
        public String getName() {
          return DYNAMIC_ANNOTATION_TYPE_NAME;
        }

        @Override
        public Collection<CAstType> getSupertypes() {
          return new HashSet<>();
        }
      };
}
