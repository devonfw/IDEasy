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
  public int addAllMatches(String text, String[] sortedCandidates, Property<?> property, Commandlet commandlet) {

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

  @Override
  public List<CompletionCandidate> getCandidates() {

    return this.candidates;
  }

  @Override
  public String toString() {

    return this.candidates.toString();
  }

}
