package com.devonfw.tools.ide.security;

import java.util.Collection;

import com.devonfw.tools.ide.url.model.file.json.Cve;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Container for an option of a {@link VersionIdentifier version} to chose for a specific tool and edition. Use to suggest updates (or downgrade) of a tool to
 * fix or reduce {@link Cve}s.
 *
 * @param version the suggested {@link VersionIdentifier} to install.
 * @param option the logical name of this option to be chosen by the end-user.
 * @param issues the {@link Cve}s of the specified version. Ideally empty.
 */
public record ToolVersionChoice(VersionIdentifier version, String option, Collection<Cve> issues) {

  /** @see #ofCurrent(VersionIdentifier, Collection) */
  public static final String CVE_OPTION_CURRENT = "current";

  /** @see #ofLatest(VersionIdentifier, Collection) */
  public static final String CVE_OPTION_LATEST = "latest";

  /** @see #ofNearest(VersionIdentifier, Collection) */
  public static final String CVE_OPTION_NEAREST = "nearest";

  /**
   * @param version the {@link #version() version}.
   * @param issues the {@link #issues() issues}.
   * @return the current {@link ToolVersionChoice}.
   */
  public static ToolVersionChoice ofCurrent(VersionIdentifier version, Collection<Cve> issues) {
    return new ToolVersionChoice(version, CVE_OPTION_CURRENT, issues);
  }

  /**
   * @param version the {@link #version() version}.
   * @param issues the {@link #issues() issues}.
   * @return the latest {@link ToolVersionChoice}.
   */
  public static ToolVersionChoice ofLatest(VersionIdentifier version, Collection<Cve> issues) {
    return new ToolVersionChoice(version, CVE_OPTION_LATEST, issues);
  }

  /**
   * @param version the {@link #version() version}.
   * @param issues the {@link #issues() issues}.
   * @return the nearest {@link ToolVersionChoice}.
   */
  public static ToolVersionChoice ofNearest(VersionIdentifier version, Collection<Cve> issues) {
    return new ToolVersionChoice(version, CVE_OPTION_NEAREST, issues);
  }

  @Override
  public String toString() {

    return this.option + " (" + this.version + ")";
  }
}
