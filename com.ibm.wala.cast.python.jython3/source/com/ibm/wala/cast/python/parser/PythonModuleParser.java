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

import static com.ibm.wala.cast.python.util.Util.MODULE_INITIALIZATION_ENTITY_NAME;
import static com.ibm.wala.cast.python.util.Util.PYTHON_FILE_EXTENSION;

import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.python.util.Util;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CharStream;
import org.python.antlr.ast.Import;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.alias;

public class PythonModuleParser extends PythonParser<ModuleEntry> {

  private static final Logger LOGGER = Logger.getLogger(PythonModuleParser.class.getName());

  private final Set<SourceModule> localModules = HashSetFactory.make();

  private final SourceModule fileName;

  protected URL getParsedURL() {
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
      public CAstNode visitImport(Import imp) throws Exception {
        Optional<String> s =
            imp.getInternalNames().stream()
                .map(alias::getInternalName)
                .reduce((a, b) -> a + "/" + b);

        if (s.isPresent()) {
          String moduleName = s.get().replace('.', '/');
          LOGGER.finer("Module name from " + imp + " is: " + moduleName);

          // if it's a package.
          if (moduleName.indexOf('/') != -1) {
            if (!isLocalModule(moduleName)) moduleName += "/" + MODULE_INITIALIZATION_ENTITY_NAME;

            LOGGER.finer("Module name from " + imp + " is: " + moduleName);

            if (isLocalModule(moduleName))
              return createImportNode(imp.getInternalNames(), moduleName, true);
          }
        }

        return super.visitImport(imp);
      }

      /**
       * Returns an import {@link CAstNode} with the given {@link List} of {@link alias}s as import
       * names within the given module.
       *
       * @param importNames The names to import.
       * @param moduleName The name of the containing module.
       * @return An import {@link CAstNode} with the given {@link List} of {@link alias}s as import
       *     names within the given module.
       */
      private CAstNode createImportNode(List<alias> importNames, String moduleName) {
        return createImportNode(importNames, moduleName, false);
      }

      /**
       * Returns an import {@link CAstNode} with the given {@link List} of {@link alias}s as import
       * names within the given module.
       *
       * @param importNames The names to import.
       * @param moduleName The name of the containing module.
       * @param useInitializationFile Whether to use the `__init__.py` file.
       * @return An import {@link CAstNode} with the given {@link List} of {@link alias}s as import
       *     names within the given module.
       */
      private CAstNode createImportNode(
          List<alias> importNames, String moduleName, boolean useInitializationFile) {
        moduleName = adjustModuleName(moduleName, useInitializationFile);

        String yuck = moduleName;
        return Ast.makeNode(
            CAstNode.BLOCK_STMT,
            importNames.stream()
                .map(alias::getInternalName)
                .map(
                    n -> {
                      n = n.split("\\.")[0];

                      return Ast.makeNode(
                          CAstNode.DECL_STMT,
                          Ast.makeConstant(new CAstSymbolImpl(n, PythonCAstToIRTranslator.Any)),
                          Ast.makeNode(
                              CAstNode.PRIMITIVE,
                              Ast.makeConstant("import"),
                              Ast.makeConstant(yuck),
                              Ast.makeConstant(n)));
                    })
                .collect(Collectors.toList()));
      }

      @Override
      public CAstNode visitImportFrom(ImportFrom importFrom) throws Exception {
        Optional<String> s =
            importFrom.getInternalModuleNames().stream()
                .map(Name::getInternalId)
                .reduce((a, b) -> a + "/" + b);

        if (s.isPresent()) {
          String moduleName = s.get();
          LOGGER.finer("Module name from " + importFrom + " is: " + moduleName);

          if (moduleName.startsWith(".")) {
            LOGGER.info("Found relative import: " + moduleName);
            moduleName = this.resolveRelativeImport(moduleName);
            LOGGER.fine("Resolved relative import: " + moduleName);
          }

          if (!isLocalModule(moduleName)) moduleName += "/" + MODULE_INITIALIZATION_ENTITY_NAME;

          LOGGER.finer("Module name from " + importFrom + " is: " + moduleName);

          if (isLocalModule(moduleName))
            return createImportNode(importFrom.getInternalNames(), moduleName);
        }

        return super.visitImportFrom(importFrom);
      }

      /**
       * Adjust the given module name relative to the PYTHONPATH.
       *
       * @param moduleName The module name to adjust.
       * @param useInitializationFile Whether to use the `__init__.py` file.
       * @return The given module's name potentially adjusted per the PYTHONPATH.
       */
      private String adjustModuleName(String moduleName, boolean useInitializationFile) {
        List<File> pythonPath = PythonModuleParser.this.getPythonPath();
        LOGGER.info("PYTHONPATH is: " + pythonPath);

        // If there is a PYTHONPATH specified.
        if (pythonPath != null && !pythonPath.isEmpty()) {
          // Adjust the module name per the PYTHONPATH.
          Optional<SourceModule> localModule = getLocalModule(moduleName);

          for (File pathEntry : pythonPath) {
            Path modulePath = getPath(localModule);
            LOGGER.finer("Found path: " + modulePath);

            if (modulePath.startsWith(pathEntry.toPath())) {
              // Found it.
              Path scriptRelativePath = pathEntry.toPath().relativize(modulePath);
              LOGGER.finer("Relativized path is: " + scriptRelativePath);

              // Remove the file extension if it exists.
              moduleName = scriptRelativePath.toString().replaceFirst("\\.py$", "");

              if (useInitializationFile)
                // Use the beginning segment initialization file.
                moduleName = moduleName.split("/")[0] + "/" + MODULE_INITIALIZATION_ENTITY_NAME;

              LOGGER.fine("Using module name: " + moduleName);
              break;
            }
          }
        }

        return moduleName;
      }

      /**
       * Given a relative import, e.g., ".", "..", ".P", "..P", where "P" represents a package,
       * subpackage, or module, returns the corresponding actual package, subpackage, or module
       * name.
       *
       * @param importName The relative package, subpackage, or module to resolve.
       * @return The actual corresponding package, subpackage, or module name.
       */
      private String resolveRelativeImport(String importName) {
        assert importName.startsWith(".") : "Relative import must start with a period.";

        // Replace path separators for dots except for the last one. We'll use this to resolve the
        // import.
        importName = importName.replaceAll("\\./\\B", ".");

        URL url = PythonModuleParser.this.getParsedURL();
        String file = url.getFile();
        Path path = Path.of(file);
        Path resolvedSibling = path.resolveSibling(importName);
        Path normalizedPath = resolvedSibling.normalize();

        // Replace the dots with the actual (sub)packages.
        int numBeginningDots = getNumberOfBeginningDots(importName);

        Path subpath =
            normalizedPath.subpath(
                normalizedPath.getNameCount() - numBeginningDots, normalizedPath.getNameCount());

        return subpath.toString();
      }

      private static int getNumberOfBeginningDots(String string) {
        int ret = 0;

        for (int i = 0; i < string.length(); i++) {
          char character = string.charAt(i);

          if (character == '.') ++ret;
          else break;
        }

        return ret;
      }
    };
  }

  public PythonModuleParser(
      SourceModule fileName,
      CAstTypeDictionaryImpl<String> types,
      List<Module> allModules,
      List<File> pythonPath) {
    super(types, pythonPath);
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
                        SourceModule sourceModule = (SourceModule) f;
                        String scriptName = scriptName(sourceModule);
                        LOGGER.fine(() -> "**CLS: " + scriptName);
                        localModules.add(sourceModule);
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
    URL url = new URI(args[0]).toURL();
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

  /**
   * Returns the {@link Path} corresponding to the given {@link SourceModule}. If a {@link
   * SourceModule} is not supplied, an {@link IllegalStateException} is thrown.
   *
   * @param module The {@link SourceModule} for which to extract a {@link Path}.
   * @return The {@link Path} corresponding to the given {@link SourceModule}.
   * @throws IllegalStateException If the given {@link SourceModule} is not present.
   */
  private static Path getPath(Optional<SourceModule> module) {
    return module
        .map(SourceModule::getURL)
        .map(URL::getFile)
        .map(Path::of)
        .orElseThrow(IllegalStateException::new);
  }

  /**
   * Get the {@link Path} of the parsed {@link SourceModule}.
   *
   * @see getPath(Optional<SourceModule>)
   * @return The {@link Path} corresponding to the parsed {@link SourceModule}.
   */
  @SuppressWarnings("unused")
  private Path getPath() {
    return getPath(Optional.of(this.fileName));
  }

  private boolean isLocalModule(String moduleName) {
    boolean ret = this.getLocalModule(moduleName).isPresent();
    LOGGER.finer("Module: " + moduleName + (ret ? " is" : " isn't") + " local.");
    return ret;
  }

  /**
   * Gets the local Python {@link SourceModule} represented by the given {@link String}.
   *
   * @param moduleName The name of the local Python module as a {@link String}.
   * @return The corresponding {@link SourceModule}.
   */
  private Optional<SourceModule> getLocalModule(String moduleName) {
    // A map of paths to known local modules.
    Map<String, SourceModule> pathToLocalModule = new HashMap<>();

    for (SourceModule module : this.localModules) {
      String scriptName = scriptName(module);
      pathToLocalModule.put(scriptName, module);
    }

    // first, check the current directory, i.e., the directory where the import statement is
    // executed. If the module is found here, the search stops.
    String scriptName = scriptName();
    String scriptDirectory = scriptName.substring(0, scriptName.lastIndexOf('/') + 1);
    String moduleFileName = moduleName + "." + PYTHON_FILE_EXTENSION;
    String modulePath = scriptDirectory + moduleFileName;
    SourceModule module = pathToLocalModule.get(modulePath);

    if (module != null) return Optional.of(module);

    // otherwise, go through the local modules. NOTE: Should instead traverse PYTHONPATH here per
    // https://g.co/gemini/share/310ca39fbd43. However, the problem is that the local modules may
    // not be on disk. As such, this is our best approximation.
    return pathToLocalModule.keySet().stream()
        .filter(p -> p.endsWith(moduleFileName))
        .map(p -> pathToLocalModule.get(p))
        .findFirst();
  }
}
