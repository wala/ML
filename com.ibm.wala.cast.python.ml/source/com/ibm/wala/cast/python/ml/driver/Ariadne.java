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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import com.ibm.wala.cast.lsp.Util;
import com.ibm.wala.cast.lsp.WALAServer;
import com.ibm.wala.cast.python.ml.driver.DiagnosticsFormatter.FORMAT;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class Ariadne {
	static enum MODE {
		linter,
		server,
		daemon,
		client,
		stdio
	};

	private final static MODE default_mode = MODE.linter;
	private final static FORMAT default_format = FORMAT.pretty;

	static private <T> String collToString(Collection<T> set) {
		return set.stream()
				.map(x -> x.toString())
				.collect(Collectors.joining(","));
	}

	
	private static Set<DiagnosticSeverity> default_severityList =
			EnumSet.of(DiagnosticSeverity.Error, DiagnosticSeverity.Warning);
	
	
	public static void main(String args[]) throws ClassHierarchyException, IOException, IllegalArgumentException, CancelException {
		final CommandLineParser optionParser = new DefaultParser();
		/* Command line options */
		final Options options = new Options();

		final Map<String, Set<MODE>> optModes = new HashMap<String, Set<MODE>>();

		final Option modeOpt = Option.builder().longOpt("mode")
				.hasArg().argName("mode")
				.desc("Specify what mode to run in.  Allowed values: (" + collToString(EnumSet.allOf(MODE.class)) + ").  Default: " + default_mode + ".")
				.required(false).build();
		options.addOption(modeOpt);

		final EnumSet<MODE> portOptModes = EnumSet.of(MODE.server, MODE.daemon, MODE.client);
		final Option portOpt = Option.builder().longOpt("port")
				.hasArg().argName("port")
				.desc("Specify the tcp port that should be used [modes: " + collToString(portOptModes) + "].")
				.required(false).build();
		options.addOption(portOpt);
		optModes.put(portOpt.getLongOpt(), portOptModes);

		final EnumSet<MODE> formatOptModes = EnumSet.of(MODE.linter);
		final Option formatOption = Option.builder().longOpt("format")
				.hasArg().argName("format")
				.desc("Format of output (" + collToString(EnumSet.allOf(FORMAT.class)) + "). Default: " + default_format.toString() + " [modes: " + collToString(portOptModes) + "]")
				.required(false).build();
		options.addOption(formatOption);
		optModes.put(formatOption.getLongOpt(), formatOptModes);

		final EnumSet<MODE> severityOptModes = EnumSet.of(MODE.linter);
		final Option severityOption = Option.builder().longOpt("severity")
				.hasArgs().valueSeparator(',').argName("severity")
				.desc("List of diagnostic severity levels to emit.  Can be a list of (" + collToString(EnumSet.allOf(DiagnosticSeverity.class)) + ").  Default: " + collToString(default_severityList) + " [modes: " + collToString(severityOptModes) + "]")
				.required(false).build();
		options.addOption(severityOption);
		optModes.put(severityOption.getLongOpt(), severityOptModes);

		final EnumSet<MODE> relatedOptModes = EnumSet.of(MODE.linter);
		final Option relatedOption = Option.builder().longOpt("related")
				.hasArgs().argName("related")
				.desc("The maximum number of related items to print.  (either a number or \"unlimited\").  Default: \"unlimited\"" + " [modes: " + collToString(relatedOptModes) + "]")
				.required(false).build();
		options.addOption(relatedOption);
		optModes.put(relatedOption.getLongOpt(), relatedOptModes);
		
		
		final Option helpOpt = Option.builder().longOpt("help").argName("help")
				.desc("Print usage information").required(false).build();
		options.addOption(helpOpt);

		MODE mode = default_mode;
		int port = -1;

		FORMAT format = default_format;
		Set<DiagnosticSeverity> severityList = default_severityList;
		int related = -1;
		Map<String,String> uriTextPairs = new HashMap<String,String>();


		try {
			/* Parse command line */
			final CommandLine cmd = optionParser.parse(options, args);
			if (cmd.hasOption("help")) {
				printUsage(options);
				return;
			}

			final String modeString = cmd.getOptionValue("mode");
			if(modeString != null) {
				try {
					mode = MODE.valueOf(modeString);
				} catch(IllegalArgumentException e) {
					System.err.println("Error: mode passed to --mode option is not valid.  Please specify one of (" + collToString(EnumSet.allOf(MODE.class)) + ")");

					printUsage(options);
					System.exit(1);
				}
			}

			// Validate that all options are only used if applicable for the current mode
			for(String opt : optModes.keySet()) {
				if(cmd.hasOption(opt)) {
					Set<MODE> modeSet = optModes.get(opt);
					if(! modeSet.contains(mode)) {
						System.err.println("Error: option " + opt + " was specified while running in " + modeString.toString() + " mode.  This option is only applicable when running as one of (" + collToString(modeSet) + ")");

						printUsage(options);
						System.exit(1);
					}
				}
			}

			final String portString = cmd.getOptionValue("port");
			if(portString != null) {
				final String trimmedString = portString.trim();
				if(trimmedString.isEmpty()) {
					port = 0;
				} else {
					try {
						port = Integer.parseInt(trimmedString);
						if(port < 0) {
							System.err.println("Error: port value of '" + port + "' specified.  Negative ports are not valid.");
						}
					} catch(NumberFormatException e) {
						System.err.println("Error: port value of '" + port + "' specified");
						printUsage(options);
						System.exit(1);
					}
				}
			}
			
			final String formatString = cmd.getOptionValue("format");
			if(formatString != null) {
				try {
					format = FORMAT.valueOf(formatString);
				} catch(IllegalArgumentException e) {
					System.err.println("Error: format passed to --format option is not valid.  Please specify one of (" + collToString(EnumSet.allOf(FORMAT.class)) + ")");

					printUsage(options);
					System.exit(1);
				}
			}
			
			String[] severityStrings = cmd.getOptionValues("severity");
			if(severityStrings != null) {
				severityList = EnumSet.noneOf(DiagnosticSeverity.class);
				for(int i = 0; i < severityStrings.length; i++) {
					final String sevStr = severityStrings[i];
					try {
						severityList.add(DiagnosticSeverity.valueOf(sevStr));
					} catch(IllegalArgumentException e) {
						System.err.println("Error: severity passed to --severity option is not valid.  Please specify some of (" + collToString(EnumSet.allOf(DiagnosticSeverity.class)) + ")");

						printUsage(options);
						System.exit(1);
					}
				}
			}
			
			final String relatedString = cmd.getOptionValue("related");
			if(relatedString != null) {
				if(relatedString.equalsIgnoreCase("unlimited") || relatedString.equalsIgnoreCase("all")) {
					related = -1;
				} else {
					try {
						related = Integer.parseInt(relatedString);
						if(related < 0) {
							related = -1;
						}
					} catch(IllegalArgumentException e) {
						System.err.println("Error: related count passed to --related option is not valid.  Please specify either a number of the string \"unlimited\"");
	
						printUsage(options);
						System.exit(1);
					}
				}
			}
			
			switch(mode) {
			case linter: {
				List<String> files = cmd.getArgList();
				if(files.isEmpty()) {
					System.err.println("Warning: Ariadne run as a linter, but no files were specified.  Run with --help for usage information.");
				}

				for(String fileName : files) {
					try {
						final Path path = Paths.get(fileName);
						String uri = Util.mangleUri(path.toUri().toString());
						String text = new String(Files.readAllBytes(path));
						if(uriTextPairs.containsKey(uri)) {
							System.err.println("WARNING: ignoring repeated filename: " + fileName);
						} else {
							uriTextPairs.put(fileName, text);
						}
					} catch(IOException e) {
						System.err.println("Failed to read file: " + fileName);
						System.exit(1);
					}
				}
				break;
			}
			default:
				List<String> extraArgs = cmd.getArgList();
				if(! extraArgs.isEmpty()) {
					System.err.println("Warning: extra arguments being ignored: (" + collToString(extraArgs) + ").  If these were meant to be files, file arguments are not supported in this mode (" + mode.toString() + ")");
				}
			}


		} catch (final ParseException e) {
			printUsage(options);
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		switch(mode) {
		case stdio: {
			WALAServer.launchOnStdio(PythonDriver.python);
			break;
		}
		case client: {
			final WALAServer server = WALAServer.launchOnClientPort(null, port, PythonDriver.python);
			break;
		}
		case server: {
			final WALAServer server = 
					WALAServer.launchOnServerPort(port, PythonDriver.python, false);
			final Integer actualPort = server.getServerPort();
			System.err.println("Server up, listening on port: " + actualPort);
			break;
		}
		case daemon: {
			final WALAServer server = 
					WALAServer.launchOnServerPort(port, PythonDriver.python, true);
			final Integer actualPort = server.getServerPort();
			System.err.println("Server up, listening on port: " + actualPort);
			break;
		}
		case linter: {
			if(! uriTextPairs.isEmpty()) {
				Map<String, List<Diagnostic>> diagnostics = PythonDriver.getDiagnostics(uriTextPairs);
				if(diagnostics == null) {
					System.err.println("There was an error generating diagnostics");
					System.exit(1);
				}
				Map<String, List<Diagnostic>> filteredDiagnostics = DiagnosticsFormatter.filterSeverity(diagnostics, severityList);
				format.print(System.out, uriTextPairs, filteredDiagnostics, related);
			}
			break;
		}
		}

	}
	
	
	private static final String APP_NAME = "Ariadne";
	private static final String APP_DESCRIPTION = "Ariadne: A Language Server Protocol server and linter for WALA Python/ML analysis";

	private static void printUsage(final Options options) {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(APP_NAME + " [arguments] [filenames]", APP_DESCRIPTION, options, null);
	}

}
