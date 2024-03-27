package com.ibm.wala.cast.python.util;

import static com.google.common.collect.Iterables.concat;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.ibm.wala.cast.python.ipa.callgraph.PytestEntrypointBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class Util {

  private static final Logger LOGGER = Logger.getLogger(Util.class.getName());

  /**
   * Add Pytest entrypoints to the given {@link PropagationCallGraphBuilder}.
   *
   * @param callGraphBuilder The {@link PropagationCallGraphBuilder} for which to add Pytest
   *     entrypoints.
   */
  public static void addPytestEntrypoints(PropagationCallGraphBuilder callGraphBuilder) {
    Iterable<? extends Entrypoint> defaultEntrypoints =
        callGraphBuilder.getOptions().getEntrypoints();

    Iterable<Entrypoint> pytestEntrypoints =
        new PytestEntrypointBuilder().createEntrypoints(callGraphBuilder.getClassHierarchy());

    Iterable<Entrypoint> entrypoints = concat(defaultEntrypoints, pytestEntrypoints);

    callGraphBuilder.getOptions().setEntrypoints(entrypoints);

    for (Entrypoint ep : callGraphBuilder.getOptions().getEntrypoints())
      LOGGER.info(() -> "Using entrypoint: " + ep.getMethod().getDeclaringClass().getName() + ".");
  }

  /**
   * Get a {@link List} of {@link File}s corresponding to the sequence of paths represented by the
   * given {@link String}. The paths are seperated by a colon.
   *
   * @param pathSequence A colon-seperated list of paths.
   * @return A {@link List} of {@link File}s constructed from the given path sequence.
   */
  public static List<File> getPathFiles(String pathSequence) {
    if (pathSequence == null || pathSequence.isEmpty() || pathSequence.isBlank())
      return emptyList();
    return Arrays.asList(pathSequence.split(":")).stream().map(s -> new File(s)).collect(toList());
  }

  private Util() {}
}
