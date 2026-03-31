package com.devonfw.tools.ide.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link BooleanProperty}.
 */
class BooleanPropertyTest {

  @Test
  void testParse() {
    IdeContext context = new IdeTestContext();
    BooleanProperty boolProp = new BooleanProperty("name", false, "alias");

    assertThat(boolProp.parse("true", context)).isTrue();
    assertThat(boolProp.parse("True", context)).isTrue();
    assertThat(boolProp.parse("TRUE", context)).isTrue();
    assertThat(boolProp.parse("yes", context)).isTrue();
    assertThat(boolProp.parse("YES", context)).isTrue();

    assertThat(boolProp.parse("false", context)).isFalse();
    assertThat(boolProp.parse("False", context)).isFalse();
    assertThat(boolProp.parse("FALSE", context)).isFalse();
    assertThat(boolProp.parse("no", context)).isFalse();
    assertThat(boolProp.parse("NO", context)).isFalse();

    assertThrows(IllegalArgumentException.class, () -> boolProp.parse("notabooleanvalue", context));
    assertThrows(IllegalArgumentException.class, () -> boolProp.parse(null, context));
  }
}
