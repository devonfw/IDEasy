package com.devonfw.tools.ide.completion;

import java.util.List;

/**
 * Candidate for auto-completion.
 *
 * @param text the text to suggest (CLI argument value).
 * @param description the description of the candidate.
 */
public record CompletionCandidate(List<String> entries,
                                  String description,
                                  boolean complete) implements Comparable<CompletionCandidate> {

  @Override
  public int compareTo(CompletionCandidate o) {

    return this.text().compareTo(o.text());
  }

  public String text() {
    return String.join(" ", this.entries);
  }
}
