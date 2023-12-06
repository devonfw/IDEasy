package com.devonfw.tools.ide.url.model.file;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  /***
   * A simple container with the information about a security warning.
   *
   * @param versionRange the version range, specifying the versions of the tool to which the security risk applies.
   * @param severity the severity of the security risk.
   * @param severityVersion Indicating from which version the {@code severity} was obtained. As of December 2023, this
   *        is either v2 or v3.
   * @param cveName the name of the CVE (Common Vulnerabilities and Exposures).
   * @param description the description of the CVE.
   * @param nistUrl the url to the CVE on the NIST website.
   * @param referenceUrl the urls where additional information about the CVE can be found.
   */
  public record UrlSecurityWarning(VersionRange versionRange, BigDecimal severity, String severityVersion,
      String cveName, String description, String nistUrl, List<String> referenceUrl) {
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

  /***
   * Adds a new security warning to the security json file.
   *
   * @param versionRange the version range, specifying the versions of the tool to which the security risk applies.
   * @param severity the severity of the security risk.
   * @param severityVersion Indicating from which version the {@code severity} was obtained. As of December 2023, this
   *        is either v2 or v3.
   * @param cveName the name of the CVE (Common Vulnerabilities and Exposures).
   * @param description the description of the CVE.
   * @param nistUrl the url to the CVE on the NIST website.
   * @param referenceUrl the urls where additional information about the CVE can be found.
   * @return {@code true} if the security match was added, {@code false} if it was already present.
   */
  public boolean addSecurityWarning(VersionRange versionRange, BigDecimal severity, String severityVersion,
      String cveName, String description, String nistUrl, List<String> referenceUrl) {

    UrlSecurityWarning newWarning = new UrlSecurityWarning(versionRange, severity, severityVersion, cveName,
        description, nistUrl, referenceUrl);
    boolean added = warnings.add(newWarning);
    this.modified = this.modified || added;
    return added;
  }

  /***
   * For a given version, returns whether there is a security risk by locking at the warnings in the security json file.
   *
   * @param version the version to check for security risks.
   * @return {@code true} if there is a security risk for the given version, {@code false} otherwise.
   */
  public boolean contains(VersionIdentifier version) {

    for (UrlSecurityWarning warning : this.warnings) {
      if (warning.versionRange().contains(version)) {
        return true;
      }
    }
    return false;
  }

  public Set<UrlSecurityWarning> getMatchingSecurityWarnings(VersionIdentifier version) {

    Set<UrlSecurityWarning> matchedWarnings = new HashSet<>();
    for (UrlSecurityWarning warning : this.warnings) {
      if (warning.versionRange().contains(version)) {
        matchedWarnings.add(warning);
      }
    }
    return matchedWarnings;
  }

  public void clearSecurityWarnings() {

    this.warnings.clear();
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