package com.devonfw.tools.ide.tool.extra;

import com.devonfw.tools.ide.json.StandardJsonObjectMapper;

/**
 * Mapper of {@link ExtraTools} from/to JSON.
 */
public class ExtraToolsMapper extends StandardJsonObjectMapper<ExtraTools> {

  private static final ExtraToolsMapper INSTANCE = new ExtraToolsMapper();

  private ExtraToolsMapper() {
    super(ExtraTools.class);
  }

  @Override
  public String getStandardFilename() {

    return "ide-extra-tools.json";
  }

  /**
   * @return the singleton instance of {@link ExtraToolsMapper}.
   */
  public static ExtraToolsMapper get() {

    return INSTANCE;
  }

}
