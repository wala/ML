package com.ibm.wala.cast.python;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.SymbolInformation;
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
		id.setUri(scriptUri);

		TextDocumentPositionParams a = new TextDocumentPositionParams();
		a.setTextDocument(id);
		for(int i = 0; i < args.length; i += 2) {
			Position p = new Position();
			p.setLine(Integer.parseInt(args[i]));
			p.setCharacter(Integer.parseInt(args[i+1]));
			a.setPosition(p);
			CompletableFuture<Hover> data = client.server.getTextDocumentService().hover(a);
			Hover t = data.get();
			Either<List<Either<String, MarkedString>>, MarkupContent> contents = t.getContents();
			if(contents.isLeft()) {
				String xx = "";
				for(Either<String, MarkedString> hd : contents.getLeft()) {
					xx += hd.getLeft();			
				}
				process.accept(xx);
			} else {
				process.accept(contents.getRight().getValue());
			}
		}
	
		DocumentSymbolParams ds = new DocumentSymbolParams();
		ds.setTextDocument(id);
		CompletableFuture<List<? extends SymbolInformation>> symbolFuture = client.server.getTextDocumentService().documentSymbol(ds);
		System.err.println("symbols of " + ds.getTextDocument().getUri());
		for(SymbolInformation sym : symbolFuture.get()) {
			System.err.println(sym);
		}
	}
}