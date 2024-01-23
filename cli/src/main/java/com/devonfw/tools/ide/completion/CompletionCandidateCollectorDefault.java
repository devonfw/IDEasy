package com.devonfw.tools.ide.completion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.Property;

/**
 * Collects the {@link CompletionCandidate}s for auto-completion.
 */
public class CompletionCandidateCollectorDefault implements CompletionCandidateCollector {

  private final List<CompletionCandidate> candidates;

  private final IdeContext context;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CompletionCandidateCollectorDefault(IdeContext context) {

    super();
    this.candidates = new ArrayList<>();
    this.context = context;
  }

  @Override
  public void add(String text, Property<?> property, Commandlet commandlet) {

    CompletionCandidate candidate = new CompletionCandidate(text);
    this.candidates.add(candidate);
    this.context.trace("Added {} for auto-completion of property {}.{}", candidate, commandlet, property);
  }

  @Override
  public List<CompletionCandidate> getCandidates() {

    return this.candidates;
  }

  @Override
  public String toString() {

    return this.candidates.toString();
  }

}
