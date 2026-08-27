package com.devonfw.tools.ide.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.property.Property;

/**
 * A completion candidate that may have one or more synonyms. When any one of the candidate or its synonyms has already been provided on the command line, none
 * of them will be suggested again.
 */
public class CompletionEntry {

  /** The primary candidate string. */
  private String candidate;

  /** List of synonym strings for this candidate. */
  private List<String> synonyms = new ArrayList<>();

  private List<CompletionEntry> alternatives = new ArrayList<>();

  /**
   * The constructor.
   *
   * @param candidate the primary candidate to add.
   */
  public CompletionEntry(String candidate) {
    this.candidate = candidate;
  }

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

  public boolean isProvided(Set<String> alreadyProvided) {
    return alreadyProvided.contains(this.candidate) || this.synonyms.stream().anyMatch(alreadyProvided::contains);
  }

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
}
