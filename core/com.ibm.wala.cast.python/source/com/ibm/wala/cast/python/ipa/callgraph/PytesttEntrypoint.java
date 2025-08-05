package com.ibm.wala.cast.python.ipa.callgraph;

import static com.ibm.wala.cast.python.types.PythonTypes.Root;
import static com.ibm.wala.cast.python.types.PythonTypes.object;

import com.ibm.wala.cast.ir.ssa.AstInstructionFactory;
import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.loader.PythonLoader;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;

/**
 * An {@link Entrypoint} of a <a href="http://pytest.org">Pytest</a> test case.
 *
 * @author <a href="mailto:khatchad@hunter.cuny.edu">Raffi Khatchadourian</a>
 */
public class PytesttEntrypoint extends DefaultEntrypoint {

  public PytesttEntrypoint(IMethod method, IClassHierarchy cha) {
    super(method, cha);
  }

  public PytesttEntrypoint(MethodReference method, IClassHierarchy cha) {
    super(method, cha);
  }

  /**
   * @see {@link TurtleSummary#turtleEntryPoint(IMethod)}.
   */
  @Override
  public SSAAbstractInvokeInstruction addCall(AbstractRootMethod m) {
    int paramValues[] = new int[getNumberOfParameters()];

    for (int j = 0; j < paramValues.length; j++) {
      AstInstructionFactory insts = PythonLanguage.Python.instructionFactory();

      String methodDeclaringClassName = getMethod().getDeclaringClass().getName().toString();

      if (j == 0 && methodDeclaringClassName.contains("/")) {
        int v = m.nextLocal++;
        paramValues[j] = v;

        if (getMethod().getDeclaringClass() instanceof PythonLoader.DynamicMethodBody) {
          FieldReference global =
              FieldReference.findOrCreate(
                  PythonTypes.Root,
                  Atom.findOrCreateUnicodeAtom(
                      "global "
                          + methodDeclaringClassName.substring(
                              1, methodDeclaringClassName.lastIndexOf('/'))),
                  PythonTypes.Root);

          int idx = m.statements.size();
          int cls = m.nextLocal++;
          int obj = m.nextLocal++;

          m.statements.add(insts.GlobalRead(m.statements.size(), cls, global));
          idx = m.statements.size();

          @SuppressWarnings("unchecked")
          PythonInvokeInstruction invokeInstruction =
              new PythonInvokeInstruction(
                  idx,
                  obj,
                  m.nextLocal++,
                  new DynamicCallSiteReference(PythonTypes.CodeBody, idx),
                  new int[] {cls},
                  new Pair[0]);

          m.statements.add(invokeInstruction);
          idx = m.statements.size();

          String method = methodDeclaringClassName;
          String field = method.substring(method.lastIndexOf('/') + 1);

          FieldReference f =
              FieldReference.findOrCreate(
                  PythonTypes.Root, Atom.findOrCreateUnicodeAtom(field), PythonTypes.Root);

          m.statements.add(insts.GetInstruction(idx, v, obj, f));
        } else {
          FieldReference global =
              FieldReference.findOrCreate(
                  PythonTypes.Root,
                  Atom.findOrCreateUnicodeAtom("global " + methodDeclaringClassName.substring(1)),
                  PythonTypes.Root);

          m.statements.add(insts.GlobalRead(m.statements.size(), v, global));
        }
      } else paramValues[j] = makeArgument(m, j);

      if (paramValues[j] == -1)
        // there was a problem
        return null;

      TypeReference x[] = getParameterTypes(j);

      if (x.length == 1 && x[0].equals(object))
        m.statements.add(
            insts.PutInstruction(
                m.statements.size(),
                paramValues[j],
                paramValues[j],
                FieldReference.findOrCreate(object, Atom.findOrCreateUnicodeAtom("pytest"), Root)));
    }

    int pc = m.statements.size();

    @SuppressWarnings("unchecked")
    PythonInvokeInstruction call =
        new PythonInvokeInstruction(
            pc,
            m.nextLocal++,
            m.nextLocal++,
            new DynamicCallSiteReference(PythonTypes.CodeBody, pc),
            paramValues,
            new Pair[0]);

    m.statements.add(call);

    return call;
  }
}
