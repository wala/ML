package com.ibm.wala.cast.python;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

public class ClientDriver implements LanguageClient {
	private LanguageServer server;
	
	public ClientDriver() {
		// TODO Auto-generated constructor stub
	}

	public void connect(LanguageServer s) {
		server = s;
	}
	
	@Override
	public void telemetryEvent(Object object) {

	}

	@Override
	public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
		// TODO Auto-generated method stub

	}

	@Override
	public void showMessage(MessageParams messageParams) {
		System.out.println(messageParams.getMessage());
	}

	@Override
	public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void logMessage(MessageParams message) {
		// TODO Auto-generated method stuff
	}

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		Socket s = new Socket(); 
		s.connect(new InetSocketAddress("localhost", 6660));
		ClientDriver client = new ClientDriver();
		Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(client, s.getInputStream(), s.getOutputStream());
		client.connect(launcher.getRemoteProxy());
		launcher.startListening();	
		System.err.println("started");
		System.err.println(client.server.getTextDocumentService());
		
		InitializeParams x = new InitializeParams();
		ClientCapabilities c = new ClientCapabilities();
		x.setCapabilities(c);
		CompletableFuture<InitializeResult> y = client.server.initialize(x);
		System.err.println(y.get());
		InitializedParams z = new InitializedParams();
		client.server.initialized(z);
		
		TextDocumentPositionParams a = new TextDocumentPositionParams();
		client.server.getTextDocumentService().hover(a);
	}
}