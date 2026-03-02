package com.devonfw.tools.ide.json;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@link JsonSerializer} that deserializes a {@link JsonObject}.
 *
 * @param <T> type of {@link JsonObject} to serialize.
 */
public abstract class JsonObjectSerializer<T extends JsonObject> extends JsonSerializer<T> {

  @Override
  public void serialize(T object, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
    if (object == null) {
      jgen.writeNull();
      return;
    }
    jgen.writeStartObject();
    serializeProperties(object, jgen, serializerProvider);
    jgen.writeEndObject();
  }

  protected <V> void writeArray(Collection<V> values, JsonGenerator jgen) throws IOException {

    jgen.writeStartArray();
    for (V value : values) {
      jgen.writeObject(value);
    }
    jgen.writeEndArray();
  }

  protected abstract void serializeProperties(T object, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException;

}
