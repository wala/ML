package com.ibm.wala.cast.python.modref;

import com.ibm.wala.cast.ipa.callgraph.AstHeapModel;
import com.ibm.wala.cast.ipa.modref.AstModRef;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstPropertyWrite;
import com.ibm.wala.cast.python.ssa.PythonInstructionVisitor;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import java.util.Collection;
import java.util.Iterator;

public class PythonModRef extends AstModRef<InstanceKey> {

  public static class PythonRefVisitor<T extends InstanceKey> extends AstRefVisitor<T>
      implements PythonInstructionVisitor {

    public PythonRefVisitor(
        CGNode n, Collection<PointerKey> result, PointerAnalysis<T> pa, ExtendedHeapModel h) {
      super(n, result, pa, (AstHeapModel) h);
    }
  }

  @Override
  protected RefVisitor<InstanceKey, ? extends ExtendedHeapModel> makeRefVisitor(
      CGNode n,
      Collection<PointerKey> result,
      PointerAnalysis<InstanceKey> pa,
      ExtendedHeapModel h) {
    return new PythonRefVisitor<>(n, result, pa, h);
  }

  public static class PythonModVisitor<T extends InstanceKey> extends AstModVisitor<T>
      implements PythonInstructionVisitor {

    public PythonModVisitor(
        CGNode n,
        Collection<PointerKey> result,
        ExtendedHeapModel h,
        PointerAnalysis<T> pa,
        boolean ignoreAllocHeapDefs) {
      super(n, result, (AstHeapModel) h, pa);
    }

    @Override
    public void visitAstGlobalWrite(AstGlobalWrite instruction) {
      super.visitAstGlobalWrite(instruction);
      String globalName = instruction.getGlobalName();

      // find the pointer key corresponding to this global.
      for (PointerKey pk : this.pa.getPointerKeys()) {
        if (pk instanceof StaticFieldKey) {
          StaticFieldKey staticFieldKey = (StaticFieldKey) pk;
          IField field = staticFieldKey.getField();
          Atom fieldName = field.getName();
          if (fieldName.toString().equals(globalName))
            this.result.add(this.h.getPointerKeyForStaticField(field));
        }
      }
    }

    @Override
    public void visitPropertyWrite(AstPropertyWrite instruction) {
      super.visitPropertyWrite(instruction);

      // prune "writes" to ConstantKeys. See https://github.com/wala/ML/issues/103.
      for (Iterator<PointerKey> it = this.result.iterator(); it.hasNext(); ) {
        PointerKey pointerKey = it.next();

        if (pointerKey instanceof InstanceFieldPointerKey) {
          InstanceFieldPointerKey instanceFieldPointerKey = (InstanceFieldPointerKey) pointerKey;
          InstanceKey instanceKey = instanceFieldPointerKey.getInstanceKey();

          if (instanceKey instanceof ConstantKey) it.remove();
        }
      }
    }
  }

  @Override
  protected ModVisitor<InstanceKey, ? extends ExtendedHeapModel> makeModVisitor(
      CGNode n,
      Collection<PointerKey> result,
      PointerAnalysis<InstanceKey> pa,
      ExtendedHeapModel h,
      boolean ignoreAllocHeapDefs) {
    return new PythonModVisitor<>(n, result, h, pa, ignoreAllocHeapDefs);
  }
}
