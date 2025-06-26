package com.devonfw.tools.ide.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility class for operations with maps.
 */
public class MapUtil {

  /**
   * Creates a {@link HashMap} with the given {@code keys} and {@code values} which are passed as {@link List lists}. The map is populated by iterating through
   * both lists simultaneously until one of the list is exhausted.
   */
  public static <K, V> Map<K, V> createMapfromLists(List<K> keys, List<V> values) {

    Map<K, V> resultMap = new HashMap<>();

    // Create iterators for both lists
    Iterator<K> keysIterator = keys.iterator();
    Iterator<V> valuesIterator = values.iterator();

    // Iterate through both iterators simultaneously
    while (keysIterator.hasNext() && valuesIterator.hasNext()) {
      K key = keysIterator.next();
      V value = valuesIterator.next();
      resultMap.put(key, value);
    }

    return resultMap;
  }
}
