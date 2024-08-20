package com.devonfw.tools.ide.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;

class NumberPropertyTest {

  @Test
  public void testParse() {
    IdeContext context = IdeTestContextMock.get();
    NumberProperty numberProp = new NumberProperty("", false, "");

    assertThat(numberProp.parse("12345", context)).isEqualTo(12345);
    assertThrows(IllegalArgumentException.class, () -> numberProp.parse(null, context));
    assertThrows(IllegalArgumentException.class, () -> numberProp.parse("notanumber", context));

  }


}
