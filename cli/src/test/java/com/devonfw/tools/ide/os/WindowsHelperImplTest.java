package com.devonfw.tools.ide.os;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.AbstractIdeTestContext;
import com.devonfw.tools.ide.context.IdeSlf4jContext;

/**
 * Tests for {@link WindowsHelperImpl}.
 */
class WindowsHelperImplTest extends AbstractIdeContextTest {

  /**
   * Tests if the USER_PATH registry entry can be parsed properly.
   */
  @Test
  void testWindowsHelperParseRegString() {
    // arrange
    AbstractIdeTestContext context = new IdeSlf4jContext();
    WindowsHelperImpl helper = new WindowsHelperImpl(context);
    List<String> output = new ArrayList<>();
    output.add("");
    output.add("HKEY_CURRENT_USER\\Environment");
    output.add("    PATH    REG_SZ    D:\\projects\\_ide\\installation\\bin;");
    output.add("");
    // act
    String regString = helper.retrieveRegString("PATH", output);
    // assert
    assertThat(regString).isEqualTo("D:\\projects\\_ide\\installation\\bin;");
  }

  /**
   * Tests if an empty list of outputs will result in null.
   */
  @Test
  void testWindowsHelperParseEmptyRegStringReturnsNull() {
    // arrange
    AbstractIdeTestContext context = new IdeSlf4jContext();
    WindowsHelperImpl helper = new WindowsHelperImpl(context);
    List<String> output = new ArrayList<>();
    // act
    String regString = helper.retrieveRegString("PATH", output);
    // assert
    assertThat(regString).isNull();
  }

}
