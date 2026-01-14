package com.devonfw.tools.ide.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * {@link JsonDeserializer} that deserializes a {@link JsonObject}.
 *
 * @param <T> type of {@link JsonObject} to deserialize.
 */
public abstract class JsonObjectDeserializer<T extends JsonObject> extends JsonDeserializer<T> {

  @Override
  public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

    JsonBuilder<T> builder = createBuilder();
    JsonToken token = p.getCurrentToken();
    if (token == JsonToken.START_OBJECT) {
      token = p.nextToken();
      while (token == JsonToken.FIELD_NAME) {
        String property = p.currentName();
        token = p.nextToken();
        assert (token != JsonToken.FIELD_NAME) && (token != JsonToken.END_OBJECT) && (token != JsonToken.END_ARRAY);
        builder.setProperty(property, p, ctxt);
        token = p.nextToken();
      }
      assert (token == JsonToken.END_OBJECT) : "expected END_OBJECT but found " + token;
      return builder.build();
    } else if (token == JsonToken.VALUE_NULL) {
      return null;
    } else {
      throw new IllegalStateException("Unexpected token " + token);
    }
  }

  protected abstract JsonBuilder<T> createBuilder();

  protected <V> List<V> readArray(JsonParser p, Class<V> valueType, String property, List<V> old) throws IOException {
    JsonToken token = p.getCurrentToken();
    if (token == JsonToken.START_ARRAY) {
      List<V> list;
      if (old == null) {
        list = new ArrayList<>();
      } else {
        assert false : "Duplicate property: " + property;
        list = old;
      }
      token = p.nextToken();
      while (token != JsonToken.END_ARRAY) {
        V value = p.readValueAs(valueType);
        list.add(value);
        token = p.nextToken();
      }
      return list;
    } else if (token == JsonToken.VALUE_NULL) {
      return null;
    } else {
      throw new IllegalStateException("Unexpected token " + token);
    }
  }

  protected String readValueAsString(JsonParser p, String property, String old) throws IOException {

    return readValue(p, String.class, property, old);
  }

  protected Boolean readValueAsBoolean(JsonParser p, String property, Boolean old) throws IOException {

    return readValue(p, Boolean.class, property, old);
  }

  protected Double readValueAsDouble(JsonParser p, String property, Double old) throws IOException {

    return readValue(p, Double.class, property, old);
  }

  protected <T> T readValue(JsonParser p, Class<T> type, String property, T old) throws IOException {

    T value = null;
    Object simpleValue = null;
    if (type == String.class) {
      simpleValue = p.getValueAsString();
    } else if (type == Boolean.class) {
      JsonToken token = p.getCurrentToken();
      if (token == JsonToken.VALUE_TRUE) {
        simpleValue = Boolean.TRUE;
      } else if (token == JsonToken.VALUE_FALSE) {
        simpleValue = Boolean.FALSE;
      } else if (token != JsonToken.VALUE_NULL) {
        throw new IllegalStateException("Unexpected token for boolean value: " + token);
      }
    } else if (type == Double.class) {
      simpleValue = p.getValueAsDouble();
    } else if (type == Integer.class) {
      simpleValue = p.getValueAsInt();
    } else if (type == Long.class) {
      simpleValue = p.getValueAsLong();
    } else {
      value = p.readValueAs(type);
    }
    if ((value == null) && (simpleValue != null)) {
      value = type.cast(simpleValue);
    }
    if (old != null) {
      assert false : "Duplicate value for property " + property + ": already got " + old + ", ignoring " + value;
      return old;
    }
    return value;
  }

}
