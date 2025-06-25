package com.ibm.wala.cast.python.ml.driver;

import com.ibm.wala.util.collections.HashSetFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbol;
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
import org.eclipse.lsp4j.ReferenceContext;
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

public class ClientDriver implements LanguageClient {
  private String[] args;
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
  public void telemetryEvent(Object object) {}

  @Override
  public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(ApplyWorkspaceEditParams params) {
    return CompletableFuture.supplyAsync(
        () -> {
          for (Map.Entry<String, List<TextEdit>> change :
              params.getEdit().getChanges().entrySet()) {
            System.err.println("for document " + change.getKey());
            for (TextEdit edit : change.getValue()) {
              process.accept(edit);
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
    for (Diagnostic d : diagnostics.getDiagnostics()) {
      CodeActionParams act = new CodeActionParams();
      act.setRange(d.getRange());
      TextDocumentIdentifier did = new TextDocumentIdentifier();
      did.setUri(d.getSource());
      act.setTextDocument(did);
      CodeActionContext ctxt = new CodeActionContext();
      ctxt.setDiagnostics(Collections.singletonList(d));
      act.setContext(ctxt);
      CompletableFuture<List<Either<Command, CodeAction>>> codeFuture =
          server.getTextDocumentService().codeAction(act);
      codeFuture.thenAccept(
          (List<Either<Command, CodeAction>> data) -> {
            for (Either<Command, CodeAction> ecmd : data) {
              Command cmd = ecmd.getLeft();
              process.accept(cmd);
              // System.err.println(cmd);
              ExecuteCommandParams p = new ExecuteCommandParams();
              p.setCommand(cmd.getCommand());
              p.setArguments(cmd.getArguments());
              CompletableFuture<Object> resultFuture =
                  server.getWorkspaceService().executeCommand(p);
              resultFuture.thenAccept(
                  (Object o) -> {
                    process.accept(o);
                    System.err.println(o);
                  });
            }
          });
    }

    TextDocumentIdentifier id = new TextDocumentIdentifier();
    id.setUri(args[0]);
    TextDocumentPositionParams a = new TextDocumentPositionParams();
    a.setTextDocument(id);
    for (int i = 1; i < args.length; i += 2) {
      Position p = new Position();
      p.setLine(Integer.parseInt(args[i]));
      p.setCharacter(Integer.parseInt(args[i + 1]));
      a.setPosition(p);
      CompletableFuture<Hover> data = server.getTextDocumentService().hover(a);
      data.thenAccept(
          (Hover t) -> {
            Either<List<Either<String, MarkedString>>, MarkupContent> contents = t.getContents();
            if (contents != null) {
              if (contents.isLeft()) {
                String xx = "";
                for (Either<String, MarkedString> hd : contents.getLeft()) {
                  xx += hd.getLeft();
                }
                process.accept(xx);
              } else {
                process.accept(contents.getRight().getValue());
              }
            }
          });
    }

    DocumentSymbolParams ds = new DocumentSymbolParams();
    ds.setTextDocument(id);
    CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> symbolFuture =
        server.getTextDocumentService().documentSymbol(ds);
    symbolFuture.thenAccept(
        (List<Either<SymbolInformation, DocumentSymbol>> xx) -> {
          System.err.println("symbols of " + ds.getTextDocument().getUri());
          for (Either<SymbolInformation, DocumentSymbol> sym : xx) {
            System.err.println(sym.getLeft());
            process.accept(sym.getLeft());
          }
        });

    CodeLensParams cs = new CodeLensParams();
    cs.setTextDocument(id);
    CompletableFuture<List<? extends CodeLens>> lensesFuture =
        server.getTextDocumentService().codeLens(cs);
    lensesFuture.thenAccept(
        (List<? extends CodeLens> xx) -> {
          System.err.println("lenses of " + ds.getTextDocument().getUri());
          for (CodeLens sym : xx) {
            process.accept(sym);
            System.err.println(sym);
            ExecuteCommandParams params = new ExecuteCommandParams();
            params.setCommand(sym.getCommand().getCommand());
            params.setArguments(sym.getCommand().getArguments());
            CompletableFuture<Object> result = server.getWorkspaceService().executeCommand(params);
            result.thenAccept(
                (Object xxx) -> {
                  process.accept(xxx);
                });
          }
        });

    ReferenceParams rp = new ReferenceParams();
    rp.setTextDocument(id);
    Position p = new Position();
    p.setLine(31);
    p.setCharacter(0);
    rp.setPosition(p);
    ReferenceContext rc = new ReferenceContext();
    rc.setIncludeDeclaration(false);
    rp.setContext(rc);
    CompletableFuture<List<? extends Location>> refsFuture =
        server.getTextDocumentService().references(rp);
    refsFuture.thenAccept(
        (List<? extends Location> xx) -> {
          System.err.println(xx);
        });
  }

  @Override
  public void showMessage(MessageParams messageParams) {
    System.out.println(messageParams.getMessage());
  }

  @Override
  public CompletableFuture<MessageActionItem> showMessageRequest(
      ShowMessageRequestParams requestParams) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void logMessage(MessageParams message) {
    // TODO Auto-generated method stuff
  }

  public static void main(String[] args)
      throws IOException, InterruptedException, ExecutionException {
    main(
        args,
        (Object s) -> {
          System.err.println(s);
        });
  }

  public static void main(String[] args, Consumer<Object> process)
      throws IOException, InterruptedException, ExecutionException {
    @SuppressWarnings("resource")
    Socket s = new Socket();
    s.connect(new InetSocketAddress("localhost", 6661));
    main(args, s.getInputStream(), s.getOutputStream(), process);
  }

  private CompletableFuture<Void> sendClientStuff(String[] args) {
    InitializeParams x = new InitializeParams();
    ClientCapabilities c = new ClientCapabilities();
    TextDocumentClientCapabilities tc = new TextDocumentClientCapabilities();
    PublishDiagnosticsCapabilities pc = new PublishDiagnosticsCapabilities();
    pc.setRelatedInformation(true);
    tc.setPublishDiagnostics(pc);
    c.setTextDocument(tc);
    x.setCapabilities(c);
    CompletableFuture<InitializeResult> y = server.initialize(x);
    return y.thenAccept(
        (InitializeResult xx) -> {
          System.err.println(xx);

          InitializedParams z = new InitializedParams();
          server.initialized(z);

          try {
            sendFile(args);
          } catch (URISyntaxException e) {
            e.printStackTrace();
            assert false;
          }
        });
  }

  private void sendFile(String[] args) throws URISyntaxException {
    String scriptUri = args[0];

    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(new URI(scriptUri).toURL().openStream()));
    } catch (IOException e1) {
      e1.printStackTrace();
      assert false;
    }
    StringBuffer fileData = new StringBuffer();
    String line;
    try {
      while ((line = br.readLine()) != null) {
        fileData.append(line).append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
      assert false;
    }

    DidOpenTextDocumentParams open = new DidOpenTextDocumentParams();
    TextDocumentItem script = new TextDocumentItem();
    open.setTextDocument(script);
    script.setLanguageId("python");
    script.setUri(scriptUri);
    script.setText(fileData.toString());
    server.getTextDocumentService().didOpen(open);
  }

  public static void main(String[] args, InputStream in, OutputStream out, Consumer<Object> process)
      throws IOException, InterruptedException, ExecutionException {
    ClientDriver client = new ClientDriver();
    client.args = args;
    client.process = process;

    Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(client, in, out);
    client.connect(launcher.getRemoteProxy());
    launcher.startListening();
    System.err.println("started");

    client.sendClientStuff(args).join();
  }
}
