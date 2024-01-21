package com.devonfw.tools.ide.url.model.file.json;

import com.devonfw.tools.ide.version.VersionRange;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * A simple container with the information about a security warning.
 */
public class UrlSecurityWarning {

  private VersionRange versionRange;

  private String matchedCpe;

  private String interval;

  private BigDecimal severity;

  private String severityVersion;

  private String cveName;

  private String description;

  private String nistUrl;

  public UrlSecurityWarning() {

    super();
  }

  /**
   * The constructor.
   *
   * @param versionRange the {@link VersionRange}, specifying the versions of the tool to which the security risk
   *        applies.
   * @param matchedCpe the matched CPE.
   * @param interval the interval provided by OWASP vulnerability that was used to determine the {@link VersionRange}.
   *        This is used to check if the mapping from CPE version to UrlVersion was correct.
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

  @Override
  public int hashCode() {

    String versionRangeString = Optional.ofNullable(this.versionRange).map(Object::toString).orElse("");
    String severity = Optional.ofNullable(this.severity).map(Object::toString).orElse("");
    String s = versionRangeString + severity + this.severityVersion + this.cveName + this.description + this.nistUrl;
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

    return true;
  }
}