package com.devonfw.tools.ide.tool.androidstudio;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test class for {@link AndroidStudio Android Studio IDE} tests.
 */
public class AndroidStudioTest extends AbstractIdeContextTest {

  private static final String ANDROID_STUDIO = "android-studio";

  private final IdeTestContext context = newContext(ANDROID_STUDIO);

  /**
   * Tests if {@link AndroidStudio Android Studio IDE} can be installed.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testAndroidStudioInstall(String os) {
    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    AndroidStudio commandlet = new AndroidStudio(this.context);

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);
  }

  /**
   * Tests if {@link AndroidStudio Android Studio IDE} can be run.
   *
   * @param os String of the OS to use.
   */
  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testAndroidStudioRun(String os) {
    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    this.context.setSystemInfo(systemInfo);
    AndroidStudio commandlet = new AndroidStudio(this.context);

    // act
    commandlet.run();

    // assert
    assertThat(this.context).logAtInfo().hasMessage(ANDROID_STUDIO + " " + this.context.getSystemInfo().getOs() + " " + this.context.getWorkspacePath());

    checkInstallation(this.context);
  }

  private void checkInstallation(IdeTestContext context) {
    // commandlet - android-studio
    assertThat(context.getSoftwarePath().resolve("android-studio/.ide.software.version")).exists().hasContent("2024.1.1.1");
    assertThat(context.getVariables().get("STUDIO_PROPERTIES")).isEqualTo(context.getWorkspacePath().resolve("studio.properties").toString());
    assertThat(context).logAtSuccess().hasMessage("Successfully installed android-studio in version 2024.1.1.1");
  }

}
