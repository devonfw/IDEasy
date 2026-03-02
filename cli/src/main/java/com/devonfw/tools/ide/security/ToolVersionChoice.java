package com.devonfw.tools.ide.security;

import com.devonfw.tools.ide.log.IdeLogger;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.url.model.file.json.Cve;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Container for an option of a {@link VersionIdentifier version} to choose for a specific tool and edition. Used to suggest updates (or downgrades) of a tool
 * to fix or reduce {@link Cve}s.
 *
 * @param toolEditionAndVersion the {@link ToolEditionAndVersion} to install.
 * @param option the logical name of this option to be chosen by the end-user.
 * @param vulnerabilities the {@link Cve}s of the specified version. Ideally empty.
 */
public record ToolVersionChoice(ToolEditionAndVersion toolEditionAndVersion, String option, ToolVulnerabilities vulnerabilities) {

  /** @see #ofCurrent(ToolEditionAndVersion, ToolVulnerabilities) */
  public static final String CVE_OPTION_CURRENT = "current";

  /** @see #ofLatest(ToolEditionAndVersion, ToolVulnerabilities) */
  public static final String CVE_OPTION_LATEST = "latest";

  /** @see #ofNearest(ToolEditionAndVersion, ToolVulnerabilities) */
  public static final String CVE_OPTION_NEAREST = "nearest";

  /**
   * @param toolEditionAndVersion the {@link #toolEditionAndVersion() toolEditionAndVersion}.
   * @param issues the {@link #vulnerabilities() vulnerabilities}.
   * @return the current {@link ToolVersionChoice}.
   */
  public static ToolVersionChoice ofCurrent(ToolEditionAndVersion toolEditionAndVersion, ToolVulnerabilities issues) {
    return new ToolVersionChoice(toolEditionAndVersion, CVE_OPTION_CURRENT, issues);
  }

  /**
   * @param toolEditionAndVersion the {@link #toolEditionAndVersion() toolEditionAndVersion}.
   * @param issues the {@link #vulnerabilities() vulnerabilities}.
   * @return the latest {@link ToolVersionChoice}.
   */
  public static ToolVersionChoice ofLatest(ToolEditionAndVersion toolEditionAndVersion, ToolVulnerabilities issues) {
    return new ToolVersionChoice(toolEditionAndVersion, CVE_OPTION_LATEST, issues);
  }

  /**
   * @param toolEditionAndVersion the {@link #toolEditionAndVersion() toolEditionAndVersion}.
   * @param issues the {@link #vulnerabilities() vulnerabilities}.
   * @return the nearest {@link ToolVersionChoice}.
   */
  public static ToolVersionChoice ofNearest(ToolEditionAndVersion toolEditionAndVersion, ToolVulnerabilities issues) {
    return new ToolVersionChoice(toolEditionAndVersion, CVE_OPTION_NEAREST, issues);
  }

  /**
   * @param logger the {@link IdeLogger}.
   * @return {@code true} if {@link ToolVulnerabilities#EMPTY empty} (no vulnerabilities), {@code false} otherwise.
   */
  public boolean logAndCheckIfEmpty(IdeLogger logger) {

    String message = this.vulnerabilities.toString(this.toolEditionAndVersion);
    if (this.vulnerabilities.getIssues().isEmpty()) {
      logger.success(message);
      return true;
    } else {
      logger.warning(message);
      return false;
    }
  }

  @Override
  public String toString() {

    String state;
    if (this.vulnerabilities.getIssues().isEmpty()) {
      state = "safe";
    } else {
      state = "unsafe";
    }
    return this.option + " (" + this.toolEditionAndVersion.getResolvedVersion() + " - " + state + ")";
  }
}
