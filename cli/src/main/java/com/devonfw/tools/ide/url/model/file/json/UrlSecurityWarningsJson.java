package com.devonfw.tools.ide.url.model.file.json;

import java.util.HashSet;
import java.util.Set;

/**
 * Java model class representing a "security.json" file.
 *
 * @see com.devonfw.tools.ide.url.model.file.UrlSecurityFile
 */
public class UrlSecurityWarningsJson {

  private Set<UrlSecurityWarning> warnings = new HashSet<>();

  public UrlSecurityWarningsJson() {

    super();
  }

  public Set<UrlSecurityWarning> getWarnings() {

    return this.warnings;
  }

  public void setWarnings(Set<UrlSecurityWarning> warnings) {

    this.warnings = warnings;
  }

}
