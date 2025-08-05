package com.ibm.wala.cast.python.driver;

import com.ibm.wala.cast.python.client.PytestAnalysisEngine;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.util.CancelException;
import java.io.IOException;

public class PytestDriver extends Driver {

  public static void main(String[] args) throws IOException, CancelException {
    PytestAnalysisEngine<Void> E =
        new PytestAnalysisEngine<Void>() {
          @Override
          public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
            return null;
          }
        };

    new PytestDriver().runit(E, args);
  }
}
