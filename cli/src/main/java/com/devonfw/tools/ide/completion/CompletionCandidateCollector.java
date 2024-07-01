package com.devonfw.tools.ide.completion;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.property.Property;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collects the {@link CompletionCandidate}s for auto-completion.
 */
public interface CompletionCandidateCollector {

  /**
   * @param text the suggested word to add to auto-completion.
   * @param description the description of the suggestion candidate or {@code null} to determine automatically form the given parameters.
   * @param property the {@link Property} that triggered this suggestion.
   * @param commandlet the {@link Commandlet} owning the {@link Property}.
   */
  void add(String text, String description, Property<?> property, Commandlet commandlet);

  /**
   * @param text the suggested word to add to auto-completion.
   * @param description the description of the suggestion candidate or {@code null} to determine automatically form the given parameters.
   * @param property the {@link Property} that triggered this suggestion.
   * @param commandlet the {@link Commandlet} owning the {@link Property}.
   * @return the {@link CompletionCandidate} for the given parameters.
   */
  default CompletionCandidate createCandidate(String text, String description, Property<?> property, Commandlet commandlet) {

    if (description == null) {
      // compute description from property + commandlet like in HelpCommandlet?
    }
    CompletionCandidate candidate = new CompletionCandidate(text, description);
    return candidate;
  }

  /**
   * @param text the suggested word to add to auto-completion.
   * @param sortedCandidates the array of candidates sorted in ascending order.
   * @param property the {@link Property} that triggered this suggestion.
   * @param commandlet the {@link Commandlet} owning the {@link Property}.
   * @return the number of {@link CompletionCandidate}s that have been added.
   */
  default int addAllMatches(String text, String[] sortedCandidates, Property<?> property, Commandlet commandlet) {

    if (text.isEmpty()) {
      for (String candidate : sortedCandidates) {
        add(candidate, "", property, commandlet);
      }
      return sortedCandidates.length;
    }

    List<String> prefixWords = Arrays.asList(sortedCandidates).stream().filter(word -> word.startsWith(text))
        .collect(Collectors.toList());

    for (String match : prefixWords) {
      add(match, "", property, commandlet);
    }

    return prefixWords.size();
  }

  /**
   * Resets this {@link CompletionCandidateCollector} to reuse trying the next {@link Commandlet}.
   */
  default void clear() {

    getCandidates().clear();
  }

  /**
   * @return the list of {@link CompletionCandidate}s.
   */
  List<CompletionCandidate> getCandidates();

  /**
   * Disables the {@link #getSortedCandidates() sorting}.
   */
  void disableSorting();

  /**
   * @return the sorted {@link #getCandidates() candidates}.
   */
  List<CompletionCandidate> getSortedCandidates();

}
