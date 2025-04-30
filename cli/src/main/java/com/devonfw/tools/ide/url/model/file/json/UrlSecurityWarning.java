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


  /**
   * Default constructor for Jackson (de)serialization.
   */
  public UrlSecurityWarning() {
    
  }

  /**
   * The constructor.
   *
   * @param versionRange the {@link VersionRange}, specifying the versions of the tool to which the security risk applies.
   * @param severity the severity of the security risk.
   * @param cveName the name of the CVE (Common Vulnerabilities and Exposures).
   */
  public UrlSecurityWarning(VersionRange versionRange, BigDecimal severity, String cveName) {

    this.versionRange = versionRange;
    this.severity = severity;
    this.cveName = cveName;
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


  @Override
  public int hashCode() {

    String versionRangeString = Optional.ofNullable(this.versionRange).map(Object::toString).orElse("");
    String severity = Optional.ofNullable(this.severity).map(Object::toString).orElse("");
    String s = versionRangeString + severity + this.cveName;
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
    return this.cveName.equals(other.cveName);
  }
}
