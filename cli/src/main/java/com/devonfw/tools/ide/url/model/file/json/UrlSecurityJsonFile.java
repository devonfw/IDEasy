package com.devonfw.tools.ide.url.model.file.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.url.model.file.AbstractUrlFile;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UrlSecurityJsonFile extends AbstractUrlFile<UrlEdition> {

  /** {@link #getName() Name} of security json file. */
  public static final String FILENAME_SECURITY = "security.json";

  private static final Logger LOG = LoggerFactory.getLogger(UrlSecurityJsonFile.class);

  List<UrlSecurityMatch> matches;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   */
  public UrlSecurityJsonFile(UrlEdition parent) {

    super(parent, FILENAME_SECURITY);
    this.matches = new ArrayList<>();
  }

  public boolean addSecurityMatch(VersionRange versionRange, double severity, String severityVersion, String cveName,
      String description, String nistUrl, List<String> referenceUrl) {

    UrlSecurityWarning newWarning = new UrlSecurityWarning(severity, severityVersion, cveName, description, nistUrl,
        referenceUrl);
    for (UrlSecurityMatch match : matches) {
      if (match.getVersionRange().equals(versionRange)) {
        boolean added = match.addWarning(newWarning);
        this.modified = this.modified || added;
        return added;
      }
    }
    UrlSecurityMatch newMatch = new UrlSecurityMatch(versionRange);
    newMatch.addWarning(newWarning);
    this.modified = true;
    return matches.add(newMatch);
  }

  public boolean removeSecurityMatch(VersionRange versionRange) {

    for (UrlSecurityMatch match : matches) {
      if (match.getVersionRange().equals(versionRange)) {
        boolean removed = matches.remove(match);
        this.modified = this.modified || removed;
        return removed;
      }
    }
    return false;
  }

  public boolean contains(VersionIdentifier version) {

    for (UrlSecurityMatch match : matches) {
      if (match.getVersionRange().contains(version)) {
        return true;
      }
    }
    return false;
  }

  public Set<UrlSecurityWarning> getSecurityWarnings(VersionIdentifier version) {

    Set<UrlSecurityWarning> warnings = new HashSet<>();
    for (UrlSecurityMatch match : matches) {
      if (match.getVersionRange().contains(version)) {
        warnings.addAll(match.getWarnings());
      }
    }
    return warnings;
  }

  public void clearSecurityMatches() {

    this.matches.clear();
  }

  @Override
  protected void doLoad() {

    if (!Files.exists(getPath())) {
      return;
    }
    ObjectMapper mapper = JsonMapping.create();
    try {
      matches = mapper.readValue(getPath().toFile(), new TypeReference<List<UrlSecurityMatch>>() {
      });
    } catch (IOException e) {
      throw new IllegalStateException("The UrlSecurityJsonFile " + getPath() + " could not be parsed.", e);
    }
  }

  @Override
  protected void doSave() {

    Path path = getPath();
    ObjectMapper mapper = JsonMapping.create();

    if (this.matches.isEmpty() && !Files.exists(path)) {
      return;
    }

    String jsonString;
    try {
      jsonString = mapper.writeValueAsString(matches);
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

class UrlSecurityMatch {
  private final VersionRange versionRange;

  private final Set<UrlSecurityWarning> warnings;

  public UrlSecurityMatch() {

    // this constructor is needed for jackson deserialization
    this.versionRange = null;
    this.warnings = new HashSet<>();
  }

  public UrlSecurityMatch(VersionRange versionRange) {

    this.versionRange = versionRange;
    this.warnings = new HashSet<>();
  }

  public VersionRange getVersionRange() {

    return versionRange;
  }

  public Set<UrlSecurityWarning> getWarnings() {

    return warnings;
  }

  public boolean addWarning(UrlSecurityWarning warning) {

    return this.warnings.add(warning);
  }

}

// severity could be java.math.BigDecimal; instead of double (unsing BigDecimal("123.4").setScale(1,
// BigDecimal.ROUND_HALF_UP);)
record UrlSecurityWarning(double severity, String severityVersion, String cveName, String description, String nistUrl,
    List<String> referenceUrl) {
};