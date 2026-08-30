package com.devonfw.tools.ide.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.property.Property;

/**
 * A completion candidate that may have synonyms, alternatives, or dependencies. An entry will not be suggested if it or its synonyms/alternatives are already
 * provided, or if its dependencies are not satisfied.
 */
public class CompletionEntry {

  /** The primary candidate string. */
  private String candidate;

  /** List of synonym strings for this candidate. */
  private List<String> synonyms = new ArrayList<>();

  /** List of alternatives (symmetric relationship). */
  private List<CompletionEntry> alternatives = new ArrayList<>();

  /** List of dependency groups (AND logic between groups, OR logic within a group). */
  private List<List<CompletionEntry>> dependencies = new ArrayList<>();

  /**
   * The constructor.
   *
   * @param candidate the primary candidate to add.
   */
  public CompletionEntry(String candidate) {
    this.candidate = candidate;
  }

  /**
   * @return the primary candidate string.
   */
  public String getCandidate() {
    return candidate;
  }

  /**
   * Adds a synonym for this candidate.
   *
   * @param synonym the synonym to add.
   */
  public void addSynonym(String synonym) {
    this.synonyms.add(synonym);
  }

  /**
   * Performs auto-completion for this entry, skipping it entirely if the candidate or any of its synonyms has already been provided on the command line.
   *
   * @param arg the current argument being completed.
   * @param collector the {@link CompletionCandidateCollector} to add matching candidates to.
   * @param property the {@link Property} that triggered completion.
   * @param commandlet the {@link Commandlet} owning the property.
   */
  public void complete(String arg, CompletionCandidateCollector collector, Property<?> property, Commandlet commandlet) {

    Set<String> alreadyProvided = collector.getAlreadyProvided();
    if (alreadyProvided != null) {
      if (!isDependencySatisfied(alreadyProvided)) {
        return;
      }

      if (isProvided(alreadyProvided)) {
        return;
      }

      for (CompletionEntry alternative : this.alternatives) {
        if (alternative.isProvided(alreadyProvided)) {
          return;
        }
      }
    }

    if (candidate.startsWith(arg)) {
      collector.add(candidate, "", property, commandlet);
    }

    for (String synonym : synonyms) {
      if (synonym.startsWith(arg)) {
        collector.add(synonym, "", property, commandlet);
      }
    }
  }

  /**
   * Checks whether all configured dependency groups are satisfied.
   *
   * @param alreadyProvided the set of already provided arguments.
   * @return {@code true} if all dependency groups are satisfied, {@code false} otherwise.
   */
  private boolean isDependencySatisfied(Set<String> alreadyProvided) {

    for (List<CompletionEntry> group : this.dependencies) {
      boolean groupSatisfied = false;
      for (CompletionEntry entry : group) {
        if (entry.isProvided(alreadyProvided)) {
          groupSatisfied = true;
          break;
        }
      }
      if (!groupSatisfied) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if this candidate or any of its synonyms was already provided.
   *
   * @param alreadyProvided the set of already provided arguments.
   * @return {@code true} if already provided, {@code false} otherwise.
   */
  public boolean isProvided(Set<String> alreadyProvided) {
    return alreadyProvided.contains(this.candidate) || this.synonyms.stream().anyMatch(alreadyProvided::contains);
  }

  /**
   * Adds a symmetric alternative relation between this entry and another.
   *
   * @param alternative the alternative {@link CompletionEntry}.
   */
  public void addAlternative(CompletionEntry alternative) {

    if ((alternative == null) || (alternative == this)) {
      return;
    }

    if (!this.alternatives.contains(alternative)) {
      this.alternatives.add(alternative);
    }

    if (!alternative.alternatives.contains(this)) {
      alternative.alternatives.add(this);
    }
  }

  /**
   * Adds an OR-dependency group to this entry.
   *
   * @param entries array of {@link CompletionEntry} objects of which at least one must be provided.
   */
  public void addDependency(CompletionEntry[] entries) {
    if ((entries == null) || (entries.length == 0)) {
      return;
    }
    this.dependencies.add(List.of(entries));
  }
}
