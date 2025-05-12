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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.translator.AbstractClassEntity;
import com.ibm.wala.cast.ir.translator.AbstractCodeEntity;
import com.ibm.wala.cast.ir.translator.AbstractFieldEntity;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.jep.Util;
import com.ibm.wala.cast.python.loader.JepPythonLoaderFactory;
import com.ibm.wala.cast.python.loader.PythonLoaderFactory;
import com.ibm.wala.cast.python.parser.AbstractParser;
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
import com.ibm.wala.cast.tree.impl.CAstNodeTypeMapRecorder;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstSourcePositionRecorder;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
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
import com.ibm.wala.ssa.SSAOptions.DefaultValues;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;

import jep.Interpreter;
import jep.python.PyObject;

/**
 * Create a WALA CASst representation using the standard Python ASTs given
 * source code.
 */
public class CPythonAstToCAstTranslator extends AbstractParser implements TranslatorToCAst {

	private static CAstType codeBody = new CAstType() {
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
	 * 
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
	 * 
	 * @param ast a Python AST as a @PyObject
	 * @return JSON form of the AST as tree of @Map objects
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, ?> getJSON(PyObject ast) {
		Interpreter interp = interps.get();

		interp.set("theast", ast);
		interp.exec("thejson = ast2json.ast2json(theast)");

		return (Map<String, ?>) interp.getValue("thejson");

	}

	@SuppressWarnings("unchecked")
	public static Collection<String> properties(PyObject obj) {
		Interpreter interp = interps.get();

		interp.set("obj", obj);
		interp.exec("d = dir(obj)");

		return (Collection<String>) interp.getValue("d");

	}

	@Override
	public <C extends RewriteContext<K>, K extends CopyKey<K>> void addRewriter(CAstRewriterFactory<C, K> factory,
			boolean prepend) {

	}

	private static final CAstPattern nm = CAstPattern.parse("ASSIGN(VAR(<n>*),**)");

	public static final class PythonScriptEntity extends AbstractCodeEntity {
		private final String fn;

		private static CAstType makeType(String fn) {
			return new CAstType.Function() {

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
					return CAstType.DYNAMIC;
				}

				@Override
				public Collection<CAstType> getExceptionTypes() {
					return Collections.singletonList(CAstType.DYNAMIC);
				}

				@Override
				public List<CAstType> getArgumentTypes() {
					return Collections.singletonList(this);
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

			TranslationVisitor x = new TranslationVisitor(this, new CAstTypeDictionaryImpl<String>()) {
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
			return 1;
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
		Scope scope();

		CAstEntity entity();

		List<String> inits();

		WalkContext getParent();
	}

	public static class ScriptContext extends FunctionContext implements WalkContext {
		private final Scope scope = new Scope() {
			@Override
			Scope parent() {
				// no parent here
				return null;
			}
		};

		private final PyObject ast;
		PythonScriptEntity self;

		protected ScriptContext(PyObject s, CAstImpl ast, PythonScriptEntity self) {
			super(null, s, Collections.emptyList());
			this.ast = s;
			this.self = self;
		}

		@Override
		public CAstEntity entity() {
			return self;
		}

		public Scope scope() {
			return scope;
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
			return null;
		}

		@Override
		public CAstNode getCatchTarget(String s) {
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

	private abstract static class Scope {
		abstract Scope parent();

		Set<String> nonLocalNames = HashSetFactory.make();
		Set<String> globalNames = HashSetFactory.make();
		Set<String> localNames = HashSetFactory.make();
	}

	private static class FunctionContext extends TranslatorToCAst.FunctionContext<WalkContext, PyObject>
			implements WalkContext {
		private final Scope scope;
		private CAstEntity entity;

		private final List<String> inits = new ArrayList<>();

		public WalkContext getParent() {
			return (WalkContext) super.getParent();
		}

		public Scope scope() {
			return scope;
		}

		void addInit(String n) {
			inits.add(n);
		}

		@Override
		public List<String> inits() {
			return inits;
		}

		protected FunctionContext(WalkContext parent, PyObject s, List<String> argumentNames) {
			super(parent, s);
			scope = new Scope() {
				@Override
				Scope parent() {
					return parent.scope();
				}
			};
			argumentNames.forEach(nm -> scope.localNames.add(nm));
		}

		@Override
		public CAstEntity entity() {
			return entity;
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
		public List<String> inits() {
			return parent.inits();
		}

		@Override
		public Scope scope() {
			return parent.scope();
		}

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

		@Override
		public CAstEntity entity() {
			return parent.entity();
		}
	}

	public static abstract class TranslationVisitor extends AbstractParser.CAstVisitor
			implements JepAstVisitor<CAstNode, WalkContext> {
		CAst ast = new CAstImpl();
		private int label = 0;
		private final CAstEntity entity;

		private TranslationVisitor(CAstEntity self, CAstTypeDictionaryImpl<String> types) {
			this.types = types;
			entity = self;
		}

		@Override
		public URL url() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected CAstNode notePosition(CAstNode makeNode, Position noInformation) {
			return makeNode;
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
			CAstNode bodyAst = ast.makeNode(CAstNode.BLOCK_STMT,
					body.stream().map(f -> visit(f, context)).collect(Collectors.toList()));

			List<CAstNode> defaults = new LinkedList<>();
			defaultImports(defaults);

			bodyAst = ast.makeNode(CAstNode.BLOCK_STMT, ast.makeNode(CAstNode.BLOCK_STMT, defaults), bodyAst);

			Set<String> exposedNames = exposedNames(bodyAst);
			if (exposedNames.size() > 0)
				return ast.makeNode(CAstNode.UNWIND, bodyAst,
						ast.makeNode(CAstNode.BLOCK_STMT,
								exposedNames.stream().map(n -> ast.makeNode(CAstNode.ASSIGN,
										ast.makeNode(CAstNode.OBJECT_REF, ast.makeNode(CAstNode.THIS),
												ast.makeConstant(n)),
										ast.makeNode(CAstNode.VAR, ast.makeConstant(n))))
										.collect(Collectors.toList())));
			else
				return bodyAst;
		}

		public CAstNode visitFunctionDef(PyObject o, WalkContext context) {
			CAstNode fe = doFunction(o.getAttr("args"), o, o.getAttr("name", String.class), context);
			Object x = fe.getChild(0).getValue();
			CAstEntity fun = (CAstEntity) (x != null ? x : fe.getChild(1).getChild(0).getValue());
			return ast.makeNode(CAstNode.DECL_STMT,
					ast.makeConstant(new CAstSymbolImpl(o.getAttr("name", String.class), fun.getType())), fe);
		}

		public CAstNode doFunction(Object rawArgs, PyObject o, String functionName, WalkContext context) {
			List<PyObject> arguments = extractArguments(rawArgs);
			return doFunction(o, functionName, context, arguments, getDefaults(o, functionName, context), getCode(o),
					getArgumentNames(arguments));
		}

		public CAstNode doFunction(PyObject o, String functionName, WalkContext context, List<PyObject> arguments,
				CAstNode[] defaultCode, List<PyObject> code, List<String> argumentNames) {
			int argumentCount = arguments.size();

			List<CAstType> argumentTypes = Collections.nCopies(argumentCount + 1, CAstType.DYNAMIC);

			class PythonCodeType implements CAstType.Function {

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
			}
			;

			CAstType.Function funType;
			if (context.entity() != null && context.entity().getKind() == CAstEntity.TYPE_ENTITY) {
				class PythonMethodType extends PythonCodeType implements CAstType.Method {
					@Override
					public CAstType getDeclaringType() {
						return context.entity().getType();
					}

					@Override
					public boolean isStatic() {
						return false;
					}
				}

				funType = new PythonMethodType();
			} else {
				funType = new PythonCodeType();
			}

			class FunctionEntity extends AbstractCodeEntity {
				private CAstNode ast;

				private FunctionEntity(CAstType type) {
					super(type);
				}

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
					List<String> names = arguments.stream()
							.map(a -> Util.typeName(a).equals("Name") ? a.getAttr("id", String.class)
									: (String) a.getAttr("arg"))
							.collect(Collectors.toList());
					names.add(0, "the function");
					return names.toArray(new String[names.size()]);
				}

				@Override
				public CAstNode[] getArgumentDefaults() {
					return defaultCode;
				}

				@Override
				public int getArgumentCount() {
					return argumentCount + 1;
				}

				@Override
				public CAstNode getAST() {
					return ast;
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

				@Override
				public Collection<CAstQualifier> getQualifiers() {
					return Collections.emptySet();
				}
			}
			;

			FunctionEntity fun = new FunctionEntity(funType);

			WalkContext fc = new FunctionContext(context, o, argumentNames) {

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

			};

			CAstNode b1 = visit(CAstNode.BLOCK_STMT, code, fc);
			CAstNode body = ast.makeNode(CAstNode.LOCAL_SCOPE,
					ast.makeNode(CAstNode.BLOCK_STMT,
							ast.makeNode(CAstNode.BLOCK_STMT,
									fc.inits().stream()
											.map(n -> ast.makeNode(CAstNode.DECL_STMT,
													ast.makeConstant(new CAstSymbolImpl(n, CAstType.DYNAMIC))))
											.collect(Collectors.toList())),
							b1));

			if (funType instanceof CAstType.Method) {
				body = ast.makeNode(CAstNode.BLOCK_STMT,
						ast.makeNode(CAstNode.DECL_STMT,
								ast.makeConstant(new CAstSymbolImpl("super", PythonCAstToIRTranslator.Any)),
								ast.makeNode(CAstNode.NEW, ast.makeConstant("superfun"))),
						ast.makeNode(CAstNode.BLOCK_STMT, ast.makeNode(CAstNode.ASSIGN,
								ast.makeNode(CAstNode.OBJECT_REF, ast.makeNode(CAstNode.VAR, Ast.makeConstant("super")),
										ast.makeConstant("$class")),
								ast.makeNode(CAstNode.VAR, ast.makeConstant(context.entity().getType().getName()))),
								ast.makeNode(CAstNode.ASSIGN,
										ast.makeNode(CAstNode.OBJECT_REF,
												ast.makeNode(CAstNode.VAR, Ast.makeConstant("super")),
												ast.makeConstant("$self")),
										ast.makeNode(CAstNode.VAR, Ast.makeConstant(fun.getArgumentNames()[1])))),
						body);
			}

			fun.ast = body;

			CAstNode fe = ast.makeNode(CAstNode.FUNCTION_EXPR, ast.makeConstant(fun));
			context.addScopedEntity(fe, fun);

			if (defaultCode.length == 0) {
				return fe;
			} else {
				return ast.makeNode(CAstNode.BLOCK_EXPR, ast.makeNode(CAstNode.BLOCK_EXPR, defaultCode), fe);
			}
		}

		private List<String> getArgumentNames(List<PyObject> arguments) {
			return arguments.stream().map(a -> (String) a.getAttr("arg")).collect(Collectors.toList());
		}

		@SuppressWarnings("unchecked")
		private List<PyObject> getCode(PyObject o) {
			List<PyObject> code;
			if (o.getAttr("body") instanceof List) {
				code = ((List<PyObject>) o.getAttr("body")).stream().collect(Collectors.toList());
			} else {
				code = Collections.singletonList(o.getAttr("body", PyObject.class));
			}
			return code;
		}

		@SuppressWarnings("unchecked")
		private CAstNode[] getDefaults(PyObject o, String functionName, WalkContext context) {
			Object rawDefaults = o.getAttr("args", PyObject.class).getAttr("defaults");
			List<PyObject> defaults;
			if (rawDefaults instanceof List) {
				defaults = (List<PyObject>) rawDefaults;
			} else {
				defaults = Collections.singletonList((PyObject) rawDefaults);
			}

			CAstNode[] defaultVars;
			CAstNode[] defaultCode;
			if (defaults != null && defaults.size() > 0) {
				int arg = 0;
				defaultVars = new CAstNode[defaults.size()];
				defaultCode = new CAstNode[defaults.size()];
				for (PyObject dflt : defaults) {
					String name = functionName + "_default_" + arg;
					context.scope().globalNames.add(name);
					defaultCode[arg] = ast.makeNode(CAstNode.ASSIGN, ast.makeNode(CAstNode.VAR, Ast.makeConstant(name)),
							visit(dflt, context));
					defaultVars[arg++] = Ast.makeNode(CAstNode.VAR, Ast.makeConstant(name));
				}
			} else {
				defaultVars = defaultCode = new CAstNode[0];
			}
			return defaultCode;
		}

		@SuppressWarnings("unchecked")
		private List<PyObject> extractArguments(Object rawArgs) {
			List<PyObject> arguments;
			if (rawArgs instanceof List) {
				arguments = (List<PyObject>) rawArgs;
			} else if (rawArgs instanceof PyObject) {
				arguments = ((List<PyObject>) ((PyObject) rawArgs).getAttr("args"));
			} else {
				arguments = null;
				assert false;
			}
			return arguments;
		}

		public CAstNode visitName(PyObject o, WalkContext context) {
			String nm = (String) o.getAttr("id");

			if (!context.scope().localNames.contains(nm)) {
				context.scope().nonLocalNames.add(nm);
			}

			return ast.makeNode(CAstNode.VAR, ast.makeConstant(nm));
		}

		public CAstNode visitConstant(PyObject o, WalkContext context) {
			Object nm = o.getAttr("value");
			return ast.makeConstant(nm);
		}

		public CAstNode visitNamedExpr(PyObject o, WalkContext context) {
			PyObject rhs = o.getAttr("value", PyObject.class);
			PyObject lhs = o.getAttr("target", PyObject.class);
			Scope scope = context.scope();
			if (!scope.globalNames.contains(lhs.toString())
					           && 
			    !scope.nonLocalNames.contains(lhs.toString()) && !context.inits().contains(lhs.toString())) 
			{
				context.inits().add(lhs.toString());
				scope.localNames.add(lhs.toString());
			}
			
			return ast.makeNode(CAstNode.ASSIGN, visit(lhs, context), visit(rhs, context));
		}
		
		public CAstNode visitAssign(PyObject o, WalkContext context) {
			@SuppressWarnings("unchecked")
			List<PyObject> body = (List<PyObject>) o.getAttr("targets");
			PyObject value = (PyObject) o.getAttr("value");
			CAstNode rhs = visit(value, context);
			boolean isClass = context.entity() != null && context.entity().getKind() == CAstEntity.TYPE_ENTITY;
			if (isClass) {
				for (PyObject lhs : body) {
					context.addScopedEntity(null, new AbstractFieldEntity(lhs.getAttr("id", String.class),
							Collections.emptySet(), false, context.entity()) {
						@Override
						public CAstNode getAST() {
							return rhs;
						}

						@Override
						public Position getPosition(int arg) {
							return null;
						}

						@Override
						public Position getNamePosition() {
							return null;
						}
					});
				}
				return ast.makeNode(CAstNode.EMPTY);
			} else {
				return ast.makeNode(CAstNode.BLOCK_STMT, body.stream().map(f -> {
					Scope scope = context.scope();
					if (!isClass && !scope.globalNames.contains(f.toString())
							&& !scope.nonLocalNames.contains(f.toString()) && !context.inits().contains(f.toString())) {
						context.inits().add(f.toString());
						scope.localNames.add(f.toString());
					}
					return ast.makeNode(CAstNode.ASSIGN, visit(f, context), rhs);
				}).collect(Collectors.toList()));
			}
		}

		private CAstNode visit(int node, List<PyObject> l, WalkContext context) {
			return ast.makeNode(node, l.stream().map(f -> visit(f, context)).collect(Collectors.toList()));
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
			for (PyObject k : keywords) {
				ak.add(ast.makeNode(CAstNode.ARRAY_LITERAL, ast.makeConstant(k.getAttr("arg", String.class)),
						visit(k.getAttr("value", PyObject.class), context)));
			}

			CAstNode call = ast.makeNode(CAstNode.CALL, ak);
			context.cfg().map(call, call);

			if (context.getCatchTarget("Exception") != null) {
				context.cfg().add(call, context.getCatchTarget("Exception"), "Exception");
			}

			return call;

		}

		public CAstNode visitImportFrom(PyObject o, WalkContext context) {
			String module = (String) o.getAttr("module");
			CAstNode mod = ast.makeNode(CAstNode.DECL_STMT,
					ast.makeConstant(new CAstSymbolImpl(module, PythonCAstToIRTranslator.Any)),
					ast.makeNode(CAstNode.PRIMITIVE, ast.makeConstant("import"), ast.makeConstant(module)));
			@SuppressWarnings("unchecked")
			List<PyObject> alias = (List<PyObject>) o.getAttr("names");
			return ast.makeNode(CAstNode.BLOCK_STMT, mod,
					ast.makeNode(CAstNode.BLOCK_STMT, alias.stream()
							.map(a -> ast.makeNode(CAstNode.ASSIGN,
									ast.makeNode(CAstNode.VAR, ast.makeConstant(a.getAttr("name"))),
									ast.makeNode(CAstNode.OBJECT_REF,
											ast.makeNode(CAstNode.VAR, ast.makeConstant(module)),
											ast.makeConstant(a.getAttr("name")))))
							.collect(Collectors.toList())));
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
				CAstNode cn = ast.makeNode(CAstNode.LABEL_STMT, ast.makeConstant("label_" + label++),
						visit(contLabel, context));
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
				CAstNode bn = ast.makeNode(CAstNode.LABEL_STMT, ast.makeConstant("label_" + label++),
						visit(breakLabel, context));
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
				CAstNode op = translateOperator(
						ops.next().getAttr("__class__", PyObject.class).getAttr("__name__", String.class));
				CAstNode rhs = visit(exprs.next(), context);
				CAstNode cmpop = ast.makeNode(CAstNode.BINARY_EXPR, op, ln, rhs);
				expr = expr == null ? cmpop : ast.makeNode(CAstNode.IF_EXPR, cmpop, expr, ast.makeConstant(false));
			}

			return expr;
		}

		public CAstNode visitIf(PyObject ifstmt, WalkContext context) {
			@SuppressWarnings("unchecked")
			List<PyObject> body = (List<PyObject>) ifstmt.getAttr("body");
			@SuppressWarnings("unchecked")
			List<PyObject> orelse = (List<PyObject>) ifstmt.getAttr("orelse");
			return ast.makeNode(CAstNode.IF_STMT, visit(ifstmt.getAttr("test", PyObject.class), context),
					visit(CAstNode.BLOCK_STMT, body, context), visit(CAstNode.BLOCK_STMT, orelse, context));
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
			PyObject left = binop.getAttr("left", PyObject.class);
			PyObject right = binop.getAttr("right", PyObject.class);
			return ast.makeNode(
					CAstNode.BINARY_EXPR, translateOperator(binop.getAttr("op", PyObject.class)
							.getAttr("__class__", PyObject.class).getAttr("__name__", String.class)),
					visit(left, context), visit(right, context));
		}

		public CAstNode visitImport(PyObject importStmt, WalkContext context) {
			@SuppressWarnings("unchecked")
			List<PyObject> body = (List<PyObject>) importStmt.getAttr("names");
			return ast.makeNode(CAstNode.BLOCK_STMT, body.stream().map(s -> {
				String importedName = s.getAttr("name", String.class);
				String declName = s.getAttr("asname", String.class);
				return ast.makeNode(CAstNode.DECL_STMT,
						ast.makeConstant(new CAstSymbolImpl(declName == null ? importedName : declName,
								PythonCAstToIRTranslator.Any)),
						ast.makeNode(CAstNode.PRIMITIVE, ast.makeConstant("import"), ast.makeConstant(importedName)));
			}).collect(Collectors.toList()));
		}

		public CAstNode visitDict(PyObject dict, WalkContext context) {
			List<CAstNode> x = new LinkedList<>();
			x.add(ast.makeNode(CAstNode.NEW, ast.makeConstant("dict")));

			@SuppressWarnings("unchecked")
			Iterator<PyObject> keys = ((List<PyObject>) dict.getAttr("keys")).iterator();
			@SuppressWarnings("unchecked")
			Iterator<PyObject> values = ((List<PyObject>) dict.getAttr("values")).iterator();
			while (keys.hasNext()) {
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

		private int lambdaCount = 1;

		public CAstNode visitLambda(PyObject lambda, WalkContext context) {
			return doFunction(lambda.getAttr("args"), lambda, "lambda" + lambdaCount++, context);
		}

		public CAstNode handleList(String type, String field, PyObject list, WalkContext context) {
			List<CAstNode> x = new LinkedList<>();
			x.add(ast.makeNode(CAstNode.NEW, ast.makeConstant(type)));

			int n = 0;
			@SuppressWarnings("unchecked")
			Iterator<PyObject> values = ((List<PyObject>) list.getAttr("elts")).iterator();
			while (values.hasNext()) {
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
			return ast.makeNode(
					CAstNode.UNARY_EXPR, translateOperator(unop.getAttr("op", PyObject.class)
							.getAttr("__class__", PyObject.class).getAttr("__name__", String.class)),
					visit(op, context));
		}

		public CAstNode visitTuple(PyObject tp, WalkContext context) {
			return handleList("tuple", "elts", tp, context);
		}

		/*
		 * public CAstNode visitFor(PyObject fl, WalkContext context) { CAstNode target
		 * = visit(fl.getAttr("target", PyObject.class), context); }
		 */

		public CAstNode visitSubscript(PyObject subscript, WalkContext context) {
			CAstNode obj = visit(subscript.getAttr("value", PyObject.class), context);
			CAstNode f = visit(subscript.getAttr("slice", PyObject.class), context);
			return ast.makeNode(CAstNode.OBJECT_REF, obj, f);
		}

		public CAstNode visitPass(PyObject pass, WalkContext context) {
			return ast.makeNode(CAstNode.EMPTY);
		}

		public CAstNode visitReturn(PyObject ret, WalkContext context) {
			return ast.makeNode(CAstNode.RETURN, visit(ret.getAttr("value", PyObject.class), context));
		}

		public CAstNode visitGlobal(PyObject global, WalkContext context) {
			Scope s = context.scope();
			@SuppressWarnings("unchecked")
			List<String> names = (List<String>) global.getAttr("names");
			s.globalNames.addAll(names);
			return ast.makeNode(CAstNode.EMPTY);
		}

		public CAstNode visitNonLocal(PyObject nonlocal, WalkContext context) {
			Scope s = context.scope();
			@SuppressWarnings("unchecked")
			List<String> names = (List<String>) nonlocal.getAttr("names");
			s.nonLocalNames.addAll(names);
			return ast.makeNode(CAstNode.EMPTY);
		}

		private final CAstTypeDictionaryImpl<String> types;

		private final Map<String, CAstType> missingTypes = HashMapFactory.make();

		protected CAstType getMissingType(String name) {
			if (!missingTypes.containsKey(name)) {
				missingTypes.put(name, new MissingType() {

					@Override
					public String getName() {
						return name;
					}

					@Override
					public Collection<CAstType> getSupertypes() {
						return Collections.emptySet();
					}
				});
			}

			return missingTypes.get(name);
		}

		public CAstNode visitClassDef(PyObject arg0, WalkContext context) throws Exception {
			WalkContext parent = context;

			CAstType.Class cls = new CAstType.Class() {
				@Override
				public Collection<CAstType> getSupertypes() {
					Collection<CAstType> supertypes = HashSetFactory.make();
					@SuppressWarnings("unchecked")
					List<PyObject> bases = (List<PyObject>) arg0.getAttr("bases");
					for (PyObject e : bases) {
						try {
							CAstType type = types.getCAstTypeFor(e.getAttr("id", String.class));
							if (type != null) {
								supertypes.add(type);
							} else {
								supertypes.add(getMissingType(e.getAttr("id", String.class)));
							}
						} catch (Exception e1) {
							assert false : e1;
						}
					}
					return supertypes;
				}

				@Override
				public String getName() {
					return arg0.getAttr("name", String.class);
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
			// TODO: CURRENTLY THIS WILL NOT BE CORRECT FOR EXTENDING CLASSES IMPORTED FROM
			// ANOTHER MODULE
			types.map(cls.getName(), cls);

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
					return null;
				}

				@Override
				public Position getNamePosition() {
					return null;
				}
			};

			Scope classScope = new Scope() {
				@Override
				Scope parent() {
					return context.scope();
				}

			};

			WalkContext child = new WalkContext() {
				private final CAstSourcePositionRecorder pos = new CAstSourcePositionRecorder();

				@Override
				public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
					return Collections.singletonMap(null, members);
				}

				@Override
				public PyObject top() {
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
				public PyObject getContinueFor(String label) {
					assert false;
					return null;
				}

				@Override
				public PyObject getBreakFor(String label) {
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

				@Override
				public Scope scope() {
					return classScope;
				}

				private List<String> inits = new ArrayList<>();

				@Override
				public List<String> inits() {
					return inits;
				}
			};

			URL url = this.url();
			TranslationVisitor v = new TranslationVisitor(clse, new CAstTypeDictionaryImpl<String>()) {
				@Override
				public URL url() {
					return url;
				}
			};

			List<CAstNode> elts = new ArrayList<>();
			@SuppressWarnings("unchecked")
			List<PyObject> body = (List<PyObject>) arg0.getAttr("body");
			for (PyObject e : body) {
				elts.add(v.visit(e, child));
			}

			CAstNode x = ast.makeNode(CAstNode.CLASS_STMT, ast.makeConstant(clse));
			context.addScopedEntity(x, clse);
			return x;
		}

		private Set<String> exposedNames(CAstNode tree) {
			return nm.new Matcher().findAll(new Context() {
				@Override
				public CAstEntity top() {
					return entity;
				}

				@Override
				public CAstSourcePositionMap getSourceMap() {
					return entity.getSourceMap();
				}
			}, tree).stream().map(s -> (String) ((CAstNode) s.get("n")).getValue()).collect(Collectors.toSet());
		}

		private class TryCatchContext extends TranslatorToCAst.TryCatchContext<WalkContext, PyObject>
				implements WalkContext {

			private TryCatchContext(WalkContext parent, Map<String, CAstNode> catchNode) {
				super(parent, catchNode);
			}

			@Override
			public WalkContext getParent() {
				return (WalkContext) super.getParent();
			}

			@Override
			public Scope scope() {
				return getParent().scope();
			}

			@Override
			public CAstEntity entity() {
				return getParent().entity();
			}

			@Override
			public List<String> inits() {
				return getParent().inits();
			}
		}

		public CAstNode visitAssert(PyObject asrt, WalkContext context) throws Exception {
			return ast.makeNode(CAstNode.ASSERT, visit(asrt.getAttr("test", PyObject.class), context));
		}

		@SuppressWarnings("unchecked")
		public CAstNode visitTry(PyObject tryNode, WalkContext context) throws Exception {
			WalkContext cc = context;
			Map<String, CAstNode> catches = new LinkedHashMap<>();
			if (tryNode.getAttr("handlers") != null) {
				((List<PyObject>) tryNode.getAttr("handlers", List.class)).stream().forEach(c -> {
					CAstNode catchBlock = ast.makeNode(CAstNode.CATCH,
							ast.makeConstant(c.getAttr("name") == null ? "$dummy" : c.getAttr("name", String.class)),
							visit(CAstNode.BLOCK_STMT, c.getAttr("body", List.class), context));
					context.cfg().map(catchBlock, catchBlock);
					catches.put(c.getAttr("type") != null ? c.getAttr("type", String.class) : "Exception", catchBlock);
				});

				cc = new TryCatchContext(context, catches);
			}

			CAstNode body = visit(CAstNode.BLOCK_STMT, (List<PyObject>) tryNode.getAttr("body", List.class), cc);

			if (tryNode.getAttr("orelse") != null) {
				body = ast.makeNode(CAstNode.BLOCK_STMT, body,
						visit(CAstNode.BLOCK_STMT, (List<PyObject>) tryNode.getAttr("orelse", List.class), context));
			}

			body = ast.makeNode(CAstNode.TRY, body, catches.values().toArray(new CAstNode[catches.size()]));

			if (tryNode.getAttr("finalbody") != null) {
				body = ast.makeNode(CAstNode.UNWIND, body,
						visit(CAstNode.BLOCK_STMT, (List<PyObject>) tryNode.getAttr("finalbody", List.class), context));
			}

			return body;
		}

		private int tmpIndex = 0;

		interface Comprehension {
			PyObject target();

			PyObject iter();

			List<PyObject> ifs();
		}

		@SuppressWarnings("unchecked")
		public CAstNode visitFor(PyObject tryNode, WalkContext context) throws Exception {
			PyObject target = tryNode.getAttr("target", PyObject.class);
			PyObject iter = tryNode.getAttr("iter", PyObject.class);
			java.util.List<PyObject> internalBody = tryNode.getAttr("body", List.class);

			return handleFor(target, iter, internalBody, context);

		}

		private CAstNode handleFor(PyObject target, PyObject iter, List<PyObject> internalBody, WalkContext context)
				throws Exception {
			PyObject contLabel = runit("ast.Pass()");
			PyObject breakLabel = runit("ast.Pass()");
			LoopContext x = new LoopContext(context, breakLabel, contLabel);

			int i = 0;
			CAstNode[] body = new CAstNode[internalBody.size()];
			for (PyObject s : internalBody) {
				body[i++] = visit(s, x);
			}

			CAstNode breakStmt = x.broke ? visit(breakLabel, context) : ast.makeNode(CAstNode.EMPTY);
			if (x.broke) {
				context.cfg().map(breakLabel, breakStmt);
			}

			CAstNode continueStmt = x.continued ? visit(contLabel, context) : ast.makeNode(CAstNode.EMPTY);
			if (x.continued) {
				context.cfg().map(contLabel, continueStmt);
			}

			Comprehension g = new Comprehension() {
				public PyObject target() {
					return target;
				}

				public PyObject iter() {
					return iter;
				}

				public List<PyObject> ifs() {
					return Collections.emptyList();
				}
			};

			return ast.makeNode(CAstNode.BLOCK_EXPR, 
				doGenerators(
					Collections.singletonList(g),
					ast.makeNode(CAstNode.BLOCK_EXPR, Ast.makeNode(CAstNode.BLOCK_EXPR, body), continueStmt), context),
				breakStmt);
		}

		private CAstNode doGenerators(
				List<com.ibm.wala.cast.python.jep.ast.CPythonAstToCAstTranslator.TranslationVisitor.Comprehension> generators,
				CAstNode body, 
				WalkContext context) {
			CAstNode result = body;

			for (com.ibm.wala.cast.python.jep.ast.CPythonAstToCAstTranslator.TranslationVisitor.Comprehension c : generators) {
				int j = c.ifs().size();
				if (j > 0) {
					for (PyObject test : c.ifs()) {
						CAstNode v = visit(test, context);
						result = ast.makeNode(CAstNode.IF_STMT, v, result);
					}
				}

				String tempName = "temp " + ++tmpIndex;
				String tempName2 = "temp " + ++tmpIndex;

				CAstNode test = ast.makeNode(CAstNode.BINARY_EXPR, CAstOperator.OP_NE, ast.makeConstant(null),
						ast.makeNode(CAstNode.BLOCK_EXPR,
								ast.makeNode(CAstNode.ASSIGN, ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName2)),
										ast.makeNode(CAstNode.EACH_ELEMENT_GET,
												ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName)),
												ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName2))))));

				CAstNode x = visit(c.target(), context);
				if (x.getKind() == CAstNode.VAR) {
					String nm = (String) x.getChild(0).getValue();
					context.scope().localNames.add(nm);
					context.inits().add(nm);
				}
				
				result = notePosition(
						ast.makeNode(CAstNode.BLOCK_EXPR,
								ast.makeNode(CAstNode.DECL_STMT,
										ast.makeConstant(new CAstSymbolImpl(tempName, PythonCAstToIRTranslator.Any)),
										visit(c.iter(), context)),
								ast.makeNode(CAstNode.DECL_STMT,
										ast.makeConstant(new CAstSymbolImpl(tempName2, PythonCAstToIRTranslator.Any))),
								ast.makeNode(CAstNode.ASSIGN, ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName2)),
										ast.makeNode(CAstNode.EACH_ELEMENT_GET,
												ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName)),
												ast.makeConstant(null))),
								ast.makeNode(CAstNode.LOOP, test,
										ast.makeNode(CAstNode.BLOCK_EXPR,
												ast.makeNode(CAstNode.ASSIGN,
													visit(c.target(), context),	
													ast.makeNode(CAstNode.PRIMITIVE,
														ast.makeConstant("forElementGet"),
														ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName)),
														ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName2)))),
												result,
												ast.makeNode(CAstNode.ASSIGN, 
														ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName2)),
														ast.makeNode(CAstNode.BINARY_EXPR, 
																CAstOperator.OP_ADD,
																ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName2)),
																ast.makeConstant(1)))))),
						context.pos().getPosition(body));
			}

			return ast.makeNode(CAstNode.LOCAL_SCOPE, result);
		}

		private CAstType filter = new CAstType() {

			@Override
			public String getName() {
				return "filter";
			}

			@Override
			public Collection<CAstType> getSupertypes() {
				return Collections.singleton(codeBody);
			}
		};

		@SuppressWarnings("unchecked")
		private void getComprehensionArguments(PyObject arg, List<PyObject> arguments) {
			if ("Tuple".equals(Util.typeName(arg))) {
				((List<PyObject>) arg.getAttr("elts", List.class)).forEach(elt -> {
					getComprehensionArguments(elt, arguments);
				});
			} else {
				arguments.add(arg);
			}
		}

		private CAstNode visitComp(PyObject key, PyObject value, List<Comprehension> gen, TypeReference type, WalkContext context)
				throws Exception {
			CAstNode objDecl = ast.makeNode(CAstNode.DECL_STMT,
					ast.makeConstant(new CAstSymbolImpl("__collection__", CAstType.DYNAMIC)), 
					ast.makeNode(CAstNode.NEW, ast.makeConstant(type.getName().toString().substring(1))));
			CAstNode idxDecl = ast.makeNode(CAstNode.DECL_STMT,
					ast.makeConstant(new CAstSymbolImpl("__idx__", CAstType.DYNAMIC)), 
					ast.makeConstant(0));
			CAstNode body = ast.makeNode(CAstNode.ASSIGN,
					ast.makeNode(CAstNode.OBJECT_REF,
						ast.makeNode(CAstNode.VAR, ast.makeConstant("__collection__")),
						key == null? ast.makeNode(CAstNode.VAR, ast.makeConstant("__idx__")): visit(key, context)),						
					visit(value, context));
			return ast.makeNode(CAstNode.BLOCK_EXPR,
					objDecl,
					idxDecl,
					doGenerators(gen, body, context),
					ast.makeNode(CAstNode.VAR, ast.makeConstant("__collection__")));
		}

		public CAstNode visitDictComp(PyObject lc, WalkContext context) throws Exception {
			List<Comprehension> cs = toComprehensions(lc);
			return visitComp(lc.getAttr("key", PyObject.class), lc.getAttr("value", PyObject.class), cs, PythonTypes.dict, context);
		}

		public CAstNode visitListComp(PyObject lc, WalkContext context) throws Exception {
			List<Comprehension> cs = toComprehensions(lc);
			return visitComp(null, lc.getAttr("elt", PyObject.class), cs, PythonTypes.list, context);
		}

		public CAstNode visitSetComp(PyObject sc, WalkContext context) throws Exception {
			List<Comprehension> cs = toComprehensions(sc);
			return visitComp(null, sc.getAttr("elt", PyObject.class), cs, PythonTypes.set, context);
		}

		private List<Comprehension> toComprehensions(PyObject lc) {
			@SuppressWarnings("unchecked")
			List<PyObject> gen = lc.getAttr("generators", List.class);
			List<Comprehension> cs = gen.stream().map(g -> new Comprehension() {

				@Override
				public PyObject target() {
					return g.getAttr("target", PyObject.class);
				}

				@Override
				public PyObject iter() {
					return g.getAttr("iter", PyObject.class);
				}

				@SuppressWarnings("unchecked")
				@Override
				public List<PyObject> ifs() {
					return g.getAttr("ifs", List.class);
				}
			}).collect(Collectors.toList());
			return cs;
		}

	}

	public static IClassHierarchy load(Set<SourceModule> files) throws ClassHierarchyException {
		PythonLoaderFactory loaders = new JepPythonLoaderFactory(Collections.emptyList());

		AnalysisScope scope = new AnalysisScope(Collections.singleton(PythonLanguage.Python)) {
			{
				loadersByName.put(PythonTypes.pythonLoaderName, PythonTypes.pythonLoader);
				loadersByName.put(SYNTHETIC,
						new ClassLoaderReference(SYNTHETIC, PythonLanguage.Python.getName(), PythonTypes.pythonLoader));
			}
		};

		for (SourceModule fn : files) {
			scope.addToScope(PythonTypes.pythonLoader, fn);
		}

		return SeqClassHierarchyFactory.make(scope, loaders);
	}

	// example of using this implementation
	public static void main(String... args) throws IOException, Error, ClassHierarchyException {
		IRFactory<IMethod> irs = AstIRFactory.makeDefaultFactory();

		Set<SourceModule> sources = Arrays.stream(args).map(file -> new SourceFileModule(new File(file), file, null))
				.collect(Collectors.toSet());

		IClassHierarchy cha = load(sources);

		SSAOptions.defaultOptions().setDefaultValues(new DefaultValues() {
			@Override
			public int getDefaultValue(SymbolTable symtab, int valueNumber) {
				return symtab.getNullConstant();
			}
		});

		cha.forEach(c -> {
			System.err.println(c);
			c.getDeclaredMethods().forEach(m -> {
				System.err.println(m);
				System.err.println(irs.makeIR(m, Everywhere.EVERYWHERE, SSAOptions.defaultOptions()));
			});
		});
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
