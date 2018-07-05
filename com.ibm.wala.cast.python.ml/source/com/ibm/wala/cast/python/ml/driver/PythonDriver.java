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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.lsp.WALAServer;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ml.analysis.TensorVariable;
import com.ibm.wala.cast.python.ml.client.PythonTensorAnalysisEngine;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.CancelException;

public class PythonDriver {
	private static String getTypeNameString(TypeName typ) {
		String str = typ.toString();
		if(str.startsWith("L")) {
			str = str.substring(1);
		}
		str = str.replaceAll("/", ".");
		return str;
	}
	public static final Function<WALAServer, Function<String, AbstractAnalysisEngine<InstanceKey, ? extends PropagationCallGraphBuilder, ?>>> python = (WALAServer lsp) -> {
		return (String language) -> {
			assert "python".equals(language) : language;
			PythonTensorAnalysisEngine engine = new PythonTensorAnalysisEngine() {

				@Override
				public TensorTypeAnalysis performAnalysis(
						PropagationCallGraphBuilder builder) throws CancelException {

					TensorTypeAnalysis tt = super.performAnalysis(builder);

					CallGraph CG = builder.getCallGraph();
					CG.iterator().forEachRemaining((CGNode n) -> { 
						IMethod M = n.getMethod();
						if (M instanceof AstMethod) {
							IR ir = n.getIR();
							ir.iterateAllInstructions().forEachRemaining((SSAInstruction inst) -> {
								if (inst.iindex != -1) {
									Position pos = ((AstMethod)M).debugInfo().getInstructionPosition(inst.iindex);
									if (pos != null) {
										lsp.add(pos, new int[] {CG.getNumber(n), inst.iindex});
									}
									if (inst.hasDef()) {
										PointerKey v = builder.getPointerAnalysis().getHeapModel().getPointerKeyForLocal(n, inst.getDef());
										if (M instanceof AstMethod) {
											if (pos != null) {
												lsp.add(pos, v);
											}
										}
									}
								}
							});
						}
					});

					lsp.addValueAnalysis("type", builder.getPointerAnalysis().getHeapGraph(), (Boolean useMarkdown, PointerKey v) -> {
						if (builder.getPropagationSystem().isImplicit(v)) {
							return null;
						} else {
							PointsToSetVariable pts = builder.getPropagationSystem().findOrCreatePointsToSet(v);
							if (tt.getProblem().getFlowGraph().containsNode(pts)) {
								TensorVariable vv = tt.getOut(pts);
								String str = vv.toCString(useMarkdown);
								return str;
							} else {
								return null;
							}
						}
					});

					lsp.addInstructionAnalysis("target", (Boolean useMarkdown, int[] instId) -> {
						CGNode node = builder.getCallGraph().getNode(instId[0]);
						SSAInstruction[] insts = node.getIR().getInstructions();
						if (insts.length > instId[1]) {
							SSAInstruction inst = insts[instId[1]];
							if (inst instanceof SSAAbstractInvokeInstruction) {
								CallSiteReference ref = ((SSAAbstractInvokeInstruction)inst).getCallSite();
								final Set<CGNode> possibleTargets = builder.getCallGraph().getPossibleTargets(node, ref);

								if(possibleTargets.isEmpty()) {
									return null;
								}

								final String delim;
								if(useMarkdown) {
									delim = "     _or_ ";
								} else {
									delim = "     or ";
								}

								final String targetStringList = possibleTargets
										.stream()
										.map(callee ->
										getTypeNameString(callee.getMethod().getDeclaringClass().getName()))
										.distinct()
										.collect(Collectors.joining(delim));

								return targetStringList;
							}
						}
						return null;
					});	
					
					lsp.setFindDefinitionAnalysis((int[] instId) -> {
						CGNode node = builder.getCallGraph().getNode(instId[0]);
						SSAInstruction inst = node.getIR().getInstructions()[instId[1]];
						if (inst instanceof SSAAbstractInvokeInstruction) {
							CallSiteReference ref = ((SSAAbstractInvokeInstruction)inst).getCallSite();
								final Set<CGNode> possibleTargets = builder.getCallGraph().getPossibleTargets(node, ref);


							final Set<Position> targetPositions = possibleTargets
							.stream()
							.map(callee -> {
								IMethod method = callee.getMethod();
								if (method instanceof AstMethod) {
									AstMethod amethod = (AstMethod)method;
									return amethod.getSourcePosition();
								} else {
									return null;
								}
							})
							.filter(x -> x != null)
							.distinct()
							.collect(Collectors.toSet());

							return targetPositions;
						} else {
							return null;
						}
					});	
					
					lsp.addValueErrors(language, this.getErrors());
					
					return tt;
				}	
			};

			return engine;
		};
	};

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
				WALAServer.launchOnStdio(python);
			} else {
				final WALAServer server = 
					WALAServer.launchOnServerPort(serverPort, python, runAsDaemon);
				if(serverPort == 0) {
					final Integer actualPort = server.getServerPort();
					System.err.println("Server up, listening on port: " + actualPort);
				}
			}
		} else {
			if(serverPort < 0) {
				@SuppressWarnings("unused")
				final WALAServer server = WALAServer.launchOnClientPort(null, clientPort, python);
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
