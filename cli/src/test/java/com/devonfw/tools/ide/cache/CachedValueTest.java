package com.devonfw.tools.ide.cache;

import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link CachedValue}.
 */
public class CachedValueTest extends Assertions {

  /**
   * Test that {@link CachedValue#get()} updates only if retention expired or invalidated.
   */
  @Test
  public void testFlow() throws Exception {

    // arrange
    Sequence sequence = new Sequence();
    CachedValue<Integer> cachedValue = new CachedValue<>(sequence, 1000);
    sequence.increment();
    // act
    assertThat(cachedValue.get()).isEqualTo(1);
    assertThat(cachedValue.get()).isEqualTo(1);
    sequence.increment();
    assertThat(cachedValue.get()).isEqualTo(1);
    Thread.sleep(2000);
    assertThat(cachedValue.get()).isEqualTo(2);
    sequence.increment();
    assertThat(cachedValue.get()).isEqualTo(2);
    cachedValue.invalidate();
    assertThat(cachedValue.get()).isEqualTo(3);
  }

  private static class Sequence implements Supplier<Integer> {

    private int value;

    @Override
    public Integer get() {

      return this.value;
    }

    public void increment() {
      this.value++;
    }
  }

}
