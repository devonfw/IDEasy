package com.devonfw.tools.ide.json;

import java.io.IOException;

import com.devonfw.tools.ide.tool.custom.CustomTool;
import com.devonfw.tools.ide.tool.custom.CustomToolJsonDeserializer;
import com.devonfw.tools.ide.tool.custom.CustomToolJsonSerializer;
import com.devonfw.tools.ide.tool.custom.CustomTools;
import com.devonfw.tools.ide.tool.custom.CustomToolsJsonDeserializer;
import com.devonfw.tools.ide.tool.custom.CustomToolsJsonSerializer;
import com.devonfw.tools.ide.tool.extra.ExtraToolInstallation;
import com.devonfw.tools.ide.tool.extra.ExtraToolInstallationJsonDeserializer;
import com.devonfw.tools.ide.tool.extra.ExtraToolInstallationJsonSerializer;
import com.devonfw.tools.ide.tool.extra.ExtraTools;
import com.devonfw.tools.ide.tool.extra.ExtraToolsJsonDeserializer;
import com.devonfw.tools.ide.tool.extra.ExtraToolsJsonSerializer;
import com.devonfw.tools.ide.tool.npm.NpmJs;
import com.devonfw.tools.ide.tool.npm.NpmJsDist;
import com.devonfw.tools.ide.tool.npm.NpmJsDistJsonDeserializer;
import com.devonfw.tools.ide.tool.npm.NpmJsDistJsonSerializer;
import com.devonfw.tools.ide.tool.npm.NpmJsJsonDeserializer;
import com.devonfw.tools.ide.tool.npm.NpmJsJsonSerializer;
import com.devonfw.tools.ide.tool.npm.NpmJsVersion;
import com.devonfw.tools.ide.tool.npm.NpmJsVersionJsonDeserializer;
import com.devonfw.tools.ide.tool.npm.NpmJsVersionJsonSerializer;
import com.devonfw.tools.ide.tool.npm.NpmJsVersions;
import com.devonfw.tools.ide.tool.npm.NpmJsVersionsJsonDeserializer;
import com.devonfw.tools.ide.tool.npm.NpmJsVersionsJsonSerializer;
import com.devonfw.tools.ide.tool.pip.PypiObject;
import com.devonfw.tools.ide.tool.pip.PypiObjectJsonDeserializer;
import com.devonfw.tools.ide.tool.pip.PypiObjectJsonSerializer;
import com.devonfw.tools.ide.url.model.file.json.Cve;
import com.devonfw.tools.ide.url.model.file.json.CveJsonDeserializer;
import com.devonfw.tools.ide.url.model.file.json.CveJsonSerializer;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.url.model.file.json.ToolDependencyDeserializer;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurityJsonDeserializer;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurityJsonSerializer;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionIdentifierDeserializer;
import com.devonfw.tools.ide.version.VersionRange;
import com.devonfw.tools.ide.version.VersionRangeDeserializer;
import com.devonfw.tools.ide.version.VersionRangeKeyDeserializer;
import com.devonfw.tools.ide.version.VersionRangeSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Simple factory for Jackson {@link ObjectMapper} to read and write JSON with centralized mapping configuration.
 */
public class JsonMapping {

  /**
   * @return a new instance of {@link ObjectMapper} pre-configured for reasonable JSON mapping.
   */
  public static ObjectMapper create() {
    return create(false);
  }

  /**
   * <b>ATTENTION:</b>
   * Never use this method directly inside IDEasy {@code cli} module!
   *
   * @return a new instance of {@link ObjectMapper} pre-configured for reasonable JSON mapping.
   */
  public static ObjectMapper createWithReflectionSupportForUrlUpdaters() {

    return create(true);
  }

  private static ObjectMapper create(boolean supportReflection) {

    ObjectMapper mapper;
    if (supportReflection) {
      mapper = new ObjectMapper();
    } else {
      mapper = new ObjectMapper(null, IdeasySerializerProvider.get(), new DefaultDeserializationContext.Impl(IdeasyDeserializerFactory.get()));
    }
    mapper = mapper.registerModule(new JavaTimeModule());
    mapper = mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper = mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper = mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    SimpleModule customModule = new SimpleModule();
    // version mappings
    customModule.addDeserializer(VersionIdentifier.class, new VersionIdentifierDeserializer());
    customModule.addDeserializer(VersionRange.class, new VersionRangeDeserializer());
    customModule.addSerializer(VersionRange.class, new VersionRangeSerializer());
    customModule.addKeyDeserializer(VersionRange.class, new VersionRangeKeyDeserializer());
    // dependency mapping
    customModule.addDeserializer(ToolDependency.class, new ToolDependencyDeserializer());
    // security mapping
    customModule.addSerializer(ToolSecurity.class, new ToolSecurityJsonSerializer());
    customModule.addDeserializer(ToolSecurity.class, new ToolSecurityJsonDeserializer());
    customModule.addSerializer(Cve.class, new CveJsonSerializer());
    customModule.addDeserializer(Cve.class, new CveJsonDeserializer());
    // pypi mapping
    customModule.addDeserializer(PypiObject.class, new PypiObjectJsonDeserializer());
    customModule.addSerializer(PypiObject.class, new PypiObjectJsonSerializer());
    // custom tools mapping
    customModule.addDeserializer(CustomTools.class, new CustomToolsJsonDeserializer());
    customModule.addSerializer(CustomTools.class, new CustomToolsJsonSerializer());
    customModule.addDeserializer(CustomTool.class, new CustomToolJsonDeserializer());
    customModule.addSerializer(CustomTool.class, new CustomToolJsonSerializer());
    // extra tools mapping
    customModule.addDeserializer(ExtraTools.class, new ExtraToolsJsonDeserializer());
    customModule.addSerializer(ExtraTools.class, new ExtraToolsJsonSerializer());
    customModule.addDeserializer(ExtraToolInstallation.class, new ExtraToolInstallationJsonDeserializer());
    customModule.addSerializer(ExtraToolInstallation.class, new ExtraToolInstallationJsonSerializer());
    // npmjs mapping
    customModule.addSerializer(NpmJs.class, new NpmJsJsonSerializer());
    customModule.addDeserializer(NpmJs.class, new NpmJsJsonDeserializer());
    customModule.addSerializer(NpmJsVersion.class, new NpmJsVersionJsonSerializer());
    customModule.addDeserializer(NpmJsVersion.class, new NpmJsVersionJsonDeserializer());
    customModule.addSerializer(NpmJsVersions.class, new NpmJsVersionsJsonSerializer());
    customModule.addDeserializer(NpmJsVersions.class, new NpmJsVersionsJsonDeserializer());
    customModule.addSerializer(NpmJsDist.class, new NpmJsDistJsonSerializer());
    customModule.addDeserializer(NpmJsDist.class, new NpmJsDistJsonDeserializer());
    mapper = mapper.registerModule(customModule);
    mapper.setDefaultPrettyPrinter(new JsonPrettyPrinter());
    return mapper;
  }

  /**
   * @param p the {@link JsonParser}.
   * @throws IOException in case of an error.
   */
  public static void skipCurrentField(JsonParser p, String property) throws IOException {

    if (property != null) {
      // currently cannot log here due to https://github.com/devonfw/IDEasy/issues/404
      //LOG.debug("Ignoring unexpected property {}", property);
    }
    JsonToken jsonToken = p.getCurrentToken();
    if ((jsonToken == JsonToken.START_OBJECT) || (jsonToken == JsonToken.START_ARRAY)) {
      p.skipChildren();
    }
  }

}
