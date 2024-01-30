package com.ibm.wala.cast.python.ml.types;

import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

/**
 * Types found in the TensorFlow library.
 *
 * @author <a href="mailto:khatchd@hunter.cuny.edu">Raffi Khatchadourian</a>
 */
public class TensorFlowTypes extends PythonTypes {

  public static final TypeReference DATASET =
      TypeReference.findOrCreate(pythonLoader, TypeName.findOrCreate("Ltensorflow/data/Dataset"));

  private TensorFlowTypes() {}
}
