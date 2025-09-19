package com.ibm.wala.cast.python.jep.ast;

import static com.ibm.wala.cast.python.jep.Util.fixForCompilation;
import static com.ibm.wala.cast.python.jep.Util.has;
import static com.ibm.wala.cast.python.jep.Util.runit;
import static com.ibm.wala.cast.python.jep.Util.typeName;

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
import jep.python.PyObject;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/** Create a WALA CASst representation using the standard Python ASTs given source code. */
public class CPythonAstToCAstTranslator extends AbstractParser implements TranslatorToCAst {

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

  private static CAstType asyncCodeBody =
      new CAstType() {
        @Override
        public String getName() {
          return "AsyncCodeBody";
        }

        @Override
        public Collection<CAstType> getSupertypes() {
          return Collections.singleton(codeBody);
        }
      };

  private static CAstType methodBody =
      new CAstType() {
        @Override
        public String getName() {
          return "MethodBody";
        }

        @Override
        public Collection<CAstType> getSupertypes() {
          return Collections.singleton(codeBody);
        }
      };

  private static CAstType asyncMethodBody =
      new CAstType() {
        @Override
        public String getName() {
          return "AsyncMethodBody";
        }

        @Override
        public Collection<CAstType> getSupertypes() {
          return Collections.singleton(methodBody);
        }
      };

  private static CAstType baseException =
      new CAstType() {
        @Override
        public String getName() {
          return "BaseException";
        }

        @Override
        public Collection<CAstType> getSupertypes() {
          return Collections.singleton(PythonCAstToIRTranslator.Exception);
        }
      };

  private static CAstType lambda =
      new CAstType() {
        @Override
        public String getName() {
          return "lambda";
        }

        @Override
        public Collection<CAstType> getSupertypes() {
          return Collections.singleton(codeBody);
        }
      };

  private static CAstType lambdaMethod =
      new CAstType() {
        @Override
        public String getName() {
          return "lambdaMethod";
        }

        @Override
        public Collection<CAstType> getSupertypes() {
          return Collections.singleton(methodBody);
        }
      };

  private static CAstTypeDictionaryImpl<String> initTypeDictionary() {
    CAstTypeDictionaryImpl<String> types = new CAstTypeDictionaryImpl<String>();
    types.map("Exception", PythonCAstToIRTranslator.Exception);
    types.map("BaseException", baseException);
    return types;
  }

  @Override
  public <C extends RewriteContext<K>, K extends CopyKey<K>> void addRewriter(
      CAstRewriterFactory<C, K> factory, boolean prepend) {}

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
      PyObject ast = Util.getAST(scriptCode);

      TranslationVisitor x =
          new TranslationVisitor(this, initTypeDictionary()) {
            @SuppressWarnings("deprecation")
            @Override
            public URL url() {
              try {
                return new URL("file:" + fn);
              } catch (MalformedURLException e) {
                return null;
              }
            }
          };

      class ScriptContext extends FunctionContext implements WalkContext {
        private final Scope scope =
            new Scope() {
              @Override
              Scope parent() {
                // no parent here
                return null;
              }
            };

        private final PyObject ast;
        PythonScriptEntity self;

        protected ScriptContext(PyObject s, CAstImpl ast, PythonScriptEntity self) {
          super(null, s, Collections.emptyList(), false);
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
      return new String[] {"self"};
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
    default boolean isAsync() {
      return getParent().isAsync();
    }

    default boolean isGenerator() {
      return getParent().isGenerator();
    }

    default boolean isGenerator(boolean v) {
      return getParent().isGenerator(v);
    }

    default boolean isFunctionScope() {
      return getParent().isFunctionScope();
    }

    default CAstType classSelfVar() {
      return getParent().classSelfVar();
    }

    default WalkContext codeParent() {
      return getParent().codeParent();
    }

    WalkContext getParent();

    default Scope scope() {
      return getParent().scope();
    }

    default CAstEntity entity() {
      return getParent().entity();
    }

    default List<String> inits() {
      return getParent().inits();
    }

    default CAstNode matchVar() {
      return getParent().matchVar();
    }

    default Set<String> matchDeclNames() {
      return getParent().matchDeclNames();
    }
  }

  private abstract static class Scope {
    abstract Scope parent();

    Set<String> nonLocalNames = HashSetFactory.make();
    Set<String> globalNames = HashSetFactory.make();
    Set<String> localNames = HashSetFactory.make();
  }

  private static class FunctionContext
      extends TranslatorToCAst.FunctionContext<WalkContext, PyObject> implements WalkContext {
    private final Scope scope;
    private CAstEntity entity;
    private boolean generator = false;
    private final boolean async;

    private final List<String> inits = new ArrayList<>();

    public WalkContext getParent() {
      return (WalkContext) super.getParent();
    }

    public Scope scope() {
      return scope;
    }

    @Override
    public List<String> inits() {
      return inits;
    }

    protected FunctionContext(
        WalkContext parent, PyObject s, List<String> argumentNames, boolean async) {
      super(parent, s);
      this.async = async;
      scope =
          new Scope() {
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

    @Override
    public boolean isGenerator() {
      return generator;
    }

    @Override
    public boolean isGenerator(boolean v) {
      return generator = v;
    }

    @Override
    public boolean isFunctionScope() {
      return true;
    }

    @Override
    public CAstType classSelfVar() {
      return null;
    }

    @Override
    public WalkContext codeParent() {
      return this;
    }

    @Override
    public boolean isAsync() {
      return async;
    }

    @Override
    public Set<String> matchDeclNames() {
      return Collections.emptySet();
    }
  }

  public abstract static class TranslationVisitor extends AbstractParser.CAstVisitor
      implements JepAstVisitor<CAstNode, WalkContext> {
    CAst ast = new CAstImpl();
    private int label = 0;
    private final CAstEntity entity;

    TranslationVisitor(CAstEntity self, CAstTypeDictionaryImpl<String> types) {
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

    public CAstNode visitAugAssign(PyObject o, WalkContext context) {
      PyObject value = (PyObject) o.getAttr("value");
      CAstNode rhs = visit(value, context);

      PyObject target = (PyObject) o.getAttr("target");
      CAstNode lhs = visit(target, context);

      PyObject op = (PyObject) o.getAttr("op");

      return ast.makeNode(CAstNode.ASSIGN_POST_OP, lhs, rhs, translateOperator(getAstNodeType(op)));
    }

    public CAstNode visitAnnAssign(PyObject o, WalkContext context) {
      List<CAstNode> code = new LinkedList<>();
      PyObject targetObj = o.getAttr("target", PyObject.class);
      CAstNode target = visit(targetObj, context);
      int simple = o.getAttr("simple", Integer.class);

      PyObject annotation = o.getAttr("annotation", PyObject.class);

      boolean isClass = isClassContext(context);
      if (context.isFunctionScope()) {
        if (simple > 0) {
          String varName = targetObj.getAttr("id", String.class);
          context.scope().localNames.add(varName);
          context.inits().add(varName);
        }
        System.err.println(target + " " + simple + " " + annotation);
      } else if (isClass) {
        PyObject codeForAnnotation = (PyObject) fixForCompilation(annotation);
        Object annotatedValue = codeForAnnotation != null ? Util.run(codeForAnnotation) : null;

        CAstNode annotatedCode = visit(annotation, context.codeParent());
        if (annotatedValue instanceof PyObject) {
          String type = typeName((PyObject) annotatedValue);
          if (type.contains("function") || type.contains("lambda")) {
            annotatedValue = null;
          }
        }

        if (simple > 0) {
          if (annotatedValue != null) {
            code.add(annotatedCode);
            code.add(
                ast.makeNode(
                    CAstNode.ASSIGN,
                    ast.makeNode(
                        CAstNode.OBJECT_REF,
                        ast.makeNode(
                            CAstNode.OBJECT_REF,
                            ast.makeNode(
                                CAstNode.VAR, ast.makeConstant(context.classSelfVar().getName())),
                            ast.makeConstant("__annotations__")),
                        ast.makeConstant(targetObj.getAttr("id", String.class))),
                    ast.makeConstant(
                        annotatedValue instanceof PyObject
                            ? annotatedValue.toString()
                            : annotatedValue)));
          } else {
            code.add(
                ast.makeNode(
                    CAstNode.ASSIGN,
                    ast.makeNode(
                        CAstNode.OBJECT_REF,
                        ast.makeNode(
                            CAstNode.OBJECT_REF,
                            ast.makeNode(
                                CAstNode.VAR, ast.makeConstant(context.classSelfVar().getName())),
                            ast.makeConstant("__annotations__")),
                        ast.makeConstant(targetObj.getAttr("id", String.class))),
                    annotatedCode));
          }
        } else {
          code.add(annotatedCode);
        }

        System.err.println(
            target
                + " "
                + simple
                + " "
                + annotation
                + " "
                + codeForAnnotation
                + " "
                + annotatedValue
                + " "
                + annotatedCode);
      }

      if (has(o, "value")) {
        CAstNode rhs = visit(o.getAttr("value", PyObject.class), context);
        if (isClass) {
          addField(context, rhs, targetObj);
        } else {
          code.add(ast.makeNode(CAstNode.ASSIGN, target, rhs));
        }
      }

      if (code.isEmpty()) {
        return ast.makeNode(CAstNode.EMPTY);
      } else if (code.size() == 1) {
        return code.get(0);
      } else {
        return ast.makeNode(CAstNode.BLOCK_EXPR, code.toArray(new CAstNode[code.size()]));
      }
    }

    private String getAstNodeType(PyObject op) {
      return op.getAttr("op", PyObject.class)
          .getAttr("__class__", PyObject.class)
          .getAttr("__name__", String.class);
    }

    private CAstNode combine(Iterator<PyObject> elements, boolean isAnd, WalkContext context) {
      CAstNode elt = visit(elements.next(), context);
      if (!elements.hasNext()) {
        return elt;
      } else if (isAnd) {
        return ast.makeNode(
            CAstNode.IF_EXPR, elt, combine(elements, isAnd, context), ast.makeConstant(false));
      } else {
        return ast.makeNode(
            CAstNode.IF_EXPR, elt, ast.makeConstant(true), combine(elements, isAnd, context));
      }
    }

    public CAstNode visitBoolOp(PyObject o, WalkContext context) {
      boolean isAnd = "And".equals(o.getAttr("op", PyObject.class).toString());
      @SuppressWarnings("unchecked")
      List<PyObject> exprs = o.getAttr("expr", List.class);
      return combine(exprs.stream().iterator(), isAnd, context);
    }

    public CAstNode visitIfExp(PyObject o, WalkContext context) {
      return ast.makeNode(
          CAstNode.IF_EXPR,
          visit(o.getAttr("test", PyObject.class), context),
          visit(o.getAttr("body", PyObject.class), context),
          visit(o.getAttr("orelse", PyObject.class), context));
    }

    @Override
    public CAstNode visitModule(PyObject o, WalkContext context) {
      @SuppressWarnings("unchecked")
      List<PyObject> body = (List<PyObject>) o.getAttr("body");
      CAstNode bodyAst =
          ast.makeNode(
              CAstNode.BLOCK_STMT,
              body.stream().map(f -> visit(f, context)).collect(Collectors.toList()));

      List<CAstNode> defaults = new LinkedList<>();
      defaultImports(defaults);
      bodyAst =
          ast.makeNode(CAstNode.BLOCK_STMT, ast.makeNode(CAstNode.BLOCK_STMT, defaults), bodyAst);

      if (!context.inits().isEmpty()) {
        bodyAst = ast.makeNode(CAstNode.BLOCK_STMT, handleInits(context), bodyAst);
      }

      Set<String> exposedNames = exposedNames(bodyAst);
      if (exposedNames.size() > 0)
        return ast.makeNode(
            CAstNode.UNWIND,
            bodyAst,
            ast.makeNode(
                CAstNode.BLOCK_STMT,
                exposedNames.stream()
                    .map(
                        n ->
                            ast.makeNode(
                                CAstNode.ASSIGN,
                                ast.makeNode(
                                    CAstNode.OBJECT_REF,
                                    ast.makeNode(CAstNode.THIS),
                                    ast.makeConstant(n)),
                                ast.makeNode(CAstNode.VAR, ast.makeConstant(n))))
                    .collect(Collectors.toList())));
      else return bodyAst;
    }

    private CAstNode visitFunction(PyObject o, WalkContext context, CAstType type) {
      CAstNode fe =
          doFunction(o.getAttr("args"), o, o.getAttr("name", String.class), context, type);
      if (isClassContext(context)) {
        return ast.makeNode(CAstNode.EMPTY);
      } else {
        return ast.makeNode(
            CAstNode.DECL_STMT,
            ast.makeConstant(new CAstSymbolImpl(o.getAttr("name", String.class), type)),
            fe);
      }
    }

    public CAstNode visitFunctionDef(PyObject o, WalkContext context) {
      return visitFunction(o, context, isClassContext(context) ? methodBody : codeBody);
    }

    public CAstNode visitAsyncFunctionDef(PyObject o, WalkContext context) {
      return visitFunction(o, context, isClassContext(context) ? asyncMethodBody : asyncCodeBody);
    }

    public CAstNode doFunction(
        Object rawArgs, PyObject o, String functionName, WalkContext context, CAstType superType) {
      List<PyObject> arguments = extractArguments(rawArgs);
      return doFunction(
          o,
          functionName,
          context,
          arguments,
          getDefaults(o, functionName, context),
          Either.forLeft(getCode(o)),
          getArgumentNames(arguments),
          superType);
    }

    public CAstNode doFunction(
        PyObject o,
        String functionName,
        WalkContext context,
        List<PyObject> arguments,
        CAstNode[] defaultCode,
        Either<List<PyObject>, CAstNode> code,
        List<String> argumentNames,
        CAstType superType) {
      int argumentCount = arguments.size();

      List<CAstType> argumentTypes = Collections.nCopies(argumentCount + 1, CAstType.DYNAMIC);

      class PythonCodeType implements CAstType.Function {

        @Override
        public Collection<CAstType> getSupertypes() {
          return Collections.singleton(superType);
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
      String ft = typeName(o);
      if (isClassContext(context) && !"Lambda".equals(ft)) {
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
          List<String> names =
              arguments.stream()
                  .map(
                      a ->
                          Util.typeName(a).equals("Name")
                              ? a.getAttr("id", String.class)
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

      WalkContext fc =
          new FunctionContext(
              context,
              o,
              argumentNames,
              superType == asyncCodeBody || superType == asyncMethodBody) {

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

            @Override
            public boolean isFunctionScope() {
              return true;
            }
          };

      CAstNode b1;
      if (code.isLeft()) {
        b1 = visit(CAstNode.BLOCK_STMT, code.getLeft(), fc);
      } else {
        b1 = code.getRight();
      }

      CAstNode body =
          ast.makeNode(
              CAstNode.LOCAL_SCOPE, ast.makeNode(CAstNode.BLOCK_STMT, handleInits(fc), b1));

      if (fc.isGenerator()) {
        body =
            ast.makeNode(
                CAstNode.BLOCK_STMT,
                body,
                ast.makeNode(
                    CAstNode.RETURN, ast.makeNode(CAstNode.VAR, ast.makeConstant("the function"))));
      }

      if (funType instanceof CAstType.Method) {
        body =
            ast.makeNode(
                CAstNode.BLOCK_STMT,
                ast.makeNode(
                    CAstNode.DECL_STMT,
                    ast.makeConstant(new CAstSymbolImpl("super", PythonCAstToIRTranslator.Any)),
                    ast.makeNode(CAstNode.NEW, ast.makeConstant("superfun"))),
                ast.makeNode(
                    CAstNode.BLOCK_STMT,
                    ast.makeNode(
                        CAstNode.ASSIGN,
                        ast.makeNode(
                            CAstNode.OBJECT_REF,
                            ast.makeNode(CAstNode.VAR, Ast.makeConstant("super")),
                            ast.makeConstant("$class")),
                        ast.makeNode(
                            CAstNode.VAR, ast.makeConstant(context.entity().getType().getName()))),
                    ast.makeNode(
                        CAstNode.ASSIGN,
                        ast.makeNode(
                            CAstNode.OBJECT_REF,
                            ast.makeNode(CAstNode.VAR, Ast.makeConstant("super")),
                            ast.makeConstant("$self")),
                        ast.makeNode(CAstNode.VAR, Ast.makeConstant(fun.getArgumentNames()[1])))),
                body);
      }

      fun.ast = body;

      CAstNode fe = ast.makeNode(CAstNode.FUNCTION_EXPR, ast.makeConstant(fun));
      context.addScopedEntity(fe, fun);

      List<CAstNode> annotationCode = new ArrayList<>();
      List<PyObject> argAnnotations =
          arguments.stream()
              .filter(a -> has(a, "annotation") && a.getAttr("annotation") != null)
              .toList();
      if (argAnnotations != null && argAnnotations.size() > 0) {
        annotationCode.add(
            ast.makeNode(
                CAstNode.DECL_STMT,
                ast.makeConstant(new CAstSymbolImpl("__ann__", CAstType.DYNAMIC)),
                ast.makeNode(CAstNode.NEW, ast.makeConstant("dict"))));
        annotationCode.add(
            ast.makeNode(
                CAstNode.ASSIGN,
                ast.makeNode(
                    CAstNode.OBJECT_REF,
                    ast.makeNode(CAstNode.VAR, ast.makeConstant("__fe__")),
                    ast.makeConstant("__annotations__")),
                ast.makeNode(CAstNode.VAR, ast.makeConstant("__ann__"))));
        annotationCode.addAll(
            argAnnotations.stream()
                .map(
                    a ->
                        ast.makeNode(
                            CAstNode.ASSIGN,
                            ast.makeNode(
                                CAstNode.OBJECT_REF,
                                ast.makeNode(CAstNode.VAR, ast.makeConstant("__ann__")),
                                ast.makeConstant(a.getAttr("arg", String.class))),
                            visit(a.getAttr("annotation", PyObject.class), context)))
                .toList());
      }
      if (has(o, "returns") && o.getAttr("returns") != null) {
        annotationCode.add(
            ast.makeNode(
                CAstNode.ASSIGN,
                ast.makeNode(
                    CAstNode.OBJECT_REF,
                    ast.makeNode(CAstNode.VAR, ast.makeConstant("__ann__")),
                    ast.makeConstant("return")),
                visit(o.getAttr("returns", PyObject.class), context)));
      }

      if (defaultCode.length == 0 && annotationCode.size() == 0) {
        return fe;
      } else {
        if (annotationCode.size() > 0) {
          fe =
              ast.makeNode(
                  CAstNode.BLOCK_EXPR,
                  ast.makeNode(
                      CAstNode.DECL_STMT,
                      ast.makeConstant(new CAstSymbolImpl("__fe__", funType)),
                      fe),
                  annotationCode.isEmpty()
                      ? ast.makeNode(CAstNode.EMPTY)
                      : ast.makeNode(CAstNode.BLOCK_STMT, annotationCode),
                  ast.makeNode(CAstNode.VAR, ast.makeConstant("__fe__")));
        }

        return ast.makeNode(
            CAstNode.BLOCK_EXPR,
            defaultCode.length == 0
                ? ast.makeNode(CAstNode.EMPTY)
                : ast.makeNode(CAstNode.BLOCK_EXPR, defaultCode),
            fe);
      }
    }

    private CAstNode handleInits(WalkContext fc) {
      return ast.makeNode(
          CAstNode.BLOCK_STMT,
          fc.inits().stream()
              .map(
                  n ->
                      ast.makeNode(
                          CAstNode.DECL_STMT,
                          ast.makeConstant(new CAstSymbolImpl(n, CAstType.DYNAMIC))))
              .collect(Collectors.toList()));
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
          defaultCode[arg] =
              ast.makeNode(
                  CAstNode.ASSIGN,
                  ast.makeNode(CAstNode.VAR, Ast.makeConstant(name)),
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
          && !scope.nonLocalNames.contains(lhs.toString())
          && !context.inits().contains(lhs.toString())) {
        context.inits().add(lhs.toString());
        scope.localNames.add(lhs.toString());
      }

      return ast.makeNode(CAstNode.ASSIGN, visit(lhs, context), visit(rhs, context));
    }

    public CAstNode visitAssign(PyObject o, WalkContext context) {
      @SuppressWarnings("unchecked")
      List<PyObject> body = (List<PyObject>) o.getAttr("targets");
      PyObject value = (PyObject) o.getAttr("value");
      boolean isClass = isClassContext(context);
      if (isClass) {
        CAstNode rhs = visit(value, context);
        for (PyObject lhs : body) {
          addField(context, rhs, lhs);
        }
        return ast.makeNode(CAstNode.EMPTY);
      } else {
        CAstNode rhs = visit(value, context);
        return ast.makeNode(
            CAstNode.BLOCK_STMT,
            body.stream()
                .map(
                    f -> {
                      Scope scope = context.scope();
                      if (!isClass
                          && !scope.globalNames.contains(f.toString())
                          && !scope.nonLocalNames.contains(f.toString())
                          && !context.inits().contains(f.toString())) {
                        context.inits().add(f.toString());
                        scope.localNames.add(f.toString());
                      }
                      return ast.makeNode(CAstNode.ASSIGN, visit(f, context), rhs);
                    })
                .collect(Collectors.toList()));
      }
    }

    private boolean isClassContext(WalkContext context) {
      return context.entity() != null && context.entity().getKind() == CAstEntity.TYPE_ENTITY;
    }

    private AbstractFieldEntity addField(WalkContext context, CAstNode rhs, PyObject lhs) {
      AbstractFieldEntity f =
          new AbstractFieldEntity(
              lhs.getAttr("id", String.class), Collections.emptySet(), false, context.entity()) {
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
          };

      context.addScopedEntity(null, f);
      return f;
    }

    private CAstNode visit(int node, List<PyObject> l, WalkContext context) {
      return ast.makeNode(
          node, l.stream().map(f -> visit(f, context)).collect(Collectors.toList()));
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

      args.forEach(
          a -> {
            ak.add(visit(a, context));
          });
      for (PyObject k : keywords) {
        ak.add(
            ast.makeNode(
                CAstNode.ARRAY_LITERAL,
                ast.makeConstant(k.getAttr("arg", String.class)),
                visit(k.getAttr("value", PyObject.class), context)));
      }

      CAstNode call = ast.makeNode(CAstNode.CALL, ak);
      context.cfg().map(call, call);

      if (context.getCatchTarget("<class 'Exception'>") != null) {
        context
            .cfg()
            .add(call, context.getCatchTarget("<class 'Exception'>"), "<class 'Exception'>");
      }

      return call;
    }

    public CAstNode visitImportFrom(PyObject o, WalkContext context) {
      String module = (String) o.getAttr("module");
      CAstNode mod =
          ast.makeNode(
              CAstNode.DECL_STMT,
              ast.makeConstant(new CAstSymbolImpl(module, PythonCAstToIRTranslator.Any)),
              ast.makeNode(
                  CAstNode.PRIMITIVE, ast.makeConstant("import"), ast.makeConstant(module)));
      @SuppressWarnings("unchecked")
      List<PyObject> alias = (List<PyObject>) o.getAttr("names");
      return ast.makeNode(
          CAstNode.BLOCK_STMT,
          mod,
          ast.makeNode(
              CAstNode.BLOCK_STMT,
              alias.stream()
                  .map(
                      a ->
                          ast.makeNode(
                              CAstNode.ASSIGN,
                              ast.makeNode(CAstNode.VAR, ast.makeConstant(a.getAttr("name"))),
                              ast.makeNode(
                                  CAstNode.OBJECT_REF,
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
    }

    private CAstNode visitLoopCore(
        PyObject loop, WalkContext context, CAstNode testNode, CAstNode update) {
      @SuppressWarnings("unchecked")
      List<PyObject> body = (List<PyObject>) loop.getAttr("body");
      @SuppressWarnings("unchecked")
      List<PyObject> orelse = (List<PyObject>) loop.getAttr("orelse");

      PyObject contLabel = runit("ast.Pass()");
      PyObject breakLabel = runit("ast.Pass()");
      LoopContext lc = new LoopContext(context, breakLabel, contLabel);

      CAstNode bodyNode = visit(CAstNode.BLOCK_STMT, body, lc);

      if (lc.continued) {
        CAstNode cn =
            ast.makeNode(
                CAstNode.LABEL_STMT,
                ast.makeConstant("label_" + label++),
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
        CAstNode bn =
            ast.makeNode(
                CAstNode.LABEL_STMT,
                ast.makeConstant("label_" + label++),
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
        CAstNode op =
            translateOperator(
                ops.next().getAttr("__class__", PyObject.class).getAttr("__name__", String.class));
        PyObject exp = exprs.next();
        CAstNode rhs = visit(exp, context);
        CAstNode cmpop = ast.makeNode(CAstNode.BINARY_EXPR, op, ln, rhs);
        ln = visit(exp, context);
        expr =
            expr == null
                ? cmpop
                : ast.makeNode(CAstNode.IF_EXPR, cmpop, expr, ast.makeConstant(false));
      }

      return expr;
    }

    public CAstNode visitIf(PyObject ifstmt, WalkContext context) {
      @SuppressWarnings("unchecked")
      List<PyObject> body = (List<PyObject>) ifstmt.getAttr("body");
      @SuppressWarnings("unchecked")
      List<PyObject> orelse = (List<PyObject>) ifstmt.getAttr("orelse");
      return ast.makeNode(
          CAstNode.IF_STMT,
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
      PyObject left = binop.getAttr("left", PyObject.class);
      PyObject right = binop.getAttr("right", PyObject.class);
      return ast.makeNode(
          CAstNode.BINARY_EXPR,
          translateOperator(getAstNodeType(binop)),
          visit(left, context),
          visit(right, context));
    }

    public CAstNode visitImport(PyObject importStmt, WalkContext context) {
      @SuppressWarnings("unchecked")
      List<PyObject> body = (List<PyObject>) importStmt.getAttr("names");
      return ast.makeNode(
          CAstNode.BLOCK_STMT,
          body.stream()
              .map(
                  s -> {
                    String importedName = s.getAttr("name", String.class);
                    String declName = s.getAttr("asname", String.class);
                    return ast.makeNode(
                        CAstNode.DECL_STMT,
                        ast.makeConstant(
                            new CAstSymbolImpl(
                                declName == null ? importedName : declName,
                                PythonCAstToIRTranslator.Any)),
                        ast.makeNode(
                            CAstNode.PRIMITIVE,
                            ast.makeConstant("import"),
                            ast.makeConstant(importedName)));
                  })
              .collect(Collectors.toList()));
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

    public CAstNode visitLambda(PyObject lambdaNode, WalkContext context) {
      return doFunction(
          lambdaNode.getAttr("args"),
          lambdaNode,
          "lambda" + lambdaCount++,
          context.codeParent(),
          isClassContext(context) ? lambdaMethod : lambda);
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

    public CAstNode visitSet(PyObject set, WalkContext context) {
      return handleList("set", "elts", set, context);
    }

    public CAstNode visitUnaryOp(PyObject unop, WalkContext context) {
      PyObject op = unop.getAttr("operand", PyObject.class);
      return ast.makeNode(
          CAstNode.UNARY_EXPR, translateOperator(getAstNodeType(unop)), visit(op, context));
    }

    public CAstNode visitTuple(PyObject tp, WalkContext context) {
      return handleList("tuple", "elts", tp, context);
    }

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
        missingTypes.put(
            name,
            new MissingType() {

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

      CAstType.Class cls =
          new CAstType.Class() {
            @Override
            public Collection<CAstType> getSupertypes() {
              Collection<CAstType> supertypes = HashSetFactory.make();
              @SuppressWarnings("unchecked")
              List<PyObject> bases = (List<PyObject>) arg0.getAttr("bases");
              for (PyObject e : bases) {
                try {
                  String superType = e.getAttr("id", String.class);
                  CAstType type = types.getCAstTypeFor(superType);
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
              return null;
            }

            @Override
            public Position getNamePosition() {
              return null;
            }
          };

      Scope classScope =
          new Scope() {
            @Override
            Scope parent() {
              return context.scope();
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
            public PyObject top() {
              return arg0;
            }

            @Override
            public void addScopedEntity(CAstNode newNode, CAstEntity visit) {
              members.add(visit);
            }

            public WalkContext codeParent() {
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

            @Override
            public boolean isAsync() {
              assert false;
              return false;
            }

            @Override
            public boolean isGenerator() {
              assert false;
              return false;
            }

            @Override
            public boolean isGenerator(boolean v) {
              assert false;
              return false;
            }

            @Override
            public boolean isFunctionScope() {
              return false;
            }

            @Override
            public CAstType classSelfVar() {
              return cls;
            }
          };

      URL url = this.url();
      TranslationVisitor v =
          new TranslationVisitor(clse, types) {
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

      CAstNode x, y = ast.makeNode(CAstNode.CLASS_STMT, ast.makeConstant(clse));
      if (elts.isEmpty()) {
        x = y;
      } else {
        elts.add(
            0,
            ast.makeNode(
                CAstNode.ASSIGN,
                ast.makeNode(
                    CAstNode.OBJECT_REF,
                    ast.makeNode(CAstNode.VAR, ast.makeConstant(child.classSelfVar().getName())),
                    ast.makeConstant("__annotations__")),
                ast.makeNode(
                    CAstNode.NEW,
                    ast.makeConstant(PythonTypes.dict.getName().toString().substring(1)))));
        elts.add(0, y);
        elts.add(ast.makeNode(CAstNode.VAR, ast.makeConstant(child.classSelfVar().getName())));
        x = ast.makeNode(CAstNode.BLOCK_EXPR, elts.toArray(new CAstNode[elts.size()]));
      }

      context.addScopedEntity(y, clse);
      return x;
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
                  tree)
              .stream()
              .map(s -> (String) ((CAstNode) s.get("n")).getValue())
              .collect(Collectors.toSet());
    }

    public CAstNode visitAssert(PyObject asrt, WalkContext context) throws Exception {
      return ast.makeNode(CAstNode.ASSERT, visit(asrt.getAttr("test", PyObject.class), context));
    }

    public CAstNode visitYieldFrom(PyObject o, WalkContext context) {
      context.isGenerator(true);
      return ast.makeNode(
          CAstNode.ASSIGN,
          ast.makeNode(
              CAstNode.OBJECT_REF,
              ast.makeNode(CAstNode.VAR, ast.makeConstant("the function")),
              ast.makeConstant("__content__")),
          ast.makeNode(
              CAstNode.OBJECT_REF,
              visit(o.getAttr("value", PyObject.class), context),
              ast.makeConstant("__content__")));
    }

    public CAstNode visitAwait(PyObject o, WalkContext context) {
      return ast.makeNode(
          CAstNode.OBJECT_REF,
          visit(o.getAttr("value", PyObject.class), context),
          ast.makeConstant("__async_content__"));
    }

    public CAstNode visitYield(PyObject o, WalkContext context) {
      context.isGenerator(true);
      return ast.makeNode(
          CAstNode.ASSIGN,
          ast.makeNode(
              CAstNode.OBJECT_REF,
              ast.makeNode(CAstNode.VAR, ast.makeConstant("the function")),
              ast.makeConstant("__content__")),
          visit(o.getAttr("value", PyObject.class), context));
    }

    public CAstNode visitRaise(PyObject raise, WalkContext context) throws Exception {
      CAstNode exp = visit(raise.getAttr("exc", PyObject.class), context);
      CAstNode raiseNode, result;
      if (has(raise, "cause") && raise.getAttr("cause") != null) {
        result =
            ast.makeNode(
                CAstNode.LOCAL_SCOPE,
                ast.makeNode(
                    CAstNode.BLOCK_STMT,
                    ast.makeNode(
                        CAstNode.DECL_STMT,
                        ast.makeConstant(new CAstSymbolImpl("__tmp__", CAstType.DYNAMIC)),
                        exp),
                    ast.makeNode(
                        CAstNode.ASSIGN,
                        ast.makeNode(
                            CAstNode.OBJECT_REF,
                            ast.makeNode(CAstNode.VAR, ast.makeConstant("__tmp__")),
                            ast.makeConstant("__cause__")),
                        visit(raise.getAttr("cause", PyObject.class), context)),
                    raiseNode =
                        ast.makeNode(
                            CAstNode.THROW,
                            ast.makeNode(CAstNode.VAR, ast.makeConstant("__tmp__")))));

      } else {
        result = raiseNode = ast.makeNode(CAstNode.THROW, exp);
      }

      if (context.getCatchTarget("<class 'Exception'>") != null) {
        context.cfg().map(raise, raiseNode);
        context
            .cfg()
            .add(raise, context.getCatchTarget("<class 'Exception'>"), "<class 'Exception'>");
      }

      return result;
    }

    public CAstNode visitTryStar(PyObject tryNode, WalkContext context) throws Exception {
      return visitTry(tryNode, context);
    }

    @SuppressWarnings("unchecked")
    public CAstNode visitTry(PyObject tryNode, WalkContext context) throws Exception {
      WalkContext cc = context;
      Map<String, CAstNode> catches = new LinkedHashMap<>();
      if (tryNode.getAttr("handlers") != null) {
        ((List<PyObject>) tryNode.getAttr("handlers", List.class))
            .stream()
                .forEach(
                    c -> {
                      String name =
                          c.getAttr("name") == null ? "$dummy" : c.getAttr("name", String.class);
                      context.scope().localNames.add(name);
                      context.inits().add(name);
                      CAstNode catchBlock =
                          ast.makeNode(
                              CAstNode.CATCH,
                              ast.makeConstant(name),
                              visit(CAstNode.BLOCK_STMT, c.getAttr("body", List.class), context));
                      context.cfg().map(catchBlock, catchBlock);
                      catches.put(
                          has(c, "type")
                                  && fixForCompilation(c.getAttr("type", PyObject.class)) != null
                              ? String.valueOf(
                                  Util.run(fixForCompilation(c.getAttr("type", PyObject.class))))
                              : "<class 'Exception'>",
                          catchBlock);
                    });

        class TryCatchContext extends TranslatorToCAst.TryCatchContext<WalkContext, PyObject>
            implements WalkContext {

          private TryCatchContext(WalkContext parent, Map<String, CAstNode> catchNode) {
            super(parent, catchNode);
          }

          @Override
          public WalkContext getParent() {
            return (WalkContext) super.getParent();
          }
        }

        cc = new TryCatchContext(context, catches);
      }

      CAstNode body =
          visit(CAstNode.BLOCK_STMT, (List<PyObject>) tryNode.getAttr("body", List.class), cc);

      if (tryNode.getAttr("orelse") != null) {
        List<PyObject> oe = (List<PyObject>) tryNode.getAttr("orelse", List.class);
        if (!oe.isEmpty()) {
          body = ast.makeNode(CAstNode.BLOCK_STMT, body, visit(CAstNode.BLOCK_STMT, oe, context));
        }
      }

      body =
          ast.makeNode(CAstNode.TRY, body, catches.values().toArray(new CAstNode[catches.size()]));

      if (tryNode.getAttr("finalbody") != null) {
        List<PyObject> fb = (List<PyObject>) tryNode.getAttr("finalbody", List.class);
        if (!fb.isEmpty()) {
          body = ast.makeNode(CAstNode.UNWIND, body, visit(CAstNode.BLOCK_STMT, fb, context));
        }
      }

      return body;
    }

    private int tmpIndex = 0;

    interface Comprehension {
      PyObject target();

      PyObject iter();

      List<PyObject> ifs();

      default boolean async() {
        return false;
      }
    }

    private int generator = 0;

    public CAstNode visitGeneratorExp(PyObject ge, WalkContext context) {
      String me = " _generator_" + (generator++);
      PyObject body = ge.getAttr("elt", PyObject.class);
      @SuppressWarnings("unchecked")
      List<PyObject> gens = ge.getAttr("generators", List.class);
      CAstNode bodyNode =
          doGenerators(
              gens.stream()
                  .map(
                      o ->
                          new Comprehension() {

                            @Override
                            public PyObject target() {
                              return o.getAttr("target", PyObject.class);
                            }

                            @Override
                            public PyObject iter() {
                              return o.getAttr("iter", PyObject.class);
                            }

                            @SuppressWarnings("unchecked")
                            @Override
                            public List<PyObject> ifs() {
                              return o.getAttr("ifs", List.class);
                            }
                          })
                  .collect(Collectors.toList()),
              ast.makeNode(
                  CAstNode.ASSIGN,
                  ast.makeNode(
                      CAstNode.OBJECT_REF,
                      ast.makeNode(CAstNode.VAR, ast.makeConstant(me)),
                      ast.makeConstant("__contents__")),
                  visit(body, context)),
              context);

      return ast.makeNode(
          CAstNode.LOCAL_SCOPE,
          ast.makeNode(
              CAstNode.BLOCK_EXPR,
              ast.makeNode(
                  CAstNode.DECL_STMT,
                  ast.makeConstant(new CAstSymbolImpl(me, CAstType.DYNAMIC)),
                  ast.makeNode(
                      CAstNode.NEW,
                      ast.makeConstant(PythonTypes.iterator.getName().toString().substring(1)))),
              bodyNode,
              ast.makeNode(CAstNode.VAR, ast.makeConstant(me))));
    }

    @SuppressWarnings("unchecked")
    public CAstNode visitFor(PyObject forNode, WalkContext context) throws Exception {
      PyObject target = forNode.getAttr("target", PyObject.class);
      PyObject iter = forNode.getAttr("iter", PyObject.class);
      java.util.List<PyObject> internalBody = forNode.getAttr("body", List.class);

      return handleFor(target, iter, internalBody, context, false);
    }

    @SuppressWarnings("unchecked")
    public CAstNode visitAsyncFor(PyObject forNode, WalkContext context) throws Exception {
      PyObject target = forNode.getAttr("target", PyObject.class);
      PyObject iter = forNode.getAttr("iter", PyObject.class);
      java.util.List<PyObject> internalBody = forNode.getAttr("body", List.class);

      return handleFor(target, iter, internalBody, context, true);
    }

    private CAstNode handleFor(
        PyObject target,
        PyObject iter,
        List<PyObject> internalBody,
        WalkContext context,
        boolean async)
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

      CAstNode continueStmt =
          x.continued ? visit(contLabel, context) : ast.makeNode(CAstNode.EMPTY);
      if (x.continued) {
        context.cfg().map(contLabel, continueStmt);
      }

      Comprehension g =
          new Comprehension() {
            public PyObject target() {
              return target;
            }

            public PyObject iter() {
              return iter;
            }

            public List<PyObject> ifs() {
              return Collections.emptyList();
            }

            @Override
            public boolean async() {
              return async;
            }
          };

      return ast.makeNode(
          CAstNode.BLOCK_EXPR,
          doGenerators(
              Collections.singletonList(g),
              ast.makeNode(
                  CAstNode.BLOCK_EXPR, Ast.makeNode(CAstNode.BLOCK_EXPR, body), continueStmt),
              context),
          breakStmt);
    }

    private CAstNode doGenerators(
        List<
                com.ibm.wala.cast.python.jep.ast.CPythonAstToCAstTranslator.TranslationVisitor
                    .Comprehension>
            generators,
        CAstNode body,
        WalkContext context) {
      CAstNode result = body;

      for (com.ibm.wala.cast.python.jep.ast.CPythonAstToCAstTranslator.TranslationVisitor
              .Comprehension
          c : generators) {
        int j = c.ifs().size();
        if (j > 0) {
          for (PyObject test : c.ifs()) {
            CAstNode v = visit(test, context);
            result = ast.makeNode(CAstNode.IF_STMT, v, result);
          }
        }

        String tempName = "temp " + ++tmpIndex;
        String tempName2 = "temp " + ++tmpIndex;

        CAstNode test =
            ast.makeNode(
                CAstNode.BINARY_EXPR,
                CAstOperator.OP_NE,
                ast.makeConstant(null),
                ast.makeNode(
                    CAstNode.BLOCK_EXPR,
                    ast.makeNode(
                        CAstNode.ASSIGN,
                        ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName2)),
                        ast.makeNode(
                            CAstNode.EACH_ELEMENT_GET,
                            ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName)),
                            ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName2))))));

        CAstNode x = visit(c.target(), context);
        if (x.getKind() == CAstNode.VAR) {
          String nm = (String) x.getChild(0).getValue();
          context.scope().localNames.add(nm);
          context.inits().add(nm);
        }

        result =
            notePosition(
                ast.makeNode(
                    CAstNode.BLOCK_EXPR,
                    ast.makeNode(
                        CAstNode.DECL_STMT,
                        ast.makeConstant(
                            new CAstSymbolImpl(tempName, PythonCAstToIRTranslator.Any)),
                        visit(c.iter(), context)),
                    ast.makeNode(
                        CAstNode.DECL_STMT,
                        ast.makeConstant(
                            new CAstSymbolImpl(tempName2, PythonCAstToIRTranslator.Any))),
                    ast.makeNode(
                        CAstNode.ASSIGN,
                        ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName2)),
                        ast.makeNode(
                            CAstNode.EACH_ELEMENT_GET,
                            ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName)),
                            ast.makeConstant(null))),
                    ast.makeNode(
                        CAstNode.LOOP,
                        test,
                        ast.makeNode(
                            CAstNode.BLOCK_EXPR,
                            ast.makeNode(
                                CAstNode.ASSIGN,
                                visit(c.target(), context),
                                ast.makeNode(
                                    CAstNode.PRIMITIVE,
                                    ast.makeConstant("forElementGet"),
                                    ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName)),
                                    ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName2)))),
                            result,
                            ast.makeNode(
                                CAstNode.ASSIGN,
                                ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName2)),
                                ast.makeNode(
                                    CAstNode.BINARY_EXPR,
                                    CAstOperator.OP_ADD,
                                    ast.makeNode(CAstNode.VAR, ast.makeConstant(tempName2)),
                                    ast.makeConstant(1)))))),
                context.pos().getPosition(body));
      }

      return ast.makeNode(CAstNode.LOCAL_SCOPE, result);
    }

    private CAstNode visitComp(
        PyObject key,
        PyObject value,
        List<Comprehension> gen,
        TypeReference type,
        WalkContext context)
        throws Exception {
      CAstNode objDecl =
          ast.makeNode(
              CAstNode.DECL_STMT,
              ast.makeConstant(new CAstSymbolImpl("__collection__", CAstType.DYNAMIC)),
              ast.makeNode(CAstNode.NEW, ast.makeConstant(type.getName().toString().substring(1))));
      CAstNode idxDecl =
          ast.makeNode(
              CAstNode.DECL_STMT,
              ast.makeConstant(new CAstSymbolImpl("__idx__", CAstType.DYNAMIC)),
              ast.makeConstant(0));
      CAstNode body =
          ast.makeNode(
              CAstNode.ASSIGN,
              ast.makeNode(
                  CAstNode.OBJECT_REF,
                  ast.makeNode(CAstNode.VAR, ast.makeConstant("__collection__")),
                  key == null
                      ? ast.makeNode(CAstNode.VAR, ast.makeConstant("__idx__"))
                      : visit(key, context)),
              visit(value, context));
      return ast.makeNode(
          CAstNode.BLOCK_EXPR,
          objDecl,
          idxDecl,
          doGenerators(gen, body, context),
          ast.makeNode(CAstNode.VAR, ast.makeConstant("__collection__")));
    }

    public CAstNode visitDictComp(PyObject lc, WalkContext context) throws Exception {
      List<Comprehension> cs = toComprehensions(lc);
      return visitComp(
          lc.getAttr("key", PyObject.class),
          lc.getAttr("value", PyObject.class),
          cs,
          PythonTypes.dict,
          context);
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
      List<Comprehension> cs =
          gen.stream()
              .map(
                  g ->
                      new Comprehension() {

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
                      })
              .collect(Collectors.toList());
      return cs;
    }

    public CAstNode visitJoinedStr(PyObject js, WalkContext context) throws Exception {
      @SuppressWarnings("unchecked")
      java.util.List<PyObject> internalBody = js.getAttr("values", List.class);
      return internalBody.stream()
          .map(o -> visit(o, context))
          .reduce((l, r) -> ast.makeNode(CAstNode.BINARY_EXPR, CAstOperator.OP_ADD, l, r))
          .get();
    }

    public CAstNode visitFormattedValue(PyObject fv, WalkContext context) throws Exception {
      return visit(fv.getAttr("value", PyObject.class), context);
    }

    public CAstNode visitMatchValue(PyObject match, WalkContext context) {
      return ast.makeNode(
          CAstNode.BINARY_EXPR,
          CAstOperator.OP_EQ,
          context.matchVar(),
          visit(match.getAttr("value", PyObject.class), context));
    }

    public CAstNode visitMatchAs(PyObject match, WalkContext context) {
      String id = match.getAttr("name", String.class);
      context.matchDeclNames().add(id);
      return ast.makeNode(
          CAstNode.BLOCK_EXPR,
          ast.makeNode(
              CAstNode.ASSIGN,
              ast.makeNode(CAstNode.VAR, ast.makeConstant(id)),
              context.matchVar()),
          ast.makeConstant(true));
    }

    public CAstNode visitMatchOr(PyObject match, WalkContext context) {
        @SuppressWarnings("unchecked")
        java.util.List<PyObject> patterns = match.getAttr("patterns", List.class);
        return patterns.stream().map(o -> visit(o, context)).reduce((l, rhs) -> {
            CAstNode lhs =
                    ast.makeNode(
                        CAstNode.DECL_STMT,
                        ast.makeConstant(new CAstSymbolImpl("__lhs__", CAstType.DYNAMIC)),
                        l);
        	return 
        		ast.makeNode(CAstNode.BLOCK_EXPR,
        			lhs,
        			ast.makeNode(CAstNode.IF_EXPR, 
        				ast.makeNode(CAstNode.VAR, ast.makeConstant("__lhs__")),
        				ast.makeNode(CAstNode.VAR, ast.makeConstant("__lhs__")),
        				rhs));
        }).get();
    }
    
    public CAstNode visitMatch(PyObject match, WalkContext context) {
      CAstNode exprDecl =
          ast.makeNode(
              CAstNode.DECL_STMT,
              ast.makeConstant(new CAstSymbolImpl("__expr__", CAstType.DYNAMIC)),
              visit(match.getAttr("subject", PyObject.class), context));
      @SuppressWarnings("unchecked")
      java.util.List<PyObject> cases = match.getAttr("cases", List.class);

      Set<String> decls = HashSetFactory.make();
      WalkContext mc =
          new WalkContext() {

            @Override
            public WalkContext getParent() {
              return context;
            }

            @Override
            public CAstNode matchVar() {
              return ast.makeNode(CAstNode.VAR, ast.makeConstant("__expr__"));
            }

            @Override
            public Set<String> matchDeclNames() {
              return decls;
            }
          };

      CAstNode body =
          ast.makeNode(
              CAstNode.BLOCK_STMT,
              cases.stream()
                  .map(
                      matchCase -> {
                        CAstNode expr = visit(matchCase.getAttr("pattern", PyObject.class), mc);
                        @SuppressWarnings("unchecked")
                        CAstNode stmts =
                            ast.makeNode(
                                CAstNode.BLOCK_STMT,
                                ((List<PyObject>) matchCase.getAttr("body", List.class))
                                    .stream()
                                        .map(n -> visit(n, context))
                                        .collect(Collectors.toList()));

                        CAstNode bodyNode =
                            matchCase.getAttr("guard") != null
                                ? ast.makeNode(
                                    CAstNode.IF_STMT,
                                    visit(matchCase.getAttr("guard", PyObject.class), mc),
                                    stmts)
                                : stmts;

                        return ast.makeNode(CAstNode.IF_STMT, expr, bodyNode);
                      })
                  .collect(Collectors.toList()));

      if (!decls.isEmpty()) {
        exprDecl =
            ast.makeNode(
                CAstNode.BLOCK_STMT,
                exprDecl,
                ast.makeNode(
                    CAstNode.BLOCK_STMT,
                    decls.stream()
                        .map(
                            s ->
                                ast.makeNode(
                                    CAstNode.DECL_STMT,
                                    ast.makeConstant(new CAstSymbolImpl(s, CAstType.DYNAMIC))))
                        .collect(Collectors.toList())));
      }

      return ast.makeNode(CAstNode.BLOCK_STMT, exprDecl, body);
    }
  }

  public static IClassHierarchy load(Set<SourceModule> files) throws ClassHierarchyException {
    PythonLoaderFactory loaders = new JepPythonLoaderFactory(Collections.emptyList());

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

  // example of using this implementation
  public static void main(String... args) throws IOException, Error, ClassHierarchyException {
    IRFactory<IMethod> irs = AstIRFactory.makeDefaultFactory();

    Set<SourceModule> sources =
        Arrays.stream(args)
            .map(file -> new SourceFileModule(new File(file), file, null))
            .collect(Collectors.toSet());

    IClassHierarchy cha = load(sources);

    SSAOptions.defaultOptions()
        .setDefaultValues(
            new DefaultValues() {
              @Override
              public int getDefaultValue(SymbolTable symtab, int valueNumber) {
                return symtab.getNullConstant();
              }
            });

    cha.forEach(
        c -> {
          System.err.println(c);
          c.getDeclaredMethods()
              .forEach(
                  m -> {
                    System.err.println(m);
                    System.err.println(
                        irs.makeIR(m, Everywhere.EVERYWHERE, SSAOptions.defaultOptions()));
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
