package com.ibm.wala.cast.python.ml.test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.junit.Test;

import com.ibm.wala.cast.python.ml.driver.ClientDriver;
import com.ibm.wala.cast.python.ml.driver.PythonDriver;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.io.TemporaryFile;

public class ServerTest {


	@Test
	public void trivialClient() throws IOException, InterruptedException, ExecutionException, ClassHierarchyException, IllegalArgumentException, CancelException, URISyntaxException {
		PythonDriver.main(new String[] {"-server-port", "6660", "-daemon", "true"});
		String script = "buggy_convolutional_network.py";
		String fileName = getScript(script);
		Set<String> checks = HashSetFactory.make();
		ClientDriver.main(new String[] {fileName, "43", "10", "46", "35"}, (Object s) -> { 
			System.err.println("found " + s);
			if (s == null) {
				return;
			}
			if (s.toString().contains("pixel[?][28][28][1]")) {
				checks.add("tensor");
			} else if (s instanceof PublishDiagnosticsParams) {
				if (((PublishDiagnosticsParams)s).getDiagnostics().size() > 0) {
					checks.add("error");
				}
			} else if (s instanceof SymbolInformation) {
				checks.add(((SymbolInformation)s).getName());
			} else if (s instanceof CodeLens) {
				checks.add(((CodeLens)s).getCommand().getCommand());
			}
		});
		System.err.println(checks);
		assert (checks.contains("tensor") && 
				checks.contains("error") &&
				checks.contains("TYPES"));
		
		for(String fun : new String[] {"model_fn", "conv_net" }) {
			boolean model_fn = false;
			for(String c : checks) {
				if (c.endsWith(fun)) {
					model_fn = true;
				}
			}
			
			assert model_fn : "cannot find " + fun + " in assertions";
		}
		
	}

	private String getScript(String script) throws URISyntaxException, IOException {
		URL file = getClass().getClassLoader().getResource(script);
		if (file == null) {
			return script;
		} else {
			String prefix = script.substring(0, script.lastIndexOf('.'));
			File temp = File.createTempFile(prefix, ".py");
			TemporaryFile.urlToFile(temp, file);
			URI uri = temp.toURI();
			return Paths.get(uri).toUri().toString();
		}
	}
	
}
