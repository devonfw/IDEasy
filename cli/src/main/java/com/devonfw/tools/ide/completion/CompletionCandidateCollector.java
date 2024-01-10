package com.devonfw.tools.ide.completion;

import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.Property;

/**
 * Collects the {@link CompletionCandidate}s for auto-completion.
 */
public class CompletionCandidateCollector {

  private final List<CompletionCandidate> candidates;

  private final IdeContext context;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CompletionCandidateCollector(IdeContext context) {

    super();
    this.candidates = new ArrayList<>();
    this.context = context;
  }

  /**
   * @param text the suggested word to add to auto-completion.
   * @param property the {@link Property} that triggered this suggestion.
   * @param commandlet the {@link Commandlet} owning the {@link Property}.
   */
  public void add(String text, Property<?> property, Commandlet commandlet) {

    this.candidates.add(new CompletionCandidate(text));
  }

  /**
   * Resets this {@link CompletionCandidateCollector} to reuse trying the next {@link Commandlet}.
   */
  public void clear() {

    this.candidates.clear();
  }

  /**
   * @return the list of {@link CompletionCandidate}s.
   */
  public List<CompletionCandidate> getCandidates() {

    return this.candidates;
  }

  @Override
  public String toString() {

    return this.candidates.toString();
  }

}
