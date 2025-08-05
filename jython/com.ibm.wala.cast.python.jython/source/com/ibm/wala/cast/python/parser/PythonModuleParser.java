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
package com.ibm.wala.cast.python.parser;

import com.ibm.wala.cast.python.util.Util;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CharStream;

public class PythonModuleParser extends PythonParser<ModuleEntry> {

  private final SourceModule fileName;

  protected URL getParsedURL() throws IOException {
    return fileName.getURL();
  }

  protected WalaPythonParser makeParser() throws IOException {
    CharStream file = new ANTLRInputStream(fileName.getInputStream());
    return new WalaPythonParser(file, fileName.getName(), "UTF-8");
  }

  public PythonModuleParser(
      SourceModule fileName,
      CAstTypeDictionaryImpl<String> types,
      List<Module> allModules,
      List<File> pythonPath) {
    super(types, pythonPath);
    this.fileName = fileName;
  }

  @Override
  protected String scriptName() {
    return fileName.getName();
  }

  public static void main(String[] args) throws Exception {
    URL url = new URL(args[0]);
    List<File> pythonPath = Util.getPathFiles(args[1]);

    PythonParser<ModuleEntry> p =
        new PythonModuleParser(
            new SourceURLModule(url),
            new CAstTypeDictionaryImpl<String>(),
            Collections.singletonList(new SourceURLModule(url)),
            pythonPath);
    CAstEntity script = p.translateToCAst();
    System.err.println(script);
    System.err.println(CAstPrinter.print(script.getAST()));
  }

  @Override
  protected Reader getReader() throws IOException {
    return new InputStreamReader(fileName.getInputStream());
  }
}
