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
package com.ibm.wala.cast.python.loader;

import static java.util.logging.Level.WARNING;

import com.ibm.wala.cast.ir.translator.ConstantFoldingRewriter;
import com.ibm.wala.cast.ir.translator.RewritingTranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.python.parser.PythonModuleParser;
import com.ibm.wala.cast.python.util.Python3Interpreter;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.rewrite.AstConstantFolder;
import com.ibm.wala.cast.tree.rewrite.CAstBasicRewriter.NoKey;
import com.ibm.wala.cast.tree.rewrite.CAstBasicRewriter.NonCopyingContext;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.cast.tree.rewrite.PatternBasedRewriter;
import com.ibm.wala.cast.util.CAstPattern.Segments;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.python.core.PyObject;
import org.python.core.PySyntaxError;
import org.python.core.PyUnicode;

public class Python3Loader extends PythonLoader {

  private static final Logger logger = Logger.getLogger(Python3Loader.class.getName());

  public Python3Loader(IClassHierarchy cha, IClassLoader parent, List<File> pythonPath) {
    super(cha, parent, pythonPath);
  }

  public Python3Loader(IClassHierarchy cha, List<File> pythonPath) {
    super(cha, pythonPath);
  }

  @Override
  protected TranslatorToCAst getTranslatorToCAst(CAst ast, ModuleEntry M, List<Module> allModules)
      throws IOException {
    RewritingTranslatorToCAst x =
        new RewritingTranslatorToCAst(
            M,
            new PythonModuleParser(
                (SourceModule) M, typeDictionary, allModules, this.getPythonPath()) {
              @Override
              public CAstEntity translateToCAst() throws Error, IOException {
                CAstEntity ce = super.translateToCAst();
                return new AstConstantFolder().fold(ce);
              }
            });

    x.addRewriter(
        new CAstRewriterFactory<NonCopyingContext, NoKey>() {
          @Override
          public PatternBasedRewriter createCAstRewriter(CAst ast) {
            return new PatternBasedRewriter(
                ast,
                sliceAssign,
                (Segments s) -> {
                  return rewriteSubscriptAssign(s);
                });
          }
        },
        false);

    x.addRewriter(
        new CAstRewriterFactory<NonCopyingContext, NoKey>() {
          @Override
          public PatternBasedRewriter createCAstRewriter(CAst ast) {
            return new PatternBasedRewriter(
                ast,
                sliceAssignOp,
                (Segments s) -> {
                  return rewriteSubscriptAssignOp(s);
                });
          }
        },
        false);

    x.addRewriter(
        new CAstRewriterFactory<NonCopyingContext, NoKey>() {
          @Override
          public ConstantFoldingRewriter createCAstRewriter(CAst ast) {
            return new ConstantFoldingRewriter(ast) {
              @Override
              protected Object eval(CAstOperator op, Object lhs, Object rhs) {
                String s = lhs + " " + op.getValue() + " " + rhs;
                logger.info(() -> "Evaluating: " + s);

                // Use the Python interpreter to evaluate the expression.
                PyUnicode unicode = new PyUnicode(s);
                PyObject x;

                try {
                  x = Python3Interpreter.getInterp().eval(unicode);
                } catch (PySyntaxError e) {
                  // Handle syntax errors gracefully.
                  logger.log(WARNING, e, () -> "Syntax error in expression: " + unicode);
                  return null;
                }

                if (x.isNumberType()) {
                  // If the result is a number, return its integer value.
                  logger.info(() -> s + " -> " + x.asInt());
                  return x.asInt();
                }
                return null;
              }
            };
          }
        },
        false);
    return x;
  }
}
