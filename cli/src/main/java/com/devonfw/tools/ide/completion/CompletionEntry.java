package com.devonfw.tools.ide.completion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.property.Property;

/**
 * A completion candidate with optional alternatives and dependencies. The candidate and all of its alternatives represent completion options. Therefore, this
 * entry is not suggested if the candidate or any of its alternatives has already been provided.
 */
public class CompletionEntry {

  /** The candidate to suggest. */
  private final String candidate;

  /** The names of this entry, including the candidate itself. */
  private final Set<String> alternatives;

  /** The candidates of which at least one has to be provided before this entry may be suggested. */
  private final Set<String> dependencies = new HashSet<>();

  /**
   * The constructor for a completion entry with alternatives.
   *
   * @param candidate the candidate to suggest.
   * @param alternatives names of this entry, including {@code candidate}.
   */
  public CompletionEntry(String candidate, Set<String> alternatives) {
    this.candidate = candidate;
    this.alternatives = alternatives;
  }

  /**
   * @return the primary candidate string.
   */
  public String getCandidate() {
    return candidate;
  }

  /**
   * Performs auto-completion for this entry, skipping it entirely if the candidate or any of its alternatives has already been provided on the command line.
   *
   * @param arg the current argument being completed.
   * @param collector the {@link CompletionCandidateCollector} to add the candidates to.
   * @param property the {@link Property} that triggered the completion.
   * @param commandlet the {@link Commandlet} owning the property.
   */
  public void complete(String arg, CompletionCandidateCollector collector, Property<?> property, Commandlet commandlet) {

    Set<String> alreadyProvided = collector.getAlreadyProvided();
    if (!isDependencySatisfied(alreadyProvided)) {
      return;
    }

    if (isProvided(alreadyProvided)) {
      return;
    }

    if (candidate.startsWith(arg)) {
      collector.add(candidate, "", property, commandlet);
    }
  }

  /**
   * Checks whether the dependencies of this entry are satisfied.
   *
   * @param alreadyProvided the already provided arguments.
   * @return {@code true} if no dependency is configured or at least one dependency has been provided, {@code false} otherwise.
   */
  private boolean isDependencySatisfied(Set<String> alreadyProvided) {

    return this.dependencies.isEmpty() || this.dependencies.stream().anyMatch(alreadyProvided::contains);
  }

  /**
   * Checks if this candidate or any of its alternatives was already provided.
   *
   * @param alreadyProvided the set of already provided arguments.
   * @return {@code true} if already provided, {@code false} otherwise.
   */
  public boolean isProvided(Set<String> alreadyProvided) {
    return this.alternatives.stream().anyMatch(alreadyProvided::contains);
  }

  /**
   * Adds candidates of which at least one has to be provided before this entry may be suggested.
   *
   * @param entries array of {@link CompletionEntry} objects of which at least one must be provided.
   */
  public void addDependency(String... entries) {

    this.dependencies.addAll(List.of(entries));
  }
}
