package com.devonfw.tools.ide.commandlet;

import java.util.Collections;
import java.util.List;

import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.property.VersionProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionSegment;

/**
 * An internal {@link Commandlet} to set a tool version.
 *
 * @see ToolCommandlet#setVersion(VersionIdentifier, boolean)
 */
public class VersionSetCommandlet extends Commandlet {

  /** The tool to set the version of. */
  public final ToolProperty tool;

  /** The version to set. */
  public final VersionProperty version;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public VersionSetCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
    this.version = add(new VersionProperty("", true, "version"));
  }

  @Override
  public String getName() {

    return "set-version";
  }

  @Override
  public void run() {

    ToolCommandlet commandlet = this.tool.getValue();
    VersionIdentifier versionIdentifier = this.version.getValue();
    commandlet.setVersion(versionIdentifier, true);
  }

  @Override
  public boolean completeVersion(VersionIdentifier version2complete, CompletionCandidateCollector collector) {

    ToolCommandlet toolCmd = this.tool.getValue();
    if (toolCmd != null) {
      String text;
      if (version2complete == null) {
        text = "";
      } else {
        text = version2complete.toString();
        if (version2complete.isPattern()) {
          collector.add(text, this.version, this);
          return true;
        }
      }
      collector.add(text + VersionSegment.PATTERN_MATCH_ANY_STABLE_VERSION, this.tool, this);
      collector.add(text + VersionSegment.PATTERN_MATCH_ANY_VERSION, this.tool, this);
      List<VersionIdentifier> versions = this.context.getUrls().getSortedVersions(toolCmd.getName(),
          toolCmd.getEdition());
      Collections.reverse(versions);
      String[] sorderCandidates = versions.stream().map(v -> v.toString()).toArray(size -> new String[size]);
      collector.addAllMatches(text, sorderCandidates, this.version, this);
      return true;
    }
    return super.completeVersion(version2complete, collector);
  }

}
