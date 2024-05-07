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
package com.ibm.wala.cast.python.ir;

import static com.google.common.io.Files.getNameWithoutExtension;
import static com.ibm.wala.cast.python.ir.PythonLanguage.MODULE_INITIALIZATION_FILENAME;
import static com.ibm.wala.cast.python.ir.PythonLanguage.Python;
import static com.ibm.wala.cast.python.types.PythonTypes.pythonLoader;

import com.ibm.wala.cast.ir.ssa.AssignInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstInstructionFactory;
import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.loader.DynamicAnnotatableEntity;
import com.ibm.wala.cast.python.loader.PythonLoader;
import com.ibm.wala.cast.python.loader.PythonLoader.PythonClass;
import com.ibm.wala.cast.python.parser.AbstractParser.MissingType;
import com.ibm.wala.cast.python.parser.AbstractParser.PythonGlobalsEntity;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstControlFlowRecorder;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction.IOperator;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PythonCAstToIRTranslator extends AstTranslator {

  private static final Logger LOGGER = Logger.getLogger(PythonCAstToIRTranslator.class.getName());

  private final Map<CAstType, TypeName> walaTypeNames = HashMapFactory.make();
  private final Set<Pair<Scope, String>> globalDeclSet = new HashSet<>();
  private static boolean singleFileAnalysis = true;

  public PythonCAstToIRTranslator(IClassLoader loader) {
    super(loader);
  }

  public static boolean isSingleFileAnalysis() {
    return singleFileAnalysis;
  }

  public static void setSingleFileAnalysis(boolean singleFile) {
    PythonCAstToIRTranslator.singleFileAnalysis = singleFile;
  }

  @Override
  protected boolean liftDeclarationsForLexicalScoping() {
    return true;
  }

  @Override
  protected boolean hasImplicitGlobals() {
    return true;
  }

  @Override
  protected boolean useDefaultInitValues() {
    return true;
  }

  @Override
  protected boolean treatGlobalsAsLexicallyScoped() {
    return false;
  }

  @Override
  protected TypeReference defaultCatchType() {
    return PythonTypes.Exception;
  }

  @Override
  protected TypeReference makeType(CAstType type) {
    return TypeReference.findOrCreate(
        PythonTypes.pythonLoader, TypeName.string2TypeName(type.getName()));
  }

  @Override
  protected boolean defineType(CAstEntity type, WalkContext wc) {
    CAstType cls = type.getType();
    scriptScope(wc.currentScope()).declare(new CAstSymbolImpl(cls.getName(), cls));

    String typeNameStr = composeEntityName(wc, type);
    TypeName typeName = TypeName.findOrCreate("L" + typeNameStr);
    walaTypeNames.put(cls, typeName);

    Set<CAstType> present =
        cls.getSupertypes().stream()
            .filter(t -> !(t instanceof MissingType))
            .collect(Collectors.toSet());
    ;

    ((PythonLoader) loader)
        .defineType(
            typeName,
            present.isEmpty()
                ? PythonTypes.object.getName()
                : walaTypeNames.get(present.iterator().next()),
            type.getPosition(),
            cls.getSupertypes().stream()
                .filter(t -> t instanceof MissingType)
                .collect(Collectors.toSet()));

    return true;
  }

  @Override
  protected IOperator translateBinaryOpcode(CAstNode op) {
    if (CAstOperator.OP_IN == op || CAstOperator.OP_NOT_IN == op || CAstOperator.OP_POW == op) {
      return IBinaryOpInstruction.Operator.ADD;
    } else {
      return super.translateBinaryOpcode(op);
    }
  }

  private Scope scriptScope(Scope s) {
    if (s.getEntity().getKind() == CAstEntity.SCRIPT_ENTITY) {
      return s;
    } else {
      return scriptScope(s.getParent());
    }
  }

  @Override
  protected void declareFunction(CAstEntity N, WalkContext context) {
    for (String s : N.getArgumentNames()) {
      context.currentScope().declare(new CAstSymbolImpl(s, Any));
    }

    String fnName = composeEntityName(context, N);
    if (N.getType() instanceof CAstType.Method) {
      ((PythonLoader) loader)
          .defineMethodType(
              "L" + fnName,
              N.getPosition(),
              N,
              walaTypeNames.get(((CAstType.Method) N.getType()).getDeclaringType()),
              context);
    } else {
      ((PythonLoader) loader).defineFunctionType("L" + fnName, N.getPosition(), N, context);
    }
  }

  @Override
  protected void defineFunction(
      CAstEntity N,
      WalkContext definingContext,
      AbstractCFG<SSAInstruction, ? extends IBasicBlock<SSAInstruction>> cfg,
      SymbolTable symtab,
      boolean hasCatchBlock,
      Map<IBasicBlock<SSAInstruction>, TypeReference[]> catchTypes,
      boolean hasMonitorOp,
      AstLexicalInformation lexicalInfo,
      DebuggingInformation debugInfo) {
    String fnName = composeEntityName(definingContext, N);
    ((PythonLoader) loader)
        .defineCodeBodyCode(
            "L" + fnName,
            cfg,
            symtab,
            hasCatchBlock,
            catchTypes,
            hasMonitorOp,
            lexicalInfo,
            debugInfo,
            N.getArgumentDefaults().length);
  }

  @Override
  protected void defineField(CAstEntity topEntity, WalkContext context, CAstEntity fieldEntity) {
    ((PythonLoader) loader).defineField(walaTypeNames.get(topEntity.getType()), fieldEntity);
  }

  /*
  	@Override
  	protected String composeEntityName(WalkContext parent, CAstEntity f) {
  		if (f.getType() instanceof CAstType.Method) {
  			return ((CAstType.Method)f.getType()).getDeclaringType().getName() + "/" + f.getName();
  		} else {
  			return f.getName();
  		}
  	}
  */

  @Override
  protected String composeEntityName(WalkContext parent, CAstEntity f) {
    // Use the entity signature here to resolve entities in files under different directories.
    if (f.getKind() == CAstEntity.SCRIPT_ENTITY) return f.getSignature();
    else {
      String name;
      // if (f.getType() instanceof CAstType.Method) {
      //	name = ((CAstType.Method)f.getType()).getDeclaringType().getName() + "/" + f.getName();
      // } else {
      name = f.getName();
      // }

      return parent.getName() + "/" + name;
    }
  }

  @Override
  protected void doPrologue(WalkContext context) {
    if (context.currentScope().getEntity().getKind() == CAstEntity.SCRIPT_ENTITY) {
      context
          .cfg()
          .unknownInstructions(
              () -> {
                doGlobalWrite(
                    context,
                    context.currentScope().getEntity().getSignature(),
                    PythonTypes.Root,
                    1);
              });
    }

    super.doPrologue(context);
  }

  @Override
  protected void doThrow(WalkContext context, int exception) {
    context
        .cfg()
        .addInstruction(
            Python.instructionFactory()
                .ThrowInstruction(context.cfg().getCurrentInstruction(), exception));
  }

  @Override
  public void doArrayRead(
      WalkContext context, int result, int arrayValue, CAstNode arrayRef, int[] dimValues) {
    if (dimValues.length == 1) {
      int currentInstruction = context.cfg().getCurrentInstruction();
      context
          .cfg()
          .addInstruction(
              ((AstInstructionFactory) insts)
                  .PropertyRead(currentInstruction, result, arrayValue, dimValues[0]));
      context.cfg().noteOperands(currentInstruction, context.getSourceMap().getPosition(arrayRef));
    }
  }

  @Override
  public void doArrayWrite(
      WalkContext context, int arrayValue, CAstNode arrayRef, int[] dimValues, int rval) {
    assert dimValues.length == 1;
    context
        .cfg()
        .addInstruction(
            ((AstInstructionFactory) insts)
                .PropertyWrite(
                    context.cfg().getCurrentInstruction(), arrayValue, dimValues[0], rval));
  }

  @Override
  protected void doFieldRead(
      WalkContext context, int result, int receiver, CAstNode elt, CAstNode parent) {
    int currentInstruction = context.cfg().getCurrentInstruction();
    //		if (elt.getKind() == CAstNode.CONSTANT && elt.getValue() instanceof String) {
    //			FieldReference f = FieldReference.findOrCreate(PythonTypes.Root,
    // Atom.findOrCreateUnicodeAtom((String)elt.getValue()), PythonTypes.Root);
    //			context.cfg().addInstruction(Python.instructionFactory().GetInstruction(currentInstruction,
    // result, receiver, f));
    //		} else {
    visit(elt, context, this);
    assert context.getValue(elt) != -1;
    context
        .cfg()
        .addInstruction(
            ((AstInstructionFactory) insts)
                .PropertyRead(currentInstruction, result, receiver, context.getValue(elt)));
    //		}
    context
        .cfg()
        .noteOperands(
            currentInstruction,
            context.getSourceMap().getPosition(parent.getChild(0)),
            context.getSourceMap().getPosition(elt));
  }

  @Override
  protected void doFieldWrite(
      WalkContext context, int receiver, CAstNode elt, CAstNode parent, int rval) {
    //	    if (elt.getKind() == CAstNode.CONSTANT && elt.getValue() instanceof String) {
    //			FieldReference f = FieldReference.findOrCreate(PythonTypes.Root,
    // Atom.findOrCreateUnicodeAtom((String)elt.getValue()), PythonTypes.Root);
    //
    //	context.cfg().addInstruction(Python.instructionFactory().PutInstruction(context.cfg().getCurrentInstruction(), receiver, rval, f));
    //		} else {
    visit(elt, context, this);
    assert context.getValue(elt) != -1;
    context
        .cfg()
        .addInstruction(
            ((AstInstructionFactory) insts)
                .PropertyWrite(
                    context.cfg().getCurrentInstruction(), receiver, context.getValue(elt), rval));
    //		}
  }

  @Override
  protected void doMaterializeFunction(
      CAstNode node, WalkContext context, int result, int exception, CAstEntity fn) {

    String fnName = composeEntityName(context, fn);
    IClass cls = loader.lookupClass(TypeName.findOrCreate("L" + fnName));
    TypeReference type = cls.getReference();
    int idx = context.cfg().getCurrentInstruction();

    context
        .cfg()
        .unknownInstructions(
            () -> {
              context
                  .cfg()
                  .addInstruction(
                      Python.instructionFactory()
                          .NewInstruction(idx, result, NewSiteReference.make(idx, type)));
            });

    if (fn instanceof DynamicAnnotatableEntity) {
      if (((DynamicAnnotatableEntity) fn).dynamicAnnotations().iterator().hasNext()) {
        ((DynamicAnnotatableEntity) fn)
            .dynamicAnnotations()
            .forEach(
                a -> {
                  visit(a, context, this);
                  int pos = context.cfg().getCurrentInstruction();
                  CallSiteReference site = new DynamicCallSiteReference(PythonTypes.CodeBody, pos);
                  context
                      .cfg()
                      .addInstruction(
                          new PythonInvokeInstruction(
                              context.cfg().getCurrentInstruction(),
                              result,
                              context.currentScope().allocateTempValue(),
                              site,
                              new int[] {context.getValue(a), result},
                              new Pair[0]));
                });
      }
    }

    context
        .cfg()
        .unknownInstructions(
            () -> {
              doGlobalWrite(context, fnName, PythonTypes.Root, result);
              FieldReference fnField =
                  FieldReference.findOrCreate(
                      PythonTypes.Root,
                      Atom.findOrCreateUnicodeAtom(fn.getName()),
                      PythonTypes.Root);
              context
                  .cfg()
                  .addInstruction(
                      Python.instructionFactory()
                          .PutInstruction(
                              context.cfg().getCurrentInstruction(), 1, result, fnField));
            });
  }

  @Override
  protected void leaveFunctionEntity(
      CAstEntity n,
      WalkContext context,
      WalkContext codeContext,
      CAstVisitor<WalkContext> visitor) {
    super.leaveFunctionEntity(n, context, codeContext, visitor);

    String fnName = composeEntityName(context, n) + "_defaults";
    if (n.getArgumentDefaults() != null) {
      int first = n.getArgumentCount() - n.getArgumentDefaults().length;
      for (int i = first; i < n.getArgumentCount(); i++) {
        CAstNode dflt = n.getArgumentDefaults()[i - first];
        WalkContext cc = context.codeContext();
        visitor.visit(dflt, cc, visitor);
        doGlobalWrite(cc, "L" + fnName + "_" + i, PythonTypes.Root, cc.getValue(dflt));
      }
    }
  }

  private final Stack<PythonGlobalsEntity> globalsStack = new Stack<>();

  @Override
  protected boolean enterEntity(
      CAstEntity n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    if (n.getOriginal() instanceof PythonGlobalsEntity) {
      globalsStack.push((PythonGlobalsEntity) n.getOriginal());
    }
    return super.enterEntity(n, context, visitor);
  }

  @Override
  protected void postProcessEntity(
      CAstEntity n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    if (n.getOriginal() instanceof PythonGlobalsEntity) {
      globalsStack.pop();
    }
    super.postProcessEntity(n, context, visitor);
  }

  private boolean isGlobalVar(String name) {
    for (PythonGlobalsEntity e : globalsStack) {
      if (e.downwardGlobals().contains(name)) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected boolean visitScriptEntity(
      CAstEntity n,
      WalkContext context,
      WalkContext codeContext,
      CAstVisitor<WalkContext> visitor) {
    boolean ret = super.visitScriptEntity(n, context, codeContext, visitor);

    ModuleEntry module = context.getModule();
    String scriptName = module.getName();

    // if the module is the special initialization module.
    if (scriptName.endsWith(MODULE_INITIALIZATION_FILENAME)) {
      // we've hit a module. Get the other scripts in the module.
      PythonLoader loader = (PythonLoader) this.loader;
      IClassHierarchy classHierarchy = loader.getClassHierarchy();
      AnalysisScope scope = classHierarchy.getScope();
      List<Module> allModules = scope.getModules(pythonLoader);

      // collect the all local modules.
      Set<SourceModule> localModules = getLocalModules(allModules);

      String moduleName = Path.of(scriptName).getParent().getFileName().toString();
      LOGGER.fine("Initializing module: " + moduleName + ".");

      localModules.stream()
          .filter(
              m -> {
                LOGGER.finer("Examining: " + m + " for filteration.");

                Path path = Path.of(m.getURL().getFile());
                Path parent = path.getParent();
                String parentFileName = parent.getFileName().toString();
                String grandparentFileName = parent.getParent().getFileName().toString();

                // Return whether the script is in the module or whether we are looking at a direct
                // subpackage.
                boolean include =
                    parentFileName.equals(moduleName)
                        || (grandparentFileName.equals(moduleName)
                            && path.getFileName()
                                .toString()
                                .equals(MODULE_INITIALIZATION_FILENAME));

                LOGGER.finer(
                    (include ? "Including" : "Not including")
                        + " script: "
                        + path.getFileName()
                        + " in module: "
                        + moduleName
                        + " initialization.");

                return include;
              })
          .map(
              m -> {
                // For each module in the package, add a field referring to the script representing
                // the module.
                LOGGER.finer("Mapping: " + m + " to instructions.");

                List<File> pythonPath = loader.getPythonPath();
                LOGGER.finer("PYTHONPATH is: " + pythonPath);

                Path path = Path.of(m.getURL().getFile());
                LOGGER.finer("Found module path: " + path + ".");

                for (File pathEntry : pythonPath) {
                  LOGGER.finer("Path entry is:" + pathEntry);

                  if (path.startsWith(pathEntry.toPath())) {
                    // Found it.
                    Path scriptRelativePath = pathEntry.toPath().relativize(path);
                    LOGGER.finer("Relativized path is: " + scriptRelativePath + ".");

                    // Get the package name.
                    Path packagePath = scriptRelativePath.getParent();
                    LOGGER.fine("Package path is: " + packagePath + ".");

                    LOGGER.finer("Mapping fields for package: " + packagePath + ".");

                    List<SSAInstruction> instructions = new ArrayList<SSAInstruction>(2);
                    int res = 0;

                    // Don't add a redundant global read for `__init__.py` for `moduleName`.
                    boolean moduleInitializationFile = isModuleInitializationFile(path);

                    if (!moduleInitializationFile || !packagePath.toString().equals(moduleName)) {
                      FieldReference global =
                          makeGlobalRef("script " + packagePath + "/" + path.getFileName());

                      LOGGER.finer("Creating global field reference: " + global + ".");

                      int idx = codeContext.cfg().getCurrentInstruction();
                      res = codeContext.currentScope().allocateTempValue();

                      AstGlobalRead globalRead = new AstGlobalRead(idx, res, global);
                      instructions.add(globalRead);
                      LOGGER.finer("Adding global read: " + globalRead + ".");
                    }

                    FieldReference moduleField =
                        FieldReference.findOrCreate(
                            PythonTypes.Root,
                            // If it's the package, use the package name. Otherwise, use the
                            // filename.
                            Atom.findOrCreateUnicodeAtom(
                                getNameWithoutExtension(
                                    (moduleInitializationFile ? path.getParent() : path)
                                        .toString())),
                            PythonTypes.Root);

                    LOGGER.finer("Creating module field reference: " + moduleField + ".");

                    // If we are looking at the package for `moduleName`..
                    if (moduleInitializationFile && packagePath.toString().equals(moduleName))
                      // use the existing global read for the script.
                      res = 1;

                    SSAPutInstruction putInstruction =
                        Python.instructionFactory()
                            .PutInstruction(
                                codeContext.cfg().getCurrentInstruction(), 1, res, moduleField);

                    instructions.add(putInstruction);
                    LOGGER.finer("Adding field write: " + putInstruction + ".");

                    return instructions;
                  }
                }
                //  Not found.
                throw new IllegalStateException(
                    "Cannot find module: " + m + " in PYTHONPATH: " + pythonPath);
              })
          .flatMap(List::stream)
          .forEachOrdered(i -> codeContext.cfg().addInstruction(i));
    }

    return ret;
  }

  /**
   * Returns true iff the given {@link Path} represents the Python modularization file
   * (`__init__.py`).
   *
   * @param path The {@link Path} in question.
   * @return true iff the given {@link Path} represents the Python modularization file
   *     (`__init__.py`).
   */
  private static boolean isModuleInitializationFile(Path path) {
    return path.getFileName().toString().equals(MODULE_INITIALIZATION_FILENAME);
  }

  /**
   * Returns local modules {@link Module}s from the given {@link List} of {@link Module}s.
   *
   * @param allModules A {@link List} of {@link Module}s in question.
   * @return A {@link Set} of local {@link Module}s.
   */
  private static Set<SourceModule> getLocalModules(List<Module> allModules) {
    Set<SourceModule> ret = HashSetFactory.make();

    allModules.stream()
        .map(Module::getEntries)
        .forEach(
            it ->
                it.forEachRemaining(
                    new Consumer<ModuleEntry>() {
                      @Override
                      public void accept(ModuleEntry f) {
                        if (f.isModuleFile())
                          f.asModule().getEntries().forEachRemaining(sm -> accept(sm));
                        else ret.add((SourceModule) f);
                      }
                    }));

    return ret;
  }

  @Override
  protected void leaveVar(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    String nm = (String) n.getChild(0).getValue();
    assert nm != null : "cannot find var for " + CAstPrinter.print(n, context.getSourceMap());

    if ("0".equals(nm)) {
      System.err.println("got here");
    }

    if (isGlobalVar(nm)) {
      c.setValue(n, doGlobalRead(n, context, nm, PythonTypes.Root));
    } else {
      Symbol s = context.currentScope().lookup(nm);
      assert s != null
          : "cannot find symbol for " + nm + " at " + CAstPrinter.print(n, context.getSourceMap());
      assert s.type() != null
          : "no type for " + nm + " at " + CAstPrinter.print(n, context.getSourceMap());
      TypeReference type = makeType(s.type());
      if (context.currentScope().isGlobal(s) || isGlobal(context, nm)) {
        c.setValue(n, doGlobalRead(n, context, nm, type));
      } else if (context.currentScope().isLexicallyScoped(s)) {
        c.setValue(n, doLexicallyScopedRead(n, context, nm, type));
      } else {
        c.setValue(n, doLocalRead(context, nm, type));
      }
    }
  }

  @Override
  protected void assignValue(CAstNode n, WalkContext context, Symbol ls, String nm, int rval) {
    if (isGlobalVar(nm)) {
      doGlobalWrite(context, nm, PythonTypes.Root, rval);
    } else if (context.currentScope().isGlobal(ls) || isGlobal(context, nm))
      doGlobalWrite(context, nm, makeType(ls.type()), rval);
    else if (context.currentScope().isLexicallyScoped(ls)) {
      doLexicallyScopedWrite(context, nm, makeType(ls.type()), rval);
    } else {
      assert rval != -1 : CAstPrinter.print(n, context.top().getSourceMap());
      doLocalWrite(context, nm, makeType(ls.type()), rval);
    }
  }

  @Override
  protected void leaveVarAssignOp(
      CAstNode n,
      CAstNode v,
      CAstNode a,
      boolean pre,
      WalkContext c,
      CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    String nm = (String) n.getChild(0).getValue();
    Symbol ls = context.currentScope().lookup(nm);
    TypeReference type = makeType(ls.type());
    int temp;

    if (context.currentScope().isGlobal(ls) || isGlobal(context, nm))
      temp = doGlobalRead(n, context, nm, type);
    else if (context.currentScope().isLexicallyScoped(ls)) {
      temp = doLexicallyScopedRead(n, context, nm, type);
    } else {
      temp = doLocalRead(context, nm, type);
    }

    if (!pre) {
      int ret = context.currentScope().allocateTempValue();
      int currentInstruction = context.cfg().getCurrentInstruction();
      context.cfg().addInstruction(new AssignInstruction(currentInstruction, ret, temp));
      context
          .cfg()
          .noteOperands(currentInstruction, context.getSourceMap().getPosition(n.getChild(0)));
      c.setValue(n, ret);
    }

    int rval = processAssignOp(v, a, temp, c);

    if (pre) {
      c.setValue(n, rval);
    }

    if (context.currentScope().isGlobal(ls) || isGlobal(context, nm)) {
      doGlobalWrite(context, nm, type, rval);
    } else if (context.currentScope().isLexicallyScoped(ls)) {
      doLexicallyScopedWrite(context, nm, type, rval);
    } else {
      doLocalWrite(context, nm, type, rval);
    }
  }

  @Override
  protected void leaveTypeEntity(
      CAstEntity n,
      WalkContext context,
      WalkContext typeContext,
      CAstVisitor<WalkContext> visitor) {
    super.leaveTypeEntity(n, context, typeContext, visitor);

    WalkContext code = context.codeContext();

    int v = code.currentScope().allocateTempValue();

    int idx = code.cfg().getCurrentInstruction();
    String fnName = composeEntityName(context, n);
    IClass cls = loader.lookupClass(TypeName.findOrCreate("L" + fnName));
    TypeReference type = cls.getReference();
    code.cfg()
        .addInstruction(
            Python.instructionFactory().NewInstruction(idx, v, NewSiteReference.make(idx, type)));

    doLocalWrite(code, n.getType().getName(), type, v);
    doGlobalWrite(code, fnName, PythonTypes.Root, v);

    if (!this.entity2ExposedNames.containsKey(context.top())) {
      this.entity2ExposedNames.put(context.top(), HashSetFactory.make());
    }
    this.entity2ExposedNames.get(context.top()).add(fnName);

    for (CAstEntity field : n.getAllScopedEntities().get(null)) {
      FieldReference fr =
          FieldReference.findOrCreate(
              type, Atom.findOrCreateUnicodeAtom(field.getName()), PythonTypes.Root);
      int val;
      if (field.getKind() == CAstEntity.FIELD_ENTITY) {
        this.visit(field.getAST(), code, this);
        val = code.getValue(field.getAST());
      } else if (field.getKind() == CAstEntity.TYPE_ENTITY) {
        String className = composeEntityName(typeContext, field);
        val = doGlobalRead(null, code, className, PythonTypes.Root);
      } else {
        assert (field.getKind() == CAstEntity.FUNCTION_ENTITY);
        val = code.currentScope().allocateTempValue();
        String methodName = composeEntityName(typeContext, field);
        IClass methodCls = loader.lookupClass(TypeName.findOrCreate("L" + methodName));
        TypeReference methodType = methodCls.getReference();
        int codeIdx = code.cfg().getCurrentInstruction();
        code.cfg()
            .addInstruction(
                Python.instructionFactory()
                    .NewInstruction(codeIdx, val, NewSiteReference.make(codeIdx, methodType)));
      }
      code.cfg()
          .addInstruction(
              Python.instructionFactory()
                  .PutInstruction(code.cfg().getCurrentInstruction(), v, val, fr));
    }

    if (cls instanceof PythonClass) {
      ((PythonClass) cls)
          .getMissingTypeNames()
          .forEach(
              tn -> {
                int val = doLocalRead(code, tn, PythonTypes.Root);
                FieldReference fr =
                    FieldReference.findOrCreate(
                        cls.getReference(),
                        Atom.findOrCreateUnicodeAtom("missing_" + tn),
                        PythonTypes.Root);
                code.cfg()
                    .addInstruction(
                        Python.instructionFactory()
                            .PutInstruction(code.cfg().getCurrentInstruction(), v, val, fr));
              });
    }

    code.cfg()
        .unknownInstructions(
            () -> {
              FieldReference fnField =
                  FieldReference.findOrCreate(
                      PythonTypes.Root,
                      Atom.findOrCreateUnicodeAtom(n.getName()),
                      PythonTypes.Root);
              code.cfg()
                  .addInstruction(
                      Python.instructionFactory()
                          .PutInstruction(code.cfg().getCurrentInstruction(), 1, v, fnField));
            });
  }

  @Override
  protected void doNewObject(
      WalkContext context, CAstNode newNode, int result, Object type, int[] arguments) {
    context
        .cfg()
        .addInstruction(
            insts.NewInstruction(
                context.cfg().getCurrentInstruction(),
                result,
                NewSiteReference.make(
                    context.cfg().getCurrentInstruction(),
                    TypeReference.findOrCreate(PythonTypes.pythonLoader, "L" + type))));
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doCall(
      WalkContext context,
      CAstNode call,
      int result,
      int exception,
      CAstNode name,
      int receiver,
      int[] arguments) {
    List<Position> pospos = new ArrayList<Position>();
    List<Position> keypos = new ArrayList<Position>();
    List<Integer> posp = new ArrayList<Integer>();
    List<Pair<String, Integer>> keyp = new ArrayList<Pair<String, Integer>>();
    posp.add(receiver);
    pospos.add(context.getSourceMap().getPosition(call.getChild(0)));
    for (int i = 2; i < call.getChildCount(); i++) {
      CAstNode cl = call.getChild(i);
      if (cl.getKind() == CAstNode.ARRAY_LITERAL) {
        keyp.add(
            Pair.make(String.valueOf(cl.getChild(0).getValue()), context.getValue(cl.getChild(1))));
        keypos.add(context.getSourceMap().getPosition(cl));
      } else {
        posp.add(context.getValue(cl));
        pospos.add(context.getSourceMap().getPosition(cl));
      }
    }

    int params[] = new int[arguments.length + 1];
    params[0] = receiver;
    System.arraycopy(arguments, 0, params, 1, arguments.length);

    int[] hack = new int[posp.size()];
    for (int i = 0; i < hack.length; i++) {
      hack[i] = posp.get(i);
    }

    int pos = context.cfg().getCurrentInstruction();
    CallSiteReference site = new DynamicCallSiteReference(PythonTypes.CodeBody, pos);

    context
        .cfg()
        .addInstruction(
            new PythonInvokeInstruction(
                pos, result, exception, site, hack, keyp.toArray(new Pair[keyp.size()])));

    pospos.addAll(keypos);
    context.cfg().noteOperands(pos, pospos.toArray(new Position[pospos.size()]));
    context.cfg().addPreNode(call, context.getUnwindState());

    // this new block is for the normal termination case
    context.cfg().newBlock(true);

    // exceptional case: flow to target given in CAst, or if null, the exit node
    ((CAstControlFlowRecorder) context.getControlFlow()).map(call, call);

    if (context.getControlFlow().getTargetLabels(call).isEmpty()) {
      LOGGER.fine(() -> "no exceptions for " + CAstPrinter.print(call));
      context.cfg().addPreEdgeToExit(call, true);
    } else {
      context
          .getControlFlow()
          .getTargetLabels(call)
          .forEach(
              nm -> {
                if (context.getControlFlow().getTarget(call, nm) != null) {
                  context
                      .cfg()
                      .addPreEdge(call, context.getControlFlow().getTarget(call, nm), true);
                }
              });
    }
  }

  public static final CAstType Any =
      new CAstType() {

        @Override
        public String getName() {
          return "Any";
        }

        @Override
        public Collection<CAstType> getSupertypes() {
          return Collections.emptySet();
        }
      };

  @Override
  protected CAstType topType() {
    return Any;
  }

  public final CAstType Exception =
      new CAstType() {

        @Override
        public String getName() {
          return "Exception";
        }

        @Override
        public Collection<CAstType> getSupertypes() {
          return Collections.singleton(topType());
        }
      };

  @Override
  protected CAstType exceptionType() {
    return Any;
  }

  @Override
  protected void doPrimitive(int resultVal, WalkContext context, CAstNode primitiveCall) {
    if (primitiveCall.getChildCount() == 2
        && "import".equals(primitiveCall.getChild(0).getValue())) {
      String name = (String) primitiveCall.getChild(1).getValue();
      int idx = context.cfg().getCurrentInstruction();

      Position save = currentPosition;
      currentPosition = context.getSourceMap().getPosition(primitiveCall);
      // System.err.println("&&&&& switched to " + currentPosition);

      if (loader.lookupClass(TypeName.findOrCreate("Lscript " + name + ".py")) != null) {
        FieldReference global = makeGlobalRef("script " + name + ".py");
        context.cfg().addInstruction(new AstGlobalRead(idx, resultVal, global));
      } else {
        TypeReference imprt = TypeReference.findOrCreate(PythonTypes.pythonLoader, "L" + name);
        MethodReference call =
            MethodReference.findOrCreate(
                imprt, "import", "()L" + primitiveCall.getChild(1).getValue());
        context
            .cfg()
            .addInstruction(
                Python.instructionFactory()
                    .InvokeInstruction(
                        idx,
                        resultVal,
                        new int[0],
                        context.currentScope().allocateTempValue(),
                        CallSiteReference.make(idx, call, Dispatch.STATIC),
                        null));
      }

      currentPosition = save;
    } else if (primitiveCall.getChildCount() == 3
        && "import".equals(primitiveCall.getChild(0).getValue())) {
      String scriptName = (String) primitiveCall.getChild(1).getValue();
      String eltName = (String) primitiveCall.getChild(2).getValue();

      int idx = context.cfg().getCurrentInstruction();
      FieldReference global = makeGlobalRef("script " + scriptName + ".py");
      context.cfg().addInstruction(new AstGlobalRead(idx, resultVal, global));

      idx = context.cfg().getCurrentInstruction();
      context
          .cfg()
          .addInstruction(
              ((AstInstructionFactory) insts)
                  .PropertyRead(
                      idx, resultVal, resultVal, context.currentScope().getConstantValue(eltName)));
    }
  }

  @Override
  protected boolean visitVarAssign(
      CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) {
    String name = n.getChild(0).getValue().toString();
    if (!c.currentScope().contains(name)) {
      c.currentScope().declare(new CAstSymbolImpl(name, PythonCAstToIRTranslator.Any));
    }

    return false;
  }

  @Override
  protected boolean visitVarAssignOp(
      CAstNode n,
      CAstNode v,
      CAstNode a,
      boolean pre,
      WalkContext c,
      CAstVisitor<WalkContext> visitor) {
    return visitVarAssign(n, v, a, c, visitor);
  }

  @Override
  protected void leaveArrayLiteralAssign(
      CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) {
    int rval = c.getValue(v);
    for (int i = 1; i < n.getChildCount(); i++) {
      CAstNode var = n.getChild(i);
      if (var.getKind() == CAstNode.VAR) {
        String name = (String) var.getChild(0).getValue();
        c.currentScope().declare(new CAstSymbolImpl(name, topType()));
        Symbol ls = c.currentScope().lookup(name);

        int rvi = c.currentScope().allocateTempValue();
        int idx = c.currentScope().getConstantValue(i - 1);
        c.cfg()
            .addInstruction(
                Python.instructionFactory()
                    .PropertyRead(c.cfg().getCurrentInstruction(), rvi, rval, idx));

        c.setValue(n, rvi);
        assignValue(n, c, ls, name, rvi);
      }
    }
  }

  @Override
  protected void leaveObjectLiteralAssign(
      CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) {
    int rval = c.getValue(v);
    handleObjectLiteralAssign(n, n, rval, c, visitor);
  }

  private void handleObjectLiteralAssign(
      CAstNode n, CAstNode lvalAst, int rval, WalkContext c, CAstVisitor<WalkContext> visitor) {
    for (int i = 1; i < lvalAst.getChildCount(); i += 2) {
      int idx = c.getValue(lvalAst.getChild(i));
      if (idx == -1) {
        visitor.visit(lvalAst.getChild(i), c, visitor);
        idx = c.getValue(lvalAst.getChild(i));
      }
      CAstNode var = lvalAst.getChild(i + 1);
      if (var.getKind() == CAstNode.VAR) {
        String name = (String) var.getChild(0).getValue();
        c.currentScope().declare(new CAstSymbolImpl(name, topType()));
        Symbol ls = c.currentScope().lookup(name);

        Position save = currentPosition;
        currentPosition = c.getSourceMap().getPosition(var);

        int rvi = c.currentScope().allocateTempValue();
        c.cfg()
            .addInstruction(
                Python.instructionFactory()
                    .PropertyRead(c.cfg().getCurrentInstruction(), rvi, rval, idx));

        currentPosition = save;

        // c.setValue(n, rvi);
        assignValue(n, c, ls, name, rvi);
      } else if (var.getKind() == CAstNode.OBJECT_LITERAL) {
        int rvi = c.currentScope().allocateTempValue();
        c.cfg()
            .addInstruction(
                Python.instructionFactory()
                    .PropertyRead(c.cfg().getCurrentInstruction(), rvi, rval, idx));
        handleObjectLiteralAssign(n, var, rvi, c, visitor);
      }
    }
  }

  boolean isGlobal(WalkContext context, String varName) {
    if (singleFileAnalysis) return false;
    else {
      if (context.currentScope().getEntity().getKind() == CAstEntity.SCRIPT_ENTITY) return true;
      else {
        Pair<Scope, String> pair = Pair.make(context.currentScope(), varName);
        if (globalDeclSet.contains(pair)) return true;
        else return false;
      }
    }
  }

  void addGlobal(Scope scope, String varName) {
    Pair<Scope, String> pair = Pair.make(scope, varName);
    globalDeclSet.add(pair);
  }

  @Override
  protected boolean doVisit(CAstNode n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    if (n.getKind() == CAstNode.COMPREHENSION_EXPR) {
      int[] args = new int[n.getChild(2).getChildCount() + 2];

      visitor.visit(n.getChild(0), context, visitor);
      int obj = context.getValue(n.getChild(0));

      visitor.visit(n.getChild(1), context, visitor);
      int lambda = context.getValue(n.getChild(1));

      args[0] = lambda;
      args[1] = obj;
      for (int i = 0; i < args.length - 2; i++) {
        visitor.visit(n.getChild(2).getChild(i), context, visitor);
        args[i + 2] = context.getValue(n.getChild(2).getChild(i));
      }

      int pos = context.cfg().getCurrentInstruction();
      CallSiteReference site = new DynamicCallSiteReference(PythonTypes.CodeBody, pos);
      int result = context.currentScope().allocateTempValue();
      int exception = context.currentScope().allocateTempValue();
      context
          .cfg()
          .addInstruction(
              new PythonInvokeInstruction(pos, result, exception, site, args, new Pair[0]));

      context.setValue(n, result);
      return true;

    } else if (n.getKind() == CAstNode.GLOBAL_DECL) {
      int numOfChildren = n.getChildCount();
      for (int i = 0; i < numOfChildren; i++) {
        String val = (String) n.getChild(i).getChild(0).getValue();
        System.out.println("Hey " + val);
        addGlobal(context.currentScope(), val);
      }
      return true;

    } else {
      return super.doVisit(n, context, visitor);
    }
  }

  @Override
  protected boolean doVisitAssignNodes(
      CAstNode n, WalkContext context, CAstNode v, CAstNode a, CAstVisitor<WalkContext> visitor) {
    return super.doVisitAssignNodes(n, context, v, a, visitor);
  }

  @Override
  protected Position[] getParameterPositions(CAstEntity e) {
    Position[] ps = new Position[e.getArgumentCount()];
    for (int i = 1; i < e.getArgumentCount(); i++) {
      ps[i] = e.getPosition(i - 1);
    }
    return ps;
  }

  @Override
  protected void leaveBlockStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    // TODO Auto-generated method stub
    super.leaveBlockStmt(n, c, visitor);
  }
}
