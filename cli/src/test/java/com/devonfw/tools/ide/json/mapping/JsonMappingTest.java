package com.devonfw.tools.ide.json.mapping;

import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test of {@link JsonMapping}.
 */
public class JsonMappingTest extends Assertions {

  private static final String INSTANT_VALUE_STRING = "2001-12-31T23:59:59.987654321Z";

  private static final String INSTANT_VALUE_JSON = '"' + INSTANT_VALUE_STRING + '"';

  private static final Instant INSTANT_VALUE = Instant.parse(INSTANT_VALUE_STRING);

  /**
   * Test of {@link JsonMapping#create()} reading an {@link Instant} value.
   *
   * @throws Exception in case of an error.
   */
  @Test
  public void testReadInstant() throws Exception {

    // given
    String value = INSTANT_VALUE_JSON;
    // when
    ObjectMapper mapper = JsonMapping.create();
    Instant instant = mapper.readValue(value, Instant.class);
    // then
    assertThat(instant).isEqualTo(INSTANT_VALUE);
  }

  /**
   * Test of {@link JsonMapping#create()} writing an {@link Instant} value.
   *
   * @throws Exception in case of an error.
   */
  @Test
  public void testWriteInstant() throws Exception {

    // given
    Instant value = INSTANT_VALUE;
    // when
    ObjectMapper mapper = JsonMapping.create();
    String json = mapper.writeValueAsString(value);
    // then
    assertThat(json).isEqualTo(INSTANT_VALUE_JSON);
  }

}
