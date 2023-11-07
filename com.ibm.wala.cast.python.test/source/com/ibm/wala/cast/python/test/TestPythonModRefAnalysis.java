package com.ibm.wala.cast.python.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.modref.PythonModRef;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.OrdinalSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.junit.Test;

public class TestPythonModRefAnalysis extends TestPythonCallGraphShape {

  private OrdinalSet<PointerKey> getModSet(String filename)
      throws ClassHierarchyException, CancelException, IOException {
    PythonAnalysisEngine<?> engine = makeEngine(filename);
    SSAPropagationCallGraphBuilder builder =
        (SSAPropagationCallGraphBuilder) engine.defaultCallGraphBuilder();
    CallGraph CG = builder.makeCallGraph(builder.getOptions());

    Collection<CGNode> nodes = getNodes(CG, "script " + filename + "/f");
    assertEquals(1, nodes.size());

    assertTrue(nodes.iterator().hasNext());
    CGNode fNode = nodes.iterator().next();

    ModRef<InstanceKey> modRef = new PythonModRef();
    Map<CGNode, OrdinalSet<PointerKey>> mod = modRef.computeMod(CG, builder.getPointerAnalysis());

    // what heap locations does f() (transitively) modify?
    OrdinalSet<PointerKey> modSet = mod.get(fNode);
    return modSet;
  }

  @Test
  public void testGlobalMods()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    OrdinalSet<PointerKey> modSet = getModSet("globals.py");

    // should only modify the global.
    assertEquals(1, modSet.size());

    assertTrue(modSet.iterator().hasNext());
    PointerKey pointerKey = modSet.iterator().next();

    assertTrue(pointerKey instanceof StaticFieldKey);
    StaticFieldKey staticFieldKey = (StaticFieldKey) pointerKey;

    IField field = staticFieldKey.getField();
    Atom name = field.getName();
    assertEquals(Atom.findOrCreateAsciiAtom("global a"), name);
  }

  @Test
  public void testKeywordArgs()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    OrdinalSet<PointerKey> modSet = getModSet("kw_args.py");
    assertEquals("Should be no modifications to heap locations in this file.", 0, modSet.size());
  }
}
