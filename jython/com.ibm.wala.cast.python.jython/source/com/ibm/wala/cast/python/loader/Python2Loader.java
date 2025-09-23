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

import com.ibm.wala.cast.ir.translator.ConstantFoldingRewriter;
import com.ibm.wala.cast.ir.translator.RewritingTranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.python.parser.PythonModuleParser;
import com.ibm.wala.cast.python.util.Python2Interpreter;
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
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.python.core.PyObject;

public class Python2Loader extends PythonLoader {

  private static final Logger LOGGER = Logger.getLogger(Python2Loader.class.getName());

  public Python2Loader(IClassHierarchy cha, IClassLoader parent) {
    super(cha, parent, Collections.emptyList());
  }

  public Python2Loader(IClassHierarchy cha) {
    super(cha, Collections.emptyList());
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
                try {
                  PyObject x =
                      Python2Interpreter.getInterp().eval(lhs + " " + op.getValue() + " " + rhs);
                  if (x.isNumberType()) {
                    LOGGER.fine(
                        "Expression evaluation: "
                            + lhs
                            + " "
                            + op.getValue()
                            + " "
                            + rhs
                            + " -> "
                            + x.asInt());
                    return x.asInt();
                  }
                } catch (Exception e) {
                  // interpreter died for some reason, so no information.
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
