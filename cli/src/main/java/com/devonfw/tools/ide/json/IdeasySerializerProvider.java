package com.devonfw.tools.ide.json;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.CacheProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;

/**
 * Extends {@link DefaultSerializerProvider} to disable reflective serialization.
 */
final class IdeasySerializerProvider extends DefaultSerializerProvider {

  private static final IdeasySerializerProvider INSTANCE = new IdeasySerializerProvider();

  private IdeasySerializerProvider() {
    super();
  }

  private IdeasySerializerProvider(SerializerProvider src, SerializationConfig config, SerializerFactory f) {

    super(src, config, f);
  }

  private IdeasySerializerProvider(DefaultSerializerProvider src, CacheProvider cp) {

    super(src, cp);
  }

  @Override
  public DefaultSerializerProvider createInstance(SerializationConfig config, SerializerFactory serializerFactory) {

    return new IdeasySerializerProvider(this, config, serializerFactory);
  }

  @Override
  public DefaultSerializerProvider withCaches(CacheProvider cacheProvider) {

    return new IdeasySerializerProvider(this, cacheProvider);
  }

  @Override
  protected JsonSerializer<Object> _createUntypedSerializer(JavaType type) throws JsonMappingException {

    JsonSerializer<Object> serializer = super._createUntypedSerializer(type);
    if (serializer instanceof BeanSerializer) {
      throw new IllegalStateException(
          "Preventing reflective serialization of " + type + " due to GraalVM limitation. Implement a custom " + type.getRawClass().getSimpleName()
              + "JsonSerializer and register it in JsonMapping.");
    }
    return serializer;
  }

  static IdeasySerializerProvider get() {

    return INSTANCE;
  }
}
