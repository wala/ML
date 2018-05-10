package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.cast.python.ClientDriver;
import com.ibm.wala.cast.python.PythonDriver;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class ServerTest {

	@BeforeClass
	public static void startServer() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException {
		PythonDriver.main(new String[] {"-server-port", "6660", "-daemon", "true"});
	}

	@Test
	public void trivialClient() throws IOException, InterruptedException, ExecutionException {
		ClientDriver.main(new String[] {"41", "10", "44", "35"}, (String s) -> { 
			System.err.println("found " + s);
			assert s.contains("pixel[?][28][28][1]");
		});
	}
}
