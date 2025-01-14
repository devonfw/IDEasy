package com.devonfw.tools.ide.repo;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON representation of a single {@link CustomToolMetadata}.
 *
 * @param name the {@link CustomToolMetadata#getTool() tool name}.
 * @param version the {@link CustomToolMetadata#getVersion() tool version}.
 * @param osAgnostic {@code true} if {@link OperatingSystem} agnostic, {@code false} otherwise.
 * @param archAgnostic {@code true} if {@link com.devonfw.tools.ide.os.SystemArchitecture} agnostic, {@code false} otherwise.
 * @param url the overridden {@link CustomToolsJson#url() repository URL} or {@code null} to inherit.
 * @see CustomToolsJson#tools()
 */
public record CustomToolJson(String name, String version, @JsonProperty(value = "os-agnostic") boolean osAgnostic,
                             @JsonProperty(value = "arch-agnostic") boolean archAgnostic, String url) {

  /**
   * @return a new {@link CustomToolsJson} having {@link #url()} set to {@code null}.
   */
  public CustomToolJson withoutUrl() {

    return new CustomToolJson(name, version, osAgnostic, archAgnostic, null);
  }
}
