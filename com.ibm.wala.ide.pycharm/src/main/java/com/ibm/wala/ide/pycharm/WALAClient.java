package com.ibm.wala.ide.pycharm;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import com.ibm.wala.cast.lsp.WALAServer;
import com.ibm.wala.cast.python.PythonDriver;

public class WALAClient implements LanguageClient{

	public WALAServer wala;
	
	public WALAClient() throws IOException {
		PipedInputStream toWalaIn = new PipedInputStream();
		PipedOutputStream toWalaOut = new PipedOutputStream();
		toWalaIn.connect(toWalaOut);

		PipedInputStream fromWalaIn = new PipedInputStream();
		PipedOutputStream fromWalaOut = new PipedOutputStream();
		fromWalaIn.connect(fromWalaOut);

		wala = WALAServer.launchOnStream(PythonDriver.python, toWalaIn, fromWalaOut);
		Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(this, fromWalaIn, toWalaOut);
		launcher.startListening();
	}

	@Override
	public void telemetryEvent(java.lang.Object object) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showMessage(MessageParams messageParams) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void logMessage(MessageParams message) {
		// TODO Auto-generated method stub
		
	}
}
