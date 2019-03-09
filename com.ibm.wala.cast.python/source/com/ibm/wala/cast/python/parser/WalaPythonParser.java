package com.ibm.wala.cast.python.parser;

import org.python.antlr.AnalyzingParser;
import org.python.antlr.PythonLexer;
import org.python.antlr.PythonParser;
import org.python.antlr.PythonTokenSource;
import org.python.antlr.runtime.CharStream;
import org.python.antlr.runtime.CommonTokenStream;
import org.python.antlr.runtime.TokenRewriteStream;

public class WalaPythonParser extends AnalyzingParser {
	private String text = null;
	
	public WalaPythonParser(CharStream stream, String filename, String encoding) {
		super(stream, filename, encoding);
	}

	private TokenRewriteStream tokens;
	
	public String getText(int start, int end) {
		if (text == null) {
			text = tokens.toOriginalString();
		}
		int e = Math.min(end, text.length()-1);
		if (start >= e) {
			return "";
		} else {
			return text.substring(start, e);
		}
	}

    protected PythonParser setupParser(boolean single) {
        PythonLexer lexer = new PythonLexer(charStream);
        lexer.setErrorHandler(errorHandler);
        lexer.single = single;
        CommonTokenStream tokens = this.tokens = new TokenRewriteStream(lexer);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens, filename, single);
        tokens = new CommonTokenStream(indentedSource);
        PythonParser parser = new PythonParser(tokens, encoding);
        parser.setErrorHandler(errorHandler);
        parser.setTreeAdaptor(new AnalyzerTreeAdaptor());
        return parser;
    }
}
