package com.devonfw.tools.ide.url.model.file.json;

import com.devonfw.tools.ide.json.StandardJsonObjectMapper;
import com.devonfw.tools.ide.url.model.file.UrlSecurityFile;

/**
 * Container representing data from the "security.json" file with all {@link Cve CVE}s of a specific tool.
 *
 * @see com.devonfw.tools.ide.url.model.file.UrlSecurityFile
 */
public class ToolSecurityMapper extends StandardJsonObjectMapper<ToolSecurity> {

  private static final ToolSecurityMapper INSTANCE = new ToolSecurityMapper();

  private ToolSecurityMapper() {

    super(ToolSecurity.class);
  }

  @Override
  public String getStandardFilename() {

    return UrlSecurityFile.SECURITY_JSON;
  }

  /**
   * @return the {@link ToolSecurityMapper} instance.
   */
  public static ToolSecurityMapper get() {

    return INSTANCE;
  }
}
