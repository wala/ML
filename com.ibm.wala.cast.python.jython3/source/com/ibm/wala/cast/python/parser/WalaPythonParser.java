package com.ibm.wala.cast.python.parser;

import com.ibm.wala.util.collections.HashSetFactory;
import java.util.Set;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.TokenRewriteStream;
import org.python.antlr.AnalyzingParser;
import org.python.antlr.PythonLexer;
import org.python.antlr.PythonParser;
import org.python.antlr.PythonTokenSource;
import org.python.antlr.WalaErrorHandler;

public class WalaPythonParser extends AnalyzingParser {
  private String text = null;
  private final Set<Exception> errors = HashSetFactory.make();

  public WalaPythonParser(CharStream stream, String filename, String encoding) {
    super(stream, filename, encoding);
  }

  private TokenRewriteStream tokens;

  public void recordError(Exception e) {
    errors.add(e);
  }

  public Set<Exception> getErrors() {
    return errors;
  }

  public String getText(int start, int end) {
    if (text == null) {
      text = tokens.toOriginalString();
    }
    int e = Math.min(end, text.length() - 1);
    if (start >= e) {
      return "";
    } else {
      return text.substring(start, e);
    }
  }

  protected PythonParser setupParser(boolean single) {
    WalaErrorHandler weh = new WalaErrorHandler(errorHandler, this);
    PythonLexer lexer = new PythonLexer(charStream);
    lexer.setErrorHandler(weh);
    lexer.single = single;
    CommonTokenStream tokens = this.tokens = new TokenRewriteStream(lexer);
    PythonTokenSource indentedSource = new PythonTokenSource(tokens, filename, single);
    tokens = new CommonTokenStream(indentedSource);
    PythonParser parser = new PythonParser(tokens, encoding);
    parser.setErrorHandler(weh);
    parser.setTreeAdaptor(new AnalyzerTreeAdaptor());
    return parser;
  }
}
