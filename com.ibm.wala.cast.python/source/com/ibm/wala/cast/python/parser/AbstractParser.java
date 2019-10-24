package com.ibm.wala.cast.python.parser;

import java.util.Collection;

import com.ibm.wala.cast.python.ipa.summaries.BuiltinFunctions;
import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;

public abstract class AbstractParser<T> {

	public static  String[] defaultImportNames = new String[] {
			"__name__",
			"print",
			"super",
			"open",
			"hasattr",
			"BaseException",
			"abs",
			"del",
		};

	protected final CAst Ast = new CAstImpl();

	protected abstract class CAstVisitor {

		protected void defaultImports(Collection<CAstNode> elts) {
			for(String n : BuiltinFunctions.builtins()) {
				elts.add(
				notePosition(
				    Ast.makeNode(CAstNode.DECL_STMT,
						Ast.makeConstant(new CAstSymbolImpl(n, PythonCAstToIRTranslator.Any)),
						Ast.makeNode(CAstNode.NEW, Ast.makeConstant("wala/builtin/" + n))), 
				    CAstSourcePositionMap.NO_INFORMATION));			
			}
			for(String n : defaultImportNames) {
				elts.add(
						notePosition(
					Ast.makeNode(CAstNode.DECL_STMT,
						Ast.makeConstant(new CAstSymbolImpl(n, PythonCAstToIRTranslator.Any)),
						Ast.makeNode(CAstNode.PRIMITIVE, Ast.makeConstant("import"), Ast.makeConstant(n))),
					    CAstSourcePositionMap.NO_INFORMATION));			
			}
		}

		protected abstract CAstNode notePosition(CAstNode makeNode, Position noInformation);
		
	}
	
	public AbstractParser() {
		// TODO Auto-generated constructor stub
	}

}
