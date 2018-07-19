/******************************************************************************

 * Copyright (c) 2018 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.python.ml.driver;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.eclipse.lsp4j.Diagnostic;

import com.ibm.wala.cast.lsp.WALAServer;
import com.ibm.wala.cast.lsp.Util;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.cast.python.ml.driver.PythonDriver;

public class PythonLinter {

	public static Map<String, List<Diagnostic>> getDiagnostics(String language, Map<String,String> uriTextPairs) {
		return WALAServer.getDiagnostics(PythonDriver.python, language, uriTextPairs);
	}

	public static Map<String, List<Diagnostic>> getDiagnostics(Map<String,String> uriTextPairs) {
		return getDiagnostics("python", uriTextPairs);
	}

	static enum FORMAT {
		json{
			@Override
			public void print(PrintStream out, Map<String, List<Diagnostic>> diagnostics) {
				if(diagnostics == null || diagnostics.isEmpty()) {
					out.println("{}");
					return;
				}
				
				out.println("{");
				boolean pastFirst = false;
				for(Entry<String, List<Diagnostic>> entry : diagnostics.entrySet()) {
					if(pastFirst) {
						out.println();
						out.print(", ");
					} else {
						pastFirst = true;
					}
					out.print("\"");
					out.print(entry.getKey());
					out.print("\": [");
					List<Diagnostic> diags = entry.getValue();
					if(diags != null) {
						boolean pastFirstArr = false;
						for(Diagnostic diag : diags) {
							if(pastFirstArr) {
								out.println();
								out.print(", ");
							} else {
								pastFirstArr = true;
							}
							out.print(diags.toString());
						}
					}
					out.println("]");
				}
				out.println("}");
			}
		},
		pretty{
			@Override
			public void print(PrintStream out, Map<String, List<Diagnostic>> diagnostics) {
				System.err.println("Warning: pretty printing not yet supported.  Using json formatting instead");
				json.print(out, diagnostics);
			}

		};
	
		public abstract void print(PrintStream out, Map<String, List<Diagnostic>> diagnostics);
	};
	private final static FORMAT default_format = FORMAT.pretty;

	static private String getFormatList() {
		return Arrays.stream(FORMAT.values())
		.map(x -> x.toString())
		.collect(Collectors.joining(","));
	}

	public static void main(String args[]) throws ClassHierarchyException, IOException, IllegalArgumentException, CancelException {
		final CommandLineParser optionParser = new DefaultParser();
		/* Command line options */
		final Options options = new Options();

		final Option formatOption = Option.builder().longOpt("format")
		.hasArg().argName("format")
		.desc("Format of output (" + getFormatList() + ").  Default: " + default_format.toString())
		.required(false).build();
		options.addOption(formatOption);

		final Option helpOpt = Option.builder().longOpt("help").argName("help")
			.desc("Print usage information").required(false).build();
	    options.addOption(helpOpt);
		
		FORMAT format = default_format;

		try {
			/* Parse command line */
			final CommandLine cmd = optionParser.parse(options, args);
			if (cmd.hasOption("help")) {
			  printUsage(options);
			  return;
			}


			final String formatString = cmd.getOptionValue("formatString");
			if(formatString != null) {
				try {
					format = FORMAT.valueOf(formatString);
				} catch(IllegalArgumentException e) {
					System.err.println("Error: format passed to --format option is not valid.  Please specify one of (" + getFormatList() + ")");

					printUsage(options);
					System.exit(1);
				}
			}

			List<String> files = cmd.getArgList();
			Map<String,String> uriTextPairs = new HashMap<String,String>();
			for(String fileName : files) {
				try {
					final Path path = Paths.get(fileName);
					String uri = Util.mangleUri(path.toUri().toString());
					String text = new String(Files.readAllBytes(path));
					if(uriTextPairs.containsKey(uri)) {
						System.err.println("WARNING: ignoring repeated filename: " + fileName);
					} else {
						uriTextPairs.put(uri, text);
					}
				} catch(IOException e) {
					System.err.println("Failed to read file: " + fileName);
					System.exit(1);
				}
			}
			if(! uriTextPairs.isEmpty()) {
				Map<String, List<Diagnostic>> diagnostics = getDiagnostics(uriTextPairs);
				if(diagnostics == null) {
					System.err.println("There was an error generating diagnostics");
					System.exit(1);
				}
				format.print(System.out, diagnostics);
			}
		} catch (final ParseException e) {
			printUsage(options);
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}

	private static final String APP_NAME = "wala-lsp-linter-python-ml";
	private static final String APP_DESCRIPTION = "A Linter for WALA Python/ML analysis";

	private static void printUsage(final Options options) {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(APP_NAME, APP_DESCRIPTION, options, null);
	}

}
