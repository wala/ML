package com.ibm.wala.cast.python.cpython.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.test.TestJythonCallGraphShape;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestAsync extends TestJythonCallGraphShape {

	protected static final Object[][] assertionsAsync1 = new Object[][] {
		new Object[] { ROOT, new String[] { "script async1.py" } },
		new Object[] { "script async1.py",
				new String[] { "CodeBody:$coroutine$Lscript async1.py/fibonacci" }
		},
		new Object[] { "CodeBody:$coroutine$Lscript async1.py/fibonacci",
				new String[] { "script async1.py/fibonacci" }
		},
		new Object[] { "script async1.py/fibonacci",
				new String[] { "CodeBody:$coroutine$Lscript async1.py/fibonacci" }
		}
	};

	@Test
	public void testAsync1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = this.makeEngine("async1.py");
		PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());
		
		CAstCallGraphUtil.AVOID_DUMP.set(false); CAstCallGraphUtil.dumpCG(
			(SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
			callGraphBuilder.getPointerAnalysis(), CG);
		
		System.err.println(CG);
	    verifyGraphAssertions(CG, assertionsAsync1);

	}

	protected static final Object[][] assertionsAsync2 = new Object[][] {
		new Object[] { ROOT, new String[] { "script async2.py" } },
		new Object[] { "script async2.py",
				new String[] { "CodeBody:$coroutine$Lscript async2.py/fibonacci" }
		},
		new Object[] { "CodeBody:$coroutine$Lscript async2.py/fibonacci",
				new String[] { "script async2.py/fibonacci" }
		},
		new Object[] { "script async2.py/fibonacci",
				new String[] { "CodeBody:$coroutine$Lscript async2.py/fibonacci",
						"script async2.py/fibonacci/lambda1",
						"script async2.py/fibonacci/lambda2",
						"script async2.py/fibonacci/lambda3" }
		}
	};

	@Test
	public void testAsync2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = this.makeEngine("async2.py");
		PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());
		
		CAstCallGraphUtil.AVOID_DUMP.set(false); CAstCallGraphUtil.dumpCG(
			(SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
			callGraphBuilder.getPointerAnalysis(), CG);
		
		System.err.println(CG);
	    verifyGraphAssertions(CG, assertionsAsync2);

	}

	protected static final Object[][] assertionsAsync3 = new Object[][] {
		new Object[] { ROOT, new String[] { "script async3.py" } },
		new Object[] { "script async3.py",
				new String[] { "CodeBody:$coroutine$Lscript async3.py/fibonacci" }
		},
		new Object[] { "CodeBody:$coroutine$Lscript async3.py/fibonacci",
				new String[] { "script async3.py/fibonacci" }
		},
		new Object[] { "script async3.py/fibonacci",
				new String[] { "CodeBody:$coroutine$Lscript async3.py/fibonacci" }
		},
		new Object[] { "script async3.py/fibonacci/lambda3",
				new String[] { 
						"script async3.py/fibonacci/lambda1",
						"script async3.py/fibonacci/lambda2",
						"script async3.py/fibonacci/lambda3" }
		}
	};

	@Test
	public void testAsync3() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = this.makeEngine("async3.py");
		PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());
		
		CAstCallGraphUtil.AVOID_DUMP.set(false); CAstCallGraphUtil.dumpCG(
			(SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
			callGraphBuilder.getPointerAnalysis(), CG);
		
		System.err.println(CG);
	    verifyGraphAssertions(CG, assertionsAsync3);

	}

}
