package com.devonfw.tools.ide.tool;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogEntry;

/**
 * Test of {@link LocalToolCommandlet}.
 */
public class LocalToolCommandletTest extends AbstractIdeContextTest {

  /**
   * Dummy commandlet extending {@link LocalToolCommandlet} for testing.
   */
  public static class LocalToolDummyCommandlet extends LocalToolCommandlet {

    LocalToolDummyCommandlet(IdeContext context) {

      super(context, "dummy", Set.of(Tag.TEST));
    }
  }

  /**
   * Test {@link LocalToolCommandlet#getValidInstalledSoftwareRepoPath(Path, Path)} with a long installation path, as commonly encountered on macOS systems.
   */
  @Test
  public void testGetValidInstalledSoftwareRepoPathWithLongPath() {
    // arrange
    Path installPath = Path.of("/projects/_ide/software/default/java/java/21.0.7_6/Contents/Resources/app/");
    Path softwareRepoPath = Path.of("/projects/_ide/software/");
    Path expectedResultPath = Path.of("/projects/_ide/software/default/java/java/21.0.7_6/");
    IdeTestContext context = newContext(PROJECT_BASIC);
    LocalToolDummyCommandlet localToolCommandlet = new LocalToolDummyCommandlet(context);

    // act
    Path resultPath = localToolCommandlet.getValidInstalledSoftwareRepoPath(installPath, softwareRepoPath);

    // assert
    assertThat(resultPath).isEqualTo(expectedResultPath);
  }

  /**
   * Test {@link LocalToolCommandlet#getValidInstalledSoftwareRepoPath(Path, Path)} with a valid installation path.
   */
  @Test
  public void testGetValidInstalledSoftwareRepoPathWithValidPath() {
    // arrange
    Path installPath = Path.of("/projects/_ide/software/default/java/java/21.0.7_6/");
    Path softwareRepoPath = Path.of("/projects/_ide/software/");
    IdeTestContext context = newContext(PROJECT_BASIC);
    LocalToolDummyCommandlet localToolCommandlet = new LocalToolDummyCommandlet(context);

    // act
    Path resultPath = localToolCommandlet.getValidInstalledSoftwareRepoPath(installPath, softwareRepoPath);

    // assert
    assertThat(resultPath).isEqualTo(installPath);
  }

  /**
   * Test {@link LocalToolCommandlet#getValidInstalledSoftwareRepoPath(Path, Path)} with an installation path that is too short.
   */
  @Test
  public void testGetValidInstalledSoftwareRepoPathWithShortPath() {
    // arrange
    Path installPath = Path.of("/projects/_ide/software/default/java/java/");
    Path softwareRepoPath = Path.of("/projects/_ide/software/");
    IdeTestContext context = newContext(PROJECT_BASIC);
    LocalToolDummyCommandlet localToolCommandlet = new LocalToolDummyCommandlet(context);

    // act
    Path resultPath = localToolCommandlet.getValidInstalledSoftwareRepoPath(installPath, softwareRepoPath);

    // assert
    assertThat(resultPath).isNull();
    assertThat(context).log().hasEntries(IdeLogEntry.ofWarning("The installation path is faulty " + installPath + "."));
  }

  /**
   * Test {@link LocalToolCommandlet#getValidInstalledSoftwareRepoPath(Path, Path)} with an installation path that completely differs from the software
   * repository path.
   */
  @Test
  public void testGetValidInstalledSoftwareRepoPathWithWrongPath() {
    // arrange
    Path installPath = Path.of("/projects/IDEasy/workspaces/main/IDEasy/software/java/");
    Path softwareRepoPath = Path.of("/projects/_ide/software/");
    IdeTestContext context = newContext(PROJECT_BASIC);
    LocalToolDummyCommandlet localToolCommandlet = new LocalToolDummyCommandlet(context);

    // act
    Path resultPath = localToolCommandlet.getValidInstalledSoftwareRepoPath(installPath, softwareRepoPath);

    // assert
    assertThat(resultPath).isNull();
    assertThat(context).log().hasEntries(IdeLogEntry.ofWarning("The installation path is not located within the software repository " + installPath + "."));
  }
}
