package com.ibm.wala.cast.python.ml.driver;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticRelatedInformation;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.wala.cast.lsp.WALAServer;

public class DiagnosticsFormatter {

	public static String positionToString(Position pos) {
		return "" + (pos.getLine()+1) + ":" + (pos.getCharacter()+1);
	}
	
	public static String rangeToString(Range range) {
		return positionToString(range.getStart()) + "-" + positionToString(range.getEnd());
		
	}
	
	public static String locationToString(String uri, Range range) {
		return uri + ":" + rangeToString(range);
	}

	public static String locationToString(Location loc) {
		return locationToString(loc.getUri(), loc.getRange());
	}
	
	public static void displayTextRange(String pre, PrintStream out, String uri, String[] lines, Range range) {
		final Position start = range.getStart();
		final Position end = range.getEnd();
		if(start.getLine() > end.getLine()) {
			throw new IllegalArgumentException("Invalid range: end line " + end.getLine() + " before start line " + start.getLine() + " of " + uri);
		}
		if(end.getLine() >= lines.length) {
			throw new IllegalArgumentException("Invalid range: end line " + end.getLine() + " after the last line " + lines.length + " of " + uri);			
		}
		for(int i = start.getLine(); i <= end.getLine(); i++) {
			out.print(pre);
			String prefix = uri + ":" + (i+1) + ":    ";
			out.print(prefix);
			String line = lines[i];
			out.println(line);
			int skipStart = 0;
			if(i == start.getLine()) {
				skipStart = start.getCharacter();
			}
			int numArrows = line.length();
			if(i == end.getLine()) {
				numArrows = end.getCharacter();
			}
			numArrows = numArrows - skipStart;
			
			out.print(pre);
			out.print(Stream.generate(()->" ").limit(prefix.length() + skipStart).collect(Collectors.joining()));
			if(numArrows >= 0) {
				out.println(Stream.generate(()->"^").limit(numArrows).collect(Collectors.joining()));
			}
		}
	}
	
	public static void displayDiagnostic(String pre, PrintStream out, String uri, Diagnostic diagnostic, Map<String,String[]> lines, int relatedCount) {
		out.print(pre);
		out.print(locationToString(uri, diagnostic.getRange()));
		out.print(":    [");
		out.print(diagnostic.getSeverity().toString());
		out.print("] ");
		out.println(diagnostic.getMessage());
		if(lines.containsKey(uri)) {
			displayTextRange(pre, out, uri, lines.get(uri), diagnostic.getRange());

		}
		
		List<DiagnosticRelatedInformation> relatedInfos = diagnostic.getRelatedInformation();
		String relatedPre = "    " + pre;
		if(relatedCount != 0) {
			for(DiagnosticRelatedInformation related : relatedInfos) {
				if(relatedCount == 0) {
					break;
				}
				if(relatedCount > 0) {
					relatedCount--;
				}
				final Location loc = related.getLocation();
				if(loc == null) {
					 continue;
				}
				
				out.print(relatedPre);
				out.print(locationToString(loc));
				out.print(":    [related] ");
				out.println(related.getMessage());
				final String relatedUri = loc.getUri();
				if(lines.containsKey(relatedUri)) {
					displayTextRange(relatedPre, out, uri, lines.get(relatedUri), loc.getRange());
				}
			}
		}
	}
	
	public static JsonObject positionToJson(Position pos) {
		JsonObject opos = new JsonObject();
		opos.addProperty("line", pos.getLine());
		opos.addProperty("character", pos.getCharacter());
		return opos;
	}
	
	public static JsonObject rangeToJson(Range range) {
		JsonObject orange = new JsonObject();
		orange.add("start", positionToJson(range.getStart()));
		orange.add("start", positionToJson(range.getEnd()));
		return orange;
	}
	
	public static JsonObject locationToJson(Location loc) {
		JsonObject oloc = new JsonObject();
		oloc.addProperty("uri", loc.getUri());
		oloc.add("range", rangeToJson(loc.getRange()));
		return oloc;
	}

	public static JsonObject diagnosticToJson(Diagnostic diagnostic, int relatedCount) {
		JsonObject odiag = new JsonObject();
		odiag.addProperty("severity", diagnostic.getSeverity().toString());
		odiag.addProperty("source", diagnostic.getSource());
		odiag.addProperty("message", diagnostic.getMessage());
		odiag.add("range", rangeToJson(diagnostic.getRange()));
		List<DiagnosticRelatedInformation> relatedInformation = diagnostic.getRelatedInformation();
		JsonArray orelated = new JsonArray();
		if(relatedCount != 0) {
			for(DiagnosticRelatedInformation info : relatedInformation) {
				if(relatedCount == 0) {
					break;
				}
				if(relatedCount > 0) {
					relatedCount--;
				}
				JsonObject oinfo = new JsonObject();
				oinfo.addProperty("message", info.getMessage());
				oinfo.add("location", locationToJson(info.getLocation()));
				orelated.add(oinfo);
			}
		}
		odiag.add("relatedInformation", orelated);
		return odiag;
	}
		
	static enum FORMAT {
		json{
			public JsonObject toJson(Map<String, List<Diagnostic>> diagnostics, int relatedCount) {
				JsonObject odiagMap = new JsonObject();
				for(Entry<String, List<Diagnostic>> entry : diagnostics.entrySet()) {
					JsonArray odiags = new JsonArray();
					for(Diagnostic diag : entry.getValue()) {
						odiags.add(diagnosticToJson(diag, relatedCount));
					}
					odiagMap.add(entry.getKey(), odiags);
				}
				return odiagMap;
			}
			
			
			@Override
			public void print(PrintStream out, Map<String, String> texts, Map<String, List<Diagnostic>> diagnostics, int related) {
				JsonObject json = toJson(diagnostics, related);
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				String jsonString = gson.toJson(json);
				out.println(jsonString);
			}
		},
		pretty{
			@Override
			public void print(PrintStream out, Map<String, String> texts, Map<String, List<Diagnostic>> diagnostics, int related) {
				final Map<String, String[]> lines = new HashMap<String, String[]>(texts.size());
				for(Map.Entry<String, String> kv : texts.entrySet()) {
					lines.put(kv.getKey(), 
	            	new BufferedReader(new StringReader(kv.getValue()))
	            		.lines()
	            		.toArray(String[]::new));
				}
				
				final String pre = "";
				for(Map.Entry<String, List<Diagnostic>> kv : diagnostics.entrySet()) {
					String uri = kv.getKey();
					if(kv.getValue() != null) {
						for(Diagnostic diagnostic : kv.getValue()) {
							displayDiagnostic(pre, out, uri, diagnostic, lines, related);
						}
					}
					
				}				
			}

		};
	
		public abstract void print(PrintStream out, Map<String, String> texts, Map<String, List<Diagnostic>> diagnostics, int related);
	};
	
	public static Map<String, List<Diagnostic>> filterSeverity(Map<String, List<Diagnostic>> diagnostics,
			Set<DiagnosticSeverity> severityList) {
		
		Map<String, List<Diagnostic>> ret = new HashMap<String, List<Diagnostic>>(diagnostics.size());
		for(Entry<String, List<Diagnostic>> entries : diagnostics.entrySet()) {
			List<Diagnostic> vals = entries.getValue().stream()
					.filter(e -> severityList.contains(e.getSeverity()))
					.collect(Collectors.toList());
			if(vals != null && ! vals.isEmpty()) {
				ret.put(entries.getKey(), vals);
			}
		}
		return ret;
	}
}
