package com.devonfw.tools.ide.completion;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.property.Property;

/**
 * Registry for tool-specific auto-completion candidates.
 */
public class AutoCompletionRegistry {

  /**
   * The registered completion entries mapped by their candidate names.
   */
  private final Map<String, CompletionEntry> entryMap = new LinkedHashMap<>();

  /**
   * Adds a completion candidate together with its alternatives.
   *
   * @param candidate the candidate to add.
   * @param alternatives to add a long with the candidate
   * @return the {@link CompletionEntry} created for {@code candidate} for configuration.
   */
  public CompletionEntry add(String candidate, String... alternatives) {

    Set<String> names = new LinkedHashSet<>();
    names.add(candidate);
    names.addAll(List.of(alternatives));

    Set<String> immutableNames = Set.copyOf(names);

    CompletionEntry entry = new CompletionEntry(candidate, names);
    this.entryMap.put(candidate, entry);

    for (String alternative : immutableNames) {
      if (!alternative.equals(candidate)) {
        this.entryMap.put(alternative, new CompletionEntry(alternative, immutableNames));
      }
    }
    return entry;
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

    for (CompletionEntry entry : this.entryMap.values()) {
      entry.complete(arg, collector, property, commandlet);
    }
  }
}
