package com.ibm.wala.cast.python.parser;

import com.ibm.wala.cast.python.ipa.summaries.BuiltinFunctions;
import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import java.util.Collection;

public abstract class AbstractParser {

  public interface MissingType extends CAstType {}

  public interface PythonGlobalsEntity {
    java.util.Set<String> downwardGlobals();
  }

  public static String[] defaultImportNames =
      new String[] {
        "BaseException",
        "DeprecationWarning",
        "Exception",
        "FutureWarning",
        "NameError",
        "None",
        "RuntimeError",
        "StopIteration",
        "TypeError",
        "UserWarning",
        "ValueError",
        "__doc__",
        "__file__",
        "__name__",
        "abs",
        "all",
        "any",
        "bin",
        "bool",
        "bytes",
        "callable",
        "chr",
        "complex",
        "del",
        "dict",
        "dir",
        "divmod",
        "eval",
        "exec",
        "exit",
        "filter",
        "float",
        "format",
        "frozenset",
        "get_ipython",
        "getattr",
        "globals",
        "hasattr",
        "help",
        "hex",
        "id",
        "input",
        "isinstance",
        "locals",
        "map",
        "max",
        "min",
        "object",
        "open",
        "ord",
        "pow",
        "print",
        "property",
        "repr",
        "reversed",
        "set",
        "super",
        "tuple",
        "vars",

        // names found in recent IPython
        "NotImplementedError",
        "Warning",
        "cd",
        "clear",
        "pylab",
        "RuntimeWarning",
        "hist",
        "matplotlib",
        "recall",
        "history",
        "time",
        "KeyError",
        "display"
      };

  protected abstract static class CAstVisitor {

    protected final CAst Ast = new CAstImpl();

    protected void defaultImports(Collection<CAstNode> elts) {
      for (String n : BuiltinFunctions.builtins()) {
        elts.add(
            notePosition(
                Ast.makeNode(
                    CAstNode.DECL_STMT,
                    Ast.makeConstant(new CAstSymbolImpl(n, PythonCAstToIRTranslator.Any)),
                    Ast.makeNode(CAstNode.NEW, Ast.makeConstant("wala/builtin/" + n))),
                CAstSourcePositionMap.NO_INFORMATION));
      }
      for (String n : defaultImportNames) {
        elts.add(
            notePosition(
                Ast.makeNode(
                    CAstNode.DECL_STMT,
                    Ast.makeConstant(new CAstSymbolImpl(n, PythonCAstToIRTranslator.Any)),
                    Ast.makeNode(
                        CAstNode.PRIMITIVE, Ast.makeConstant("import"), Ast.makeConstant(n))),
                CAstSourcePositionMap.NO_INFORMATION));
      }
    }

    protected abstract CAstNode notePosition(CAstNode makeNode, Position noInformation);
  }

  public AbstractParser() {
    // TODO Auto-generated constructor stub
  }
}
