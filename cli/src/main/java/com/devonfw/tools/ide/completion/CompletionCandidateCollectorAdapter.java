package com.devonfw.tools.ide.completion;

import java.util.List;

/**
 * Implementation of {@link CompletionCandidateCollector} that wraps an existing {@link CompletionCandidateCollector} adding a prefix.
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
  public void add(CompletionCandidate completion) {
    this.delegate.add(this.prefix + completion.text(), completion.description());
  }

  @Override
  public void add(String text, String description) {
    this.delegate.add(this.prefix + text, description);
  }

  @Override
  public List<CompletionCandidate> getCandidates() {

    return this.delegate.getCandidates();
  }

  @Override
  public List<CompletionCandidate> getSortedCandidates() {

    return this.delegate.getSortedCandidates();
  }

  @Override
  public void disableSorting() {

    this.delegate.disableSorting();
  }
}
