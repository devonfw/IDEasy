package com.devonfw.tools.ide.json;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BasicDeserializerFactory;
import com.fasterxml.jackson.databind.deser.DeserializerFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

final class IdeasyDeserializerFactory extends BasicDeserializerFactory {

  private static final IdeasyDeserializerFactory INSTANCE = new IdeasyDeserializerFactory();

  private IdeasyDeserializerFactory() {
    super(new DeserializerFactoryConfig());
  }

  private IdeasyDeserializerFactory(DeserializerFactoryConfig config) {
    super(config);
  }

  @Override
  protected DeserializerFactory withConfig(DeserializerFactoryConfig config) {

    if (_factoryConfig == config) {
      return this;
    }
    ClassUtil.verifyMustOverride(IdeasyDeserializerFactory.class, this, "withConfig");
    return new IdeasyDeserializerFactory(config);
  }

  @Override
  public JsonDeserializer<Object> createBeanDeserializer(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc) throws JsonMappingException {

    final DeserializationConfig config = ctxt.getConfig();
    JsonDeserializer<Object> deser = _findCustomBeanDeserializer(type, config, beanDesc);
    if (deser != null) {
      return deser;
    }
    throw new IllegalStateException(
        "Preventing reflective deserialization of " + type + " due to GraalVM limitation. Implement a custom " + type.getRawClass().getSimpleName()
            + "JsonDeserializer and register it in JsonMapping.");
  }

  @Override
  public JsonDeserializer<Object> createBuilderBasedDeserializer(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc, Class<?> builderClass)
      throws JsonMappingException {

    return createBeanDeserializer(ctxt, type, beanDesc);
  }

  static IdeasyDeserializerFactory get() {
    return INSTANCE;
  }
}
