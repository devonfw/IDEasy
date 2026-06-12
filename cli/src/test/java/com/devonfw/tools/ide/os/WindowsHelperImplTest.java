package com.devonfw.tools.ide.os;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.AbstractIdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Tests for {@link WindowsHelperImpl}.
 */
class WindowsHelperImplTest extends AbstractIdeContextTest {

  private static final String TEST_APP_NAME = "TestApp";

  private static final String UNKNOWN_TEST_APP_NAME = "UnknownApp";

  /**
   * Tests if the USER_PATH registry entry can be parsed properly.
   */
  @Test
  void testWindowsHelperParseRegString() {
    // arrange
    AbstractIdeTestContext context = new IdeTestContext();
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
    AbstractIdeTestContext context = new IdeTestContext();
    WindowsHelperImpl helper = new WindowsHelperImpl(context);
    List<String> output = new ArrayList<>();
    // act
    String regString = helper.retrieveRegString("PATH", output);
    // assert
    assertThat(regString).isNull();
  }

  @Test
  @DisplayName("Test if WindowsAppInstallation can be properly retrieved from registry mock")
  void testRegistryLookupReturnsCorrectInstallationInfo() {
    WindowsAppInstallation result = getWindowsAppInstallation();

    assertThat(result).isNotNull();
    assertThat(result.version()).isEqualTo("1.1.1");
    assertThat(result.icon()).isEqualTo("C:\\Program Files\\TestApp\\testapp.exe,0");
    assertThat(result.uninstallString()).isEqualTo("\"C:\\Program Files\\TestApp\\uninstall.exe\"");
    assertThat(result.installLocation()).isEqualTo("C:\\Program Files\\TestApp");
  }

  /**
   * Helper method to set up the WindowsHelperMock with a test app installation and retrieve it.
   *
   * @return the WindowsAppInstallation from the mock registry
   */
  private static WindowsAppInstallation getWindowsAppInstallation() {
    AbstractIdeTestContext context = new IdeTestContext();
    WindowsHelperMock helper = (WindowsHelperMock) context.getWindowsHelper();
    WindowsAppInstallation installation = new WindowsAppInstallation(
        "1.1.1",
        "C:\\Program Files\\TestApp\\testapp.exe,0",
        "\"C:\\Program Files\\TestApp\\uninstall.exe\"",
        "C:\\Program Files\\TestApp"
    );
    helper.setAppInstallationFromRegistry(TEST_APP_NAME, installation);

    return helper.getAppInstallationFromRegistry(TEST_APP_NAME);
  }

  /*
   *//**
   * Tests if correct keys can be found in registry output for app name filter.
   *//*
  @Test
  void testRegistryLookupReturnsCorrectEntryIfFound() {
    // arrange
    AbstractIdeTestContext context = new IdeTestContext();
    WindowsHelperImpl helper = new WindowsHelperImplTestable(context);

    // act
    String displayVersion = helper.getDisplayVersionFromRegistry(TEST_APP_NAME);
    String icon = helper.getDisplayIconFromRegistry(TEST_APP_NAME);
    String uninstall = helper.getUninstallStringFromRegistry(TEST_APP_NAME);
    String location = helper.getInstallLocationFromRegistry(TEST_APP_NAME);

    // assert
    assertThat(displayVersion).isEqualTo("1.1.1");
    assertThat(icon).isEqualTo("C:\\Program Files\\TestApp\\testapp.exe,0");
    assertThat(uninstall).isEqualTo("\"C:\\Program Files\\TestApp\\uninstall.exe\"");
    assertThat(location).isEqualTo("C:\\Program Files\\TestApp");
  }

  *//**
   * Tests if registry lookup return nulls on unknown app name filter.
   *//*
  @Test
  void testRegistryLookupReturnsNullIfNotFound() {
    // arrange
    AbstractIdeTestContext context = new IdeTestContext();
    WindowsHelperImpl helper = new WindowsHelperImplTestable(context);

    // act
    String displayVersion = helper.getDisplayVersionFromRegistry(UNKNOWN_TEST_APP_NAME);
    String icon = helper.getDisplayIconFromRegistry(UNKNOWN_TEST_APP_NAME);
    String uninstall = helper.getUninstallStringFromRegistry(UNKNOWN_TEST_APP_NAME);
    String location = helper.getInstallLocationFromRegistry(UNKNOWN_TEST_APP_NAME);

    // assert
    assertThat(displayVersion).isNull();
    assertThat(icon).isNull();
    assertThat(uninstall).isNull();
    assertThat(location).isNull();
  }*/
}
