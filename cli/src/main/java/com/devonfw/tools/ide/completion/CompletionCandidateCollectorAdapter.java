package com.devonfw.tools.ide.completion;

import java.util.List;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.property.Property;

/**
 * Implementation of {@link CompletionCandidateCollector} that wraps an existing {@link CompletionCandidateCollector}
 * adding a prefix.
 */
public class CompletionCandidateCollectorAdapter implements CompletionCandidateCollector {

  private final String prefix;

  private final CompletionCandidateCollector delegate;

  /**
   * The constructor.
   *
   * @param prefix the prefix to add to the completions.
   * @param delegate the {@link CompletionCandidateCollector} to wrap.
   */
  public CompletionCandidateCollectorAdapter(String prefix, CompletionCandidateCollector delegate) {

    super();
    this.prefix = prefix;
    this.delegate = delegate;
  }

  @Override
  public void add(String text, Property<?> property, Commandlet commandlet) {

    this.delegate.add(this.prefix + text, property, commandlet);
  }

  @Override
  public List<CompletionCandidate> getCandidates() {

    return this.delegate.getCandidates();
  }
}
