package com.ibm.wala.cast.python.driver;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.loader.PythonLoaderFactory;
import com.ibm.wala.cast.python.util.PythonInterpreter;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;

public class Driver {

	static {
		try {
			Class<?> j3 = Class.forName("com.ibm.wala.cast.python.loader.Python3LoaderFactory");
			PythonAnalysisEngine.setLoaderFactory((Class<? extends PythonLoaderFactory>) j3);
			Class<?> i3 = Class.forName("com.ibm.wala.cast.python.util.Python3Interpreter");
			PythonInterpreter.setInterpreter((PythonInterpreter)i3.newInstance());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			try {
				Class<?> j2 = Class.forName("com.ibm.wala.cast.python.loader.Python2LoaderFactory");			
				PythonAnalysisEngine.setLoaderFactory((Class<? extends PythonLoaderFactory>) j2);
				Class<?> i2 = Class.forName("com.ibm.wala.cast.python.util.Python2Interpreter");
				PythonInterpreter.setInterpreter((PythonInterpreter)i2.newInstance());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e1) {
				assert false : e.getMessage() + ", then " + e1.getMessage();
			}
		}
	}

	public static void main(String... args) throws IllegalArgumentException, IOException, CallGraphBuilderCancelException {
	
		PythonAnalysisEngine<Void> E = new PythonAnalysisEngine<Void>() {
			@Override
			public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
				return null;
			}
		};
		
		Set<Module> sources = HashSetFactory.make();
		for(String file : args) {
			sources.add(new SourceFileModule(new File(file), file, null));
		}
		E.setModuleFiles(sources);
		
		CallGraphBuilder<? super InstanceKey> builder = E.defaultCallGraphBuilder();
		PointerAnalysis<InstanceKey> PA = (PointerAnalysis<InstanceKey>) builder.getPointerAnalysis();

		CallGraph CG = builder.makeCallGraph(E.getOptions(), new NullProgressMonitor());
		System.err.println(CG);
		
		SDG<InstanceKey> SDG = new SDG<InstanceKey>(CG, PA, DataDependenceOptions.NO_EXCEPTIONS, ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
		System.err.println(SDG);
	}
	
}
