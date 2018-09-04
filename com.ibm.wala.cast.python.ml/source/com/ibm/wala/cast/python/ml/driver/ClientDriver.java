package com.ibm.wala.cast.python.ml.driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import com.ibm.wala.util.collections.HashSetFactory;

public class ClientDriver implements LanguageClient {
	private LanguageServer server;
	private Consumer<Object> process;
	
	private Set<PublishDiagnosticsParams> diags = HashSetFactory.make();
	
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
	public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(ApplyWorkspaceEditParams params) {
		return CompletableFuture.supplyAsync(() -> {
			for(Map.Entry<String, List<TextEdit>> change : params.getEdit().getChanges().entrySet()) {
				System.err.println("for document " + change.getKey());
				for(TextEdit edit : change.getValue()) {
					System.err.println("text " + edit.getNewText() + " at " + edit.getRange());
				}
			}
			ApplyWorkspaceEditResponse ret = new ApplyWorkspaceEditResponse();
			ret.setApplied(true);
			return ret;
		});
	}

	@Override
	public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
		process.accept(diagnostics);
		diags.add(diagnostics);
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
		main(args, (Object s) -> { System.err.println(s); });
	}
	
	public static void main(String[] args, Consumer<Object> process) throws IOException, InterruptedException, ExecutionException {
		@SuppressWarnings("resource")
		Socket s = new Socket(); 
		s.connect(new InetSocketAddress("localhost", 6660));
		
		ClientDriver client = new ClientDriver();
		client.process = process;
		
		Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(client, s.getInputStream(), s.getOutputStream());
		client.connect(launcher.getRemoteProxy());
		launcher.startListening();	
		System.err.println("started");
		//System.err.println(client.server.getTextDocumentService());
		
		InitializeParams x = new InitializeParams();
		ClientCapabilities c = new ClientCapabilities();
		TextDocumentClientCapabilities tc = new TextDocumentClientCapabilities();
		PublishDiagnosticsCapabilities pc = new PublishDiagnosticsCapabilities();
		pc.setRelatedInformation(true);
		tc.setPublishDiagnostics(pc);
		c.setTextDocument(tc);
		x.setCapabilities(c);
		CompletableFuture<InitializeResult> y = client.server.initialize(x);
		System.err.println(y.get());
		
		InitializedParams z = new InitializedParams();
		client.server.initialized(z);
		
		String scriptUri = args[0];
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new URL(scriptUri).openStream()));
		StringBuffer fileData = new StringBuffer();
		String line;
	    while((line = br.readLine()) != null) {
	    	fileData.append(line).append("\n");
	    }

	    DidOpenTextDocumentParams open = new DidOpenTextDocumentParams();
		TextDocumentItem script = new TextDocumentItem();
		open.setTextDocument(script);
		script.setLanguageId("python");
		script.setUri(scriptUri);
		script.setText(fileData.toString());
		client.server.getTextDocumentService().didOpen(open);
		
		Thread.sleep(10000);
		
		TextDocumentIdentifier id = new TextDocumentIdentifier();
		id.setUri(scriptUri);

		TextDocumentPositionParams a = new TextDocumentPositionParams();
		a.setTextDocument(id);
		for(int i = 1; i < args.length; i += 2) {
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
			process.accept(sym);
		}
		
		CodeLensParams cs = new CodeLensParams();
		cs.setTextDocument(id);
		CompletableFuture<List<? extends CodeLens>> lensesFuture = client.server.getTextDocumentService().codeLens(cs);
		System.err.println("lenses of " + ds.getTextDocument().getUri());
		for(CodeLens sym : lensesFuture.get()) {
			process.accept(sym);
			ExecuteCommandParams params = new ExecuteCommandParams();
			params.setCommand(sym.getCommand().getCommand());
			params.setArguments(sym.getCommand().getArguments());
			CompletableFuture<Object> result = client.server.getWorkspaceService().executeCommand(params);
			process.accept(result.get());
		}
		
		for(PublishDiagnosticsParams diagnostics : client.diags) {
			for(Diagnostic d : diagnostics.getDiagnostics()) {
				CodeActionParams act = new CodeActionParams();
				act.setRange(d.getRange());
				TextDocumentIdentifier did = new TextDocumentIdentifier();
				did.setUri(d.getSource());
				act.setTextDocument(did);
				CodeActionContext ctxt = new CodeActionContext();
				ctxt.setDiagnostics(Collections.singletonList(d));
				act.setContext(ctxt);
				CompletableFuture<List<? extends Command>> codeFuture = client.server.getTextDocumentService().codeAction(act);
				try {
					for(Command cmd : codeFuture.get()) {
						ExecuteCommandParams p = new ExecuteCommandParams();
						p.setCommand(cmd.getCommand());
						p.setArguments(cmd.getArguments());
						CompletableFuture<Object> resultFuture = client.server.getWorkspaceService().executeCommand(p);
						process.accept(resultFuture.get());
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
					assert false;
				}
			}
		}
		
		ReferenceParams rp = new ReferenceParams();
		rp.setTextDocument(id);
		Position p = new Position();
		p.setLine(32);
		p.setCharacter(0);
		rp.setPosition(p);
		CompletableFuture<List<? extends Location>> refsFuture = client.server.getTextDocumentService().references(rp);
		System.err.println(refsFuture.get());
	}
}