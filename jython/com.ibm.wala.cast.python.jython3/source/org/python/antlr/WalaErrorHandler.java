package org.python.antlr;

import com.ibm.wala.cast.python.parser.WalaPythonParser;
import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.BitSet;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.RecognitionException;
import org.python.antlr.base.expr;
import org.python.antlr.base.mod;
import org.python.antlr.base.slice;
import org.python.antlr.base.stmt;

public class WalaErrorHandler implements ErrorHandler {
  private final ErrorHandler base;
  private final WalaPythonParser parser;

  public WalaErrorHandler(ErrorHandler base, WalaPythonParser parser) {
    this.base = base;
    this.parser = parser;
  }

  @Override
  public void reportError(BaseRecognizer br, RecognitionException re) {
    parser.recordError(re);
    base.reportError(br, re);
  }

  @Override
  public void recover(BaseRecognizer br, IntStream input, RecognitionException re) {
    base.recover(br, input, re);
  }

  @Override
  public void recover(Lexer lex, RecognitionException re) {
    base.recover(lex, re);
  }

  @Override
  public boolean mismatch(BaseRecognizer br, IntStream input, int ttype, BitSet follow)
      throws RecognitionException {
    return base.mismatch(br, input, ttype, follow);
  }

  @Override
  public Object recoverFromMismatchedToken(
      BaseRecognizer br, IntStream input, int ttype, BitSet follow) throws RecognitionException {
    return base.recoverFromMismatchedToken(br, input, ttype, follow);
  }

  @Override
  public expr errorExpr(PythonTree t) {
    return base.errorExpr(t);
  }

  @Override
  public mod errorMod(PythonTree t) {
    return base.errorMod(t);
  }

  @Override
  public slice errorSlice(PythonTree t) {
    return base.errorSlice(t);
  }

  @Override
  public stmt errorStmt(PythonTree t) {
    return base.errorStmt(t);
  }

  @Override
  public void error(String message, PythonTree t) {
    base.error(message, t);
  }
}
