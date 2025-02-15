package com.devonfw.tools.ide.property;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.validation.PropertyValidator;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionSegment;

/**
 * {@link Property} for {@link VersionIdentifier} as {@link #getValueType() value type}.
 */
public class VersionProperty extends Property<VersionIdentifier> {

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   */
  public VersionProperty(String name, boolean required, String alias) {

    this(name, required, alias, null);
  }

  /**
   * The constructor.
   *
   * @param name the {@link #getName() property name}.
   * @param required the {@link #isRequired() required flag}.
   * @param alias the {@link #getAlias() property alias}.
   * @param validator the {@link PropertyValidator} used to {@link #validate() validate} the {@link #getValue() value}.
   */
  public VersionProperty(String name, boolean required, String alias, PropertyValidator<VersionIdentifier> validator) {

    super(name, required, alias, false, validator);
  }

  @Override
  public Class<VersionIdentifier> getValueType() {

    return VersionIdentifier.class;
  }

  @Override
  public VersionIdentifier parse(String valueAsString, IdeContext context) {

    return VersionIdentifier.of(valueAsString);
  }

  @Override
  protected void completeValue(String arg, IdeContext context, Commandlet commandlet, CompletionCandidateCollector collector) {

    ToolCommandlet tool = commandlet.getToolForCompletion();
    if (tool != null) {
      completeVersion(VersionIdentifier.of(arg), tool, context, commandlet, collector);
    }
  }

  private void completeVersion(VersionIdentifier version2complete, ToolCommandlet tool, IdeContext context, Commandlet commandlet,
      CompletionCandidateCollector collector) {

    collector.disableSorting();
    if (tool != null) {
      String text;
      if (version2complete == null) {
        text = "";
      } else {
        text = version2complete.toString();
        if (version2complete.isPattern()) {
          collector.add(text, "Given version pattern.", this, commandlet);
          return;
        }
      }
      ToolRepository toolRepository = tool.getToolRepository();
      List<VersionIdentifier> versions = toolRepository.getSortedVersions(tool.getName(), tool.getConfiguredEdition(), tool);
      int size = versions.size();
      String[] sortedCandidates = IntStream.rangeClosed(1, size).mapToObj(i -> versions.get(size - i).toString()).toArray(String[]::new);
      collector.addAllMatches(text, sortedCandidates, this, commandlet);
      List<CompletionCandidate> candidates = collector.getCandidates();
      Collections.reverse(candidates);
      CompletionCandidate latest = collector.createCandidate(text + VersionSegment.PATTERN_MATCH_ANY_STABLE_VERSION,
          "Latest stable matching version", this, commandlet);
      if (candidates.isEmpty()) {
        candidates.add(latest);
      } else {
        candidates.add(1, latest);
      }
      collector.add(text + VersionSegment.PATTERN_MATCH_ANY_VERSION, "Latest matching version including unstable versions", this, commandlet);
    }
  }
}
