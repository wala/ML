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

import static com.ibm.wala.cast.python.util.Util.removeFileProtocolFromPath;

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
import com.ibm.wala.core.util.warnings.Warning;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.ReverseIterator;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
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

public abstract class PythonParser<T> extends AbstractParser implements TranslatorToCAst {

  private static boolean COMPREHENSION_IR = true;

  private CAstType codeBody =
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

  interface WalkContext extends TranslatorToCAst.WalkContext<WalkContext, PythonTree> {

    WalkContext getParent();

    default CAstEntity entity() {
      return getParent().entity();
    }
  }

  private static class RootContext extends TranslatorToCAst.RootContext<WalkContext, PythonTree>
      implements WalkContext {
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

  private static class FunctionContext
      extends TranslatorToCAst.FunctionContext<WalkContext, PythonTree> implements WalkContext {
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

  private class CAstVisitor extends AbstractParser.CAstVisitor implements VisitorIF<CAstNode> {
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
      String s = parser.getText(p.getCharStartIndex(), p.getCharStopIndex());
      String[] lines = s.split("\n");
      int last_col;
      int last_line = p.getLineno() + lines.length - 1;
      if ("".equals(s) || lines.length <= 1) {
        last_col = p.getCol_offset() + (p.getCharStopIndex() - p.getCharStartIndex());
      } else {
        assert (lines.length > 1);
        last_col = lines[lines.length - 1].length();
      }

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
          return p.getLineno();
        }

        @Override
        public int getFirstCol() {
          return p.getCol_offset();
        }

        @Override
        public int getLastLine() {
          return last_line;
        }

        @Override
        public int getLastCol() {
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

    public CAstNode notePosition(CAstNode n, Position pos) {
      pushSourcePosition(context, n, pos);
      return n;
    }

    @Override
    public CAstNode visitAssert(Assert arg0) throws Exception {
      return Ast.makeNode(CAstNode.EMPTY);
      // return notePosition(Ast.makeNode(CAstNode.ASSERT, arg0.getInternalTest().accept(this)),
      // arg0);
    }

    private int assign = 0;

    @Override
    public CAstNode visitAssign(Assign arg0) throws Exception {
      String rvalName = "rval" + assign++;
      CAstNode v = notePosition(arg0.getInternalValue().accept(this), arg0.getInternalValue());

      if (context.entity().getKind() == CAstEntity.TYPE_ENTITY) {
        for (expr lhs : arg0.getInternalTargets()) {
          context.addScopedEntity(
              null,
              new AbstractFieldEntity(
                  lhs.getText(), Collections.emptySet(), false, context.entity()) {
                @Override
                public CAstNode getAST() {
                  return v;
                }

                @Override
                public Position getPosition(int arg) {
                  return null;
                }

                @Override
                public Position getNamePosition() {
                  return makePosition(lhs);
                }
              });
        }
        return Ast.makeNode(CAstNode.EMPTY);
      } else {
        java.util.List<CAstNode> nodes = new ArrayList<CAstNode>();
        if (arg0.getInternalTargets().size() > 1) {
          CAstNode rval =
              Ast.makeNode(
                  CAstNode.DECL_STMT,
                  Ast.makeConstant(new CAstSymbolImpl(rvalName, PythonCAstToIRTranslator.Any)),
                  v);
          nodes.add(rval);
          for (expr lhs : arg0.getInternalTargets()) {
            nodes.add(
                notePosition(
                    Ast.makeNode(
                        CAstNode.ASSIGN,
                        notePosition(lhs.accept(this), lhs),
                        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(rvalName))),
                    lhs));
          }
        } else {
          for (expr lhs : arg0.getInternalTargets()) {
            nodes.add(
                notePosition(
                    Ast.makeNode(CAstNode.ASSIGN, notePosition(lhs.accept(this), lhs), v), lhs));
          }
        }
        return Ast.makeNode(CAstNode.BLOCK_EXPR, nodes.toArray(new CAstNode[nodes.size()]));
      }
    }

    @Override
    public CAstNode visitAttribute(Attribute arg0) throws Exception {
      return notePosition(
          Ast.makeNode(
              CAstNode.OBJECT_REF,
              notePosition(arg0.getInternalValue().accept(this), arg0.getInternalValue()),
              Ast.makeConstant(arg0.getInternalAttr())),
          arg0);
    }

    @Override
    public CAstNode visitAugAssign(AugAssign arg0) throws Exception {
      return notePosition(
          Ast.makeNode(
              CAstNode.ASSIGN_POST_OP,
              arg0.getInternalTarget().accept(this),
              arg0.getInternalValue().accept(this),
              translateOperator(arg0.getInternalOp())),
          arg0);
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
            v =
                notePosition(
                    Ast.makeNode(
                        CAstNode.IF_EXPR,
                        Ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, n),
                        v,
                        Ast.makeConstant(false)),
                    arg0);
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
      CAstNode args[] =
          new CAstNode[arg0.getInternalArgs().size() + arg0.getInternalKeywords().size() + 1];
      args[i++] = Ast.makeNode(CAstNode.EMPTY);
      for (expr e : arg0.getInternalArgs()) {
        args[i++] = notePosition(e.accept(this), e);
      }
      for (keyword k : arg0.getInternalKeywords()) {
        args[i++] =
            notePosition(
                Ast.makeNode(
                    CAstNode.ARRAY_LITERAL,
                    Ast.makeConstant(k.getInternalArg()),
                    notePosition(k.getInternalValue().accept(this), k.getInternalValue())),
                k);
      }

      CAstNode f = notePosition(arg0.getInternalFunc().accept(this), arg0.getInternalFunc());

      CAstNode call = notePosition(Ast.makeNode(CAstNode.CALL, f, args), arg0);

      return call;
    }

    @Override
    public CAstNode visitClassDef(ClassDef arg0) throws Exception {
      WalkContext parent = this.context;

      CAstType.Class cls =
          new CAstType.Class() {
            @Override
            public Collection<CAstType> getSupertypes() {
              Collection<CAstType> supertypes = HashSetFactory.make();
              for (expr e : arg0.getInternalBases()) {
                System.out.println(
                    arg0.getInternalName()
                        + " "
                        + arg0.getType()
                        + " extends "
                        + e.getText()
                        + " "
                        + e.getType());
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

            @Override
            public CAstControlFlowRecorder cfg() {
              return (CAstControlFlowRecorder) parent.entity().getControlFlow();
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
      for (stmt e : arg0.getInternalBody()) {
        if (!(e instanceof Pass)) {
          e.accept(v);
        }
      }

      CAstNode x = Ast.makeNode(CAstNode.CLASS_STMT, Ast.makeConstant(clse));
      context.addScopedEntity(x, clse);
      return x;
    }

    private int compareTmp = 0;

    private CAstNode compare(CAstNode lhs, Iterator<cmpopType> ops, Iterator<expr> rhss)
        throws Exception {
      if (ops.hasNext()) {
        String vn = "" + compareTmp++;

        Ast.makeNode(
            CAstNode.ASSIGN,
            Ast.makeNode(
                CAstNode.OBJECT_REF,
                Ast.makeNode(CAstNode.VAR, Ast.makeConstant(vn)),
                rhss.next().accept(this)));

        CAstOperator op = translateOperator(ops.next());

        CAstNode rest = compare(Ast.makeNode(CAstNode.VAR, Ast.makeConstant(vn)), ops, rhss);

        return Ast.makeNode(
            CAstNode.IF_EXPR,
            Ast.makeNode(
                CAstNode.BINARY_EXPR, op, lhs, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(vn))),
            Ast.makeConstant(true),
            rest);
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
      return notePosition(
          compare(
              arg0.getInternalLeft().accept(this),
              arg0.getInternalOps().iterator(),
              arg0.getInternalComparators().iterator()),
          arg0);
    }

    @Override
    public CAstNode visitContinue(Continue arg0) throws Exception {
      PyObject target = context.getContinueFor(null);
      context.cfg().add(arg0, target, null);
      CAstNode gt = notePosition(Ast.makeNode(CAstNode.GOTO), arg0);
      context.cfg().map(arg0, gt);
      return gt;
    }

    @Override
    public CAstNode visitDelete(Delete arg0) throws Exception {
      int i = 0;
      CAstNode[] dels = new CAstNode[arg0.getInternalTargets().size()];
      for (expr e : arg0.getInternalTargets()) {
        dels[i++] =
            notePosition(
                Ast.makeNode(
                    CAstNode.CALL,
                    Ast.makeNode(CAstNode.VAR, Ast.makeConstant("__delete__")),
                    e.accept(this)),
                e);
      }

      return Ast.makeNode(CAstNode.BLOCK_EXPR, dels);
    }

    @Override
    public CAstNode visitDict(Dict arg0) throws Exception {
      int i = 0;
      CAstNode args[] = new CAstNode[arg0.getInternalKeys().size() * 2 + 1];
      Iterator<expr> keys = arg0.getInternalKeys().iterator();
      Iterator<expr> vals = arg0.getInternalValues().iterator();
      args[i++] = Ast.makeNode(CAstNode.NEW, Ast.makeConstant("dict"));
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
          Ast.makeNode(
              CAstNode.ASSIGN,
              Ast.makeNode(
                  CAstNode.OBJECT_REF,
                  Ast.makeNode(CAstNode.VAR, Ast.makeConstant(dictName)),
                  arg0.getInternalKey().accept(this)),
              arg0.getInternalValue().accept(this));

      return Ast.makeNode(
          CAstNode.BLOCK_EXPR,
          Ast.makeNode(
              CAstNode.DECL_STMT,
              Ast.makeConstant(new CAstSymbolImpl(dictName, PythonCAstToIRTranslator.Any)),
              Ast.makeNode(CAstNode.NEW, Ast.makeConstant(PythonTypes.dict))),
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
      CAstNode children[] = new CAstNode[arg0.getInternalDims().size()];
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
      CAstNode[] body = new CAstNode[arg0.getInternalBody().size()];
      for (stmt s : arg0.getInternalBody()) {
        body[i++] = s.accept(child);
      }

      comprehension g = new comprehension();
      g.setIter(arg0.getIter());
      g.setTarget(arg0.getTarget());

      return Ast.makeNode(
          CAstNode.BLOCK_EXPR,
          doGenerators(
              Collections.singletonList(g),
              Ast.makeNode(
                  CAstNode.BLOCK_EXPR, Ast.makeNode(CAstNode.BLOCK_EXPR, body), continueStmt)),
          breakStmt);
    }

    @Override
    public CAstNode visitFunctionDef(FunctionDef arg0) throws Exception {
      return defineFunction(
          arg0.getInternalName(),
          arg0.getInternalArgs().getInternalArgs(),
          arg0.getInternalBody(),
          arg0,
          makePosition(arg0.getInternalNameNode()),
          codeBody,
          arg0.getInternalArgs().getInternalDefaults());
    }

    private <S extends PythonTree> CAstNode defineFunction(
        String functionName,
        java.util.List<expr> arguments,
        java.util.List<S> body,
        PythonTree function,
        Position namePos,
        CAstType superType,
        java.util.List<expr> defaults)
        throws Exception {
      int i = 0;
      CAstNode[] nodes = new CAstNode[body.size()];

      CAstNode[] defaultVars;
      CAstNode[] defaultCode;
      if (defaults != null && defaults.size() > 0) {
        int arg = 0;
        defaultVars = new CAstNode[defaults.size()];
        defaultCode = new CAstNode[defaults.size()];
        for (expr dflt : defaults) {
          String name = functionName + "_default_" + arg;
          defaultCode[arg] =
              Ast.makeNode(
                  CAstNode.DECL_STMT,
                  Ast.makeConstant(new CAstSymbolImpl(name, PythonCAstToIRTranslator.Any)),
                  dflt.accept(this));
          defaultVars[arg++] = Ast.makeNode(CAstNode.VAR, Ast.makeConstant(name));
        }
      } else {
        defaultVars = defaultCode = new CAstNode[0];
      }

      class PythonCodeType implements CAstType {

        @Override
        public Collection<CAstType> getSupertypes() {
          return Collections.singleton(superType);
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
          for (int i = 0; i < getArgumentCount() + 1; i++) {
            types.add(CAstType.DYNAMIC);
          }
          return types;
        }

        public int getArgumentCount() {
          int sz = 1;
          for (expr e : arguments) {
            sz += (e instanceof Tuple) ? ((Tuple) e).getInternalElts().size() : 1;
          }
          return sz + 1;
        }

        @Override
        public String toString() {
          return getName();
        }
      }
      ;

      CAstType functionType;
      boolean isMethod = context.entity().getKind() == CAstEntity.TYPE_ENTITY;
      if (isMethod) {
        class PythonMethod extends PythonCodeType implements CAstType.Method {
          @Override
          public CAstType getDeclaringType() {
            return context.entity().getType();
          }

          @Override
          public boolean isStatic() {
            return false;
          }
        }
        ;

        functionType = new PythonMethod();
      } else {
        class PythonFunction extends PythonCodeType implements CAstType.Function {}
        ;

        functionType = new PythonFunction();
      }

      int x = 0;
      int sz = 1;
      for (expr e : arguments) {
        sz += (e instanceof Tuple) ? ((Tuple) e).getInternalElts().size() : 1;
      }
      String[] argumentNames = new String[sz];
      int[] argumentMap = new int[sz];
      int argIndex = 0;
      argumentNames[x++] = "the function";
      argumentMap[argIndex++] = 0;
      for (expr a : arguments) {
        if (a instanceof Tuple) {
          Tuple t = (Tuple) a;
          for (expr e : t.getInternalElts()) {
            CAstNode cast = e.accept(this);
            String name = cast.getChild(0).getValue().toString();
            argumentMap[x] = argIndex;
            argumentNames[x++] = name;
          }
        } else {
          String name = a.accept(this).getChild(0).getValue().toString();
          argumentMap[x] = argIndex;
          argumentNames[x++] = name;
        }
        argIndex++;
      }

      AbstractCodeEntity fun =
          new AbstractCodeEntity(functionType) {
            @Override
            public int getKind() {
              return CAstEntity.FUNCTION_ENTITY;
            }

            @Override
            public CAstNode getAST() {
              if (function instanceof FunctionDef) {
                if (isMethod) {
                  CAst Ast = PythonParser.this.Ast;
                  CAstNode[] newNodes = new CAstNode[nodes.length + 2];
                  System.arraycopy(nodes, 0, newNodes, 2, nodes.length);

                  newNodes[0] =
                      Ast.makeNode(
                          CAstNode.DECL_STMT,
                          Ast.makeConstant(
                              new CAstSymbolImpl("super", PythonCAstToIRTranslator.Any)),
                          Ast.makeNode(CAstNode.NEW, Ast.makeConstant("superfun")));
                  newNodes[1] =
                      Ast.makeNode(
                          CAstNode.BLOCK_STMT,
                          Ast.makeNode(
                              CAstNode.ASSIGN,
                              Ast.makeNode(
                                  CAstNode.OBJECT_REF,
                                  Ast.makeNode(CAstNode.VAR, Ast.makeConstant("super")),
                                  Ast.makeConstant("$class")),
                              Ast.makeNode(
                                  CAstNode.VAR,
                                  Ast.makeConstant(context.entity().getType().getName()))),
                          Ast.makeNode(
                              CAstNode.ASSIGN,
                              Ast.makeNode(
                                  CAstNode.OBJECT_REF,
                                  Ast.makeNode(CAstNode.VAR, Ast.makeConstant("super")),
                                  Ast.makeConstant("$self")),
                              Ast.makeNode(CAstNode.VAR, Ast.makeConstant(getArgumentNames()[1]))));

                  return PythonParser.this.Ast.makeNode(CAstNode.BLOCK_STMT, newNodes);
                } else {
                  return PythonParser.this.Ast.makeNode(CAstNode.BLOCK_STMT, nodes);
                }
              } else {
                return PythonParser.this.Ast.makeNode(
                    CAstNode.RETURN, PythonParser.this.Ast.makeNode(CAstNode.BLOCK_EXPR, nodes));
              }
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
              return defaultVars;
            }

            @Override
            public int getArgumentCount() {
              return argumentNames.length;
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
              return makePosition(arguments.get(argumentMap[arg]));
            }

            @Override
            public Position getNamePosition() {
              return namePos;
            }
          };

      PythonParser.FunctionContext child = new PythonParser.FunctionContext(context, fun, function);
      CAstVisitor cv = new CAstVisitor(child, parser);
      for (S s : body) {
        nodes[i++] = s.accept(cv);
      }

      if (isMethod) {
        context.addScopedEntity(null, fun);
        return null;

      } else {
        CAstNode stmt = Ast.makeNode(CAstNode.FUNCTION_EXPR, Ast.makeConstant(fun));
        context.addScopedEntity(stmt, fun);
        CAstNode val =
            !(function instanceof FunctionDef)
                ? stmt
                : Ast.makeNode(
                    CAstNode.DECL_STMT,
                    Ast.makeConstant(
                        new CAstSymbolImpl(fun.getName(), PythonCAstToIRTranslator.Any)),
                    stmt);

        if (defaultCode.length == 0) {
          return val;
        } else {
          return Ast.makeNode(
              CAstNode.BLOCK_EXPR, Ast.makeNode(CAstNode.BLOCK_EXPR, defaultCode), val);
        }
      }
    }

    @Override
    public CAstNode visitGeneratorExp(GeneratorExp arg0) throws Exception {
      return fail(arg0);
    }

    @Override
    public CAstNode visitGlobal(Global arg0) throws Exception {
      java.util.List<Name> internalNames = arg0.getInternalNameNodes();
      CAstNode[] x = new CAstNode[arg0.getInternalNameNodes().size()];
      for (int i = 0; i < x.length; i++) x[i] = internalNames.get(i).accept(this);
      return Ast.makeNode(CAstNode.GLOBAL_DECL, x);
    }

    private CAstNode block(java.util.List<stmt> block) throws Exception {
      CAstNode[] x = new CAstNode[block.size()];
      for (int i = 0; i < block.size(); i++) {
        x[i] = block.get(i).accept(this);
      }
      return Ast.makeNode(CAstNode.BLOCK_STMT, x);
    }

    @Override
    public CAstNode visitIf(If arg0) throws Exception {
      return Ast.makeNode(
          CAstNode.IF_STMT,
          arg0.getInternalTest().accept(this),
          block(arg0.getInternalBody()),
          block(arg0.getInternalOrelse()));
    }

    @Override
    public CAstNode visitIfExp(IfExp arg0) throws Exception {
      return Ast.makeNode(
          CAstNode.IF_EXPR,
          arg0.getInternalTest().accept(this),
          arg0.getInternalBody().accept(this),
          arg0.getInternalOrelse().accept(this));
    }

    private String name(alias n) {
      String s = n.getInternalAsname() == null ? n.getInternalName() : n.getInternalAsname();
      if (s.contains(".")) {
        s = s.substring(s.lastIndexOf('.') + 1);
      }
      return s;
    }

    @Override
    public CAstNode visitImport(Import arg0) throws Exception {
      int i = 0;
      CAstNode[] elts = new CAstNode[arg0.getInternalNames().size()];
      for (alias n : arg0.getInternalNames()) {
        CAstNode obj = importAst(arg0, n.getInternalNameNodes());
        elts[i++] =
            notePosition(
                Ast.makeNode(
                    CAstNode.DECL_STMT,
                    Ast.makeConstant(new CAstSymbolImpl(name(n), PythonCAstToIRTranslator.Any)),
                    obj != null
                        ? obj
                        : Ast.makeNode(
                            CAstNode.PRIMITIVE,
                            Ast.makeConstant("import"),
                            Ast.makeConstant(n.getInternalName()))),
                n);
      }
      return Ast.makeNode(CAstNode.BLOCK_STMT, elts);
    }

    @Override
    public CAstNode visitImportFrom(ImportFrom arg0) throws Exception {
      String tree = "importTree" + (++tmpIndex);
      CAstNode[] elts = new CAstNode[arg0.getInternalNames().size() + 1];

      elts[0] =
          Ast.makeNode(
              CAstNode.DECL_STMT,
              Ast.makeConstant(new CAstSymbolImpl(tree, PythonCAstToIRTranslator.Any)),
              importAst(arg0, arg0.getInternalModuleNames()));

      int i = 1;
      for (alias n : arg0.getInternalNames()) {
        elts[i++] =
            notePosition(
                Ast.makeNode(
                    CAstNode.DECL_STMT,
                    Ast.makeConstant(new CAstSymbolImpl(name(n), PythonCAstToIRTranslator.Any)),
                    Ast.makeNode(
                        CAstNode.OBJECT_REF,
                        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(tree)),
                        Ast.makeConstant(n.getInternalName()))),
                n);
      }

      return Ast.makeNode(CAstNode.BLOCK_STMT, elts);
    }

    private final boolean wholeStatement = true;

    private <R extends stmt> CAstNode importAst(R importNode, java.util.List<Name> names) {
      CAstNode importAst =
          notePosition(
              Ast.makeNode(
                  CAstNode.PRIMITIVE,
                  Ast.makeConstant("import"),
                  Ast.makeConstant(names.get(0).getInternalId())),
              wholeStatement ? importNode : names.get(0));
      for (int i = 1; i < names.size(); i++) {
        importAst =
            notePosition(
                Ast.makeNode(
                    CAstNode.OBJECT_REF, importAst, Ast.makeConstant(names.get(i).getInternalId())),
                names.get(i));
      }
      return importAst;
    }

    @Override
    public CAstNode visitIndex(Index arg0) throws Exception {
      return arg0.getInternalValue().accept(this);
    }

    @Override
    public CAstNode visitInteractive(Interactive arg0) throws Exception {
      return fail(arg0);
    }

    private CAstType lambda =
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

    @Override
    public CAstNode visitLambda(Lambda arg0) throws Exception {
      arguments lambdaArgs = arg0.getInternalArgs();
      expr lambdaBody = arg0.getInternalBody();
      return defineFunction(
          "lambda" + (++tmpIndex),
          lambdaArgs.getInternalArgs(),
          Collections.singletonList(lambdaBody),
          arg0,
          makePosition(arg0.getChildren().get(0)),
          lambda,
          lambdaArgs.getInternalDefaults());
    }

    private CAstNode collectObjects(java.util.List<expr> eltList, String type) throws Exception {
      int i = 0, j = 0;
      CAstNode[] elts = new CAstNode[2 * eltList.size() + 1];
      elts[i++] = Ast.makeNode(CAstNode.NEW, Ast.makeConstant(type));
      for (expr e : eltList) {
        elts[i++] = Ast.makeConstant(j++);
        elts[i++] = e.accept(this);
      }
      return Ast.makeNode(CAstNode.OBJECT_LITERAL, elts);
    }

    @Override
    public CAstNode visitList(List arg0) throws Exception {
      return collectObjects(arg0.getInternalElts(), "list");
    }

    @Override
    public CAstNode visitListComp(ListComp arg0) throws Exception {
      return visitComp(arg0.getInternalElt(), arg0.getInternalGenerators(), PythonTypes.list);
    }

    private CAstType comprehension =
        new CAstType() {

          @Override
          public String getName() {
            return "comprehension";
          }

          @Override
          public Collection<CAstType> getSupertypes() {
            return Collections.singleton(codeBody);
          }
        };

    private CAstNode comprehensionLambda(expr value, java.util.List<comprehension> gen)
        throws Exception {
      String name = "comprehension" + (++tmpIndex);

      java.util.List<expr> arguments = new LinkedList<>();
      gen.forEach(
          (x) -> {
            arguments.add(x.getInternalTarget());
          });

      return defineFunction(
          name, arguments, Collections.singletonList(value), value, null, comprehension, null);
    }

    private CAstType filter =
        new CAstType() {

          @Override
          public String getName() {
            return "filter";
          }

          @Override
          public Collection<CAstType> getSupertypes() {
            return Collections.singleton(codeBody);
          }
        };

    private CAstNode[] comprehensionFilters(java.util.List<comprehension> gen) throws Exception {
      String name = "filter" + (++tmpIndex);

      java.util.List<expr> arguments = new LinkedList<>();
      gen.forEach(
          (x) -> {
            arguments.add(x.getInternalTarget());
          });

      java.util.List<CAstNode> filters = new LinkedList<>();
      for (comprehension g : gen) {
        for (expr test : g.getInternalIfs()) {
          filters.add(
              defineFunction(
                  name, arguments, Collections.singletonList(test), g, null, filter, null));
        }
      }

      return filters.toArray(new CAstNode[filters.size()]);
    }

    private CAstNode visitComp(expr value, java.util.List<comprehension> gen, TypeReference type)
        throws Exception {
      if (COMPREHENSION_IR) {
        java.util.List<CAstNode> arguments = new LinkedList<>();
        for (comprehension x : gen) {
          arguments.add(x.getInternalIter().accept(this));
        }

        CAstNode lambda = comprehensionLambda(value, gen);
        CAstNode[] filters = comprehensionFilters(gen);

        return Ast.makeNode(
            CAstNode.COMPREHENSION_EXPR,
            Ast.makeNode(CAstNode.NEW, Ast.makeConstant(type.getName().toString().substring(1))),
            lambda,
            Ast.makeNode(CAstNode.EXPR_LIST, arguments.toArray(new CAstNode[arguments.size()])),
            Ast.makeNode(CAstNode.EXPR_LIST, filters));

      } else {
        String listName = "temp " + ++tmpIndex;
        String indexName = "temp " + ++tmpIndex;
        CAstNode body =
            Ast.makeNode(
                CAstNode.BLOCK_EXPR,
                Ast.makeNode(
                    CAstNode.ASSIGN,
                    Ast.makeNode(
                        CAstNode.ARRAY_REF,
                        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(listName)),
                        Ast.makeConstant(PythonTypes.Root),
                        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(indexName))),
                    value.accept(this)),
                Ast.makeNode(
                    CAstNode.ASSIGN,
                    Ast.makeNode(CAstNode.VAR, Ast.makeConstant(indexName)),
                    Ast.makeNode(
                        CAstNode.BINARY_EXPR,
                        CAstOperator.OP_ADD,
                        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(indexName)),
                        Ast.makeConstant(1))));

        return Ast.makeNode(
            CAstNode.BLOCK_EXPR,
            Ast.makeNode(
                CAstNode.DECL_STMT,
                Ast.makeConstant(new CAstSymbolImpl(listName, PythonCAstToIRTranslator.Any)),
                Ast.makeNode(CAstNode.NEW, Ast.makeConstant(type))),
            Ast.makeNode(
                CAstNode.DECL_STMT,
                Ast.makeConstant(new CAstSymbolImpl(indexName, PythonCAstToIRTranslator.Any)),
                Ast.makeConstant(0)),
            doGenerators(gen, body),
            Ast.makeNode(CAstNode.VAR, Ast.makeConstant(listName)));
      }
    }

    private CAstNode doGenerators(java.util.List<comprehension> generators, CAstNode body)
        throws Exception {
      CAstNode result = body;

      for (comprehension c : generators) {
        if (c.getInternalIfs() != null) {
          int j = c.getInternalIfs().size();
          if (j > 0) {
            for (expr test : c.getInternalIfs()) {
              CAstNode v = test.accept(this);
              result = Ast.makeNode(CAstNode.IF_EXPR, v, body);
            }
          }
        }

        String tempName = "temp " + ++tmpIndex;

        CAstNode test =
            Ast.makeNode(
                CAstNode.BINARY_EXPR,
                CAstOperator.OP_NE,
                Ast.makeConstant(null),
                Ast.makeNode(
                    CAstNode.BLOCK_EXPR,
                    Ast.makeNode(
                        CAstNode.ASSIGN,
                        c.getInternalTarget().accept(this),
                        Ast.makeNode(
                            CAstNode.EACH_ELEMENT_GET,
                            Ast.makeNode(CAstNode.VAR, Ast.makeConstant(tempName)),
                            c.getInternalTarget().accept(this)))));

        result =
            Ast.makeNode(
                CAstNode.BLOCK_EXPR,
                Ast.makeNode(
                    CAstNode.DECL_STMT,
                    Ast.makeConstant(new CAstSymbolImpl(tempName, PythonCAstToIRTranslator.Any)),
                    c.getInternalIter().accept(this)),
                Ast.makeNode(
                    CAstNode.LOOP,
                    test,
                    Ast.makeNode(
                        CAstNode.BLOCK_EXPR,
                        Ast.makeNode(
                            CAstNode.ASSIGN,
                            c.getInternalTarget().accept(this),
                            Ast.makeNode(
                                CAstNode.OBJECT_REF,
                                Ast.makeNode(CAstNode.VAR, Ast.makeConstant(tempName)),
                                c.getInternalTarget().accept(this))),
                        result)));
      }

      return result;
    }

    @Override
    public CAstNode visitModule(Module arg0) throws Exception {
      if (arg0.getChildren() != null) {
        java.util.List<CAstNode> elts = new ArrayList<CAstNode>(arg0.getChildCount());
        defaultImports(elts);
        for (PythonTree c : arg0.getChildren()) {
          elts.add(c.accept(this));
        }
        return Ast.makeNode(CAstNode.BLOCK_EXPR, elts.toArray(new CAstNode[elts.size()]));
      } else {
        return Ast.makeNode(CAstNode.EMPTY);
      }
    }

    @Override
    public CAstNode visitName(Name arg0) throws Exception {
      String name = arg0.getText();
      if (name.equals("True")) return Ast.makeConstant(true);
      else if (name.equals("False")) return Ast.makeConstant(false);
      else if (name.equals("None")) return Ast.makeConstant(null);

      return notePosition(Ast.makeNode(CAstNode.VAR, Ast.makeConstant(name)), arg0);
    }

    @Override
    public CAstNode visitNum(Num arg0) throws Exception {
      String numStr = arg0.getInternalN().toString();

      if (numStr.contains("l") | numStr.contains("L"))
        return Ast.makeConstant(Long.parseLong(numStr.substring(0, numStr.length() - 1)));
      else {
        try {
          return Ast.makeConstant(Long.parseLong(numStr));
        } catch (NumberFormatException e) {
          try {
            return Ast.makeConstant(Double.parseDouble(numStr));
          } catch (NumberFormatException ee) {
            return Ast.makeConstant(arg0.getInternalN());
          }
        }
      }
    }

    @Override
    public CAstNode visitPass(Pass arg0) throws Exception {
      String label = "temp " + ++tmpIndex;
      CAstNode nothing =
          Ast.makeNode(CAstNode.LABEL_STMT, Ast.makeConstant(label), Ast.makeNode(CAstNode.EMPTY));
      context.cfg().map(arg0, nothing);
      return nothing;
    }

    @Override
    public CAstNode visitPrint(Print arg0) throws Exception {
      int i = 0;
      CAstNode[] elts = new CAstNode[arg0.getInternalValues().size()];
      for (expr e : arg0.getInternalValues()) {
        elts[i++] = e.accept(this);
      }
      return Ast.makeNode(CAstNode.ECHO, elts);
    }

    @Override
    public CAstNode visitRaise(Raise arg0) throws Exception {
      if (arg0.getInternalType() == null) {
        return Ast.makeNode(
            CAstNode.THROW, Ast.makeNode(CAstNode.VAR, Ast.makeConstant("$currentException")));
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
      if (arg0.getInternalValue() == null)
        return Ast.makeNode(CAstNode.RETURN, Ast.makeNode(CAstNode.VAR, Ast.makeConstant("None")));
      else return Ast.makeNode(CAstNode.RETURN, arg0.getInternalValue().accept(this));
    }

    @Override
    public CAstNode visitSet(Set arg0) throws Exception {
      return collectObjects(arg0.getInternalElts(), "set");
    }

    @Override
    public CAstNode visitSetComp(SetComp arg0) throws Exception {
      return visitComp(arg0.getInternalElt(), arg0.getInternalGenerators(), PythonTypes.set);
    }

    private CAstNode acceptOrNull(PythonTree x) throws Exception {
      return (x == null) ? Ast.makeNode(CAstNode.EMPTY) : notePosition(x.accept(this), x);
    }

    @Override
    public CAstNode visitSlice(Slice arg0) throws Exception {
      return Ast.makeNode(
          CAstNode.ARRAY_LITERAL,
          acceptOrNull(arg0.getInternalLower()),
          acceptOrNull(arg0.getInternalUpper()),
          acceptOrNull(arg0.getInternalStep()));
    }

    @Override
    public CAstNode visitStr(Str arg0) throws Exception {
      return notePosition(Ast.makeConstant(arg0.getInternalS().toString()), arg0);
    }

    @Override
    public CAstNode visitSubscript(Subscript arg0) throws Exception {
      slice s = arg0.getInternalSlice();
      if (s instanceof Index) {
        return notePosition(
            Ast.makeNode(
                CAstNode.OBJECT_REF,
                acceptOrNull(arg0.getInternalValue()),
                acceptOrNull(arg0.getInternalSlice())),
            arg0);
      } else if (s instanceof Slice) {
        Slice S = (Slice) s;
        return notePosition(
            Ast.makeNode(
                CAstNode.CALL,
                Ast.makeNode(CAstNode.VAR, Ast.makeConstant("slice")),
                Ast.makeNode(CAstNode.EMPTY),
                acceptOrNull(arg0.getInternalValue()),
                acceptOrNull(S.getInternalLower()),
                acceptOrNull(S.getInternalUpper()),
                acceptOrNull(S.getInternalStep())),
            arg0);
      } else {
        return acceptOrNull(arg0.getInternalValue());
      }
    }

    @Override
    public CAstNode visitSuite(Suite arg0) throws Exception {
      return block(arg0.getInternalBody());
    }

    private class TryCatchContext extends TranslatorToCAst.TryCatchContext<WalkContext, PythonTree>
        implements WalkContext {

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
      Map<String, CAstNode> handlers = HashMapFactory.make();
      for (PyObject x : arg0.getChildren()) {
        if (x instanceof ExceptHandler) {
          ExceptHandler h = (ExceptHandler) x;
          CAstNode name =
              h.getInternalName() == null
                  ? Ast.makeConstant("x")
                  : h.getInternalName().accept(this);
          CAstNode type =
              h.getInternalType() == null
                  ? Ast.makeConstant("any")
                  : h.getInternalType().accept(this);
          CAstNode body = block(h.getInternalBody());
          handlers.put(
              type.toString(),
              Ast.makeNode(
                  CAstNode.CATCH,
                  Ast.makeConstant(name),
                  Ast.makeNode(
                      CAstNode.BLOCK_STMT,
                      Ast.makeNode(
                          CAstNode.ASSIGN,
                          Ast.makeNode(CAstNode.VAR, Ast.makeConstant("$currentException")),
                          Ast.makeNode(CAstNode.VAR, Ast.makeConstant(name.getValue()))),
                      body)));

          if (h.getInternalType() != null) {
            context.getNodeTypeMap().add(name, types.getCAstTypeFor(h.getInternalType()));
          }
        }
      }

      TryCatchContext catches = new TryCatchContext(context, handlers);
      CAstVisitor child = new CAstVisitor(catches, parser);
      CAstNode block = child.block(arg0.getInternalBody());

      return Ast.makeNode(
          CAstNode.TRY,
          Ast.makeNode(CAstNode.BLOCK_EXPR, block, block(arg0.getInternalOrelse())),
          handlers.values().toArray(new CAstNode[handlers.size()]));
    }

    @Override
    public CAstNode visitTryFinally(TryFinally arg0) throws Exception {
      return Ast.makeNode(
          CAstNode.UNWIND, block(arg0.getInternalBody()), block(arg0.getInternalFinalbody()));
    }

    @Override
    public CAstNode visitTuple(Tuple arg0) throws Exception {
      return collectObjects(arg0.getInternalElts(), "tuple");
      /*
      			int i = 0;
      			CAstNode[] elts = new CAstNode[ arg0.getInternalElts().size()+1 ];

      			elts[i++] = Ast.makeNode(CAstNode.NEW, Ast.makeConstant("tuple"));
      			for(expr e : arg0.getInternalElts()) {
      				elts[i++] = e.accept(this);
      			}
      			return Ast.makeNode(CAstNode.ARRAY_LITERAL, elts);
      */
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

    private class LoopContext extends TranslatorToCAst.LoopContext<WalkContext, PythonTree>
        implements WalkContext {

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
        return Ast.makeNode(
            CAstNode.BLOCK_EXPR,
            Ast.makeNode(
                CAstNode.LOOP,
                arg0.getInternalTest().accept(child),
                Ast.makeNode(
                    CAstNode.BLOCK_EXPR, child.block(arg0.getInternalBody()), c.accept(child))),
            b.accept(child));
      } else {
        return Ast.makeNode(
            CAstNode.BLOCK_EXPR,
            Ast.makeNode(
                CAstNode.LOOP,
                Ast.makeNode(
                    CAstNode.ASSIGN,
                    Ast.makeNode(CAstNode.VAR, Ast.makeConstant("test tmp")),
                    arg0.getInternalTest().accept(child)),
                Ast.makeNode(
                    CAstNode.BLOCK_EXPR, child.block(arg0.getInternalBody()), c.accept(child))),
            Ast.makeNode(
                CAstNode.IF_STMT,
                Ast.makeNode(
                    CAstNode.UNARY_EXPR,
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
      CAstNode[] blk = new CAstNode[arg0.getInternalBody().size()];
      for (stmt s : arg0.getInternalBody()) {
        blk[i++] = s.accept(this);
      }

      String tmpName = "tmp_" + ++tmpIndex;

      Supplier<CAstNode> v =
          () -> {
            try {
              return arg0.getInternalOptional_vars() == null
                  ? Ast.makeNode(CAstNode.VAR, Ast.makeConstant(tmpName))
                  : arg0.getInternalOptional_vars().accept(this);
            } catch (Exception e) {
              assert false : e.toString();
              return null;
            }
          };

      return Ast.makeNode(
          CAstNode.BLOCK_STMT,
          Ast.makeNode(
              CAstNode.DECL_STMT,
              Ast.makeConstant(new CAstSymbolImpl(tmpName, PythonCAstToIRTranslator.Any))),
          Ast.makeNode(
              CAstNode.DECL_STMT,
              Ast.makeConstant(
                  new CAstSymbolImpl(
                      v.get().getChild(0).getValue().toString(), PythonCAstToIRTranslator.Any)),
              arg0.getInternalContext_expr().accept(this)),
          Ast.makeNode(
              CAstNode.UNWIND,
              Ast.makeNode(
                  CAstNode.BLOCK_EXPR,
                  Ast.makeNode(
                      CAstNode.CALL,
                      Ast.makeNode(CAstNode.OBJECT_REF, v.get(), Ast.makeConstant("__begin__")),
                      Ast.makeNode(CAstNode.EMPTY)),
                  blk),
              Ast.makeNode(
                  CAstNode.CALL,
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

  /**
   * The <a href="https://docs.python.org/3/using/cmdline.html#envvar-PYTHONPATH">PYTHONPATH</a> to
   * use in the analysis.
   *
   * @apiNote PYTHONPATH is currently only supported for Python 3.
   * @see https://docs.python.org/3/tutorial/modules.html#the-module-search-path.
   */
  protected java.util.List<File> pythonPath = Collections.emptyList();

  protected PythonParser(CAstTypeDictionaryImpl<String> types, java.util.List<File> pythonPath) {
    this.types = types;
    this.pythonPath = pythonPath;
  }

  @Override
  public <C extends RewriteContext<K>, K extends CopyKey<K>> void addRewriter(
      CAstRewriterFactory<C, K> factory, boolean prepend) {
    // TODO Auto-generated method stub

  }

  @Override
  public CAstEntity translateToCAst() throws Error, IOException {
    WalaPythonParser parser = makeParser();
    Module pythonAst = (Module) parser.parseModule();

    if (!parser.getErrors().isEmpty()) {
      java.util.Set<Warning> warnings = HashSetFactory.make();
      parser
          .getErrors()
          .forEach(
              (e) -> {
                warnings.add(
                    new Warning() {

                      @Override
                      public byte getLevel() {
                        return Warning.SEVERE;
                      }

                      @Override
                      public String getMsg() {
                        return e.toString();
                      }
                    });
              });
      throw new TranslatorToCAst.Error(warnings);
    }

    try {
      CAstType scriptType =
          new CAstType() {
            @Override
            public String getName() {
              return scriptName();
            }

            @Override
            public Collection<CAstType> getSupertypes() {
              return Collections.singleton(codeBody);
            }
          };

      WalkContext root = new PythonParser.RootContext(pythonAst);
      CAstEntity script =
          new AbstractScriptEntity(scriptName(), scriptType) {

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

            @Override
            public Position getNamePosition() {
              return null;
            }

            @Override
            public String getSignature() {
              File file = this.getFile();
              java.util.List<File> pythonPath = getPythonPath();

              // If the PYTHONPATH isn't specified.
              if (pythonPath.isEmpty())
                // Revert to just the name.
                return this.getName();

              for (File pathEntry : pythonPath) {
                String pathEntryAbsolutePath = pathEntry.getAbsoluteFile().getPath();
                // Remove protocol.
                pathEntryAbsolutePath = removeFileProtocolFromPath(pathEntryAbsolutePath);

                String fileAbsolutePath = file.getAbsolutePath();

                if (fileAbsolutePath.startsWith(pathEntryAbsolutePath)) {
                  // Found it.
                  Path filePath = Paths.get(fileAbsolutePath);
                  Path pathEntryPath = Paths.get(pathEntryAbsolutePath);

                  Path scriptRelativePath = pathEntryPath.relativize(filePath);
                  return "script " + scriptRelativePath.toString();
                }
              }
              return null; // Not found.
            }
          };

      return script;
    } catch (Exception e) {
      throw new Error(
          Collections.singleton(
              new Warning(Warning.SEVERE) {
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

  /**
   * Gets the <a
   * href="https://docs.python.org/3/using/cmdline.html#envvar-PYTHONPATH">PYTHONPATH</a> to use in
   * the analysis.
   *
   * @apiNote PYTHONPATH is currently only supported for Python 3.
   * @see https://docs.python.org/3/tutorial/modules.html#the-module-search-path.
   */
  public java.util.List<File> getPythonPath() {
    return pythonPath;
  }
}
