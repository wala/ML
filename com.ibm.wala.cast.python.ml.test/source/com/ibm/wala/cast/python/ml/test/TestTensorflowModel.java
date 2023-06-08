package com.ibm.wala.cast.python.ml.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.ibm.wala.cast.loader.AstMethod;
//import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ml.analysis.TensorVariable;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestTensorflowModel extends TestPythonMLCallGraphShape {

	private static class Tf2TestFile {
		private String filename;

		private int expectedLineNumberForParameters;

		public Tf2TestFile(String filename, int expectedLineNumberForParameters) {
			this.filename = filename;
			this.expectedLineNumberForParameters = expectedLineNumberForParameters;
		}

		public String getFilename() {
			return this.filename;
		}

		public int getExpectedLineNumberForParameters() {
			return this.expectedLineNumberForParameters;
		}
	}

	@Test
	public void testTf1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<TensorTypeAnalysis> E = makeEngine("tf1.py");
		PythonSSAPropagationCallGraphBuilder builder = E.defaultCallGraphBuilder();
		CallGraph CG = builder.makeCallGraph(builder.getOptions());

//		CAstCallGraphUtil.AVOID_DUMP = false;
//		CAstCallGraphUtil.dumpCG(((SSAPropagationCallGraphBuilder)builder).getCFAContextInterpreter(), builder.getPointerAnalysis(), CG);

//		System.err.println(CG);

		Collection<CGNode> nodes = getNodes(CG, "script tf1.py/model_fn");
		assert ! nodes.isEmpty() : "model_fn should be called";
		check: {
			for(CGNode node : nodes) {
				for(Iterator<CGNode> ns = CG.getPredNodes(node); ns.hasNext(); ) {
					if (ns.next().getMethod().isWalaSynthetic()) {
						break check;
					}
				}

				assert false : node + " should have synthetic caller";
			}
		}
	}

	@Test
	public void testTf2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		final Set<Tf2TestFile> filesToTest = new LinkedHashSet<>();
		filesToTest.add(new Tf2TestFile("tf2.py", 3));
		filesToTest.add(new Tf2TestFile("tf2b.py", 3));
		filesToTest.add(new Tf2TestFile("tf2c.py", 4));
		filesToTest.add(new Tf2TestFile("tf2d.py", 3));

		for (Tf2TestFile testFile : filesToTest)
			testTf2(testFile);
	}

	private void testTf2(Tf2TestFile testFile) throws ClassHierarchyException, CancelException, IOException {
		PythonAnalysisEngine<TensorTypeAnalysis> E = makeEngine(testFile.getFilename());
		PythonSSAPropagationCallGraphBuilder builder = E.defaultCallGraphBuilder();

		CallGraph CG = builder.makeCallGraph(builder.getOptions());
		assertNotNull(CG);

//		CAstCallGraphUtil.AVOID_DUMP = false;
//		CAstCallGraphUtil.dumpCG(builder.getCFAContextInterpreter(), builder.getPointerAnalysis(), CG);

//		System.err.println(CG);

		TensorTypeAnalysis analysis = E.performAnalysis(builder);

		// Create a mapping from method signatures to pointer keys.
		Map<String, Set<LocalPointerKey>> methodSignatureToPointerKeys = new HashMap<>();

		// Create a mapping from method signatures to tensor variables.
		Map<String, Set<TensorVariable>> methodSignatureToTensorVariables = new HashMap<>();

		// for each pointer key, tensor variable pair.
		analysis.forEach(p -> {
			PointerKey pointerKey = p.fst;
			LocalPointerKey localPointerKey = (LocalPointerKey) pointerKey;

			// get the call graph node associated with the
			CGNode node = localPointerKey.getNode();

			// get the method associated with the call graph node.
			IMethod method = node.getMethod();
			String methodSignature = method.getSignature();

			// associate the method to the pointer key.
			methodSignatureToPointerKeys.compute(methodSignature, (k, v) -> {
				if (v == null)
					v = new HashSet<>();
				v.add(localPointerKey);
				return v;
			});

			TensorVariable tensorVariable = p.snd;

			// associate the method to the tensor variables.
			methodSignatureToTensorVariables.compute(methodSignature, (k, v) -> {
				if (v == null)
					v = new HashSet<>();
				v.add(tensorVariable);
				return v;
			});
		});

		// we should have two methods.
		assertEquals(2, methodSignatureToPointerKeys.size());
		assertEquals(2, methodSignatureToTensorVariables.size());

		final String addFunctionSignature = "script " + testFile.getFilename() + ".add.do()LRoot;";

		// get the pointer keys for the add() function.
		Set<LocalPointerKey> addFunctionPointerKeys = methodSignatureToPointerKeys.get(addFunctionSignature);

		// two tensor parameters, a and b.
		assertEquals(2, addFunctionPointerKeys.size());

		// should have value numbers of 2 and 3.
		Set<Integer> valueNumberSet = addFunctionPointerKeys.stream().map(LocalPointerKey::getValueNumber)
				.collect(Collectors.toSet());
		assertEquals(2, valueNumberSet.size());
		assertTrue(valueNumberSet.contains(2));
		assertTrue(valueNumberSet.contains(3));

		// check the source positions of each function parameter.
		for (LocalPointerKey lpk : addFunctionPointerKeys) {
			AstMethod method = (AstMethod) lpk.getNode().getIR().getMethod();
			int paramIndex = lpk.getValueNumber() - 1;
			Position parameterPosition = method.getParameterPosition(paramIndex);

			// check the line.
			assertEquals(testFile.getExpectedLineNumberForParameters(), parameterPosition.getFirstLine());

			// check the columns.
			if (lpk.getValueNumber() == 2) {
				assertEquals(8, parameterPosition.getFirstCol());
				assertEquals(9, parameterPosition.getLastCol());
			} else if (lpk.getValueNumber() == 3) {
				assertEquals(11, parameterPosition.getFirstCol());
				assertEquals(12, parameterPosition.getLastCol());
			} else
				throw new IllegalStateException("Expecting value numbers 2 or 3.");
		}

		// get the tensor variables for the add() function.
		Set<TensorVariable> addFunctionTensors = methodSignatureToTensorVariables.get(addFunctionSignature);

		// two tensor parameters, a and b.
		assertEquals(2, addFunctionTensors.size());
	}
}
