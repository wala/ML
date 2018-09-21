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
package com.ibm.wala.cast.python.parser;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

import org.python.antlr.PythonTree;
import org.python.antlr.ast.Assert;
import org.python.antlr.ast.Assign;
import org.python.antlr.ast.Attribute;
import org.python.antlr.ast.AugAssign;
import org.python.antlr.ast.BinOp;
import org.python.antlr.ast.BoolOp;
import org.python.antlr.ast.Break;
import org.python.antlr.ast.Call;
import org.python.antlr.ast.ClassDef;
import org.python.antlr.ast.Compare;
import org.python.antlr.ast.Continue;
import org.python.antlr.ast.Delete;
import org.python.antlr.ast.Dict;
import org.python.antlr.ast.DictComp;
import org.python.antlr.ast.Ellipsis;
import org.python.antlr.ast.ExceptHandler;
import org.python.antlr.ast.Exec;
import org.python.antlr.ast.Expr;
import org.python.antlr.ast.Expression;
import org.python.antlr.ast.ExtSlice;
import org.python.antlr.ast.For;
import org.python.antlr.ast.FunctionDef;
import org.python.antlr.ast.GeneratorExp;
import org.python.antlr.ast.Global;
import org.python.antlr.ast.If;
import org.python.antlr.ast.IfExp;
import org.python.antlr.ast.Import;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Index;
import org.python.antlr.ast.Interactive;
import org.python.antlr.ast.Lambda;
import org.python.antlr.ast.List;
import org.python.antlr.ast.ListComp;
import org.python.antlr.ast.Module;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Num;
import org.python.antlr.ast.Pass;
import org.python.antlr.ast.Print;
import org.python.antlr.ast.Raise;
import org.python.antlr.ast.Repr;
import org.python.antlr.ast.Return;
import org.python.antlr.ast.Set;
import org.python.antlr.ast.SetComp;
import org.python.antlr.ast.Slice;
import org.python.antlr.ast.Str;
import org.python.antlr.ast.Subscript;
import org.python.antlr.ast.Suite;
import org.python.antlr.ast.TryExcept;
import org.python.antlr.ast.TryFinally;
import org.python.antlr.ast.Tuple;
import org.python.antlr.ast.UnaryOp;
import org.python.antlr.ast.VisitorIF;
import org.python.antlr.ast.While;
import org.python.antlr.ast.With;
import org.python.antlr.ast.Yield;
import org.python.antlr.ast.alias;
import org.python.antlr.ast.arguments;
import org.python.antlr.ast.cmpopType;
import org.python.antlr.ast.comprehension;
import org.python.antlr.ast.keyword;
import org.python.antlr.ast.operatorType;
import org.python.antlr.ast.unaryopType;
import org.python.antlr.base.expr;
import org.python.antlr.base.slice;
import org.python.antlr.base.stmt;
import org.python.core.PyObject;

import com.ibm.wala.cast.ir.translator.AbstractClassEntity;
import com.ibm.wala.cast.ir.translator.AbstractCodeEntity;
import com.ibm.wala.cast.ir.translator.AbstractFieldEntity;
import com.ibm.wala.cast.ir.translator.AbstractScriptEntity;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;
import com.ibm.wala.cast.tree.impl.CAstControlFlowRecorder;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstNodeTypeMapRecorder;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstSourcePositionRecorder;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.CopyKey;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.RewriteContext;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.ReverseIterator;
import com.ibm.wala.util.warnings.Warning;
import org.python.core.PyString;

abstract public class PythonParser<T> implements TranslatorToCAst {

	interface WalkContext extends TranslatorToCAst.WalkContext<WalkContext, PythonTree> {
 
		WalkContext getParent();
		
		default CAstEntity entity() {
			return getParent().entity();
		}
		
	}
	
	private static class RootContext extends TranslatorToCAst.RootContext<WalkContext, PythonTree> implements WalkContext {
		private final Module ast;
		
		private RootContext(Module ast) { 
			this.ast = ast;
		}
		
		@Override
		public PythonTree top() {
			return ast;
		}

		public WalkContext getParent() {
			assert false;
			return null;
		}
	}
	
	private static class FunctionContext extends TranslatorToCAst.FunctionContext<WalkContext, PythonTree> implements WalkContext {
		private final AbstractCodeEntity fun;
		
		public WalkContext getParent() {
			return parent;
		}
		
		private FunctionContext(WalkContext parent, AbstractCodeEntity fun, PythonTree s) {
			super(parent, s);
			this.fun = fun;
		}

		@Override
		public CAstEntity entity() {
			return fun;
		}

		@Override
		public CAstNodeTypeMapRecorder getNodeTypeMap() {
			return fun.getNodeTypeMap();
		}

		@Override
		public CAstSourcePositionRecorder pos() {
			return fun.getSourceMap();
		}

		@Override
		public CAstControlFlowRecorder cfg() {
			return fun.getControlFlow();
		}

		@Override
		public void addScopedEntity(CAstNode construct, CAstEntity e) {
			fun.addScopedEntity(construct, e);
		}

		@Override
		public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
			return fun.getAllScopedEntities();
		}
	}

	private final CAst Ast = new CAstImpl();
	
	private class CAstVisitor implements VisitorIF<CAstNode>  {
		private final PythonParser.WalkContext context;
		private final WalaPythonParser parser;
		
		private CAstNode fail(PyObject tree) {
// pretend it is a no-op for now.
//			assert false : tree;
			return Ast.makeNode(CAstNode.EMPTY);
		}
		
		private CAstVisitor(PythonParser.WalkContext context, WalaPythonParser parser) {
			this.context = context;
			this.parser = parser;
		}
		
		private Position makePosition(PythonTree p) {
			return new AbstractSourcePosition() {

				@Override
				public URL getURL() {
					try {
						return getParsedURL();
					} catch (IOException e) {
						assert false : e;
						return null;
					}
				}

				@Override
				public Reader getReader() throws IOException {
					return PythonParser.this.getReader();
				}

				@Override
				public int getFirstLine() {
					return p.getLine();
				}

				@Override
				public int getFirstCol() {
					return p.getCharPositionInLine();
				}

				boolean set_last = false;
				int last_line;
				int last_col;
				
				private void setLast() {
					String s = parser.getText(p.getCharStartIndex(), p.getCharStopIndex());
					String[] lines = s.split("\n");
					last_line = getFirstLine() + lines.length - 1;
					if ("".equals(s) || lines.length <= 1) {
						last_col = getFirstCol() + (getLastOffset() - getFirstOffset());
					} else {
						assert (lines.length > 1);
						last_col = lines[lines.length-1].length();
					} 
					set_last = true;
				}
				
				@Override
				public int getLastLine() {
					if (! set_last) setLast();
					return last_line;
				}

				@Override
				public int getLastCol() {
					if (! set_last) setLast();
					return last_col;
				}

				@Override
				public int getFirstOffset() {
					return p.getCharStartIndex();
				}

				@Override
				public int getLastOffset() {
					return p.getCharStopIndex();
				}
				
			};
		}
		
		private CAstNode notePosition(CAstNode n, PythonTree p) {
			Position pos = makePosition(p);
			pushSourcePosition(context, n, pos);
			return n;
		}
		
		@Override
		public CAstNode visitAssert(Assert arg0) throws Exception {
			return Ast.makeNode(CAstNode.EMPTY);
			//return notePosition(Ast.makeNode(CAstNode.ASSERT, arg0.getInternalTest().accept(this)), arg0);
		}

		@Override
		public CAstNode visitAssign(Assign arg0) throws Exception {
			CAstNode v = notePosition(arg0.getInternalValue().accept(this), arg0.getInternalValue());
			if (context.entity().getKind() == CAstEntity.TYPE_ENTITY) {
				for (expr lhs : arg0.getInternalTargets()) {
					context.addScopedEntity(null, new AbstractFieldEntity(lhs.getText(), Collections.emptySet(), false, context.entity()) {
						@Override
						public CAstNode getAST() {
							return v;
						}

						@Override
						public Position getPosition(int arg) {
							return null;
						}
					});
				}
				return Ast.makeNode(CAstNode.EMPTY);
			} else {
				java.util.List<CAstNode> nodes = new ArrayList<CAstNode>(); 
				for (expr lhs : arg0.getInternalTargets()) {
					nodes.add(notePosition(Ast.makeNode(CAstNode.ASSIGN, notePosition(lhs.accept(this), lhs), v), lhs));
				}
				return Ast.makeNode(CAstNode.BLOCK_EXPR, nodes.toArray(new CAstNode[nodes.size()]));
			}
		}
		@Override
		public CAstNode visitAttribute(Attribute arg0) throws Exception {
			return notePosition(Ast.makeNode(CAstNode.OBJECT_REF, 
					notePosition(arg0.getInternalValue().accept(this), arg0.getInternalValue()),
					notePosition(Ast.makeConstant(arg0.getInternalAttr()), arg0.getInternalAttrName())), arg0);
		}

		@Override
		public CAstNode visitAugAssign(AugAssign arg0) throws Exception {
			return notePosition(Ast.makeNode(CAstNode.ASSIGN_POST_OP, 
					arg0.getInternalTarget().accept(this),
					arg0.getInternalValue().accept(this),
					translateOperator(arg0.getInternalOp())), arg0);
		}

		@Override
		public CAstNode visitBinOp(BinOp arg0) throws Exception {
			CAstNode l = notePosition(arg0.getInternalLeft().accept(this), arg0.getInternalLeft());
			CAstNode r = notePosition(arg0.getInternalRight().accept(this), arg0.getInternalRight());
			CAstOperator op = translateOperator(arg0.getInternalOp());
			return notePosition(Ast.makeNode(CAstNode.BINARY_EXPR, op, l, r), arg0);
		}

		private CAstOperator translateOperator(operatorType internalOp) {
			switch (internalOp) {
			case Add:
				return CAstOperator.OP_ADD;
			case BitAnd:
				return CAstOperator.OP_BIT_AND;
			case BitOr:
				return CAstOperator.OP_BIT_OR;
			case BitXor:
				return CAstOperator.OP_BIT_XOR;
			case Div:
				return CAstOperator.OP_DIV;
			case FloorDiv:
				return CAstOperator.OP_DIV; // FIXME: need 'quotient'
			case LShift:
				return CAstOperator.OP_LSH;
			case Mod:
				return CAstOperator.OP_MOD;
			case Mult:
				return CAstOperator.OP_MUL;
			case Pow:
				return CAstOperator.OP_POW;
			case RShift:
				return CAstOperator.OP_RSH;
			case Sub:
				return CAstOperator.OP_SUB;
			case UNDEFINED:
			default:
				assert false : internalOp;
				return null;
			}
		}

		@Override
		public CAstNode visitBoolOp(BoolOp arg0) throws Exception {
			Iterator<expr> vs = ReverseIterator.reverse(arg0.getInternalValues().iterator());
			CAstNode v = vs.next().accept(this);
			switch (arg0.getInternalOp()) {
			case And:
				while (vs.hasNext()) {
					CAstNode n = vs.next().accept(this);
					v = notePosition(Ast.makeNode(CAstNode.IF_EXPR, n, v, Ast.makeConstant(false)), arg0);
				}
				return v;
			case Or:
				while (vs.hasNext()) {
					CAstNode n = vs.next().accept(this);
					v = notePosition(Ast.makeNode(CAstNode.IF_EXPR, 
							Ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, n), 
							v, 
							Ast.makeConstant(false)), arg0);
				}
				return v;
			case UNDEFINED:
			default:
				assert false;
				return null;
			}
		}

		@Override
		public CAstNode visitBreak(Break arg0) throws Exception {
			PythonTree target = context.getBreakFor(null);
			CAstNode gt = notePosition(Ast.makeNode(CAstNode.GOTO), arg0);
			
			context.cfg().map(arg0, gt);
			context.cfg().add(arg0, target, null);

			return gt;
		}

		@Override
		public CAstNode visitCall(Call arg0) throws Exception {
			int i = 0;
			CAstNode args[] = new CAstNode[ arg0.getInternalArgs().size() +  arg0.getInternalKeywords().size() + 1];
			args[i++] = Ast.makeNode(CAstNode.EMPTY);
			for(expr e : arg0.getInternalArgs()) {
				args[i++] = notePosition(e.accept(this), e);
			}
			for(keyword k : arg0.getInternalKeywords()) {
				args[i++] = 
					notePosition(
						Ast.makeNode(CAstNode.ARRAY_LITERAL, 
							Ast.makeConstant(k.getInternalArg()), 
							notePosition(k.getInternalValue().accept(this), k.getInternalValue())),
						k);
			}
			
			CAstNode f = notePosition(arg0.getInternalFunc().accept(this), arg0.getInternalFunc());
			
			return notePosition(Ast.makeNode(CAstNode.CALL, f, args), arg0);
		}

		@Override
		public CAstNode visitClassDef(ClassDef arg0) throws Exception {
			CAstVisitor visitor = this;
			WalkContext parent = this.context;
			
			CAstType.Class cls = new CAstType.Class() {				
				@Override
				public Collection<CAstType> getSupertypes() {
					Collection<CAstType> supertypes = HashSetFactory.make();
					for(expr e : arg0.getInternalBases()) {
						System.out.println(arg0.getInternalName() + " " + arg0.getType()+ " extends "  + e.getText() + " " + e.getType());
						try {
							CAstType type = types.getCAstTypeFor(e.getText());
							if (type != null) {
								supertypes.add(type);
							}
						} catch (Exception e1) {
							assert false : e1;
						}
					}
					return supertypes;
				}
				
				@Override
				public String getName() {
					return arg0.getInternalName();
				}
				
				@Override
				public boolean isInterface() {
					return false;
				}
				
				@Override
				public Collection<CAstQualifier> getQualifiers() {
					return Collections.emptySet();
				}
			};
			//TODO: CURRENTLY THIS WILL NOT BE CORRECT FOR EXTENDING CLASSES IMPORTED FROM ANOTHER MODULE
			types.map(arg0.getInternalName(), cls);
			
			Collection<CAstEntity> members = HashSetFactory.make();
			
			CAstEntity clse = new AbstractClassEntity(cls) {
				
				@Override
				public int getKind() {
					return CAstEntity.TYPE_ENTITY;
				}

				@Override
				public String getName() {
					return cls.getName();
				}

				@Override
				public CAstType getType() {
					return cls;
				}

				@Override
				public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
					return Collections.singletonMap(null, members);
				}

				@Override
				public Position getPosition(int arg) {
					return null;
				}

				@Override
				public Position getPosition() {
					return makePosition(arg0);
				}
				
			};

			WalkContext child = new WalkContext() {				
				private final CAstSourcePositionRecorder pos = new CAstSourcePositionRecorder();

				@Override
				public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
					return Collections.singletonMap(null, members);
				}

				@Override
				public PythonTree top() {
					return arg0;
				}

				@Override
				public void addScopedEntity(CAstNode newNode, CAstEntity visit) {
					members.add(visit);
				}

				@Override
				public CAstControlFlowRecorder cfg() {
					assert false;
					return null;
				}

				@Override
				public CAstSourcePositionRecorder pos() {
					return pos;
				}

				@Override
				public CAstNodeTypeMapRecorder getNodeTypeMap() {
					assert false;
					return null;
				}

				@Override
				public PythonTree getContinueFor(String label) {
					assert false;
					return null;
				}

				@Override
				public PythonTree getBreakFor(String label) {
					assert false;
					return null;
				}

				@Override
				public CAstEntity entity() {
					return clse;
				}

				@Override
				public WalkContext getParent() {
					return parent;
				}				
			};			

			CAstVisitor v = new CAstVisitor(child, parser);
			for(stmt e : arg0.getInternalBody()) {
				e.accept(v);				
			}

			CAstNode x= Ast.makeNode(CAstNode.EMPTY);
			context.addScopedEntity(x, clse);
			return x;
		}

		private CAstNode compare(CAstNode lhs, Iterator<cmpopType> ops, Iterator<expr> rhss) throws Exception {
			if (ops.hasNext()) {
				CAstNode rhs = rhss.next().accept(this);
				CAstOperator op = translateOperator(ops.next());
				
				CAstNode rest = compare(rhs, ops, rhss);
				
				return Ast.makeNode(CAstNode.IF_EXPR, 
						Ast.makeNode(CAstNode.BINARY_EXPR, op, lhs, rhs), 
						Ast.makeConstant(true), rest);
			} else {
				return Ast.makeConstant(false);
			}
		}
		private CAstOperator translateOperator(cmpopType next) {
			switch (next) {
			case Is:
			case Eq:
				return CAstOperator.OP_EQ;
			case Gt:
				return CAstOperator.OP_GT;
			case GtE:
				return CAstOperator.OP_GE;
			case Lt:
				return CAstOperator.OP_LT;
			case LtE:
				return CAstOperator.OP_LE;
			case IsNot:
			case NotEq:
				return CAstOperator.OP_NE;
			case In:
				return CAstOperator.OP_IN;
			case NotIn:
				return CAstOperator.OP_NOT_IN;
			case UNDEFINED:
			default:
 				assert false : next;
				return null;
			}
		}

		@Override
		public CAstNode visitCompare(Compare arg0) throws Exception {
			return notePosition(compare(arg0.getInternalLeft().accept(this), 
					arg0.getInternalOps().iterator(), 
					arg0.getInternalComparators().iterator()), arg0);
		}

		@Override
		public CAstNode visitContinue(Continue arg0) throws Exception {
			PyObject target = context.getContinueFor(null);
			context.cfg().add(arg0, target, true);
			CAstNode gt = notePosition(Ast.makeNode(CAstNode.GOTO), arg0);
			context.cfg().map(arg0, gt);
			return gt;
		}

		@Override
		public CAstNode visitDelete(Delete arg0) throws Exception {
				return fail(arg0);
		}

		@Override
		public CAstNode visitDict(Dict arg0) throws Exception {
			int i = 0;
			CAstNode args[] = new CAstNode[ arg0.getInternalKeys().size() * 2 + 1 ];
			Iterator<expr> keys = arg0.getInternalKeys().iterator();
			Iterator<expr> vals = arg0.getInternalValues().iterator();
			args[i++] = Ast.makeNode(CAstNode.NEW, Ast.makeConstant("object"));
			while (keys.hasNext()) {
				args[i++] = keys.next().accept(this);
				args[i++] = vals.next().accept(this);
			}
			return Ast.makeNode(CAstNode.OBJECT_LITERAL, args);
		}

		
		@Override
		public CAstNode visitDictComp(DictComp arg0) throws Exception {
			String dictName = "temp " + tmpIndex++;
			CAstNode body =
			  Ast.makeNode(CAstNode.ASSIGN,
			    Ast.makeNode(CAstNode.OBJECT_REF,
			      Ast.makeNode(CAstNode.VAR, Ast.makeConstant(dictName)),
			      arg0.getInternalKey().accept(this)),
			    arg0.getInternalValue().accept(this));
				
			return Ast.makeNode(CAstNode.BLOCK_EXPR,
					  Ast.makeNode(CAstNode.DECL_STMT, 
					    Ast.makeConstant(new CAstSymbolImpl(dictName, PythonCAstToIRTranslator.Any)),
					    Ast.makeNode(CAstNode.NEW, Ast.makeConstant(PythonTypes.object))),
					  doGenerators(arg0.getInternalGenerators(), body),
					  Ast.makeNode(CAstNode.VAR, Ast.makeConstant(dictName)));
					  
		}

		@Override
		public CAstNode visitEllipsis(Ellipsis arg0) throws Exception {
			return fail(arg0);
		}

		@Override
		public CAstNode visitExceptHandler(ExceptHandler arg0) throws Exception {
			return fail(arg0);
		}

		@Override
		public CAstNode visitExec(Exec arg0) throws Exception {
			return fail(arg0);
		}

		@Override
		public CAstNode visitExpr(Expr arg0) throws Exception {
			return notePosition(arg0.getInternalValue().accept(this), arg0);
		}

		@Override
		public CAstNode visitExpression(Expression arg0) throws Exception {
			return notePosition(arg0.getInternalBody().accept(this), arg0);
		}

		@Override
		public CAstNode visitExtSlice(ExtSlice arg0) throws Exception {
			int i = 0;
			CAstNode children[] = new CAstNode[ arg0.getInternalDims().size() ];
			for (slice x : arg0.getInternalDims()) {
				children[i++] = x.accept(this);
			}
			return notePosition(Ast.makeNode(CAstNode.ARRAY_REF, children), arg0);
		}

		@Override
		public CAstNode visitFor(For arg0) throws Exception {
			Pass b = new Pass();
			Pass c = new Pass();
			LoopContext x = new LoopContext(context, b, c);
			CAstVisitor child = new CAstVisitor(x, parser);

			CAstNode breakStmt = b.accept(this);
			context.cfg().map(b, breakStmt);
			
			CAstNode continueStmt = c.accept(this);
			context.cfg().map(c, continueStmt);
			
			int i = 0;
			CAstNode[] body = new CAstNode[ arg0.getInternalBody().size() ];
			for(stmt s : arg0.getInternalBody()) {
				body[i++] = s.accept(child);
			}
			
			comprehension g = new comprehension();
			g.setIter(arg0.getIter());
			g.setTarget(arg0.getTarget());
			
			return 
			  Ast.makeNode(CAstNode.BLOCK_EXPR,
			    doGenerators(
			      Collections.singletonList(g),
				  Ast.makeNode(CAstNode.BLOCK_EXPR, 
					Ast.makeNode(CAstNode.BLOCK_EXPR, body),
					continueStmt)),
			    breakStmt);
		}

		@Override
		public CAstNode visitFunctionDef(FunctionDef arg0) throws Exception {
			return defineFunction(arg0.getInternalName(), arg0.getInternalArgs().getInternalArgs(), arg0.getInternalBody(), arg0);
		}
		
		private <S extends PythonTree> CAstNode defineFunction(String functionName, java.util.List<expr> arguments, java.util.List<S> body, PythonTree function) throws Exception {
			int i = 0;
			CAstNode[] nodes = new CAstNode[ body.size() ];
			
			class PythonCodeType implements CAstType {
				
				@Override
				public Collection<CAstType> getSupertypes() {
					return Collections.emptySet();
				}
				
				@Override
				public String getName() {
					return functionName;
				}
				
				public CAstType getReturnType() {
					return CAstType.DYNAMIC;
				}
				
				public Collection<CAstType> getExceptionTypes() {
					return Collections.singleton(CAstType.DYNAMIC);
				}
				
				public java.util.List<CAstType> getArgumentTypes() {
					java.util.List<CAstType> types = new ArrayList<CAstType>();
					for(int i = 0; i < getArgumentCount()+1; i++) {
						types.add(CAstType.DYNAMIC);
					}
					return types;
				}
				
				public int getArgumentCount() {
					return arguments.size()+1;
				}		
				
				@Override
				public String toString() {
					return getName();
				}
			};
			
			CAstType functionType;
			if (context.entity().getKind() == CAstEntity.TYPE_ENTITY) {
				class PythonMethod extends PythonCodeType implements CAstType.Method {
					@Override
					public CAstType getDeclaringType() {
						return context.entity().getType();
					}

					@Override
					public boolean isStatic() {
						return false;
					}					
				};
				
				functionType = new PythonMethod();
			} else {
				class PythonFunction extends PythonCodeType implements CAstType.Function {
					
				};
				
				functionType = new PythonFunction();				
			}
			
			int x = 0;
			String[] argumentNames = new String[ arguments.size()+1 ];
			argumentNames[x++] = "the function";
			for(expr a : arguments) {
				String name = a.accept(this).getChild(0).getValue().toString();
				argumentNames[x++] = name;		
			}

			AbstractCodeEntity fun = new AbstractCodeEntity(functionType) {
				@Override
				public int getKind() {					
					return CAstEntity.FUNCTION_ENTITY;
				}

				@Override
				public CAstNode getAST() {
					return PythonParser.this.Ast.makeNode(CAstNode.BLOCK_STMT, nodes);
				}

				@Override
				public String getName() {
					return functionName;
				}

				@Override
				public String[] getArgumentNames() {
					return argumentNames;
				}

				@Override
				public CAstNode[] getArgumentDefaults() {
					return new CAstNode[0];
				}

				@Override
				public int getArgumentCount() {
					return arguments.size()+1;

				}

				@Override
				public Collection<CAstQualifier> getQualifiers() {
					return Collections.emptySet();
				}

				@Override
				public Position getPosition() {
					return makePosition(function);
				}		
				
				@Override
				public Position getPosition(int arg) {
					return makePosition(arguments.get(arg));
				}
			};

			PythonParser.FunctionContext child = new PythonParser.FunctionContext(context, fun, function);	
			CAstVisitor cv = new CAstVisitor(child, parser);
			for(S s : body) {
				nodes[i++] = s.accept(cv);
			}

			if (context.entity().getKind() == CAstEntity.TYPE_ENTITY) {
				context.addScopedEntity(null, fun);
				return null;
				
			} else {
				CAstNode stmt = Ast.makeNode(CAstNode.FUNCTION_EXPR, Ast.makeConstant(fun));
				context.addScopedEntity(stmt, fun);
				return 
				   function instanceof Lambda?
					  stmt:
					  Ast.makeNode(CAstNode.DECL_STMT,
					    Ast.makeConstant(new CAstSymbolImpl(fun.getName(), PythonCAstToIRTranslator.Any)),
					    stmt);
			}
		}

		@Override
		public CAstNode visitGeneratorExp(GeneratorExp arg0) throws Exception {
			return fail(arg0);
		}

		@Override
		public CAstNode visitGlobal(Global arg0) throws Exception {
			java.util.List <Name> internalNames = arg0.getInternalNameNodes();
			CAstNode[] x = new CAstNode[arg0.getInternalNameNodes().size()];
			for(int i = 0; i < x.length; i++)
				x[i] = internalNames.get(i).accept(this);
			return Ast.makeNode(CAstNode.GLOBAL_DECL, x);
		}

		private CAstNode block(java.util.List<stmt> block) throws Exception {
			CAstNode[] x = new CAstNode[ block.size() ];
			for(int i = 0; i < block.size(); i++) {
				x[i] = block.get(i).accept(this);
			}
			return Ast.makeNode(CAstNode.BLOCK_STMT, x);
		}
		
		@Override
		public CAstNode visitIf(If arg0) throws Exception {
			return Ast.makeNode(CAstNode.IF_STMT,
					arg0.getInternalTest().accept(this),
					block(arg0.getInternalBody()),
					block(arg0.getInternalOrelse()));
		}

		@Override
		public CAstNode visitIfExp(IfExp arg0) throws Exception {
			return Ast.makeNode(CAstNode.IF_EXPR, 
					arg0.getInternalTest().accept(this),
					arg0.getInternalBody().accept(this),
					arg0.getInternalOrelse().accept(this));
		}

		private String name(alias n) {
			String s = n.getInternalAsname()==null? n.getInternalName(): n.getInternalAsname();
			if (s.contains(".")) {
				s = s.substring(0, s.indexOf('.'));
			}
			return s;
		}
		
		@Override
		public CAstNode visitImport(Import arg0) throws Exception {
			int i = 0;
			CAstNode[] elts = new CAstNode[ arg0.getInternalNames().size()*2 ];
			for(alias n : arg0.getInternalNames()) {
				elts[i++] = Ast.makeNode(CAstNode.DECL_STMT,
					Ast.makeConstant(new CAstSymbolImpl(name(n), PythonCAstToIRTranslator.Any)));
				elts[i++] = Ast.makeNode(CAstNode.ASSIGN,
					Ast.makeNode(CAstNode.VAR, Ast.makeConstant(name(n))),
					Ast.makeNode(CAstNode.PRIMITIVE, Ast.makeConstant("import"), Ast.makeConstant(n.getInternalName().replaceAll("[.]", "/"))));
			}
			return Ast.makeNode(CAstNode.BLOCK_STMT, elts);
		}

		@Override
		public CAstNode visitImportFrom(ImportFrom arg0) throws Exception {
			int i = 0;
			CAstNode[] elts = new CAstNode[ arg0.getInternalNames().size() ];
			for(alias n : arg0.getInternalNames()) {
				elts[i++] = Ast.makeNode(CAstNode.DECL_STMT,
						Ast.makeConstant(new CAstSymbolImpl(name(n), PythonCAstToIRTranslator.Any)),
						Ast.makeNode(CAstNode.OBJECT_REF,
								Ast.makeNode(CAstNode.PRIMITIVE, 
									Ast.makeConstant("import"), 
									Ast.makeConstant(arg0.getInternalModule().replaceAll("[.]", "/"))),
								Ast.makeConstant(n.getInternalName())));
			}
			return Ast.makeNode(CAstNode.BLOCK_STMT, elts);
		}

		@Override
		public CAstNode visitIndex(Index arg0) throws Exception {
			return arg0.getInternalValue().accept(this);
		}

		@Override
		public CAstNode visitInteractive(Interactive arg0) throws Exception {
			return fail(arg0);
		}

		@Override
		public CAstNode visitLambda(Lambda arg0) throws Exception {
			arguments lambdaArgs = arg0.getInternalArgs();
			expr lambdaBody = arg0.getInternalBody();
			return defineFunction("lambda" + (++tmpIndex), lambdaArgs.getInternalArgs(), Collections.singletonList(lambdaBody), arg0);
		}

		@Override
		public CAstNode visitList(List arg0) throws Exception {
			int i = 0, j = 0;
			CAstNode[] elts = new CAstNode[ 2*arg0.getInternalElts().size()+1 ];
			elts[i++] = Ast.makeNode(CAstNode.NEW, Ast.makeConstant("list"));
			for(expr e : arg0.getInternalElts()) {
				elts[i++] = Ast.makeConstant("" + j++);
				elts[i++] = e.accept(this);
			}
			return Ast.makeNode(CAstNode.OBJECT_LITERAL, elts);
		}

		@Override
		public CAstNode visitListComp(ListComp arg0) throws Exception {
			String listName = "temp " + tmpIndex++;
			String indexName = "temp " + tmpIndex++;
			CAstNode body = 
			  Ast.makeNode(CAstNode.BLOCK_EXPR,
			    Ast.makeNode(CAstNode.ASSIGN,
			      Ast.makeNode(CAstNode.ARRAY_REF, 
			        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(listName)),
			        Ast.makeConstant(PythonTypes.Root),
			        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(indexName))),
			      arg0.getInternalElt().accept(this)),
			    Ast.makeNode(CAstNode.ASSIGN,
			      Ast.makeNode(CAstNode.VAR, Ast.makeConstant(indexName)),
			      Ast.makeNode(CAstNode.BINARY_EXPR, 
			        CAstOperator.OP_ADD,
			        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(indexName)),
			        Ast.makeConstant(1))));
			
			return Ast.makeNode(CAstNode.BLOCK_EXPR,
			  Ast.makeNode(CAstNode.DECL_STMT, 
			    Ast.makeConstant(new CAstSymbolImpl(listName, PythonCAstToIRTranslator.Any)),
			    Ast.makeNode(CAstNode.NEW, Ast.makeConstant(PythonTypes.list))),
			  Ast.makeNode(CAstNode.DECL_STMT, 
			    Ast.makeConstant(new CAstSymbolImpl(indexName, PythonCAstToIRTranslator.Any)),
				Ast.makeConstant(0)),
			  doGenerators(arg0.getInternalGenerators(), body),
			  Ast.makeNode(CAstNode.VAR, Ast.makeConstant(listName)));
		}

		private CAstNode doGenerators(java.util.List<comprehension> generators, CAstNode body) throws Exception {
			CAstNode result = body;
									
			for(comprehension c : generators)  {
				if (c.getInternalIfs() != null) {
					int j = c.getInternalIfs().size();
					if (j > 0) {
						for(expr test : c.getInternalIfs()) {
							CAstNode v = test.accept(this);
							result = Ast.makeNode(CAstNode.IF_EXPR, v, body);
						}
					}
				}
				
				String tempName = "temp " + tmpIndex++;
				
				CAstNode test = 
		          Ast.makeNode(CAstNode.BINARY_EXPR,
		            CAstOperator.OP_NE,
		            Ast.makeConstant(null),
		            Ast.makeNode(CAstNode.BLOCK_EXPR,
		              Ast.makeNode(CAstNode.ASSIGN, 
		                c.getInternalTarget().accept(this),
			            Ast.makeNode(CAstNode.EACH_ELEMENT_GET,   
			              Ast.makeNode(CAstNode.VAR, Ast.makeConstant(tempName)),
			              c.getInternalTarget().accept(this)))));
				
				result = Ast.makeNode(CAstNode.BLOCK_EXPR,
				  Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new CAstSymbolImpl(tempName, PythonCAstToIRTranslator.Any)), 
				    c.getInternalIter().accept(this)),
				  Ast.makeNode(CAstNode.LOOP, 
					test, 
					Ast.makeNode(CAstNode.BLOCK_EXPR,
					  Ast.makeNode(CAstNode.ASSIGN,
					    c.getInternalTarget().accept(this),
					    Ast.makeNode(CAstNode.OBJECT_REF,
					    	c.getInternalIter().accept(this),
					    	c.getInternalTarget().accept(this))),
					  result)));
				
			}

			return result;
		}

		private String[] defaultImportNames = new String[] {
				"str",
				"float",
				"int",
				"__name__",
				"print",
				"super",
				"len",
				"open",
				"hasattr",
				"BaseException",
				"abs",
				"range"
		};
		
		private void defaultImports(Collection<CAstNode> elts) {
			for(String n : defaultImportNames) {
				elts.add(
					Ast.makeNode(CAstNode.DECL_STMT,
						Ast.makeConstant(new CAstSymbolImpl(n, PythonCAstToIRTranslator.Any)),
						Ast.makeNode(CAstNode.PRIMITIVE, Ast.makeConstant("import"), Ast.makeConstant(n))));
			}
		}
		
		@Override
		public CAstNode visitModule(Module arg0) throws Exception {
			java.util.List<CAstNode> elts = new ArrayList<CAstNode>(arg0.getChildCount());
			defaultImports(elts);
			for(PythonTree c : arg0.getChildren()) {
				elts.add(c.accept(this));
			}
			return Ast.makeNode(CAstNode.BLOCK_EXPR, elts.toArray(new CAstNode[ elts.size() ]));
		}

		@Override
		public CAstNode visitName(Name arg0) throws Exception {
			String name = arg0.getText();
			if(name.equals("True"))
				return Ast.makeConstant(true);
			else if(name.equals("False"))
				return Ast.makeConstant(false);
			else if(name.equals("None"))
				return Ast.makeConstant(null);

			return notePosition(Ast.makeNode(CAstNode.VAR, Ast.makeConstant(name)), arg0);
		}

		@Override
		public CAstNode visitNum(Num arg0) throws Exception {
			String numStr = arg0.getInternalN().toString();

			if(numStr.contains("l") | numStr.contains("L"))
				return Ast.makeConstant(Long.parseLong(numStr.substring(0, numStr.length() - 1)));
			else if(!numStr.contains("."))
				return Ast.makeConstant(Long.parseLong(numStr));
			//else if(numStr.contains("j") | numStr.contains("J")) //Placeholder for implementation/modeling of imaginary numbers
			//	return Ast.makeConstant();
			else
				return Ast.makeConstant(Double.parseDouble(numStr));
		}

		@Override
		public CAstNode visitPass(Pass arg0) throws Exception {
			String label = "temp " + tmpIndex++;
			return Ast.makeNode(CAstNode.LABEL_STMT, Ast.makeConstant(label), Ast.makeNode(CAstNode.EMPTY));
		}

		@Override
		public CAstNode visitPrint(Print arg0) throws Exception {
			int i = 0;
			CAstNode[] elts = new CAstNode[ arg0.getInternalValues().size() ];
			for(expr e : arg0.getInternalValues()) {
				elts[i++] = e.accept(this);
			}
			return Ast.makeNode(CAstNode.ECHO, elts);
		}

		@Override
		public CAstNode visitRaise(Raise arg0) throws Exception {
			if (arg0.getInternalType() == null) {
				return Ast.makeNode(CAstNode.THROW);
			} else {
				return Ast.makeNode(CAstNode.THROW, arg0.getInternalType().accept(this));
			}
		}

		@Override
		public CAstNode visitRepr(Repr arg0) throws Exception {
			return fail(arg0);
		}

		@Override
		public CAstNode visitReturn(Return arg0) throws Exception {
			if(arg0.getInternalValue() == null)
				return Ast.makeNode(CAstNode.RETURN, Ast.makeNode(CAstNode.VAR, Ast.makeConstant("None")));
			else
				return Ast.makeNode(CAstNode.RETURN, arg0.getInternalValue().accept(this));
		}

		@Override
		public CAstNode visitSet(Set arg0) throws Exception {
			return fail(arg0);
		}

		@Override
		public CAstNode visitSetComp(SetComp arg0) throws Exception {
			return fail(arg0);
		}

		private CAstNode acceptOrNull(PythonTree x) throws Exception {
			return (x==null)? Ast.makeConstant(CAstNode.EMPTY): x.accept(this);
		}
		
		@Override
		public CAstNode visitSlice(Slice arg0) throws Exception {
			return Ast.makeNode(CAstNode.ARRAY_LITERAL,
					acceptOrNull(arg0.getInternalLower()),
					acceptOrNull(arg0.getInternalUpper()),
					acceptOrNull(arg0.getInternalStep()));
		}

		@Override
		public CAstNode visitStr(Str arg0) throws Exception {
			return Ast.makeConstant(arg0.getInternalS().toString());
		}

		@Override
		public CAstNode visitSubscript(Subscript arg0) throws Exception {
			return notePosition(Ast.makeNode(CAstNode.OBJECT_REF, 
					notePosition(arg0.getInternalValue().accept(this), arg0.getInternalValue()), 
					notePosition(arg0.getInternalSlice().accept(this), arg0.getInternalSlice())), arg0);
		}

		@Override
		public CAstNode visitSuite(Suite arg0) throws Exception {
			return fail(arg0);
		}
	
		private class TryCatchContext extends TranslatorToCAst.TryCatchContext<WalkContext, PythonTree> implements WalkContext {

			TryCatchContext(WalkContext parent, Map<String, CAstNode> catchNode) {
				super(parent, catchNode);
			}

			@Override
			public WalkContext getParent() {
				return (WalkContext) super.getParent();
			}

		}

		@Override
		public CAstNode visitTryExcept(TryExcept arg0) throws Exception {
			Map<String,CAstNode> handlers = HashMapFactory.make();
			for(PyObject x : arg0.getChildren()) {
				if (x instanceof ExceptHandler) {
					ExceptHandler h = (ExceptHandler) x;
					CAstNode name = h.getInternalName()==null? 
						Ast.makeConstant("x"): 
						h.getInternalName().accept(this);
					CAstNode type = h.getInternalType()==null?
							Ast.makeConstant("any"):
							h.getInternalType().accept(this);
					CAstNode body = block(h.getInternalBody());
					handlers.put(type.toString(), Ast.makeNode(CAstNode.CATCH,
						Ast.makeConstant(name),
						body));
					
					if (h.getInternalType() != null) {
						context.getNodeTypeMap().add(name, types.getCAstTypeFor(h.getInternalType()));
					}
				}
			}
				
			TryCatchContext catches = new TryCatchContext(context, handlers);
			CAstVisitor child = new CAstVisitor(catches, parser);
			CAstNode block = child.block(arg0.getInternalBody());
			
			return Ast.makeNode(CAstNode.TRY,
				Ast.makeNode(CAstNode.BLOCK_EXPR,
					block,
					block(arg0.getInternalOrelse())),
				handlers.values().toArray(new CAstNode[ handlers.size() ]));
		}

		@Override
		public CAstNode visitTryFinally(TryFinally arg0) throws Exception {
			return Ast.makeNode(CAstNode.UNWIND, block(arg0.getInternalBody()), block(arg0.getInternalFinalbody()));
		}

		@Override
		public CAstNode visitTuple(Tuple arg0) throws Exception {
			int i = 0;
			CAstNode[] elts = new CAstNode[ arg0.getInternalElts().size()+1 ];
			
			elts[i++] = Ast.makeNode(CAstNode.NEW, Ast.makeConstant("list"));
			for(expr e : arg0.getInternalElts()) {
				elts[i++] = e.accept(this);
			}
			return Ast.makeNode(CAstNode.ARRAY_LITERAL, elts);
		}

		@Override
		public CAstNode visitUnaryOp(UnaryOp arg0) throws Exception {
			CAstOperator op = translateOperator(arg0.getInternalOp());
			return Ast.makeNode(CAstNode.UNARY_EXPR, op, arg0.getInternalOperand().accept(this));
		}

		private CAstOperator translateOperator(unaryopType internalOp) {
			switch (internalOp) {
			case Invert:
				return CAstOperator.OP_BITNOT;
			case Not:
				return CAstOperator.OP_NOT;
			case UAdd:
				return CAstOperator.OP_ADD;
			case USub:
				return CAstOperator.OP_SUB;
			case UNDEFINED:
			default:
				assert false : internalOp;
				return null;
			}
		}

		private class LoopContext extends TranslatorToCAst.LoopContext<WalkContext, PythonTree> implements WalkContext {

			LoopContext(WalkContext parent, PythonTree breakTo, PythonTree continueTo) {
				super(parent, breakTo, continueTo, null);
			}

			@Override
			public WalkContext getParent() {
				return (WalkContext) super.getParent();
			}
		 }
	
		@Override
		public CAstNode visitWhile(While arg0) throws Exception {
			Pass b = new Pass();
			Pass c = new Pass();
			LoopContext x = new LoopContext(context, b, c);
			CAstVisitor child = new CAstVisitor(x, parser);
			
			if (arg0.getInternalOrelse() == null || arg0.getInternalOrelse().size() == 0) {
				return
					Ast.makeNode(CAstNode.BLOCK_EXPR,
						Ast.makeNode(CAstNode.LOOP, 
							arg0.getInternalTest().accept(child), 
							Ast.makeNode(CAstNode.BLOCK_EXPR,
								child.block(arg0.getInternalBody()),
								c.accept(child))),
						b.accept(child));
			} else {
				return Ast.makeNode(CAstNode.BLOCK_EXPR,
					Ast.makeNode(CAstNode.LOOP, 
						Ast.makeNode(CAstNode.ASSIGN, 
							Ast.makeNode(CAstNode.VAR, Ast.makeConstant("test tmp")),
							arg0.getInternalTest().accept(child)),
						Ast.makeNode(CAstNode.BLOCK_EXPR,
							child.block(arg0.getInternalBody()),
							c.accept(child))),
					Ast.makeNode(CAstNode.IF_EXPR, 
						Ast.makeNode(CAstNode.UNARY_EXPR, 
							CAstOperator.OP_NOT, 
							Ast.makeNode(CAstNode.VAR, Ast.makeConstant("test tmp"))),
						child.block(arg0.getInternalOrelse())),
					b.accept(child));
			}
		}

		private int tmpIndex = 0;
		
		@Override
		public CAstNode visitWith(With arg0) throws Exception {
			int i = 0;
			CAstNode[] blk = new CAstNode[ arg0.getInternalBody().size() ];
			for(stmt s : arg0.getInternalBody()) {
				blk[i++] = s.accept(this);
			}
		
			String tmpName = "tmp_" + tmpIndex++;
			
			Supplier<CAstNode> v = () -> { 
				try {
					return arg0.getInternalOptional_vars() == null?
							Ast.makeNode(CAstNode.VAR, Ast.makeConstant(tmpName)):
								arg0.getInternalOptional_vars().accept(this);
				} catch (Exception e) {
					assert false : e.toString();
					return null;
				}
			};
			
			return Ast.makeNode(CAstNode.BLOCK_STMT,
					Ast.makeNode(CAstNode.DECL_STMT,
							Ast.makeConstant(new CAstSymbolImpl(tmpName, PythonCAstToIRTranslator.Any))),
					Ast.makeNode(CAstNode.DECL_STMT,
							Ast.makeConstant(new CAstSymbolImpl(v.get().getChild(0).getValue().toString(), PythonCAstToIRTranslator.Any)),
							arg0.getInternalContext_expr().accept(this)),
					Ast.makeNode(CAstNode.UNWIND, 
						Ast.makeNode(CAstNode.BLOCK_EXPR,
							Ast.makeNode(CAstNode.CALL, 
								Ast.makeNode(CAstNode.OBJECT_REF, v.get(), Ast.makeConstant("__begin__")),
								Ast.makeNode(CAstNode.EMPTY)),
							blk),
						Ast.makeNode(CAstNode.CALL, 
							Ast.makeNode(CAstNode.OBJECT_REF, v.get(), Ast.makeConstant("__end__")),
							Ast.makeNode(CAstNode.EMPTY))));
		}

		@Override
		public CAstNode visitYield(Yield arg0) throws Exception {
			return Ast.makeNode(CAstNode.RETURN_WITHOUT_BRANCH, arg0.getInternalValue().accept(this));
		}
	}
	
	protected abstract WalaPythonParser makeParser() throws IOException;
	
	protected abstract Reader getReader() throws IOException;
	
	protected abstract String scriptName();

	protected abstract URL getParsedURL() throws IOException;

	private final CAstTypeDictionaryImpl<String> types;
	
	protected PythonParser(CAstTypeDictionaryImpl<String> types) {
		this.types = types;
	}
	
	@Override
	public <C extends RewriteContext<K>, K extends CopyKey<K>> void addRewriter(CAstRewriterFactory<C, K> factory,
			boolean prepend) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public CAstEntity translateToCAst() throws Error, IOException {
		WalaPythonParser parser = makeParser();
		Module pythonAst = (Module)parser.parseModule();
		try {
			WalkContext root = new PythonParser.RootContext(pythonAst);
			CAstEntity script = new AbstractScriptEntity(scriptName(), CAstType.DYNAMIC) {

				private final WalkContext context;
				private final CAstVisitor visitor;
				private final CAstNode cast;
				
				{
					context = new PythonParser.FunctionContext(root, this, pythonAst);		
					visitor = new CAstVisitor(context, parser);
					cast = pythonAst.accept(visitor);
				}
				
				@Override
				public CAstNode getAST() {
					return cast;
				}

				@Override
				public Position getPosition() {
					return visitor.makePosition(pythonAst);
				}
				
				public Position getPosition(int arg) {
					return null;
				}

			};
			
			return script;
		} catch (Exception e) {
			throw new Error(Collections.singleton(new Warning(Warning.SEVERE) {
				@Override
				public String getMsg() {
					return e.toString() + Arrays.toString(e.getStackTrace());
				}
			}));
		}
	}

	public void print(PyObject ast) {
		System.err.println(ast.getClass());
	}
}
