package com.devonfw.tools.ide.cache;

import java.util.function.Supplier;

/**
 * A cached value that allows a good balance between reducing computing overhead and still providing accuracy.
 *
 * @param <T> type of the {@link #get() value}.
 */
public class CachedValue<T> implements Supplier<T> {

  /** Default value for {@link #getRetention() retention}. */
  public static final long DEFAULT_RETENTION = 20 * 1000; // 20 seconds

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
   * @param retention the {@link #getRetention() retention}.
   */
  public CachedValue(Supplier<T> supplier, long retention) {

    super();
    this.supplier = supplier;
    this.retention = retention;
  }

  /**
   * @return the retention time as the duration in milliseconds when the {@link CachedValue} expires and its {@link #get() value} gets recomputed.
   */
  public long getRetention() {

    return this.retention;
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
   * Explicitly set the cached value by-passing its internal computation. Only use this operation with care.
   *
   * @param value the explicit {@link #get() value} to set.
   */
  public void set(T value) {
    this.value = value;
    this.timestamp = System.currentTimeMillis();
  }

  /**
   * Invalidates a potentially cached value.
   */
  public void invalidate() {

    this.timestamp = 0;
    this.value = null;
  }
}
