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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.List;

public class PythonFileParser extends PythonParser<File> {

  private final File fileName;

  public PythonFileParser(
      File fileName, CAstTypeDictionaryImpl<String> types, List<File> pythonPath) {
    super(types, pythonPath);
    this.fileName = fileName;
  }

  protected String scriptName() {
    return fileName.getName();
  }

  protected URL getParsedURL() throws IOException {
    return fileName.toURI().toURL();
  }

  protected WalaPythonParser makeParser() throws IOException {
    org.antlr.runtime.ANTLRFileStream file =
        new org.antlr.runtime.ANTLRFileStream(fileName.getAbsolutePath());
    return new WalaPythonParser(file, fileName.getAbsolutePath(), "UTF-8");
  }

  public static void main(String[] args) throws Exception {
    List<File> pythonPath = Util.getPathFiles(args[1]);

    PythonParser<File> p =
        new PythonFileParser(new File(args[0]), new CAstTypeDictionaryImpl<String>(), pythonPath);
    CAstEntity script = p.translateToCAst();
    System.err.println(script);
    System.err.println(CAstPrinter.print(script.getAST()));
  }

  @Override
  protected Reader getReader() throws IOException {
    return new FileReader(fileName);
  }
}
