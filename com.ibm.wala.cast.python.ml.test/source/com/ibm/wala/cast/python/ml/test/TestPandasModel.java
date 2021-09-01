package com.ibm.wala.cast.python.ml.test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ml.analysis.PandasReadExcelAnalysis;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;

public class TestPandasModel extends TestPythonMLCallGraphShape {

	@Test
	public void testPandas1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<TensorTypeAnalysis> engine = makeEngine("pandas1.py");
		CallGraphBuilder<? extends InstanceKey> builder = engine.defaultCallGraphBuilder();
		CallGraph CG = builder.makeCallGraph(engine.getOptions(), new NullProgressMonitor());
		PointerAnalysis<? extends InstanceKey> PA = builder.getPointerAnalysis();
		HeapModel H = PA.getHeapModel();
		
		Map<InstanceKey, Set<String>> excelTableFields = PandasReadExcelAnalysis.readExcelAnalysis(CG, PA, H);

//		System.out.println(excelTableFields);
	
		boolean foundDfqol = false;
		boolean foundDfdemog = false;
		for (Map.Entry<InstanceKey, Set<String>> e : excelTableFields.entrySet()) {
			if (e.getValue().contains("PID") && !e.getValue().contains("Patient_ID")) {
				foundDfqol = true;
			} else if (!e.getValue().contains("PID") && e.getValue().contains("Patient_ID")) {
				foundDfdemog = true;
			}
		}
		
		assert foundDfqol && foundDfdemog;
	}

}
