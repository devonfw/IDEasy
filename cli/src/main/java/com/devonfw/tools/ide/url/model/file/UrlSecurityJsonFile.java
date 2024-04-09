package com.devonfw.tools.ide.url.model.file;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.url.model.file.json.UrlSecurityWarning;
import com.devonfw.tools.ide.url.model.file.json.UrlSecurityWarningsJson;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link UrlFile} for the "security.json" file.
 */
public class UrlSecurityJsonFile extends AbstractUrlFile<UrlEdition> {

  /** {@link #getName() Name} of security json file. */
  public static final String FILENAME_SECURITY = "security.json";

  private Collection<UrlSecurityWarning> urlSecurityWarnings;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   */
  public UrlSecurityJsonFile(UrlEdition parent) {

    super(parent, FILENAME_SECURITY);
    this.urlSecurityWarnings = new HashSet<>();
  }

  /**
   * A wrapper for {@link #addSecurityWarning(VersionRange, BigDecimal, String, String, String)} used in the unit tests.
   *
   * @param versionRange the {@link VersionRange}.
   */
  public void addSecurityWarning(VersionRange versionRange) {

    UrlSecurityWarning newWarning = new UrlSecurityWarning(versionRange, null, null, null, null);
    boolean added = urlSecurityWarnings.add(newWarning);
    this.modified = this.modified || added;
  }

  /**
   * Adds a new security warning to the security json file.
   *
   * @param versionRange the version range, specifying the versions of the tool to which the security risk applies.
   * @param severity the severity of the security risk.
   * @param cveName the name of the CVE (Common Vulnerabilities and Exposures).
   * @param description the description of the CVE.
   * @param nistUrl the url to the CVE on the NIST website.
   * @return {@code true} if the security match was added, {@code false} if it was already present.
   */
  public boolean addSecurityWarning(VersionRange versionRange, BigDecimal severity, String cveName, String description,
      String nistUrl) {

    UrlSecurityWarning newWarning = new UrlSecurityWarning(versionRange, severity, cveName, description, nistUrl);
    boolean added = urlSecurityWarnings.add(newWarning);
    this.modified = this.modified || added;
    return added;
  }

  /**
   * For a given version, returns whether there is a security warning in the {@link UrlSecurityWarningsJson JSON
   * object}.
   *
   * @param version the {@link VersionIdentifier version} to check for security risks listed in the
   *        {@link UrlSecurityJsonFile}.
   * @param ignoreWarningsThatAffectAllVersions {@code true} if warnings that affect all versions should be ignored,
   *        {@code false} otherwise.
   * @param context the {@link IdeContext} to use in case {@code ignoreWarningsThatAffectAllVersions} is {@code true} to
   *        get the sorted versions of the tool.
   * @param edition the {@link UrlEdition} to use in case {@code ignoreWarningsThatAffectAllVersions} is {@code true} to
   *        get the sorted versions of the tool.
   * @return {@code true} if there is a security risk for the given version, {@code false} otherwise.
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
   * For a given version, returns whether there is a security warning in the {@link UrlSecurityWarningsJson JSON
   * object}. This method does not ignore warnings that affect all versions.
   *
   * @param version the {@link VersionIdentifier}.
   * @return {@code true} if there is a security risk for the given version, {@code false} otherwise.
   */
  public boolean contains(VersionIdentifier version) {

    return contains(version, false, null, null);
  }

  /**
   * @param version the {@link VersionIdentifier version} to check for security risks listed in the
   *        {@link UrlSecurityJsonFile}.
   * @return the {@link UrlSecurityWarning UrlSecurityWarnings} for the given {@code version} or {@code null} if no such
   *         warnings exist.
   */
  public Set<UrlSecurityWarning> getMatchingSecurityWarnings(VersionIdentifier version) {

    Set<UrlSecurityWarning> matchedWarnings = new HashSet<>();
    for (UrlSecurityWarning warning : this.urlSecurityWarnings) {
      if (warning.getVersionRange().contains(version)) {
        matchedWarnings.add(warning);
      }
    }
    return matchedWarnings;
  }

  /** Clears all security warnings. */
  public void clearSecurityWarnings() {

    this.urlSecurityWarnings.clear();
    this.modified = true;
  }

  @Override
  protected void doLoad() {

    if (!Files.exists(getPath())) {
      return;
    }
    ObjectMapper mapper = JsonMapping.create();
    try {
      urlSecurityWarnings = mapper.readValue(getPath().toFile(), new TypeReference<Set<UrlSecurityWarning>>() {
      });
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load the UrlSecurityJsonFile " + getPath(), e);
    }
  }

  @Override
  protected void doSave() {

    ObjectMapper mapper = JsonMapping.create();

    if (this.urlSecurityWarnings.isEmpty() && !Files.exists(getPath())) {
      return;
    }

    String jsonString;
    try {
      jsonString = mapper.writeValueAsString(urlSecurityWarnings);
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
   * @return Collection of {@link UrlSecurityWarning}.
   */
  public Collection<UrlSecurityWarning> getUrlSecurityWarnings() {

    return this.urlSecurityWarnings;
  }
}