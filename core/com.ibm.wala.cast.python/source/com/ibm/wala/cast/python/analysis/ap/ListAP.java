package com.ibm.wala.cast.python.analysis.ap;

import java.util.Collections;
import java.util.List;

/**
 * Access path of the form x.f.g.h ...
 *
 * <p>Use with care.
 */
public class ListAP implements IAccessPath {
  public static ListAP createListAP(IAPRoot root, List<? extends IPathElement> path) {
    return new ListAP(root, path);
  }

  private final IAPRoot root;

  private final List<? extends IPathElement> path;

  private ListAP(IAPRoot root, List<? extends IPathElement> path) {
    super();
    this.path = path;
    this.root = root;
  }

  public IAPRoot getRoot() {
    return root;
  }

  public List<IPathElement> getPath() {
    return Collections.unmodifiableList(path);
  }

  public Kind getKind() {
    return Kind.LIST;
  }

  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((path == null) ? 0 : path.hashCode());
    result = prime * result + ((root == null) ? 0 : root.hashCode());
    return result;
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ListAP other = (ListAP) obj;
    if (path == null) {
      if (other.path != null) return false;
    } else if (!path.equals(other.path)) return false;
    if (root == null) {
      if (other.root != null) return false;
    } else if (!root.equals(other.root)) return false;
    return true;
  }

  public int length() {
    return path.size() + 1;
  }

  public String toString() {
    StringBuffer result = new StringBuffer(root.toString());
    for (IPathElement e : path) {
      result.append(".");
      result.append(e);
    }
    return result.toString();
  }
}
