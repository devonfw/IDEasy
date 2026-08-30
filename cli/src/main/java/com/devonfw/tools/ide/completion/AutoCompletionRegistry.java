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

  /**
   * Registers two already-added candidates as alternatives to each other, so that once one is provided on the command line, the other is no longer
   *
   * @param candidate1 the text of the first candidate (must have been added via {@link #add(String)} before).
   * @param candidate2 the text of the second candidate (must have been added via {@link #add(String)} before).
   * @throws IllegalStateException if either candidate has not been registered via {@link #add(String)}.
   */
  public void addAlternative(String candidate1, String candidate2) {

    CompletionEntry entry1 = findEntry(candidate1);
    CompletionEntry entry2 = findEntry(candidate2);
    if ((entry1 == null) || (entry2 == null)) {
      throw new IllegalStateException("Both candidates must be added via add(String) before calling addAlternative.");
    }
    entry1.addAlternative(entry2);
  }

  /**
   * Registers a dependency for {@code candidate}: it is only suggested once at least one of {@code depends} has already been provided on the command line.
   *
   * @param candidate the text of the dependent candidate (must have been added via {@link #add(String)} before).
   * @param depends the texts of the candidates of which at least one must already be provided (OR semantics).
   * @throws IllegalStateException if {@code candidate} or any of {@code depends} has not been registered via {@link #add(String)}.
   */
  public void addDependency(String candidate, List<String> depends) {

    CompletionEntry entry = findEntry(candidate);
    if (entry == null) {
      throw new IllegalStateException("Candidate '" + candidate + "' must be added via add(String) before calling addDependency.");
    }

    CompletionEntry[] dependencyEntries = new CompletionEntry[depends.size()];
    for (int i = 0; i < depends.size(); i++) {
      CompletionEntry dependencyEntry = findEntry(depends.get(i));

      if (dependencyEntry == null) {
        throw new IllegalStateException("Candidate '" + depends.get(i) + "' must be added via add(String) before calling addDependency.");
      }

      dependencyEntries[i] = dependencyEntry;
    }
    entry.addDependency(dependencyEntries);
  }

  /**
   * @param candidate the candidate to find.
   * @return the {@link CompletionEntry} whose {@link CompletionEntry#getCandidate() candidate} matches, or {@code null} if not found.
   */
  private CompletionEntry findEntry(String candidate) {
    for (CompletionEntry entry : this.entries) {
      if (entry.getCandidate().equals(candidate)) {
        return entry;
      }
    }
    return null;
  }
}
