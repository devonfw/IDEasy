package com.devonfw.tools.ide.tool.custom;

import java.util.Collection;

import com.devonfw.tools.ide.tool.repository.ToolRepository;

/**
 * Interface for the custom {@link ToolRepository}.
 */
public interface CustomToolRepository extends ToolRepository {

  /**
   * @return the {@link Collection} with the {@link CustomToolMetadata}s. Will be {@link Collection#isEmpty() empty} if no custom tools are configured.
   */
  Collection<CustomToolMetadata> getTools();

}
