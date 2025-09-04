package com.ibm.wala.cast.python.ml.cloud;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.wala.cast.python.ml.driver.DiagnosticsFormatter;
import com.ibm.wala.cast.python.ml.driver.PythonDriver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.lsp4j.Diagnostic;

public class CloudFunction {

  public static JsonObject main(JsonObject args) {
    String code = "";
    if (args.has("code")) code = args.getAsJsonPrimitive("code").getAsString();
    JsonObject response = new JsonObject();
    response.addProperty("diagnostic", analyze(code));
    return response;
  }

  public static String analyze(String code) {
    if (code == "" || code.length() == 0) {
      return "[]";
    }
    Map<String, String> uriTextPairs = new HashMap<String, String>();
    uriTextPairs.put("fakecode.py", code);
    Map<String, List<Diagnostic>> diagnostics = PythonDriver.getDiagnostics(uriTextPairs);
    if (diagnostics == null) {
      System.err.println("There was an error generating diagnostics");
      return "Error";
    }
    // return diagnostics.toString();
    JsonObject odiagMap = new JsonObject();
    for (Entry<String, List<Diagnostic>> entry : diagnostics.entrySet()) {
      JsonArray odiags = new JsonArray();
      for (Diagnostic diag : entry.getValue()) {
        odiags.add(DiagnosticsFormatter.diagnosticToJson(diag, 0));
      }
      odiagMap.add(entry.getKey(), odiags);
    }
    // return odiagMap.getAsJsonArray("fakecode.py").toString();
    return odiagMap.toString();
  }
}
