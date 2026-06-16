package com.devonfw.tools.ide.os;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.AbstractIdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Tests for {@link WindowsHelperImpl}.
 */
class WindowsHelperImplTest extends AbstractIdeContextTest {

  private static final String TEST_APP_NAME = "TestApp";

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

  /**
   * Test if WindowsAppInstallation can be properly retrieved from registry mock
   */
  @Test
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

  /**
   * Test if runReg failure makes getRegistryValue return null
   */
  @Test
  void testGetRegistryValueReturnsNullWhenRunRegFails() {
    AbstractIdeTestContext context = new IdeTestContext();
    WindowsHelperImpl helper = new WindowsHelperImpl(context) {
      @Override
      protected List<String> runReg(String... args) {
        return null;
      }
    };
    String value = helper.getRegistryValue("HKCU\\Environment", "ANY");

    assertThat(value).isNull();
  }

  /**
   * Test if getAppInstallationFromRegistry returns partial installation when some keys missing
   */
  @Test
  void testGetAppInstallationFromRegistryWithPartialFields() {
    final String app = "TestApp";
    AbstractIdeTestContext context = new IdeTestContext();
    WindowsHelperImpl helper = new WindowsHelperImpl(context) {
      @Override
      protected List<String> runReg(String... args) {
        // search call
        if (args.length >= 5 && "/f".equalsIgnoreCase(args[3])) {
          return List.of("HKEY_LOCAL_MACHINE\\SOFTWARE\\...\\Uninstall\\TestApp", "    DisplayName    REG_SZ    TestApp");
        }

        // query exact 'uninstall' key: only version + install location
        if (args.length >= 2 && args[0].equalsIgnoreCase("query") && args[1].endsWith("\\Uninstall\\TestApp")) {
          return List.of(
              "HKEY_LOCAL_MACHINE\\SOFTWARE\\...\\Uninstall\\TestApp",
              "    DisplayVersion    REG_SZ    2.0.0",
              "    InstallLocation    REG_SZ    C:\\Program Files\\TestApp"
          );
        }
        return List.of();
      }
    };

    WindowsAppInstallation inst = helper.getAppInstallationFromRegistry(app);
    assertThat(inst).isNotNull();
    assertThat(inst.version()).isEqualTo("2.0.0");
    assertThat(inst.installLocation()).isEqualTo("C:\\Program Files\\TestApp");
    assertThat(inst.icon()).isNull();
    assertThat(inst.uninstallString()).isNull();
  }

  /**
   * Test if getAppInstallationFromRegistry returns null when no uninstall key is found
   */
  @Test
  void testGetAppInstallationFromRegistryReturnsNullWhenNoKeyFound() {
    AbstractIdeTestContext context = new IdeTestContext();
    WindowsHelperImpl helper = new WindowsHelperImpl(context) {
      @Override
      protected List<String> runReg(String... args) {
        // simulate search returning empty -> no HKEY_ line
        return List.of();
      }
    };

    assertThat(helper.getAppInstallationFromRegistry("NoSuchApp")).isNull();
  }

  /**
   * Test if getAppInstallationFromRegistry returns null when registry query returns null
   */
  @Test
  void testGetAppInstallationFromRegistryWhenQueryReturnsNull() {
    AbstractIdeTestContext context = new IdeTestContext();
    WindowsHelperImpl helper = new WindowsHelperImpl(context) {
      @Override
      protected List<String> runReg(String... args) {
        // simulate search returning the 'uninstall' key
        if (args.length >= 5 && "/f".equalsIgnoreCase(args[3])) {
          return List.of("HKEY_LOCAL_MACHINE\\SOFTWARE\\...\\Uninstall\\TestApp", "    DisplayName    REG_SZ    TestApp");
        }

        // simulate reg query failure for the exact 'uninstall' key
        if (args.length >= 2 && args[0].equalsIgnoreCase("query") && args[1].endsWith("\\Uninstall\\TestApp")) {
          return null;
        }

        return List.of();
      }
    };

    assertThat(helper.getAppInstallationFromRegistry("TestApp")).isNull();
  }

  /**
   * Test if getAppInstallationFromRegistry handles empty registry values
   */
  @Test
  void testGetAppInstallationFromRegistryWithEmptyValues() {
    AbstractIdeTestContext context = new IdeTestContext();
    WindowsHelperImpl helper = new WindowsHelperImpl(context) {
      @Override
      protected List<String> runReg(String... args) {
        if (args.length >= 5 && "/f".equalsIgnoreCase(args[3])) {
          return List.of("HKEY_LOCAL_MACHINE\\...\\Uninstall\\TestApp", "    DisplayName    REG_SZ    TestApp");
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("query")) {
          return List.of(
              "HKEY_LOCAL_MACHINE\\...\\Uninstall\\TestApp",
              "    DisplayVersion    REG_SZ    ",
              "    DisplayIcon    REG_SZ    "
          );
        }

        return List.of();
      }
    };

    WindowsAppInstallation inst = helper.getAppInstallationFromRegistry("TestApp");
    assertThat(inst).isNotNull();
    assertThat(inst.version()).isEqualTo("");
    assertThat(inst.icon()).isEqualTo("");
  }

  /**
   * Test if getAppInstallationFromRegistry returns null when no uninstall key found
   */
  @Test
  void testGetAppInstallationFromRegistryNoUninstallKey() {
    AbstractIdeTestContext context = new IdeTestContext();
    WindowsHelperImpl helper = new WindowsHelperImpl(context) {
      @Override
      protected List<String> runReg(String... args) {
        // search returns no HKEY_ lines
        if (args.length >= 5 && "/f".equalsIgnoreCase(args[3])) {
          return List.of("no matches here");
        }
        return List.of();
      }
    };

    assertThat(helper.getAppInstallationFromRegistry("NoApp")).isNull();
  }

  /**
   * Test if getUserEnvironmentValue queries HKCU\\Environment and returns value
   */
  @Test
  void testGetUserEnvironmentValueQueriesHKCU() {
    AbstractIdeTestContext context = new IdeTestContext();
    final String[] captured = new String[1];

    WindowsHelperImpl helper = new WindowsHelperImpl(context) {
      @Override
      protected List<String> runReg(String... args) {
        captured[0] = String.join(" ", args);

        // simulate reg output for HKCU\Environment PATH
        return List.of("", "HKEY_CURRENT_USER\\Environment", "    PATH    REG_SZ    C:\\some\\path;");
      }
    };

    String path = helper.getUserEnvironmentValue("PATH");
    assertThat(path).isEqualTo("C:\\some\\path;");
    assertThat(captured[0]).isEqualTo("query HKCU\\Environment /v PATH");
  }
}
