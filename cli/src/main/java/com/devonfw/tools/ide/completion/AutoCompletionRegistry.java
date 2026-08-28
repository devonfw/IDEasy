package com.devonfw.tools.ide.completion;

import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.property.Property;

/**
 * Registry for tool-specific auto-completion candidates.
 */
public class AutoCompletionRegistry {


  /**
   * The registered completion candidates.
   */
  private final List<CompletionEntry> entries = new ArrayList<>();


  /**
   * Adds a new completion candidate.
   *
   * @param candidate the candidate to add.
   */
  public void add(String candidate) {
    this.entries.add(new CompletionEntry(candidate));
  }

  /**
   * Adds a new completion candidate together with a synonym. For now this adds both values.
   *
   * @param candidate the candidate to add.
   * @param synonym to add a long with the candidate
   */
  public void add(String candidate, String synonym) {
    CompletionEntry entry = new CompletionEntry(candidate);
    entry.addSynonym(synonym);
    this.entries.add(entry);
  }

  /**
   * Adds all candidates matching the given argument to the collector.
   *
   * @param arg the current argument to complete.
   * @param collector the {@link CompletionCandidateCollector}.
   * @param property the {@link Property} that triggered completion.
   * @param commandlet the {@link Commandlet} owning the property.
   */
  public void complete(String arg, CompletionCandidateCollector collector,
      Property<?> property, Commandlet commandlet) {

    for (CompletionEntry entry : this.entries) {
      entry.complete(arg, collector, property, commandlet);
    }
  }

  public void addAlternative(String candidate1, String candidate2) {

    CompletionEntry entry1 = findEntry(candidate1);
    CompletionEntry entry2 = findEntry(candidate2);

    entry1.addAlternative(entry2);
  }

  public void addDependency(String candidate, List<String> depends) {

    CompletionEntry entry = findEntry(candidate);

    CompletionEntry[] dependencyEntries = new CompletionEntry[depends.size()];
    for (int i = 0; i < depends.size(); i++) {
      CompletionEntry dependencyEntry = findEntry(depends.get(i));
      dependencyEntries[i] = dependencyEntry;
    }
    entry.addDependency(dependencyEntries);
  }

  private CompletionEntry findEntry(String candidate) {
    for (CompletionEntry entry : this.entries) {
      if (entry.getCandidate().contains(candidate)) {
        return entry;
      }
    }
    return null;
  }
}
