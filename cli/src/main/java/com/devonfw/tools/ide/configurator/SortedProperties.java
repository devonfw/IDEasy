package com.devonfw.tools.ide.configurator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * {@link Properties} that are sorted ascending by their keys.
 *
 * @author trippl
 * @since 3.0.0
 */
public class SortedProperties extends Properties {

  private static final long serialVersionUID = 1L;

  private static final EntryComarator ENTRY_COMPARATOR = new EntryComarator();

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public synchronized Enumeration keys() {

    Enumeration keysEnum = super.keys();
    List keyList = new ArrayList();
    while (keysEnum.hasMoreElements()) {
      keyList.add(keysEnum.nextElement());
    }
    Collections.sort(keyList);
    return Collections.enumeration(keyList);
  }

  @Override
  public Set<Map.Entry<Object, Object>> entrySet() {

    SortedSet<Map.Entry<Object, Object>> set = new TreeSet<>(ENTRY_COMPARATOR);
    set.addAll(super.entrySet());
    return set;
  }

  @Override
  public Set<Object> keySet() {

    SortedSet<Object> set = new TreeSet<>(super.keySet());
    return set;
  }

  private static class EntryComarator implements Comparator<Map.Entry<Object, Object>> {

    @Override
    public int compare(Map.Entry<Object, Object> o1, Map.Entry<Object, Object> o2) {

      if (o1 == o2) {
        return 0;
      } else if (o1 == null) {
        return -1;
      } else if (o2 == null) {
        return 1;
      } else {
        return compareKeys(o1.getKey(), o2.getKey());
      }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private int compareKeys(Object k1, Object k2) {

      if (k1 == k2) {
        return 0;
      } else if (k1 == null) {
        return -1;
      } else if (k2 == null) {
        return 1;
      } else {
        Comparable c1 = (Comparable) k1;
        Comparable c2 = (Comparable) k2;
        return c1.compareTo(c2);
      }
    }
  }

}
