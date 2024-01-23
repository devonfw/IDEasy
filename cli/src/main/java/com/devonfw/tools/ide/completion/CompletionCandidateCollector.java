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
public interface CompletionCandidateCollector {

  /**
   * @param text the suggested word to add to auto-completion.
   * @param property the {@link Property} that triggered this suggestion.
   * @param commandlet the {@link Commandlet} owning the {@link Property}.
   */
  void add(String text, Property<?> property, Commandlet commandlet);

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
        add(candidate, property, commandlet);
      }
      return sortedCandidates.length;
    }
    int count = 0;
    int index = Arrays.binarySearch(sortedCandidates, text);
    if (index >= 0) {
      add(sortedCandidates[index], property, commandlet);
      index++;
      count++;
    } else {
      index = -index;
    }
    while ((index >= 0) && (index < sortedCandidates.length)) {
      if (sortedCandidates[index].startsWith(text)) {
        add(sortedCandidates[index], property, commandlet);
        count++;
      } else {
        break;
      }
      index++;
    }
    return count;
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

}
