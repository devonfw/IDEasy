package com.devonfw.tools.ide.url.model.file;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.url.model.file.json.UrlSecurityWarning;
import com.devonfw.tools.ide.url.model.file.json.UrlSecurityWarningsJson;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link UrlFile} for the "security.json" file.
 */
public class UrlSecurityJsonFile extends AbstractUrlFile<UrlEdition> {

  /** {@link #getName() Name} of security json file. */
  public static final String FILENAME_SECURITY = "security.json";

  private static final Logger LOG = LoggerFactory.getLogger(UrlSecurityJsonFile.class);

  private UrlSecurityWarningsJson urlSecurityWarningsJson = new UrlSecurityWarningsJson();

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   */
  public UrlSecurityJsonFile(UrlEdition parent) {

    super(parent, FILENAME_SECURITY);
  }

  /**
   * A wrapper for
   * {@link #addSecurityWarning(VersionRange, String, String, BigDecimal, String, String, String, String, List)} used in
   * the unit tests.
   */
  public boolean addSecurityWarning(VersionRange versionRange) {

    UrlSecurityWarning newWarning = new UrlSecurityWarning(versionRange, null, null, null, null, null, null, null,
        null);
    boolean added = this.urlSecurityWarningsJson.getWarnings().add(newWarning);
    this.modified = this.modified || added;
    return added;
  }

  /**
   * Adds a new security warning to the security json file.
   *
   * @param versionRange the version range, specifying the versions of the tool to which the security risk applies.
   * @param matchedCpe the matched CPE.
   * @param interval the interval of vulnerability that was used to determine the {@link VersionRange}. This is used to
   *        check if the mapping from CPE version to UrlVersion was correct.
   * @param severity the severity of the security risk.
   * @param severityVersion Indicating from which version the {@code severity} was obtained. As of December 2023, this
   *        is either v2 or v3.
   * @param cveName the name of the CVE (Common Vulnerabilities and Exposures).
   * @param description the description of the CVE.
   * @param nistUrl the url to the CVE on the NIST website.
   * @param referenceUrl the urls where additional information about the CVE can be found.
   * @return {@code true} if the security match was added, {@code false} if it was already present.
   */
  public boolean addSecurityWarning(VersionRange versionRange, String matchedCpe, String interval, BigDecimal severity,
      String severityVersion, String cveName, String description, String nistUrl, List<String> referenceUrl) {

    UrlSecurityWarning newWarning = new UrlSecurityWarning(versionRange, matchedCpe, interval, severity,
        severityVersion, cveName, description, nistUrl, referenceUrl);
    boolean added = this.urlSecurityWarningsJson.getWarnings().add(newWarning);
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

    List<VersionIdentifier> sortedVersions = null;
    if (ignoreWarningsThatAffectAllVersions) {
      sortedVersions = Objects.requireNonNull(context).getUrls().getSortedVersions(edition.getName(),
          edition.getName());
    }

    for (UrlSecurityWarning warning : this.urlSecurityWarningsJson.getWarnings()) {
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
   */
  public boolean contains(VersionIdentifier version) {

    return contains(version, false, null, null);
  }

  public Set<UrlSecurityWarning> getMatchingSecurityWarnings(VersionIdentifier version) {

    Set<UrlSecurityWarning> matchedWarnings = new HashSet<>();
    for (UrlSecurityWarning warning : this.urlSecurityWarningsJson.getWarnings()) {
      if (warning.getVersionRange().contains(version)) {
        matchedWarnings.add(warning);
      }
    }
    return matchedWarnings;
  }

  public void clearSecurityWarnings() {

    this.urlSecurityWarningsJson.getWarnings().clear();
    this.modified = true;
  }

  @Override
  protected void doLoad() {

    if (!Files.exists(getPath())) {
      return;
    }
    ObjectMapper mapper = JsonMapping.create();
    try {
      this.urlSecurityWarningsJson = mapper.readValue(getPath().toFile(), UrlSecurityWarningsJson.class);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load the UrlSecurityJsonFile " + getPath(), e);
    }
  }

  @Override
  protected void doSave() {

    ObjectMapper mapper = JsonMapping.create();

    if (this.urlSecurityWarningsJson.getWarnings().isEmpty() && !Files.exists(getPath())) {
      return;
    }

    String jsonString;
    try {
      jsonString = mapper.writeValueAsString(this.urlSecurityWarningsJson);
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
}