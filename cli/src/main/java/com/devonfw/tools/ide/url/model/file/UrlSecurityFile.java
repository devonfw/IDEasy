package com.devonfw.tools.ide.url.model.file;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.url.model.file.json.Cve;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.url.model.folder.AbstractUrlToolOrEdition;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link UrlFile} with the security information for an {@link UrlEdition}.
 */
public class UrlSecurityFile extends AbstractUrlFile<AbstractUrlToolOrEdition<?, ?>> {

  /** {@link #getName() Name} of security file. */
  public static final String SECURITY_JSON = "security.json";


  private ToolSecurity security = ToolSecurity.getEmpty();

  private final ObjectMapper MAPPER = JsonMapping.create();

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   */
  public UrlSecurityFile(AbstractUrlToolOrEdition<?, ?> parent) {

    super(parent, SECURITY_JSON);
  }

  /**
   * Sets the security information for this {@link UrlSecurityFile}.
   *
   * @param security the {@link ToolSecurity} object containing security information to be set.
   */
  public void setSecurity(ToolSecurity security) {
    this.security = security;
    this.modified = true;
  }

  /**
   * @return the content of the CVE map of the security.json file
   */
  public ToolSecurity getSecurity() {

    if (this.security == null) {
      return ToolSecurity.getEmpty();
    }
    return this.security;
  }

  @Override
  protected void doLoad() {
    this.security = ToolSecurity.of(getPath());
  }

  @Override
  public void doSave() {

    if ((security == null || security.getIssues().isEmpty()) && !Files.exists(getPath())) {
      System.out.println("Skipping save for " + getPath() + " (no warnings and file doesn't exist)");
      return;
    }

    try (BufferedWriter writer = Files.newBufferedWriter(getPath())) {
      MAPPER.writeValue(writer, security);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save file " + getPath(), e);
    }

  }

  /**
   * Adds a new CVE warning with detailed information, such as severity, CVE ID and a versionRange
   */

  public void addCve(Cve cve) {
    if (this.security == null || this.security == ToolSecurity.getEmpty()) {
      this.security = new ToolSecurity();
    }

    List<Cve> issues = this.security.getIssues();
    if (!issues.contains(cve)) {
      issues.add(cve);
      this.modified = true;
    }
  }


  /**
   * Clears all security warnings from this {@link UrlSecurityFile}.
   */
  public void clearSecurityWarnings() {
    if (this.security != null) {
      this.security.getIssues().clear();
      this.modified = true;
    }
  }


  /**
   * Checks if a security warning exists for a given version. Optionally, warnings affecting all versions can be ignored.
   *
   * @param version the {@link VersionIdentifier} of the version to check for security warnings.
   * @param ignoreWarningsThatAffectAllVersions {@code true} to ignore warnings that affect all versions, {@code false} to include them.
   * @param context the {@link IdeContext} providing contextual information (can be {@code null}).
   * @param edition the {@link UrlEdition} to check for security warnings.
   * @return {@code true} if a security warning exists for the given version, {@code false} otherwise.
   */

  public boolean contains(VersionIdentifier version, boolean ignoreWarningsThatAffectAllVersions, IdeContext context,
      UrlEdition edition) {

    List<VersionIdentifier> sortedVersions = List.of();
    if (ignoreWarningsThatAffectAllVersions) {
      sortedVersions = Objects.requireNonNull(context).getUrls().getSortedVersions(
          edition.getName(), edition.getName(), null);
    }

    List<Cve> issues = this.security != null ? this.security.getIssues() : List.of();

    for (Cve cve : issues) {
      for (VersionRange versionRange : cve.versions()) {
        if (ignoreWarningsThatAffectAllVersions) {
          boolean includesOldestVersion = versionRange.getMin() == null
              || versionRange.contains(sortedVersions.get(sortedVersions.size() - 1));
          boolean includesNewestVersion = versionRange.getMax() == null
              || versionRange.contains(sortedVersions.get(0));
          if (includesOldestVersion && includesNewestVersion) {
            continue;
          }
        }
        if (versionRange.contains(version)) {
          return true;
        }
      }
    }
    return false;
  }


  /**
   * Checks if a security warning exists for a given version.
   *
   * @param version the {@link VersionIdentifier} of the version to check for security warnings.
   * @return {@code true} if a security warning exists for the given version, {@code false} otherwise.
   */
  public boolean contains(VersionIdentifier version) {

    return contains(version, false, null, null);
  }


}


