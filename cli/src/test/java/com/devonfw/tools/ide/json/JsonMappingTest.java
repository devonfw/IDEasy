package com.devonfw.tools.ide.json;

import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.version.BoundaryType;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test of {@link JsonMapping}.
 */
class JsonMappingTest extends Assertions {

  private static final String INSTANT_VALUE_STRING = "2001-12-31T23:59:59.987654321Z";

  private static final String INSTANT_VALUE_JSON = '"' + INSTANT_VALUE_STRING + '"';

  private static final Instant INSTANT_VALUE = Instant.parse(INSTANT_VALUE_STRING);

  /**
   * Test of {@link JsonMapping#create()} reading an {@link Instant} value.
   *
   * @throws Exception in case of an error.
   */
  @Test
  void testReadInstant() throws Exception {

    // arrange
    String value = INSTANT_VALUE_JSON;
    // act
    ObjectMapper mapper = JsonMapping.create();
    Instant instant = mapper.readValue(value, Instant.class);
    // assert
    assertThat(instant).isEqualTo(INSTANT_VALUE);
  }

  /**
   * Test of {@link JsonMapping#create()} writing an {@link Instant} value.
   *
   * @throws Exception in case of an error.
   */
  @Test
  void testWriteInstant() throws Exception {

    // arrange
    Instant value = INSTANT_VALUE;
    // act
    ObjectMapper mapper = JsonMapping.create();
    String json = mapper.writeValueAsString(value);
    // assert
    assertThat(json).isEqualTo(INSTANT_VALUE_JSON);
  }

  @Test
  void testReadDependencies() throws Exception {

    // arrange
    String json = "{\"tool\": \"java\",\"versionRange\": \"[11,21_35]\"}";
    VersionRange expectedVersionRange = VersionRange.of(VersionIdentifier.of("11"), VersionIdentifier.of("21_35"), BoundaryType.CLOSED);
    // act
    ObjectMapper mapper = JsonMapping.create();
    ToolDependency dependencyInfo = mapper.readValue(json, ToolDependency.class);
    // assert
    assertThat(dependencyInfo.tool()).isEqualTo("java");
    assertThat(dependencyInfo.versionRange()).isEqualTo(expectedVersionRange);
  }

}
