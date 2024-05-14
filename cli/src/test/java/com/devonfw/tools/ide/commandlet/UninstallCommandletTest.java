package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test of {@link UninstallCommandlet}.
 */
public class UninstallCommandletTest extends AbstractIdeContextTest {

  private IdeTestContext context = newContext(PROJECT_BASIC);

  private static final String ECLIPSE = "eclipse";

  private static final String AWS = "aws";

  private static final String NPM = "npm";

  private static final String MVN = "mvn";

  /**
   * Test of {@link UninstallCommandlet} run.
   */
  @Test
  public void testUninstallCommandletRun_WithExistingCommandlets() {

    // arrange
    UninstallCommandlet uninstallCommandlet = context.getCommandletManager().getCommandlet(UninstallCommandlet.class);
    uninstallCommandlet.arguments.setValue(List.of(NPM, MVN));

    // act
    uninstallCommandlet.run();

    // assert
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully uninstalled " + NPM);
    assertThat(Files.notExists(context.getSoftwarePath().resolve(NPM)));

    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully uninstalled " + MVN);
    assertThat(Files.notExists(context.getSoftwarePath().resolve(MVN)));
  }

  @Test
  public void testUninstallCommandletRun_WithNonExistingCommandlets() {

    // arrange
    UninstallCommandlet uninstallCommandlet = context.getCommandletManager().getCommandlet(UninstallCommandlet.class);
    uninstallCommandlet.arguments.setValue(List.of(ECLIPSE, AWS));

    // act
    uninstallCommandlet.run();

    // assert
    assertLogMessage(context, IdeLogLevel.WARNING, "An installed version of " + ECLIPSE + " does not exist");
    assertThat(Files.notExists(context.getSoftwarePath().resolve(ECLIPSE)));

    assertLogMessage(context, IdeLogLevel.WARNING, "An installed version of " + AWS + " does not exist");
    assertThat(Files.notExists(context.getSoftwarePath().resolve(AWS)));
  }

  @Test
  public void testUninstallCommandletRun_UninstallingValidToolThrowsException() {

    // arrange
    FileAccessImpl mockFileAccess = mock(FileAccessImpl.class);
    IdeTestContext mockContext = mock(IdeTestContext.class);
    Path softwarePath = context.getSoftwarePath();

    when(mockContext.getFileAccess()).thenReturn(mockFileAccess);

    when(mockContext.getCommandletManager()).thenReturn(new CommandletManagerImpl(mockContext));
    when(mockContext.getSoftwarePath()).thenReturn(softwarePath);

    doThrow(new IllegalStateException("Couldn't uninstall")).when(mockFileAccess).delete(softwarePath);

    UninstallCommandlet uninstallCommandlet = mockContext.getCommandletManager().getCommandlet(UninstallCommandlet.class);
    uninstallCommandlet.arguments.setValue(List.of(NPM));

    // assert
    assertThrows(IllegalStateException.class, () -> {
      uninstallCommandlet.run();
    });
  }

  @Test
  public void testUninstallCommandletRun_UninstallingInvalidToolThrowsException() {

    // arrange

    UninstallCommandlet uninstallCommandlet = context.getCommandletManager().getCommandlet(UninstallCommandlet.class);
    uninstallCommandlet.arguments.setValue(List.of("xxx"));
    String expectedMessage = "The commandlet xxx is not a ToolCommandlet!";

    // act
    try {
      uninstallCommandlet.run();
    } catch (Exception e) {
      // assert
      assertThat(e).hasMessageContaining(expectedMessage);
      assertLogMessage(context, IdeLogLevel.ERROR, expectedMessage);
    }
  }
}
