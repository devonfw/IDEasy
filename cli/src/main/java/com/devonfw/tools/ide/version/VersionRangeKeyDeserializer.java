package com.devonfw.tools.ide.version;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

/**
 * {@link KeyDeserializer} for {@link VersionRange}.
 */
public class VersionRangeKeyDeserializer extends KeyDeserializer {

  @Override
  public Object deserializeKey(String key, DeserializationContext context) throws IOException {

    return VersionRange.of(key);
  }

}
