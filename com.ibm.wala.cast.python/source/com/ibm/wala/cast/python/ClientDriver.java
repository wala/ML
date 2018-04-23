package com.ibm.wala.cast.python;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
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
		main(args, (String s) -> { System.err.println(s); });
	}
	
	public static void main(String[] args, Consumer<String> process) throws IOException, InterruptedException, ExecutionException {
		@SuppressWarnings("resource")
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
		
		String scriptUri = "https://raw.githubusercontent.com/aymericdamien/TensorFlow-Examples/dd2e6dcd9603d5de008d8c766453162d0204affa/examples/3_NeuralNetworks/convolutional_network.py";
		
		DidOpenTextDocumentParams open = new DidOpenTextDocumentParams();
		TextDocumentItem script = new TextDocumentItem();
		open.setTextDocument(script);
		script.setLanguageId("python");
		script.setUri(scriptUri);
		client.server.getTextDocumentService().didOpen(open);
		
		Thread.sleep(10000);
		
		TextDocumentIdentifier id = new TextDocumentIdentifier();
		TextDocumentPositionParams a = new TextDocumentPositionParams();
		id.setUri(scriptUri);
		a.setTextDocument(id);
		Position p = new Position();
		p.setLine(42);
		p.setCharacter(12);
		a.setPosition(p);
		CompletableFuture<Hover> data = client.server.getTextDocumentService().hover(a);
		data.whenComplete(new BiConsumer<Hover, Throwable>() {
			@Override
			public void accept(Hover t, Throwable u) {
				for(Either<String, MarkedString> hd : t.getContents()) {
					process.accept(hd.getLeft());			
				}
			}
		});
	}
}