package com.devonfw.tools.ide.completion;

/**
 * Candidate for auto-completion.
 *
 * @param text the text to suggest (CLI argument value).
 */
public record CompletionCandidate(String text /* , String description */) {

}
