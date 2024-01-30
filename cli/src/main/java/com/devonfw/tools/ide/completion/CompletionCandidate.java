package com.devonfw.tools.ide.completion;

import com.devonfw.tools.ide.version.VersionSegment;

/**
 * Candidate for auto-completion.
 *
 * @param text the text to suggest (CLI argument value).
 * @param description the description of the candidate.
 */
public record CompletionCandidate(String text, String description) implements Comparable<CompletionCandidate> {

  @Override
  public int compareTo(CompletionCandidate o) {

    return text.compareTo(o.text);
  }
}
