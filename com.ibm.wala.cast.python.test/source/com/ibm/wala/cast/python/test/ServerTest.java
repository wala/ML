package com.ibm.wala.cast.python.test;

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

import com.ibm.wala.cast.python.ClientDriver;
import com.ibm.wala.cast.python.PythonDriver;
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
				checks.contains("Lconv_net") &&
				checks.contains("Lmodel_fn") &&
				checks.contains("TYPES") &&
				checks.contains("CALLS"));
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
