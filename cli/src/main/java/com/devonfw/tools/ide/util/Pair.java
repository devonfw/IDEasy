package com.devonfw.tools.ide.util;

public class Pair<K, V> {
  private final K first;
  private final V second;

  public Pair(K first, V second) {
    this.first = first;
    this.second = second;
  }

  public K getFirst() {
    return first;
  }

  public V getSecond() {
    return second;
  }

  public static <K, V> Pair<K, V> of(K first, V second) {
    return new Pair<>(first, second);
  }
}
