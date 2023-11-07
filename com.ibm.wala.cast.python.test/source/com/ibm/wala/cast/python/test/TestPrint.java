package com.ibm.wala.cast.python.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.OrdinalSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import org.junit.Test;

public class TestPrint extends TestPythonCallGraphShape {

  private static final String PRINT_FUNCTION_VARIABLE_NAME = "print";

  private static final TypeReference PRINT_FUNCTION_TYPE_REFERENCE =
      TypeReference.findOrCreate(
          PythonTypes.pythonLoader, "Lwala/builtin/" + PRINT_FUNCTION_VARIABLE_NAME);

  @Test
  public void testPrint()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> engine = makeEngine("print.py");
    SSAPropagationCallGraphBuilder builder =
        (SSAPropagationCallGraphBuilder) engine.defaultCallGraphBuilder();
    CallGraph CG = builder.makeCallGraph(builder.getOptions());

    Collection<CGNode> nodes = getNodes(CG, "script print.py/f");
    assertEquals(1, nodes.size());

    assertTrue(nodes.iterator().hasNext());
    CGNode fNode = nodes.iterator().next();

    IR fIR = fNode.getIR();

    boolean foundPrintCall = false;
    boolean foundBuiltIn = false;

    for (Iterator<SSAInstruction> iit = fIR.iterateNormalInstructions(); iit.hasNext(); ) {
      SSAInstruction instruction = iit.next();

      if (instruction instanceof PythonInvokeInstruction) {
        PythonInvokeInstruction invokeInstruction = (PythonInvokeInstruction) instruction;
        int receiver = invokeInstruction.getReceiver();
        assertTrue(receiver >= 0);

        SSAInstruction receiverDefinition = fNode.getDU().getDef(receiver);

        if (receiverDefinition instanceof AstLexicalRead) {
          AstLexicalRead read = (AstLexicalRead) receiverDefinition;

          assertEquals(1, read.getAccessCount());
          Access access = read.getAccess(0);

          if (access.variableName.equals(PRINT_FUNCTION_VARIABLE_NAME)) {
            foundPrintCall = true;

            // Found the print call. Let's ensure that it "points" to the built-in function.
            PointerKey pk =
                builder
                    .getPointerAnalysis()
                    .getHeapModel()
                    .getPointerKeyForLocal(fNode, access.valueNumber);
            OrdinalSet<InstanceKey> pointsToSet = builder.getPointerAnalysis().getPointsToSet(pk);

            for (Iterator<InstanceKey> pit = pointsToSet.iterator(); pit.hasNext(); ) {
              InstanceKey ik = pit.next();

              IClass concreteType = ik.getConcreteType();
              TypeReference typeReference = concreteType.getReference();

              if (typeReference.equals(PRINT_FUNCTION_TYPE_REFERENCE)) {
                // found the built-in function in the pointer analysis.
                foundBuiltIn = true;
                break;
              }
            }
          }
        }
      }
    }

    assertTrue(foundPrintCall && foundBuiltIn);
  }
}
