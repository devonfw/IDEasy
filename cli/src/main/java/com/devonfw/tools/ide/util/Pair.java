package com.devonfw.tools.ide.util;

/**
 * Simple generic pair of two values.
 * 
 * @param <K> type of {@link #getFirst() first value}.
 * @param <V> type of {@link #getSecond() second value}.
 */
public class Pair<K, V> {
  private final K first;

  private final V second;

  /**
   * The constructor.
   *
   * @param first the {@link #getFirst() first value}.
   * @param second the {@link #getSecond() second value}.
   */
  public Pair(K first, V second) {

    this.first = first;
    this.second = second;
  }

  /** @return the first value of the pair. */
  public K getFirst() {

    return first;
  }

  /** @return the second value of the pair. */
  public V getSecond() {

    return second;
  }

  /**
   * Returns a new instance of {@link Pair}.
   *
   * @param first the {@link #getFirst() first value}.
   * @param second the {@link #getSecond() second value}.
   * @return the new {@link Pair}.
   */
  public static <K, V> Pair<K, V> of(K first, V second) {

    return new Pair<>(first, second);
  }
}
