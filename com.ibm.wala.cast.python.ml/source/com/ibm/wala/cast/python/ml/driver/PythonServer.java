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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.ibm.wala.cast.lsp.WALAServer;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.cast.python.ml.driver.PythonDriver;


public class PythonServer {

	public static void main(String args[]) throws ClassHierarchyException, IOException, IllegalArgumentException, CancelException {
		final CommandLineParser optionParser = new DefaultParser();
		/* Command line options */
		final Options options = new Options();

		final Option runAsDaemonOpt = Option.builder().longOpt("daemon")
				.hasArg().argName("daemon")
				.desc("Run server as daemon thread")
				.required(false).build();
		options.addOption(runAsDaemonOpt);

		final Option webSocketOpt = Option.builder().longOpt("web-socket")
				.hasArg().argName("websocket")
				.desc("Run server using WebSockets")
				.required(false).build();
		options.addOption(webSocketOpt);

		final Option serverPortOpt = Option.builder().longOpt("server-port")
			.hasArg().argName("server-port")
			.desc("Specify the port that the server should start listening on.")
			.required(false).build();
		options.addOption(serverPortOpt);

		final Option clientPortOpt = Option.builder().longOpt("client-port")
		.hasArg().argName("client-port")
		.desc("Specify the exiting client port that the server should connect to")
		.required(false).build();
		options.addOption(clientPortOpt);

		final Option helpOpt = Option.builder().longOpt("help").argName("help")
			.desc("Print usage information").required(false).build();
	    options.addOption(helpOpt);

		int serverPort = -1;
		int clientPort = -1;
		boolean runAsDaemon = false;
		boolean webSockets = false;
		
		try {
			/* Parse command line */
			final CommandLine cmd = optionParser.parse(options, args);
			if (cmd.hasOption("help")) {
			  printUsage(options);
			  return;
			}

			final String serverPortString = cmd.getOptionValue("server-port");
			final String clientPortString = cmd.getOptionValue("client-port");
			if (cmd.hasOption("daemon")) {
				runAsDaemon = Boolean.parseBoolean(cmd.getOptionValue("daemon"));
			}
			if (cmd.hasOption("websocket")) {
				webSockets = Boolean.parseBoolean(cmd.getOptionValue("websocket"));
			}
			
			if(serverPortString == null) {
				serverPort = -1;
			} else {
				final String trimmedString = serverPortString.trim();
				if(trimmedString.isEmpty()) {
					serverPort = 0;
				} else if(trimmedString.equals("-") || trimmedString.equals("--")) {
					serverPort = -1;
				} else {
					try {
						serverPort = Integer.parseInt(trimmedString);
					} catch(NumberFormatException e) {
						System.err.println("Error: port value of '" + serverPortString + "' specified, which"
						+ " is neither a valid port number (integer) nor the special - indicating that standard io should be used");
						printUsage(options);
						System.exit(1);
					}
				}
			}

			if(clientPortString == null) {
				clientPort = -1;
			} else {
				final String trimmedString = clientPortString.trim();
				if(trimmedString.isEmpty()) {
					clientPort = -1;
				} else {
					try {
						clientPort = Integer.parseInt(trimmedString);
					} catch(NumberFormatException e) {
						System.err.println("Error: client port value of '" + clientPortString + "' specified, which"
						+ " is not a valid port number (integer)");
						printUsage(options);
						System.exit(1);
					}
				}
			}
		} catch (final ParseException e) {
			printUsage(options);
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		if(clientPort < 0) {
			if(serverPort < 0) {
				WALAServer.launchOnStdio(PythonDriver.python);
			} else {
				final WALAServer server = 
					WALAServer.launchOnServerPort(serverPort, PythonDriver.python, runAsDaemon);
				if(serverPort == 0) {
					final Integer actualPort = server.getServerPort();
					System.err.println("Server up, listening on port: " + actualPort);
				}
			}
		} else {
			if(serverPort < 0) {
				@SuppressWarnings("unused")
				final WALAServer server = WALAServer.launchOnClientPort(null, clientPort, PythonDriver.python);
			} else {
				System.err.println("Both of the mutually exclusive options --server-port ("
				+ serverPort + ") and --client-port (" + clientPort
				+ ") where specified.  Please pick.  ");
				printUsage(options);
				System.exit(-1);
			}
		}
	}

	private static final String APP_NAME = "wala-lsp-server-python-ml";
	private static final String APP_DESCRIPTION = "A Language Server Protocol server for WALA Python/ML analysis";

	private static void printUsage(final Options options) {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(APP_NAME, APP_DESCRIPTION, options, null);
	}

}
