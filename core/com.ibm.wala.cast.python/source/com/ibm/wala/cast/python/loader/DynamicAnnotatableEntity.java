package com.ibm.wala.cast.python.loader;

import com.ibm.wala.cast.tree.CAstNode;

public interface DynamicAnnotatableEntity {

  Iterable<CAstNode> dynamicAnnotations();
}
