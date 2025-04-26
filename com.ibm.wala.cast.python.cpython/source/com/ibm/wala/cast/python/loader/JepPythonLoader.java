package com.ibm.wala.cast.python.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.python.jep.ast.CPythonAstToCAstTranslator;
import com.ibm.wala.cast.python.loader.PythonLoader;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.CopyKey;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.RewriteContext;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class JepPythonLoader extends PythonLoader {
	
	public JepPythonLoader(IClassHierarchy cha, IClassLoader parent) {
		super(cha, parent);
		// TODO Auto-generated constructor stub
	}

	public JepPythonLoader(IClassHierarchy cha, IClassLoader parent, List<File> pythonPath) {
		super(cha, parent, pythonPath);
		// TODO Auto-generated constructor stub
	}

	public JepPythonLoader(IClassHierarchy cha) {
		super(cha);
		// TODO Auto-generated constructor stub
	}

	public JepPythonLoader(IClassHierarchy cha, List<File> pythonPath) {
		super(cha, pythonPath);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected TranslatorToCAst getTranslatorToCAst(CAst ast, ModuleEntry M, List<Module> modules) throws IOException {
		return new TranslatorToCAst() {

			@Override
			public <C extends RewriteContext<K>, K extends CopyKey<K>> void addRewriter(
					CAstRewriterFactory<C, K> factory, boolean prepend) {
				assert false;
			}

			@Override
			public CAstEntity translateToCAst() throws Error, IOException {
				try (BufferedReader src = new BufferedReader(((SourceModule)M).getInputReader())) {
					return new CPythonAstToCAstTranslator.PythonScriptEntity(M.getName(), src.lines().collect(Collectors.joining("\n")));
				}
			}
		};
	}

}
