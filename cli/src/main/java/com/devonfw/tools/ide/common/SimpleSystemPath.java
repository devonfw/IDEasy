package com.devonfw.tools.ide.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Represents the PATH variable in a structured way. Similar to {@link SystemPath} but much simper: It just tokenizes the PATH into a {@link java.util.List} of
 * {@link String}s.
 */
public class SimpleSystemPath {

  private final char separator;

  private final List<String> entries;

  private SimpleSystemPath(char separator, List<String> entries) {

    super();
    this.separator = separator;
    this.entries = entries;
  }

  /**
   * @return the entries of this PATH as a mutable {@link List}.
   */
  public List<String> getEntries() {

    return this.entries;
  }

  /**
   * Remove all entries from this PATH that match the given {@link Predicate}.
   *
   * @param filter the {@link Predicate} {@link Predicate#test(Object) deciding} what to filter and remove.
   */
  public void removeEntries(Predicate<String> filter) {

    Iterator<String> iterator = this.entries.iterator();
    while (iterator.hasNext()) {
      String entry = iterator.next();
      if (filter.test(entry)) {
        iterator.remove();
      }
    }
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String entry : this.entries) {
      if (first) {
        first = false;
      } else {
        sb.append(this.separator);
      }
      sb.append(entry);
    }
    return sb.toString();
  }

  /**
   * @param path the entire PATH as {@link String},
   * @param separator the separator character.
   * @return the {@link SimpleSystemPath}.
   */
  public static SimpleSystemPath of(String path, char separator) {

    List<String> entries = new ArrayList<>();
    int start = 0;
    int len = path.length();
    while (start < len) {
      int end = path.indexOf(separator, start);
      if (end < 0) {
        end = len;
      }
      entries.add(path.substring(start, end));
      start = end + 1;
    }
    return new SimpleSystemPath(separator, entries);
  }
}
