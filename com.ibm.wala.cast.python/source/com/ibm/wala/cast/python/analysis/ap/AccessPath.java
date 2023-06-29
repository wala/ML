package com.ibm.wala.cast.python.analysis.ap;

import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.util.debug.Assertions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Utilities for managing access paths.
 *
 * @author sjfink
 */
public class AccessPath {

  public static LocalAP localAP(int vn) {
    return LocalAP.createLocalAP(vn);
  }

  public static LocalAP returnValue() {
    return LocalAP.createLocalAP(LocalAP.RETURN_VALUE_NUMBER);
  }

  public static String getGlobalVarName(SSAFieldAccessInstruction instr) {
    assert instr instanceof AstGlobalRead || instr instanceof AstGlobalWrite;
    String result = instr.getDeclaredField().getName().toString();
    assert result.startsWith("global ");
    return result.substring(7);
  }

  public static GlobalVarAP globalVarAP(String varName) {
    return GlobalVarAP.createGlobalVarAP(varName);
  }

  public static LexicalAP lexicalVarAP(String varName, String definer) {
    return LexicalAP.createLexicalAP(varName, definer);
  }

  public static IAccessPath star(IAccessPath ap) {
    return append(ap, Collections.<IPathElement>singletonList(StarPathElement.singleton()));
  }

  public static IAccessPath appendUnknown(LocalAP ap) {
    return ListAP.createListAP(
        ap, Collections.<IPathElement>singletonList(UnknownPathElement.singleton()));
  }

  public static boolean isRootedAtLocal(IAccessPath ap) {
    switch (ap.getKind()) {
      case LOCAL:
        return true;
      case LEXICAL:
        return false;
      case CALLBACK:
        return false;
      case LIST:
        ListAP l = (ListAP) ap;
        return l.getRoot() instanceof LocalAP;
      case GLOBAL:
        return false;
      default:
        Assertions.UNREACHABLE();
        return false;
    }
  }

  public static LocalAP getLocalRoot(IAccessPath ap) {
    assert isRootedAtLocal(ap);
    switch (ap.getKind()) {
      case LOCAL:
        return (LocalAP) ap;
      case LIST:
        ListAP l = (ListAP) ap;
        return (LocalAP) l.getRoot();
      default:
        Assertions.UNREACHABLE();
        return null;
    }
  }

  public static boolean isRootedAtReturnValue(IAccessPath ap) {
    return isRootedAtLocal(LocalAP.RETURN_VALUE_NUMBER, ap);
  }

  public static boolean isRootedAtLocal(int vn, IAccessPath ap) {
    switch (ap.getKind()) {
      case LOCAL:
        LocalAP local = (LocalAP) ap;
        return local.getValueNumber() == vn;
      case LIST:
        ListAP l = (ListAP) ap;
        return isRootedAtLocal(vn, l.getRoot());
      case GLOBAL:
        return false;
      case LEXICAL:
        return false;
      case CALLBACK:
        return false;
      default:
        Assertions.UNREACHABLE();
        return false;
    }
  }

  public static boolean isRootedAtSomeGlobal(IAccessPath ap) {
    switch (ap.getKind()) {
      case LOCAL:
        return false;
      case LEXICAL:
        return false;
      case LIST:
        ListAP l = (ListAP) ap;
        return isRootedAtSomeGlobal(l.getRoot());
      case GLOBAL:
        return true;
      case CALLBACK:
        return false;
      default:
        Assertions.UNREACHABLE();
        return false;
    }
  }

  public static boolean isRootedAtSomeLexicalVar(IAccessPath ap) {
    switch (ap.getKind()) {
      case LOCAL:
        return false;
      case LEXICAL:
        return true;
      case LIST:
        ListAP l = (ListAP) ap;
        return isRootedAtSomeLexicalVar(l.getRoot());
      case GLOBAL:
        return false;
      case CALLBACK:
        return false;
      default:
        Assertions.UNREACHABLE();
        return false;
    }
  }

  public static boolean isRootedAtGlobal(String varName, IAccessPath ap) {
    switch (ap.getKind()) {
      case LOCAL:
        return false;
      case LEXICAL:
        return false;
      case CALLBACK:
        return false;
      case LIST:
        ListAP l = (ListAP) ap;
        return isRootedAtGlobal(varName, l.getRoot());
      case GLOBAL:
        GlobalVarAP s = (GlobalVarAP) ap;
        return s.getVarName().equals(varName);
      default:
        Assertions.UNREACHABLE();
        return false;
    }
  }

  public static boolean isRootedAtLexical(LexicalAP lap, IAccessPath ap) {
    switch (ap.getKind()) {
      case LOCAL:
        return false;
      case CALLBACK:
        return false;
      case LEXICAL:
        return lap.equals(ap);
      case LIST:
        ListAP l = (ListAP) ap;
        return isRootedAtLexical(lap, l.getRoot());
      case GLOBAL:
        return false;
      default:
        Assertions.UNREACHABLE();
        return false;
    }
  }

  public static IAccessPath arrayAccess(int arrayValueNumber) {
    return append(
        localAP(arrayValueNumber),
        Collections.<IPathElement>singletonList(ArrayContents.singleton()));
  }

  public static IAccessPath fieldAccess(int refValueNumber, String field) {
    return append(
        localAP(refValueNumber),
        Collections.<IPathElement>singletonList(PropertyPathElement.createFieldPathElement(field)));
  }

  public static IAccessPath methodAccess(int refValueNumber, String method) {
    return append(
        localAP(refValueNumber),
        Collections.<IPathElement>singletonList(
            PropertyPathElement.createMethodPathElement(method, null)));
  }

  public static IAccessPath append(IAccessPath prefix, List<? extends IPathElement> path) {
    if (path.isEmpty()) {
      return prefix;
    }
    switch (prefix.getKind()) {
      case CALLBACK:
      case LOCAL:
      case GLOBAL:
      case LEXICAL:
        return ListAP.createListAP((IAPRoot) prefix, path);
      case LIST:
        ListAP ap = (ListAP) prefix;
        List<IPathElement> list = new ArrayList<IPathElement>(ap.getPath());
        // don't ever append something after *
        assert !(list.size() >= 1 && list.get(list.size() - 1) instanceof StarPathElement);
        list.addAll(path);
        return append(ap.getRoot(), list);
      default:
        Assertions.UNREACHABLE();
        return null;
    }
  }

  /** Does ap begin with prefix? */
  public static boolean hasPrefix(IAccessPath ap, IAccessPath prefix) {
    switch (prefix.getKind()) {
      case LOCAL:
        {
          LocalAP lap = (LocalAP) prefix;
          return isRootedAtLocal(lap.getValueNumber(), ap);
        }
      case GLOBAL:
        {
          GlobalVarAP sap = (GlobalVarAP) prefix;
          return isRootedAtGlobal(sap.getVarName(), ap);
        }
      case LEXICAL:
        {
          LexicalAP lap = (LexicalAP) prefix;
          return isRootedAtLexical(lap, ap);
        }
      case LIST:
        {
          ListAP list = (ListAP) prefix;
          if (ap instanceof ListAP) {
            ListAP lap = (ListAP) ap;
            if (lap.getRoot().equals(list.getRoot())) {
              Iterator<IPathElement> prefixIt = list.getPath().iterator();
              Iterator<IPathElement> aIt = lap.getPath().iterator();
              while (prefixIt.hasNext()) {
                if (!aIt.hasNext()) {
                  return false;
                }
                IPathElement nextPE = prefixIt.next();
                if (!nextPE.matches(aIt.next())) {
                  return false;
                }
              }
              return true;
            }
          }
          return false;
        }
      default:
        Assertions.UNREACHABLE();
        return false;
    }
  }

  public static List<IPathElement> suffix(IAccessPath ap) {
    switch (ap.getKind()) {
      case CALLBACK:
        {
          CallbackAP cap = (CallbackAP) ap;
          if (cap.getTaintedPath() != null) {
            return cap.getTaintedPath();
          } else {
            return Collections.emptyList();
          }
        }

      case LOCAL:
      case GLOBAL:
      case LEXICAL:
        return Collections.emptyList();

      case LIST:
        {
          ListAP list = (ListAP) ap;
          return list.getPath();
        }
      default:
        Assertions.UNREACHABLE();
        return null;
    }
  }
}
