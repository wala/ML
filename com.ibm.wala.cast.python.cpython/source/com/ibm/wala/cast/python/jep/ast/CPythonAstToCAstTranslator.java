package com.ibm.wala.cast.python.jep.ast;

import static com.ibm.wala.cast.python.jep.Util.interps;
import static com.ibm.wala.cast.python.jep.Util.runit;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.cast.python.loader.JepPythonLoaderFactory;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.translator.AbstractCodeEntity;
import com.ibm.wala.cast.ir.translator.AbstractScriptEntity;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.loader.PythonLoaderFactory;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstControlFlowRecorder;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstSourcePositionRecorder;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.CopyKey;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.RewriteContext;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.cast.tree.visit.CAstVisitor.Context;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.cha.SeqClassHierarchyFactory;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.EmptyIterator;

import jep.Interpreter;
import jep.python.PyObject;

/**
 * An api for creating a WALA CASst representation of the standard Python
 * ASTs given source code.
 */
public class CPythonAstToCAstTranslator implements TranslatorToCAst {

	  private static CAstType codeBody =
		      new CAstType() {

		        @Override
		        public String getName() {
		          return "CodeBody";
		        }

		        @Override
		        public Collection<CAstType> getSupertypes() {
		          return Collections.emptySet();
		        }
		      };

	
	/**
	 * parse Python code into the standard CPython AST
	 * @param code source code to be parsed into an AST
	 * @return AST as a @PyObject
	 */
	public static PyObject getAST(String code) {
		Interpreter interp = interps.get();
		
		interp.set("code", code);
		interp.exec("theast = ast.parse(code)");
		
		return (PyObject) interp.getValue("theast");
	}
	
	/**
	 * turn an AST into a JSON representation
	 * @param ast a Python AST as a @PyObject
	 * @return JSON form of the AST as tree of @Map objects
	 */
	@SuppressWarnings("unchecked")
	public static Map<String,?> getJSON(PyObject ast) {
		Interpreter interp = interps.get();

		interp.set("theast", ast);
		interp.exec("thejson = ast2json.ast2json(theast)");
		
		return (Map<String,?>) interp.getValue("thejson");
		
	}
	
	@SuppressWarnings("unchecked")
	public static Collection<String> properties(PyObject obj) {
		Interpreter interp = interps.get();

		interp.set("obj", obj);
		interp.exec("d = dir(obj)");
		
		return (Collection<String>) interp.getValue("d");
		
	}

	private static final CAstPattern nm = CAstPattern.parse("ASSIGN(VAR(<n>*),**)");

	public static  final class PythonScriptEntity extends AbstractCodeEntity {
		private final String fn;

		private static CAstType makeType(String fn) {
	   return
	           new CAstType.Function() {
				
           @Override
           public Collection<CAstType> getSupertypes() {
             return Collections.singleton(codeBody);
           }
	             @Override
	             public String getName() {
	               return fn;
	             }

	             @Override
				public CAstType getReturnType() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public Collection<CAstType> getExceptionTypes() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public List<CAstType> getArgumentTypes() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public int getArgumentCount() {
					return 1;
				}
			};
		}
		
		   private PythonScriptEntity(String fn, Path pn) throws IOException {
				this(makeType(fn), fn, Files.readString(pn));
	   }
	
		   public PythonScriptEntity(String fn, String code) throws IOException {
				this(makeType(fn), fn, code);
	   }
			

		public PythonScriptEntity(CAstType type, String fn, String scriptCode) throws IOException {
			super(type);
			this.fn = fn;
				PyObject ast = CPythonAstToCAstTranslator.getAST(scriptCode);

				TestVisitor x = new TestVisitor(this) {
					@Override
					public URL url() {
						try {
							return new URL("file:" + fn);
						} catch (MalformedURLException e) {
							return null;
						}
					}
				};
				
				Ast = x.visit(ast, new ScriptContext(ast, new CAstImpl(), this));
			}

		@Override
		public int getKind() {
			return CAstEntity.SCRIPT_ENTITY;
		}

		@Override
		public String getName() {
			return fn;
		}

		
		@Override
		public String getSignature() {
			return "script " + getName();
		}

		@Override
		public String[] getArgumentNames() {
			return new String[] { "self" };
		}

		@Override
		public CAstNode[] getArgumentDefaults() {
			return new CAstNode[0];
		}

		@Override
		public int getArgumentCount() {
			return 0;
		}

		@Override
		public Position getNamePosition() {
			return null;
		}

		@Override
		public Position getPosition(int arg) {
			return null;
		}

		@Override
		public Collection<CAstQualifier> getQualifiers() {
			return null;
		}
	}

	public interface WalkContext extends TranslatorToCAst.WalkContext<WalkContext, PyObject> {

	}

	public static class ScriptContext extends TranslatorToCAst.FunctionContext<WalkContext, PyObject>
			implements WalkContext {
		
		private final PyObject ast;
		PythonScriptEntity self;
		
		protected ScriptContext(PyObject s, CAstImpl ast, PythonScriptEntity self) {
			super(null, s);
			this.ast = s;
			this.self = self;
		}
		
		@Override
		public PyObject top() {
			return ast;
		}

		@Override
		public WalkContext getParent() {
			assert false;
			return null;
		}

		@Override
		public CAstNode getCatchTarget() {
			assert false;
			return null;
		}

		@Override
		public CAstNode getCatchTarget(String s) {
			assert false;
			return null;
		}
		
		@Override
		public CAstControlFlowRecorder cfg() {
			return (CAstControlFlowRecorder) self.getControlFlow();
		}

		@Override
		public CAstSourcePositionRecorder pos() {
			return (CAstSourcePositionRecorder) self.getSourceMap();
		}

		@Override
		public void addScopedEntity(CAstNode construct, CAstEntity e) {
			self.addScopedEntity(construct, e);
		}

		@Override
		public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
			return self.getAllScopedEntities();
		}
	}

	private static class FunctionContext extends TranslatorToCAst.FunctionContext<WalkContext, PyObject>
		implements WalkContext {

		protected FunctionContext(WalkContext parent, PyObject s) {
			super(parent, s);
			// TODO Auto-generated constructor stub
		}

	}

	private static class LoopContext extends TranslatorToCAst.LoopContext<WalkContext, PyObject>
			implements WalkContext {

		LoopContext(WalkContext parent, PyObject breakTo, PyObject continueTo) {
			super(parent, breakTo, continueTo, null);
		}

		private boolean continued = false;
		private boolean broke = false;
		
		@Override
		public PyObject getContinueFor(String l) {
			continued = true;
			return super.getContinueFor(l);
		}


		@Override
		public PyObject getBreakFor(String l) {
			broke = true;
			return super.getBreakFor(l);
		}


		@Override
		public WalkContext getParent() {
			return (WalkContext) super.getParent();
		}
	}

	public static abstract class TestVisitor implements JepAstVisitor<CAstNode, WalkContext> {
		CAst ast = new CAstImpl();
		private int label = 0;
		private final CAstEntity entity;
		
		private TestVisitor(CAstEntity self) {
			entity = self;
		}
		
		@Override
		public CAstNode visit(PyObject o, WalkContext context) {
			Position pos = pos(o);
			CAstNode n = JepAstVisitor.super.visit(o, context);
			if (pos != null) {
				context.pos().setPosition(n, pos);
			}
			return n;
		}

		@Override
		public CAstNode visitModule(PyObject o, WalkContext context) {
			@SuppressWarnings("unchecked")
			List<PyObject> body = (List<PyObject>) o.getAttr("body");
			CAstNode bodyAst = ast.makeNode(CAstNode.BLOCK_STMT, body.stream().map(f -> visit(f, context)).collect(Collectors.toList()));
			
			Set<String> exposedNames = exposedNames(bodyAst);
			if (exposedNames.size() > 0)
				return ast.makeNode(CAstNode.UNWIND,
						bodyAst,
						ast.makeNode(CAstNode.BLOCK_STMT,
								exposedNames.stream()
								.map(n -> ast.makeNode(CAstNode.ASSIGN, 
										ast.makeNode(CAstNode.OBJECT_REF, ast.makeNode(CAstNode.THIS), ast.makeConstant(n)),
										ast.makeNode(CAstNode.VAR, ast.makeConstant(n))))
								.collect(Collectors.toList())));
			else
				return bodyAst;
		}		
		
		@SuppressWarnings("unchecked")
		public CAstNode visitFunctionDef(PyObject o, WalkContext context) {
			WalkContext fc = new FunctionContext(context, o);
			
			String functionName = (String) o.getAttr("name");
			
			CAstNode body = 
				visit(CAstNode.BLOCK_STMT, 
					  ((List<PyObject>)o.getAttr("body")).stream().collect(Collectors.toList()),
			          fc);
			
			Object rawArgs = o.getAttr("args");
			List<PyObject> arguments;
			if (rawArgs instanceof List) {
				arguments = (List<PyObject>) rawArgs;
			} else if (rawArgs instanceof PyObject) {
				arguments = Arrays.asList((PyObject)rawArgs);
			} else {
				arguments = null;
				assert false;
			}
			
			int argumentCount = arguments.size();

			List<CAstType> argumentTypes = Collections.nCopies(argumentCount, CAstType.DYNAMIC);
			
			CAstType.Function funType = new CAstType.Function() {
				
				@Override
				public Collection<CAstType> getSupertypes() {
					return Collections.singleton(codeBody);
				}
				
				@Override
				public String getName() {
					return functionName;
				}
				
				@Override
				public CAstType getReturnType() {
					return CAstType.DYNAMIC;
				}
				
				@Override
				public Collection<CAstType> getExceptionTypes() {
					return Collections.emptyList();
				}
				
				@Override
				public List<CAstType> getArgumentTypes() {
					return argumentTypes;
				}
				
				@Override
				public int getArgumentCount() {
					return argumentCount;
				}
			}; 
			
			CAstEntity fun = new AbstractScriptEntity(functionName, funType) {

				@Override
				public int getKind() {
					return CAstEntity.FUNCTION_ENTITY;
				}

				@Override
				public String getName() {
					return functionName;
				}

				@Override
				public String getSignature() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public String[] getArgumentNames() {
					List<String> names = arguments.stream().map(a -> a.toString()).collect(Collectors.toList());
					return names.toArray(new String[names.size()]);
				}

				@Override
				public CAstNode[] getArgumentDefaults() {
					return new CAstNode[0];
				}

				@Override
				public int getArgumentCount() {
					return argumentCount;
				}

				@Override
				public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
					return fc.getScopedEntities();
				}

				@Override
				public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
					if (fc.getScopedEntities().containsKey(construct)) {
						return fc.getScopedEntities().get(construct).iterator();
					} else {
						return EmptyIterator.instance();
					}
				}

				@Override
				public CAstNode getAST() {
					return body;
				}

				@Override
				public Position getNamePosition() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Position getPosition(int arg) {
					// TODO Auto-generated method stub
					return null;
				}
			};
			
			CAstNode fe = ast.makeNode(CAstNode.FUNCTION_EXPR, ast.makeConstant(fun));
			context.addScopedEntity(fe, fun);
			CAstNode node = ast.makeNode(CAstNode.DECL_STMT,
					ast.makeConstant(new CAstSymbolImpl(functionName, funType)),	
					fe);
			
			return node;
		}

		public CAstNode visitName(PyObject o, WalkContext context) {
			String nm = (String) o.getAttr("id");
			return ast.makeNode(CAstNode.VAR, ast.makeConstant(nm));
		}

		public CAstNode visitConstant(PyObject o, WalkContext context) {
			Object nm = o.getAttr("value");
			return ast.makeConstant(nm);
		}

		public CAstNode visitAssign(PyObject o, WalkContext context) {
			PyObject value = (PyObject) o.getAttr("value");
			CAstNode rhs = visit(value, context);
			@SuppressWarnings("unchecked")
			List<PyObject> body = (List<PyObject>) o.getAttr("targets");
			return ast.makeNode(CAstNode.BLOCK_STMT, body.stream()
					.map(f -> ast.makeNode(CAstNode.ASSIGN, visit(f, context), rhs))
					.collect(Collectors.toList()));

		}
		
		private CAstNode visit(int node, List<PyObject> l, WalkContext context) {
			return ast.makeNode(node, 
			  l.stream()
			    .map(f -> visit(f, context))
			    .collect(Collectors.toList()));
		}
		
		public CAstNode visitCall(PyObject o, WalkContext context) {
			PyObject func = o.getAttr("func", PyObject.class);
			@SuppressWarnings("unchecked")
			List<PyObject> args = (List<PyObject>) o.getAttr("args");
			@SuppressWarnings("unchecked")
			List<PyObject> keywords = (List<PyObject>) o.getAttr("keywords");
			
			List<CAstNode> ak = new ArrayList<>();
			ak.add(visit(func, context));
		    ak.add(ast.makeNode(CAstNode.EMPTY));

			args.forEach(a -> { 
				ak.add(visit(a, context));
			});
			for(PyObject k : keywords) {
				ak.add(ast.makeNode(
						CAstNode.ARRAY_LITERAL,
						ast.makeConstant(k.getAttr("arg", String.class)),
						visit(k.getAttr("value", PyObject.class), context)));
			}
			
			return ast.makeNode(CAstNode.CALL, ak);
					
		}

		public CAstNode visitImportFrom(PyObject o, WalkContext context) {
			String module = (String) o.getAttr("module");
			@SuppressWarnings("unchecked")
			List<PyObject> alias = (List<PyObject>) o.getAttr("names");
			return ast.makeNode(CAstNode.BLOCK_STMT, 
			alias.stream().map(a -> ast.makeNode(CAstNode.ASSIGN, 
					ast.makeNode(CAstNode.VAR, ast.makeConstant(a.getAttr("name"))),
					ast.makeNode(CAstNode.OBJECT_REF, 
						ast.makeNode(CAstNode.VAR, ast.makeConstant(module)),
						ast.makeConstant(a.getAttr("name"))))).collect(Collectors.toList()));
		}

		public CAstNode visitExpr(PyObject o, WalkContext context) {
			return visit(o.getAttr("value", PyObject.class), context);
		}

		public CAstNode visitWhile(PyObject wl, WalkContext context) {
			PyObject test = (PyObject) wl.getAttr("test");
			CAstNode testNode = visit(test, context);
			
			CAstNode loopNode = visitLoopCore(wl, context, testNode, null);
			
			return loopNode;
		}

		private CAstNode visitLoopCore(PyObject loop, WalkContext context, CAstNode testNode, CAstNode update) {
			@SuppressWarnings("unchecked")
			List<PyObject> body = (List<PyObject>) loop.getAttr("body");
			@SuppressWarnings("unchecked")
			List<PyObject> orelse = (List<PyObject>) loop.getAttr("orelse");
			
			PyObject contLabel = runit("ast.Pass()");
			PyObject breakLabel = runit("ast.Pass()");
			LoopContext lc = new LoopContext(context, breakLabel, contLabel);
			
			CAstNode bodyNode = visit(CAstNode.BLOCK_STMT, body, lc);
			
			if (lc.continued) {
				CAstNode cn = ast.makeNode(CAstNode.LABEL_STMT, ast.makeConstant("label_" + label++),visit(contLabel, context));
				context.cfg().map(contLabel, cn);
				bodyNode = ast.makeNode(CAstNode.BLOCK_STMT, bodyNode, cn);
				if (update != null) {
					bodyNode = ast.makeNode(CAstNode.BLOCK_STMT, bodyNode, update);
				}
			}
			CAstNode loopNode = ast.makeNode(CAstNode.LOOP, testNode, bodyNode);
			
			if (orelse.size() > 0) {
				CAstNode elseNode = visit(CAstNode.BLOCK_STMT, orelse, context);
				loopNode = ast.makeNode(CAstNode.BLOCK_STMT, loopNode, elseNode);
			}
			
			if (lc.broke) {
				CAstNode bn = ast.makeNode(CAstNode.LABEL_STMT, ast.makeConstant("label_" + label++), visit(breakLabel, context));
				context.cfg().map(breakLabel, bn);
				loopNode = ast.makeNode(CAstNode.BLOCK_STMT, loopNode, bn);
			}
			return loopNode;
		}

		private CAstOperator translateOperator(String next) {
			switch (next) {
			case "Invert":
			case "Not":
				return CAstOperator.OP_NOT;
			case "UAdd":
				return CAstOperator.OP_ADD;
			case "USub":
				return CAstOperator.OP_SUB;
			case "Is":
			case "Eq":
				return CAstOperator.OP_EQ;
			case "Gt":
				return CAstOperator.OP_GT;
			case "GtE":
				return CAstOperator.OP_GE;
			case "Lt":
				return CAstOperator.OP_LT;
			case "LtE":
				return CAstOperator.OP_LE;
			case "IsNot":
			case "NotEq":
				return CAstOperator.OP_NE;
			case "In":
				return CAstOperator.OP_IN;
			case "NotIn":
				return CAstOperator.OP_NOT_IN;
			case "Add":
				return CAstOperator.OP_ADD;
			case "BitAnd":
				return CAstOperator.OP_BIT_AND;
			case "BitOr":
				return CAstOperator.OP_BIT_OR;
			case "BitXor":
				return CAstOperator.OP_BIT_XOR;
			case "Div":
				return CAstOperator.OP_DIV;
			case "FloorDiv":
				return CAstOperator.OP_DIV; // FIXME: need 'quotient'
			case "LShift":
				return CAstOperator.OP_LSH;
			case "Mod":
				return CAstOperator.OP_MOD;
			case "MatMult":
				return CAstOperator.OP_MUL; // FIXME: matrix multiply
			case "Mult":
				return CAstOperator.OP_MUL;
			case "Pow":
				return CAstOperator.OP_POW;
			case "RShift":
				return CAstOperator.OP_RSH;
			case "Sub":
				return CAstOperator.OP_SUB;

			default:
				assert false : next;
				return null;
			}
		}

		public CAstNode visitCompare(PyObject cmp, WalkContext context) {
			CAstNode expr = null;
			
			PyObject lhs = (PyObject) cmp.getAttr("left");
			CAstNode ln = visit(lhs, context);
			
			@SuppressWarnings("unchecked")
			Iterator<PyObject> exprs = ((List<PyObject>) cmp.getAttr("comparators")).iterator();
	
			@SuppressWarnings("unchecked")
			Iterator<PyObject> ops = ((List<PyObject>) cmp.getAttr("ops")).iterator();
	
			while (ops.hasNext()) {
				assert exprs.hasNext();
				CAstNode op = translateOperator(ops.next().getAttr("__class__", PyObject.class).getAttr("__name__", String.class));
				CAstNode rhs = visit(exprs.next(), context);
				CAstNode cmpop = ast.makeNode(CAstNode.BINARY_EXPR, op, ln, rhs);
				expr = expr==null? cmpop: ast.makeNode(CAstNode.ANDOR_EXPR, CAstOperator.OP_REL_AND, cmpop);
			}
			
			return expr;
		}

		public CAstNode visitIf(PyObject ifstmt, WalkContext context) {
			@SuppressWarnings("unchecked")
			List<PyObject> body = (List<PyObject>) ifstmt.getAttr("body");
			@SuppressWarnings("unchecked")
			List<PyObject> orelse = (List<PyObject>) ifstmt.getAttr("orelse");
			return ast.makeNode(CAstNode.IF_STMT, 
					visit(ifstmt.getAttr("test", PyObject.class), context),
					visit(CAstNode.BLOCK_STMT, body, context),
					visit(CAstNode.BLOCK_STMT, orelse, context));
		}

		public CAstNode visitBreak(PyObject brkstmt, WalkContext context) {
			CAstNode gt = ast.makeNode(CAstNode.GOTO);
			context.cfg().map(brkstmt, gt);
			context.cfg().add(brkstmt, context.getBreakFor(null), null);
			return gt;
		}

		public CAstNode visitContinue(PyObject contstmt, WalkContext context) {
			CAstNode gt = ast.makeNode(CAstNode.GOTO);
			context.cfg().map(contstmt, gt);
			context.cfg().add(contstmt, context.getContinueFor(null), null);
			return gt;
		}

		public CAstNode visitBinOp(PyObject binop, WalkContext context) {
			PyObject left= binop.getAttr("left", PyObject.class);
			PyObject right = binop.getAttr("right", PyObject.class);
			return ast.makeNode(CAstNode.BINARY_EXPR, 
					translateOperator(binop.getAttr("op", PyObject.class).getAttr("__class__", PyObject.class).getAttr("__name__", String.class)),
					visit(left, context),
					visit(right, context));
		}
 
		public CAstNode visitImport(PyObject importStmt, WalkContext context) {
			@SuppressWarnings("unchecked")
			List<PyObject> body = (List<PyObject>) importStmt.getAttr("names");		
			return ast.makeNode(CAstNode.BLOCK_STMT, body.stream().map(s -> {
				String importedName = s.getAttr("name", String.class);
				String declName = s.getAttr("asname", String.class);             
				return 
					ast.makeNode(CAstNode.DECL_STMT,
						ast.makeConstant(new CAstSymbolImpl(declName==null? importedName: declName, PythonCAstToIRTranslator.Any)),
						ast.makeNode(
							CAstNode.PRIMITIVE,
	                            ast.makeConstant("import"),
	                            ast.makeConstant(importedName)));
			}).collect(Collectors.toList()));
		}
		
		public CAstNode visitDict(PyObject dict, WalkContext context) {
			List<CAstNode> x = new LinkedList<>();
			x.add(ast.makeNode(CAstNode.NEW, ast.makeConstant("dict")));
			
			@SuppressWarnings("unchecked")
			Iterator<PyObject> keys = ((List<PyObject>) dict.getAttr("keys")).iterator();		
			@SuppressWarnings("unchecked")
			Iterator<PyObject> values = ((List<PyObject>) dict.getAttr("values")).iterator();		
			while(keys.hasNext()) {
				x.add(visit(keys.next(), context));
				x.add(visit(values.next(), context));
			}
			return ast.makeNode(CAstNode.OBJECT_LITERAL, x);
		}

		public CAstNode visitAttribute(PyObject attr, WalkContext context) {
			CAstNode obj = visit(attr.getAttr("value", PyObject.class), context);
			String field = attr.getAttr("attr", String.class);
			return ast.makeNode(CAstNode.OBJECT_REF, obj, ast.makeConstant(field));
		}
		
		public CAstNode visitLambda(PyObject lambda, WalkContext context) {
			return ast.makeNode(CAstNode.EMPTY);
		}
		
		public CAstNode handleList(String type, String field, PyObject list, WalkContext context) {
			List<CAstNode> x = new LinkedList<>();
			x.add(ast.makeNode(CAstNode.NEW, ast.makeConstant(type)));
			
			int n = 0;
			@SuppressWarnings("unchecked")
			Iterator<PyObject> values = ((List<PyObject>) list.getAttr("elts")).iterator();		
			while(values.hasNext()) {
				x.add(ast.makeConstant(n++));
				x.add(visit(values.next(), context));
			}
			return ast.makeNode(CAstNode.OBJECT_LITERAL, x);	
		}

		public CAstNode visitList(PyObject list, WalkContext context) {
			return handleList("list", "elts", list, context);
		}
		
		public CAstNode visitUnaryOp(PyObject unop, WalkContext context) {
			PyObject op = unop.getAttr("operand", PyObject.class);
			return ast.makeNode(CAstNode.UNARY_EXPR, 
					translateOperator(unop.getAttr("op", PyObject.class).getAttr("__class__", PyObject.class).getAttr("__name__", String.class)),
					visit(op, context));
		}

		public CAstNode visitTuple(PyObject tp, WalkContext context) {
			return handleList("tuple", "elts", tp, context);
		}

		/*
		public CAstNode visitFor(PyObject fl, WalkContext context) {
			CAstNode target = visit(fl.getAttr("target", PyObject.class), context);
		}
		*/
		
		public CAstNode visitSubscript(PyObject subscript, WalkContext context) {
			CAstNode obj =  visit(subscript.getAttr("value", PyObject.class), context);
			CAstNode f =  visit(subscript.getAttr("slice", PyObject.class), context);
			return ast.makeNode(CAstNode.OBJECT_REF, obj, f);
		}

		public CAstNode visitPass(PyObject pass, WalkContext context) {
			return ast.makeNode(CAstNode.EMPTY);
		}
	
		public CAstNode visitReturn(PyObject ret, WalkContext context) {
			return ast.makeNode(CAstNode.RETURN, visit(ret.getAttr("value", PyObject.class), context));
		}
		
		public CAstNode visitClassDef(PyObject arg0, WalkContext context) throws Exception {
			return ast.makeNode(CAstNode.EMPTY);
			/*
			WalkContext parent = context;

		      CAstType.Class cls =
		          new CAstType.Class() {
		            @Override
		            public Collection<CAstType> getSupertypes() {
		              Collection<CAstType> supertypes = HashSetFactory.make();
		              for (expr e : arg0.getInternalBases()) {
		                try {
		                  CAstType type = types.getCAstTypeFor(e.getText());
		                  if (type != null) {
		                    supertypes.add(type);
		                  } else {
		                    supertypes.add(getMissingType(e.getText()));
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
		      // TODO: CURRENTLY THIS WILL NOT BE CORRECT FOR EXTENDING CLASSES IMPORTED FROM ANOTHER MODULE
		      types.map(arg0.getInternalName(), cls);

		      Collection<CAstEntity> members = HashSetFactory.make();

		      CAstEntity clse =
		          new AbstractClassEntity(cls) {

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

		            @Override
		            public Position getNamePosition() {
		              return makePosition(arg0.getInternalNameNode());
		            }
		          };

		      WalkContext child =
		          new WalkContext() {
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

		            private WalkContext codeParent() {
		              WalkContext p = parent;
		              while (p.entity().getKind() == CAstEntity.TYPE_ENTITY) {
		                p = p.getParent();
		              }
		              return p;
		            }

		            @Override
		            public CAstControlFlowRecorder cfg() {
		              return (CAstControlFlowRecorder) codeParent().entity().getControlFlow();
		            }

		            @Override
		            public CAstSourcePositionRecorder pos() {
		              return pos;
		            }

		            @Override
		            public CAstNodeTypeMapRecorder getNodeTypeMap() {
		              return codeParent().getNodeTypeMap();
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

		      URL url = this.url();
		      TestVisitor v = new TestVisitor(clse) {
				@Override
				public URL url() {
					return url;
				} };
		      for (stmt e : arg0.getInternalBody()) {
		        if (!(e instanceof Pass)) {
		          e.accept(v);
		        }
		      }

		      CAstNode x = Ast.makeNode(CAstNode.CLASS_STMT, Ast.makeConstant(clse));
		      context.addScopedEntity(x, clse);
		      return x;
		    */
		    }
			
		private Set<String> exposedNames(CAstNode tree) {
			return nm.new Matcher()
	        	.findAll(
	        			new Context() {
	        				@Override
	        				public CAstEntity top() {
	        					return entity;
	        				}

	              @Override
	              public CAstSourcePositionMap getSourceMap() {
	                return entity.getSourceMap();
	              }
	            },
	            tree).stream().map(s -> (String)((CAstNode)s.get("n")).getValue()).collect(Collectors.toSet());
		}
	}
	
	public static IClassHierarchy load(Set<SourceModule> files) throws ClassHierarchyException {
		PythonLoaderFactory loaders = new JepPythonLoaderFactory();

		AnalysisScope scope =
				new AnalysisScope(Collections.singleton(PythonLanguage.Python)) {
			{
				loadersByName.put(PythonTypes.pythonLoaderName, PythonTypes.pythonLoader);
				loadersByName.put(
						SYNTHETIC,
						new ClassLoaderReference(
								SYNTHETIC, PythonLanguage.Python.getName(), PythonTypes.pythonLoader));
			}
		};

		for (SourceModule fn : files) {
			scope.addToScope(PythonTypes.pythonLoader, fn);
		}

		return SeqClassHierarchyFactory.make(scope, loaders);		
	}
	
	// example of using this api
	public static void main(String... args) throws IOException, Error, ClassHierarchyException {
		IRFactory<IMethod> irs = AstIRFactory.makeDefaultFactory();
		
		Set<SourceModule> sources = Arrays.stream(args).map(file -> new SourceFileModule(new File(file), file, null)).collect(Collectors.toSet());
		
		IClassHierarchy cha = load(sources);
		
		cha.forEach(c -> {
			System.err.println(c);
			c.getDeclaredMethods().forEach(m -> {
				System.err.println(m);
				System.err.println(irs.makeIR(m, Everywhere.EVERYWHERE, SSAOptions.defaultOptions()));
			});
		});
		
	}

	@Override
	public <C extends RewriteContext<K>, K extends CopyKey<K>> void addRewriter(CAstRewriterFactory<C, K> factory,
			boolean prepend) {
		// TODO Auto-generated method stub
		
	}

	private String fn;
	private Path pn;
	
	public CPythonAstToCAstTranslator(String fn) {
		this.fn = fn;
		this.pn = Path.of(fn);
	}

	@Override
	public CAstEntity translateToCAst() throws Error, IOException {
		return new PythonScriptEntity(fn, pn);				
	}
}
