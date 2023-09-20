package com.devonfw.tools.ide.tool.ide;

import com.devonfw.tools.ide.common.Tags;

/**
 * Interface for a plugin descriptor of an IDE.
 */
public interface PluginDescriptor extends Tags {

  /**
   * @return the unique identifier of the plugin.
   */
  String getId();

  /**
   * @return the name of the plugin properties file excluding the extension.
   */
  String getName();

  /**
   * @return the optional plugin URL (download/update site).
   */
  String getUrl();

  /**
   * @return {@code true} if the plugin is active and shall be installed automatically when the IDE is setup,
   *         {@code false} otherwise.
   */
  boolean isActive();

}
