package com.devonfw.tools.ide.repo;

import java.util.Collection;

/**
 * Interface for the custom {@link ToolRepository}.
 */
public interface CustomToolRepository extends ToolRepository {

  /** The filename of the configuration file in the settings for this {@link CustomToolRepository}. */
  String FILE_CUSTOM_TOOLS = "ide-custom-tools.json";

  /**
   * @return the {@link Collection} with the {@link CustomTool}s. Will be {@link Collection#isEmpty() empty} if no
   *         custom tools are configured.
   */
  Collection<CustomTool> getTools();

}
