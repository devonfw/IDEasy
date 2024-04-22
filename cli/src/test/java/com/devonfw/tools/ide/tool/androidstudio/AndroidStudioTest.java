package com.devonfw.tools.ide.tool.androidstudio;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class AndroidStudioTest extends AbstractIdeContextTest {

  private static final String PROJECT_ANDROID_STUDIO = "android-studio";

  private final IdeTestContext context = newContext(PROJECT_ANDROID_STUDIO);

  @ParameterizedTest
  @ValueSource(strings = { "windows", "mac", "linux" })
  public void testAndroidStudioInstall(String os) {
    // arrange
    SystemInfo systemInfo = SystemInfoMock.of(os);
    context.setSystemInfo(systemInfo);
    IdeTestContext context = newContext(PROJECT_ANDROID_STUDIO);
    AndroidStudio commandlet = new AndroidStudio(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {
    // install - java
    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();

    // commandlet - android-studio
    if (context.getSystemInfo().isWindows()) {
      assertThat(context.getSoftwarePath().resolve("android-studio/bin/studio")).doesNotExist();
    } else {
      assertThat(context.getSoftwarePath().resolve("android-studio/bin/studio")).exists().isSymbolicLink();
    }
    assertThat(context.getSoftwarePath().resolve("android-studio/.ide.software.version")).exists().hasContent("2024.1.1.1");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed java in version 17.0.10_7");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed android-studio in version 2024.1.1.1");
  }

}
