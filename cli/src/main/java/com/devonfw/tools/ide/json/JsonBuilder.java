package com.devonfw.tools.ide.json;

import java.io.IOException;

import com.devonfw.tools.ide.common.Builder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * {@link Builder} for {@link JsonObject}s.
 *
 * @param <T> type of the {@link JsonObject} to build.
 */
public abstract class JsonBuilder<T extends JsonObject> implements Builder<T> {

  /**
   * @param property the name of the JSON property to set.
   * @param p the {@link JsonParser} to read the property value from.
   * @param ctxt the {@link DeserializationContext}.
   * @throws IOException on error.
   */
  public void setProperty(String property, JsonParser p, DeserializationContext ctxt) throws IOException {

    JsonMapping.skipCurrentField(p, property);
  }

}
