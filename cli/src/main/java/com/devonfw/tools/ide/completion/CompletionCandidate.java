package com.devonfw.tools.ide.completion;

/**
 * Candidate for auto-completion.
 *
 * @param text the text to suggest (CLI argument value).
 * @param description the description of the candidate.
 */
public record CompletionCandidate(String text, String description) implements Comparable<CompletionCandidate> {

  @Override
  public int compareTo(CompletionCandidate o) {

    return this.text.compareTo(o.text);
  }
}
