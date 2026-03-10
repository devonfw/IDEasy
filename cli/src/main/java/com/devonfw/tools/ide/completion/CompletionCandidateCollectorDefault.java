package com.devonfw.tools.ide.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.Property;

/**
 * Collects the {@link CompletionCandidate}s for auto-completion.
 */
public class CompletionCandidateCollectorDefault implements CompletionCandidateCollector {

  private static final Logger LOG = LoggerFactory.getLogger(CompletionCandidateCollectorDefault.class);

  private final List<CompletionCandidate> candidates;

  private final IdeContext context;

  private boolean sortCandidates;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CompletionCandidateCollectorDefault(IdeContext context) {

    super();
    this.candidates = new ArrayList<>();
    this.context = context;
    this.sortCandidates = true;
  }

  @Override
  public void add(String text, String description, Property<?> property, Commandlet commandlet) {

    // Check if this candidate already exists to avoid duplicates
    for (CompletionCandidate existing : this.candidates) {
      if (existing.text().equals(text)) {
        // Duplicate candidate found, do not add
        return;
      }
    }

    CompletionCandidate candidate = createCandidate(text, description, property, commandlet);
    this.candidates.add(candidate);
    LOG.trace("Added {} for auto-completion of property {}.{}", candidate, commandlet, property);
  }

  @Override
  public List<CompletionCandidate> getCandidates() {

    return this.candidates;
  }

  @Override
  public List<CompletionCandidate> getSortedCandidates() {

    if (this.sortCandidates) {
      Collections.sort(this.candidates);
    }
    return this.candidates;
  }

  @Override
  public void disableSorting() {

    this.sortCandidates = false;
  }

  @Override
  public String toString() {

    return this.candidates.toString();
  }

}
