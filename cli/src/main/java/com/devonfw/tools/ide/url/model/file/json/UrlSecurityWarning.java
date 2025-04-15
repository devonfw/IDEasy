package com.devonfw.tools.ide.url.model.file.json;

import java.math.BigDecimal;
import java.util.Optional;

import com.devonfw.tools.ide.version.VersionRange;

/**
 * A simple container with the information about a security warning.
 */
public class UrlSecurityWarning {

  private VersionRange versionRange;

  private BigDecimal severity;

  private String cveName;

  private String description;

  private String nistUrl;

  /**
   * Default constructor for Jackson (de)serialization.
   */
  public UrlSecurityWarning() {

    super();
  }

  /**
   * The constructor.
   *
   * @param versionRange the {@link VersionRange}, specifying the versions of the tool to which the security risk applies.
   * @param severity the severity of the security risk.
   * @param cveName the name of the CVE (Common Vulnerabilities and Exposures).
   * @param description the description of the CVE.
   * @param nistUrl the url to the CVE on the NIST website.
   */
  public UrlSecurityWarning(VersionRange versionRange, BigDecimal severity, String cveName, String description,
      String nistUrl) {

    super();
    this.versionRange = versionRange;
    this.severity = severity;
    this.cveName = cveName;
    this.description = description;
    this.nistUrl = nistUrl;
  }

  // these setters and getters are needed for the jackson (de)serialization

  /**
   * Sets the {@link VersionRange} that specifies the versions this security warning applies to.
   *
   * @param versionRange the version range for which the security warning is applicable.
   */
  public void setVersionRange(VersionRange versionRange) {

    this.versionRange = versionRange;
  }

  /**
   * Sets the severity of the security risk.
   *
   * @param severity the severity value representing the risk level.
   */
  public void setSeverity(BigDecimal severity) {

    this.severity = severity;
  }

  /**
   * Sets the name of the CVE associated with this security warning.
   *
   * @param cveName the CVE name.
   */
  public void setCveName(String cveName) {

    this.cveName = cveName;
  }

  /**
   * Sets the description of the CVE providing more details about the security issue.
   *
   * @param description the CVE description.
   */
  public void setDescription(String description) {

    this.description = description;
  }

  /**
   * Sets the URL pointing to the NIST page for more information about the CVE.
   *
   * @param nistUrl the NIST URL for the CVE.
   */
  public void setNistUrl(String nistUrl) {

    this.nistUrl = nistUrl;
  }

  /**
   * Retrieves the version range for which this security warning applies.
   *
   * @return the {@link VersionRange} specifying the applicable versions.
   */
  public VersionRange getVersionRange() {

    return versionRange;
  }

  /**
   * Retrieves the severity of the security risk.
   *
   * @return the severity value.
   */
  public BigDecimal getSeverity() {

    return severity;
  }

  /**
   * Retrieves the name of the CVE associated with this security warning.
   *
   * @return the CVE name.
   */
  public String getCveName() {

    return cveName;
  }

  /**
   * Retrieves the description of the CVE providing additional information about the security issue.
   *
   * @return the description of the CVE.
   */
  public String getDescription() {

    return description;
  }

  /**
   * Retrieves the NIST URL for more information about the CVE.
   *
   * @return the URL to the CVE on the NIST website.
   */
  public String getNistUrl() {

    return nistUrl;
  }

  @Override
  public int hashCode() {

    String versionRangeString = Optional.ofNullable(this.versionRange).map(Object::toString).orElse("");
    String severity = Optional.ofNullable(this.severity).map(Object::toString).orElse("");
    String s = versionRangeString + severity + this.cveName + this.description + this.nistUrl;
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
