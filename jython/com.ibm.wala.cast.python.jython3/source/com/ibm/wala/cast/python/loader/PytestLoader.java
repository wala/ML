package com.ibm.wala.cast.python.loader;

import static com.ibm.wala.cast.python.ir.PythonLanguage.Python;

import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class PytestLoader extends Python3Loader {

  public static final TypeReference pytestType =
      TypeReference.findOrCreate(PythonTypes.pythonLoader, "LPytest");

  private final Map<String, Map<String, Integer>> testParams = HashMapFactory.make();

  public PytestLoader(IClassHierarchy cha, List<File> pythonPath) {
    super(cha, pythonPath);
  }

  @Override
  protected TranslatorToIR initTranslator(Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities) {
    return new PythonCAstToIRTranslator(this) {

      private boolean isPytestEntry(CAstEntity F) {
        if (F.getType() instanceof CAstType.Function) {
          if (F.getType() instanceof CAstType.Method) {
            CAstType C = ((CAstType.Method) F.getType()).getDeclaringType();
            if (!C.getName().startsWith("Test")) {
              return false;
            }
          }

          if (!F.getName().startsWith("test_")) {
            return false;
          }

          return true;
        }

        return false;
      }

      @Override
      protected void leaveTypeEntity(
          CAstEntity arg0, WalkContext arg1, WalkContext arg2, CAstVisitor<WalkContext> arg3) {
        super.leaveTypeEntity(arg0, arg1, arg2, arg3);

        String fnName = composeEntityName(arg1, arg0);
        IClass cls = loader.lookupClass(TypeName.findOrCreate("L" + fnName));
        TypeReference type = cls.getReference();

        WalkContext code = arg1.codeContext();
        int v = code.currentScope().allocateTempValue();

        FieldReference global = makeGlobalRef(fnName);
        code.cfg().addInstruction(new AstGlobalRead(code.cfg().getCurrentInstruction(), v, global));

        for (CAstEntity field : arg0.getAllScopedEntities().get(null)) {
          FieldReference fr =
              FieldReference.findOrCreate(
                  type, Atom.findOrCreateUnicodeAtom(field.getName()), PythonTypes.Root);

          if (field.getKind() == CAstEntity.FUNCTION_ENTITY) {
            int method = code.currentScope().allocateTempValue();
            code.cfg()
                .addInstruction(
                    Python.instructionFactory()
                        .GetInstruction(code.cfg().getCurrentInstruction(), method, v, fr));

            handlePytest(arg2, field, method);
          }
        }
      }

      @Override
      protected void doMaterializeFunction(
          CAstNode node, WalkContext context, int result, int exception, CAstEntity fn) {
        super.doMaterializeFunction(node, context, result, exception, fn);

        handlePytest(context, fn, result);
      }

      private void handlePytest(WalkContext context, CAstEntity fn, int function) {
        String fnName = composeEntityName(context, fn);
        if (isPytestEntry(fn)) {
          boolean isMethod = fn.getType() instanceof CAstType.Method;
          Map<String, Integer> testArgValues = HashMapFactory.make();
          Scope definingScope = context.currentScope();
          int i = 0;
          for (String nm : fn.getArgumentNames()) {
            if (definingScope.contains(nm)) {
              Symbol x = definingScope.lookup(nm);
              if (!x.isGlobal() && i > (isMethod ? 1 : 0)) {
                testArgValues.put(nm, x.valueNumber());
              }
            }
            i++;
          }

          testParams.put(fnName, testArgValues);

          int idx = 0;
          @SuppressWarnings("unchecked")
          Pair<String, Integer>[] keys = new Pair[testArgValues.size()];
          Set<Entry<String, Integer>> parameters = testArgValues.entrySet();
          for (Entry<String, Integer> p : parameters) {
            keys[idx++] = Pair.make(p.getKey(), p.getValue());
          }
          WalkContext code = context.codeContext();
          int pos = code.cfg().getCurrentInstruction();
          CallSiteReference site = new DynamicCallSiteReference(pytestType, pos);
          code.cfg()
              .addInstruction(
                  new PythonInvokeInstruction(
                      pos,
                      context.currentScope().allocateTempValue(),
                      context.currentScope().allocateTempValue(),
                      site,
                      new int[] {function},
                      keys));
        }
      }
    };
  }

  @Override
  protected void finishTranslation() {
    super.finishTranslation();
    System.err.println("xxsymbol " + testParams);
  }
}
