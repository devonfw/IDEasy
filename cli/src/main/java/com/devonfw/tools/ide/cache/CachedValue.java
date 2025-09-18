package com.devonfw.tools.ide.cache;

import java.util.function.Supplier;

/**
 * A cached value that allows a good balance between reducing computing overhead and still providing accuracy.
 *
 * @param <T> type of the {@link #get() value}.
 */
public class CachedValue<T> implements Supplier<T> {

  private static final long DEFAULT_RETENTION = 20 * 1000; // 20 seconds

  private final Supplier<T> supplier;

  private final long retention;

  private long timestamp;

  private T value;

  /**
   * The constructor.
   *
   * @param supplier the {@link Supplier} function to compute the actual value.
   */
  public CachedValue(Supplier<T> supplier) {

    this(supplier, DEFAULT_RETENTION);
  }

  /**
   * The constructor.
   *
   * @param supplier the {@link Supplier} function to compute the actual value.
   */
  public CachedValue(Supplier<T> supplier, long retention) {

    super();
    this.supplier = supplier;
    this.retention = retention;
  }

  @Override
  public T get() {

    long now = System.currentTimeMillis();
    if ((now - this.timestamp) > retention) {
      this.value = this.supplier.get();
      this.timestamp = now;
    }
    return this.value;
  }

  /**
   * Invalidates a potentially cached value.
   */
  public void invalidate() {

    this.timestamp = 0;
    this.value = null;
  }
}
