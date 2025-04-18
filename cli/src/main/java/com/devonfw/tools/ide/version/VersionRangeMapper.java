package com.devonfw.tools.ide.version;

import java.util.HashMap;
import java.util.Map;

public class VersionRangeMapper {

  public static Map<String, Object> serializeVersionRange(VersionRange range) {
    Map<String, Object> json = new HashMap<>();

    VersionIdentifier min = range.getMin();
    VersionIdentifier max = range.getMax();

    json.put("min", min != null ? min.toString() : null);
    json.put("max", max != null ? max.toString() : null);
    json.put("boundaryType", range.getBoundaryType().toString());

    return json;
  }
}
