package com.devonfw.tools.ide.url.model.file;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.url.model.file.json.UrlSecurityWarning;
import com.devonfw.tools.ide.url.model.folder.AbstractUrlToolOrEdition;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.devonfw.tools.ide.version.VersionRangeMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link UrlFile} with the security information for an {@link UrlEdition}.
 */
public class UrlSecurityFile extends AbstractUrlFile<AbstractUrlToolOrEdition<?, ?>> {

  /** {@link #getName() Name} of security file. */
  public static final String SECURITY_JSON = "security.json";

  private final Collection<UrlSecurityWarning> urlSecurityWarnings;

  private ToolSecurity security;

  private IdeContext context;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   */
  public UrlSecurityFile(AbstractUrlToolOrEdition<?, ?> parent) {

    super(parent, SECURITY_JSON);
    this.urlSecurityWarnings = new HashSet<>();
  }

  /**
   * Sets the security information for this {@link UrlSecurityFile}.
   *
   * @param security the {@link ToolSecurity} object containing security information to be set.
   */
  public void setSecurity(ToolSecurity security) {
    this.security = security;
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

    ObjectMapper mapper = JsonMapping.create();

    if (this.urlSecurityWarnings.isEmpty() && !Files.exists(getPath())) {
      System.out.println("Skipping save for " + getPath() + " (no warnings and file doesn't exist)");
      return;
    }

    String jsonString;
    List<Map<String, Object>> serializableWarnings = urlSecurityWarnings.stream()
        .map(warning -> {
          Map<String, Object> m = new HashMap<>();
          m.put("versionRange", VersionRangeMapper.serializeVersionRange(warning.getVersionRange()));
          m.put("severity", warning.getSeverity());
          m.put("cveID", warning.getCveName());
          m.put("description", warning.getDescription());
          m.put("nistUrl", warning.getNistUrl());
          return m;
        }).toList();

    try {
      jsonString = mapper.writeValueAsString(serializableWarnings);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    try (BufferedWriter bw = Files.newBufferedWriter(getPath(), StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
      bw.write(jsonString + "\n");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to save the UrlSecurityJsonFile " + getPath(), e);
    }

  }

  /**
   * Adds a new security warning for a specified version range.
   *
   * @param versionRange the {@link VersionRange} of the tool or edition for which to add the warning.
   */
  public void addSecurityWarning(VersionRange versionRange) {

    UrlSecurityWarning newWarning = new UrlSecurityWarning(versionRange, null, null, null, null);
    boolean added = urlSecurityWarnings.add(newWarning);
    this.modified = this.modified || added;
  }

  /**
   * Adds a new security warning with detailed information, such as severity, CVE ID, description, and NIST URL.
   *
   * @param versionRange the {@link VersionRange} of the tool or edition for which to add the warning.
   * @param severity the severity of the security issue.
   * @param cveName the CVE ID of the vulnerability.
   * @param description a description of the security issue.
   * @param nistUrl the NIST URL for more information about the CVE.
   * @return {@code true} if the warning was successfully added, {@code false} if it already exists.
   */
  public boolean addSecurityWarning(VersionRange versionRange, BigDecimal severity, String cveName, String description,
      String nistUrl) {
    UrlSecurityWarning newWarning = new UrlSecurityWarning(versionRange, severity, cveName, description, nistUrl);
    boolean added = urlSecurityWarnings.add(newWarning);
    this.modified = this.modified || added;
    return added;
  }

  /**
   * Clears all security warnings from this {@link UrlSecurityFile}.
   */
  public void clearSecurityWarnings() {
    this.urlSecurityWarnings.clear();
    this.modified = true;
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
      sortedVersions = Objects.requireNonNull(context).getUrls().getSortedVersions(edition.getName(),
          edition.getName());
    }

    for (UrlSecurityWarning warning : this.urlSecurityWarnings) {
      VersionRange versionRange = warning.getVersionRange();
      if (ignoreWarningsThatAffectAllVersions) {
        boolean includesOldestVersion = versionRange.getMin() == null
            || warning.getVersionRange().contains(sortedVersions.get(sortedVersions.size() - 1));
        boolean includesNewestVersion = versionRange.getMax() == null
            || warning.getVersionRange().contains(sortedVersions.get(0));
        if (includesOldestVersion && includesNewestVersion) {
          continue;
        }
      }
      if (warning.getVersionRange().contains(version)) {
        return true;
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


