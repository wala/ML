package com.ibm.wala.cast.python.ml.test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assume.assumeThat;

import com.ibm.wala.cast.lsp.WALAServer;
import com.ibm.wala.cast.lsp.WALAServerCore;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.loader.PythonLoaderFactory;
import com.ibm.wala.cast.python.ml.driver.ClientDriver;
import com.ibm.wala.cast.python.ml.driver.PythonDriver;
import com.ibm.wala.cast.python.util.PythonInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.io.TemporaryFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ServerTest {

  static {
    try {
      @SuppressWarnings("unchecked")
      Class<? extends PythonLoaderFactory> j3 =
          (Class<? extends PythonLoaderFactory>)
              Class.forName("com.ibm.wala.cast.python.loader.Python3LoaderFactory");
      PythonAnalysisEngine.setLoaderFactory(j3);
      Class<?> i3 = Class.forName("com.ibm.wala.cast.python.util.Python3Interpreter");
      PythonInterpreter.setInterpreter(
          (PythonInterpreter) i3.getDeclaredConstructor().newInstance());
    } catch (ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException e) {
      try {
        @SuppressWarnings("unchecked")
        Class<? extends PythonLoaderFactory> j2 =
            (Class<? extends PythonLoaderFactory>)
                Class.forName("com.ibm.wala.cast.python.loader.Python2LoaderFactory");
        PythonAnalysisEngine.setLoaderFactory(j2);
        Class<?> i2 = Class.forName("com.ibm.wala.cast.python.util.Python2Interpreter");
        PythonInterpreter.setInterpreter(
            (PythonInterpreter) i2.getDeclaredConstructor().newInstance());
      } catch (ClassNotFoundException
          | InstantiationException
          | IllegalAccessException
          | IllegalArgumentException
          | InvocationTargetException
          | NoSuchMethodException
          | SecurityException e1) {
        assert false : e.getMessage() + ", then " + e1.getMessage();
      }
    }
  }

  @Test
  public void trivialClientServerPort()
      throws IOException,
          InterruptedException,
          ExecutionException,
          ClassHierarchyException,
          IllegalArgumentException,
          CancelException,
          URISyntaxException {
    WALAServerCore wala = WALAServer.launchOnServerPort(0, PythonDriver.python, true);
    try (Socket socket = new Socket("localhost", wala.getServerPort())) {
      trivialClient(socket.getInputStream(), socket.getOutputStream());
    }
  }

  @Test
  public void trivialClientStreams()
      throws IOException,
          InterruptedException,
          ExecutionException,
          ClassHierarchyException,
          IllegalArgumentException,
          CancelException,
          URISyntaxException {
    assumeThat("not running on Travis CI", System.getenv("TRAVIS"), nullValue());

    PipedInputStream clientIn = new PipedInputStream();
    PipedOutputStream serverOut = new PipedOutputStream(clientIn);

    PipedInputStream serverIn = new PipedInputStream(32767);
    PipedOutputStream clientOut = new PipedOutputStream(serverIn);

    WALAServer.launchOnStream(PythonDriver.python, serverIn, serverOut);

    trivialClient(clientIn, clientOut);
  }

  public void trivialClient(InputStream in, OutputStream out)
      throws IOException,
          InterruptedException,
          ExecutionException,
          ClassHierarchyException,
          IllegalArgumentException,
          CancelException,
          URISyntaxException {
    String script = "buggy_convolutional_network.py";
    String fileName = getScript(script);
    Set<String> checks = HashSetFactory.make();
    ClientDriver.main(
        new String[] {fileName, "37", "28", "46", "35"},
        in,
        out,
        (Object s) -> {
          if (s == null) {
            return;
          }
          // System.err.println(s);
          if (s instanceof TextEdit) {
            synchronized (checks) {
              checks.add(((TextEdit) s).getNewText());
            }
          } else if (s.toString().contains("pixel[?][28][28][1]")) {
            synchronized (checks) {
              checks.add("tensor");
            }
          } else if (s instanceof PublishDiagnosticsParams) {
            if (((PublishDiagnosticsParams) s).getDiagnostics().size() > 0) {
              synchronized (checks) {
                checks.add("error");
              }
              for (Diagnostic d : ((PublishDiagnosticsParams) s).getDiagnostics()) {
                Range r = d.getRange();
                if (r.getStart().getLine() == 37
                    && r.getStart().getCharacter() == 27
                    && r.getEnd().getLine() == 37
                    && r.getEnd().getCharacter() == 30) {
                  synchronized (checks) {
                    checks.add("xxx");
                  }
                }
              }
            }
          } else if (s instanceof SymbolInformation) {
            synchronized (checks) {
              checks.add(((SymbolInformation) s).getName());
            }
          } else if (s instanceof CodeLens) {
            synchronized (checks) {
              checks.add(((CodeLens) s).getCommand().getCommand());
            }
          } else if (s instanceof Command) {
            synchronized (checks) {
              checks.add(((Command) s).getCommand());
            }
          }
        });

    String stuff = "";
    boolean done = false;
    while (!done) {
      synchronized (checks) {
        String now = checks.toString();

        if (!now.equals(stuff)) {
          stuff = now;
          System.err.println(now);
        }
      }

      done = true;
      if (!(checks.contains("tensor")
          && checks.contains("error")
          && checks.contains("xxx")
          && checks.contains("TYPES")
          && checks.contains("FIXES")
          && checks.contains("tf.reshape(xxx, [-1, 28, 28, 1])"))) {
        done = false;
      }

      for (String fun : new String[] {"model_fn", "conv_net"}) {
        boolean model_fn = false;

        synchronized (checks) {
          for (String c : checks) {
            if (c.endsWith(fun)) {
              model_fn = true;
            }
          }
        }

        if (!model_fn) {
          done = false;
        }
      }
    }

    assert done;
  }

  private String getScript(String script) throws URISyntaxException, IOException {
    URL file = getClass().getClassLoader().getResource(script);
    if (file == null) {
      return script;
    } else {
      String prefix = script.substring(0, script.lastIndexOf('.'));
      File temp = File.createTempFile(prefix, ".py");
      TemporaryFile.urlToFile(temp, file);
      URI uri = temp.toURI();
      return Paths.get(uri).toUri().toString();
    }
  }
}
