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

import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.classLoader.FileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.util.collections.HashSetFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CharStream;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.alias;

public class PythonModuleParser extends PythonParser<ModuleEntry> {

  private static final Logger LOGGER = Logger.getLogger(PythonModuleParser.class.getName());

  private final Set<String> localModules = HashSetFactory.make();

  private final SourceModule fileName;

  protected URL getParsedURL() throws IOException {
    return fileName.getURL();
  }

  protected WalaPythonParser makeParser() throws IOException {
    CharStream file = new ANTLRInputStream(fileName.getInputStream());
    return new WalaPythonParser(file, fileName.getName(), "UTF-8");
  }

  @Override
  protected PythonParser<ModuleEntry>.CAstVisitor makeVisitor(
      WalkContext context, WalaPythonParser parser) {
    return new CAstVisitor(context, parser) {

      @Override
      public CAstNode visitImportFrom(ImportFrom importFrom) throws Exception {
        Optional<String> s =
            importFrom.getInternalModuleNames().stream()
                .map(Name::getInternalId)
                .reduce(
                    (a, b) -> {
                      return a + "/" + b;
                    });
        if (s.isPresent()) {
          String moduleName = s.get();
          LOGGER.finer("Module name from " + importFrom + " is: " + moduleName + ".");

          boolean localModule = isLocalModule(moduleName);

          if (!localModule) {
            LOGGER.finer("Module: " + moduleName + " isn't local.");
            moduleName = s.get() + "/__init__";
          } else LOGGER.finer("Module: " + moduleName + " is local.");

          LOGGER.finer("Module name from " + importFrom + " is: " + moduleName + ".");

          if (isLocalModule(moduleName)) {
            LOGGER.finer("Module: " + moduleName + ".py" + " is local.");

            String yuck = moduleName;
            return Ast.makeNode(
                CAstNode.BLOCK_STMT,
                importFrom.getInternalNames().stream()
                    .map(alias::getInternalName)
                    .map(
                        n ->
                            Ast.makeNode(
                                CAstNode.DECL_STMT,
                                Ast.makeConstant(
                                    new CAstSymbolImpl(n, PythonCAstToIRTranslator.Any)),
                                Ast.makeNode(
                                    CAstNode.PRIMITIVE,
                                    Ast.makeConstant("import"),
                                    Ast.makeConstant(yuck),
                                    Ast.makeConstant(n))))
                    .collect(Collectors.toList()));
          } else LOGGER.finer("Module: " + moduleName + ".py" + " isn't local.");
        }

        return super.visitImportFrom(importFrom);
      }
    };
  }

  public PythonModuleParser(
      SourceModule fileName, CAstTypeDictionaryImpl<String> types, List<Module> allModules) {
    super(types);
    this.fileName = fileName;
    allModules.forEach(
        m -> {
          m.getEntries()
              .forEachRemaining(
                  new Consumer<ModuleEntry>() {
                    @Override
                    public void accept(ModuleEntry f) {
                      if (f.isModuleFile()) {
                        f.asModule()
                            .getEntries()
                            .forEachRemaining(
                                sm -> {
                                  accept(sm);
                                });
                      } else {
                        LOGGER.fine(() -> "**CLS: " + scriptName((SourceModule) f));
                        localModules.add(scriptName((SourceModule) f));
                      }
                    }
                  });
        });
  }

  @Override
  protected String scriptName() {
    return scriptName(fileName);
  }

  private static String scriptName(SourceModule fileName) {
    if (fileName instanceof FileModule) {
      return fileName.getClassName();
    } else {
      return fileName.getName();
    }
  }

  public static void main(String[] args) throws Exception {
    URL url = new URL(args[0]);
    PythonParser<ModuleEntry> p =
        new PythonModuleParser(
            new SourceURLModule(url),
            new CAstTypeDictionaryImpl<String>(),
            Collections.singletonList(new SourceURLModule(url)));
    CAstEntity script = p.translateToCAst();
    System.err.println(script);
    System.err.println(CAstPrinter.print(script));
  }

  @Override
  protected Reader getReader() throws IOException {
    return new InputStreamReader(fileName.getInputStream());
  }

  private boolean isLocalModule(String moduleName) {
    return localModules.stream().anyMatch(lm -> lm.endsWith(moduleName + ".py"));
  }
}
