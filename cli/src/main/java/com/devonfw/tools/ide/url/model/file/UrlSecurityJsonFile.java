package com.devonfw.tools.ide.url.model.file;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.json.mapping.JsonMapping;
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

  /**
   * A simple container with the information about a security warning.
   */
  public static class UrlSecurityWarning {

    private VersionRange versionRange;

    private String matchedCpe;

    private String interval;

    private BigDecimal severity;

    private String severityVersion;

    private String cveName;

    private String description;

    private String nistUrl;

    private List<String> referenceUrls;

    public UrlSecurityWarning() {

      super();
    }

    /**
     * The constructor.
     *
     * @param versionRange the version range, specifying the versions of the tool to which the security risk applies.
     * @param matchedCpe the matched CPE.
     * @param interval the interval of vulnerability that was used to determine the {@link VersionRange}. This is used
     *        to check if the mapping from CPE version to UrlVersion was correct.
     * @param severity the severity of the security risk.
     * @param severityVersion Indicating from which version the {@code severity} was obtained. As of December 2023, this
     *        is either v2 or v3.
     * @param cveName the name of the CVE (Common Vulnerabilities and Exposures).
     * @param description the description of the CVE.
     * @param nistUrl the url to the CVE on the NIST website.
     * @param referenceUrl the urls where additional information about the CVE can be found.
     */
    public UrlSecurityWarning(VersionRange versionRange, String matchedCpe, String interval, BigDecimal severity,
        String severityVersion, String cveName, String description, String nistUrl, List<String> referenceUrl) {

      super();
      this.versionRange = versionRange;
      this.matchedCpe = matchedCpe;
      this.interval = interval;
      this.severity = severity;
      this.severityVersion = severityVersion;
      this.cveName = cveName;
      this.description = description;
      this.nistUrl = nistUrl;
      this.referenceUrls = referenceUrl;
    }

    // these setters and getters are needed for the jackson (de)serialization
    public void setVersionRange(VersionRange versionRange) {

      this.versionRange = versionRange;
    }

    public void setInterval(String interval) {

      this.interval = interval;
    }

    public void setMatchedCpe(String matchedCpe) {

      this.matchedCpe = matchedCpe;
    }

    public void setSeverity(BigDecimal severity) {

      this.severity = severity;
    }

    public void setSeverityVersion(String severityVersion) {

      this.severityVersion = severityVersion;
    }

    public void setCveName(String cveName) {

      this.cveName = cveName;
    }

    public void setDescription(String description) {

      this.description = description;
    }

    public void setNistUrl(String nistUrl) {

      this.nistUrl = nistUrl;
    }

    public void setReferenceUrl(List<String> referenceUrl) {

      this.referenceUrls = referenceUrl;
    }

    public VersionRange getVersionRange() {

      return versionRange;
    }

    public String getMatchedCpe() {

      return matchedCpe;
    }

    public String getInterval() {

      return interval;
    }

    public BigDecimal getSeverity() {

      return severity;
    }

    public String getSeverityVersion() {

      return severityVersion;
    }

    public String getCveName() {

      return cveName;
    }

    public String getDescription() {

      return description;
    }

    public String getNistUrl() {

      return nistUrl;
    }

    public List<String> getReferenceUrl() {

      return referenceUrls;
    }

    @Override
    public int hashCode() {

      String versionRangeString = this.versionRange == null ? "" : this.versionRange.toString();
      String severity = this.severity == null ? "" : this.severity.toString();
      String referenceUrls = this.referenceUrls == null ? "" : this.referenceUrls.toString();
      String s = versionRangeString + severity + this.severityVersion + this.cveName + this.description + this.nistUrl
          + referenceUrls;
      return s.hashCode();

    }

    @Override
    public boolean equals(Object obj) {

      if (obj == this) {
        return true;
      }
      if ((obj == null) || (obj.getClass() != getClass())) {
        return false;
      }
      UrlSecurityWarning other = (UrlSecurityWarning) obj;
      if (!this.versionRange.equals(other.versionRange)) {
        return false;
      }
      if (this.severity.compareTo(other.severity) != 0) {
        return false;
      }
      if (!this.severityVersion.equals(other.severityVersion)) {
        return false;
      }
      if (!this.cveName.equals(other.cveName)) {
        return false;
      }
      if (!this.description.equals(other.description)) {
        return false;
      }
      if (!this.nistUrl.equals(other.nistUrl)) {
        return false;
      }
      for (String url : this.referenceUrls) {
        if (!other.referenceUrls.contains(url)) {
          return false;
        }
      }
      for (String url : other.referenceUrls) {
        if (!this.referenceUrls.contains(url)) {
          return false;
        }
      }

      return true;
    }
  };

  /** {@link #getName() Name} of security json file. */
  public static final String FILENAME_SECURITY = "security.json";

  private static final Logger LOG = LoggerFactory.getLogger(UrlSecurityJsonFile.class);

  private Set<UrlSecurityWarning> warnings;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   */
  public UrlSecurityJsonFile(UrlEdition parent) {

    super(parent, FILENAME_SECURITY);
    this.warnings = new HashSet<>();
  }

  public boolean addSecurityWarning(VersionRange versionRange) {

    UrlSecurityWarning newWarning = new UrlSecurityWarning(versionRange, null, null, null, null, null, null, null,
        null);
    boolean added = warnings.add(newWarning);
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
    boolean added = warnings.add(newWarning);
    this.modified = this.modified || added;
    return added;
  }

  /**
   * For a given version, returns whether there is a security risk by locking at the warnings in the security json file.
   *
   * @param version the version to check for security risks.
   * @return {@code true} if there is a security risk for the given version, {@code false} otherwise.
   */
  public boolean contains(VersionIdentifier version, boolean ignoreWarningsThatAffectAllVersions, IdeContext context) {

    List<VersionIdentifier> sortedVersions = null;
    if (ignoreWarningsThatAffectAllVersions) {
      sortedVersions = Objects.requireNonNull(context).getUrls()
          .getSortedVersions(this.getParent().getParent().getName(), this.getParent().getName());
    }

    for (UrlSecurityWarning warning : this.warnings) {

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

  public boolean contains(VersionIdentifier version) {

    return contains(version, false, null);
  }

  public Set<UrlSecurityWarning> getMatchingSecurityWarnings(VersionIdentifier version) {

    Set<UrlSecurityWarning> matchedWarnings = new HashSet<>();
    for (UrlSecurityWarning warning : this.warnings) {
      if (warning.getVersionRange().contains(version)) {
        matchedWarnings.add(warning);
      }
    }
    return matchedWarnings;
  }

  public void clearSecurityWarnings() {

    this.warnings.clear();
    this.modified = true;
  }

  @Override
  protected void doLoad() {

    if (!Files.exists(getPath())) {
      return;
    }
    ObjectMapper mapper = JsonMapping.create();
    try {
      warnings = mapper.readValue(getPath().toFile(), new TypeReference<Set<UrlSecurityWarning>>() {
      });
    } catch (IOException e) {
      throw new IllegalStateException("The UrlSecurityJsonFile " + getPath() + " could not be parsed.", e);
    }
  }

  @Override
  protected void doSave() {

    Path path = getPath();
    ObjectMapper mapper = JsonMapping.create();

    if (this.warnings.isEmpty() && !Files.exists(path)) {
      return;
    }

    String jsonString;
    try {
      jsonString = mapper.writeValueAsString(warnings);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    try (BufferedWriter bw = Files.newBufferedWriter(path, StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
      bw.write(jsonString + "\n");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to save file " + path, e);
    }
  }
}