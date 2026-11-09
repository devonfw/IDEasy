package com.devonfw.tools.ide.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Test of {@link StringProperty}.
 */
class StringPropertyTest {

  @Test
  void testNullSingleValue() {
    // act
    StringProperty property = new StringProperty("myname", false, "myalias");
    // assert
    assertThat(property.getName()).isEqualTo("myname");
    assertThat(property.getAlias()).isEqualTo("myalias");
    assertThat(property.isRequired()).isEqualTo(false);
    assertThat(property.getValue()).isNull();
    assertThat(property.getValueCount()).isZero();
    property.setValue(null);
    assertThat(property.getValue()).isNull();
    assertThat(property.getValueCount()).isOne();
    assertThat(property.getValueOrDefault("mydefault")).isEqualTo("mydefault");
    assertThrows(IllegalStateException.class, () -> property.addValue("myvalue"));
  }

  @Test
  void testNullMultiValue() {
    // act
    StringProperty property = new StringProperty("myname", true, true, "myalias");
    // assert
    assertThat(property.getName()).isEqualTo("myname");
    assertThat(property.getAlias()).isEqualTo("myalias");
    assertThat(property.isRequired()).isEqualTo(true);
    assertThat(property.getValue()).isNull();
    property.setValue(null);
    assertThat(property.getValue()).isNull();
    assertThat(property.getValueCount()).isOne();
    assertThat(property.getValueOrDefault("mydefault")).isEqualTo("mydefault");
    property.addValue("myvalue");
    assertThat(property.getValueCount()).isEqualTo(2);
    assertThat(property.getValue(0)).isNull();
    assertThat(property.getValue(1)).isEqualTo("myvalue");

  }
}
