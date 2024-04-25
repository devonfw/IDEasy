package com.devonfw.tools.ide.tool.androidstudio;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
    if (this.context.getSystemInfo().isMac()) {
      assertLogMessage(this.context, IdeLogLevel.INFO,
          ANDROID_STUDIO + " mac -na " + commandlet.getToolPath().resolve("Contents/MacOS/studio") + " --args " + this.context.getWorkspacePath());
    } else if (this.context.getSystemInfo().isLinux()) {
      assertLogMessage(this.context, IdeLogLevel.INFO, ANDROID_STUDIO + " linux " + this.context.getWorkspacePath());
    } else if (this.context.getSystemInfo().isWindows()) {
      assertLogMessage(this.context, IdeLogLevel.INFO, ANDROID_STUDIO + " windows " + this.context.getWorkspacePath());
    }

    assertLogMessage(this.context, IdeLogLevel.SUCCESS, "Running Android Studio successfully.");
    checkInstallation(this.context);
  }

  private void checkInstallation(IdeTestContext context) {
    // commandlet - android-studio
    assertThat(context.getSoftwarePath().resolve("android-studio/.ide.software.version")).exists().hasContent("2024.1.1.1");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed android-studio in version 2024.1.1.1");
  }

}
