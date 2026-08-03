package com.devonfw.tools.ide.url.model.file.json;

import java.io.IOException;
import java.util.Objects;

import com.devonfw.tools.ide.json.JsonBuilder;
import com.devonfw.tools.ide.json.JsonObjectDeserializer;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link JsonObjectDeserializer} for {@link ToolDependency}.
 */
public class ToolDependencyDeserializer extends JsonObjectDeserializer<ToolDependency> {

  @Override
  protected JsonBuilder<ToolDependency> createBuilder() {

    return new ToolDependencyBuilder();
  }

  private class ToolDependencyBuilder extends JsonBuilder<ToolDependency> {

    private String tool;
    private VersionRange versionRange;
    private OperatingSystem os;
    private SystemArchitecture arch;

    @Override
    public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

      if (property.equals("tool")) {
        this.tool = readValueAsString(p, property, this.tool);
      } else if (property.equals("versionRange")) {
        this.versionRange = readValue(p, VersionRange.class, property, this.versionRange);
      } else if (property.equals("os")) {
        String value = readValueAsString(p, property, null);
        this.os = OperatingSystem.of(value);
      } else if (property.equals("arch")) {
        String value = readValueAsString(p, property, null);
        this.arch = SystemArchitecture.of(value);
      } else {
        super.setProperty(property, p, ctxt);
      }
    }

    @Override
    public ToolDependency build() {

      Objects.requireNonNull(this.tool);
      Objects.requireNonNull(this.versionRange);
      return new ToolDependency(this.tool, this.versionRange, this.os, this.arch);
    }
  }

}
