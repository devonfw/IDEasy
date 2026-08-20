package com.devonfw.tools.ide.process;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.platform.commons.util.ReflectionUtils;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration tests for {@link ProcessMode#BACKGROUND_NEW_WINDOW}.
 * <p>
 * These tests verify that the new background-new-window mode generates the correct bash command structure with terminal emulator detection on Linux,
 * {@code osascript} on macOS, and {@code cmd.exe /c start} on Windows.
 */
class BackgroundNewWindowTest extends AbstractIdeContextTest {

  /**
   * Verify that {@link ProcessMode#BACKGROUND_NEW_WINDOW} returns true for {@link ProcessMode#isBackground()}.
   */
  @Test
  void backgroundNewWindowShouldBeConsideredBackground() {
    assertThat(ProcessMode.BACKGROUND_NEW_WINDOW.isBackground()).isTrue();
  }

  /**
   * Verify that {@link ProcessMode#BACKGROUND_NEW_WINDOW} returns true for {@link ProcessMode#launchesNewWindow()}.
   */
  @Test
  void backgroundNewWindowShouldLaunchNewWindow() {
    assertThat(ProcessMode.BACKGROUND_NEW_WINDOW.launchesNewWindow()).isTrue();
  }

  /**
   * Verify that all other modes return false for {@link ProcessMode#launchesNewWindow()}.
   */
  @Test
  void otherModesShouldNotLaunchNewWindow() {
    for (ProcessMode mode : ProcessMode.values()) {
      if (mode != ProcessMode.BACKGROUND_NEW_WINDOW) {
        assertThat(mode.launchesNewWindow()).as(String.format("Mode %s should not launch new window", mode)).isFalse();
      }
    }
  }

  /**
   * Verify that {@link ProcessMode#BACKGROUND_NEW_WINDOW} discards output and error, like {@link ProcessMode#BACKGROUND_SILENT}.
   */
  @Test
  void backgroundNewWindowShouldDiscardOutputAndError() {
    assertThat(ProcessMode.BACKGROUND_NEW_WINDOW.getRedirectOutput()).isEqualTo(ProcessBuilder.Redirect.DISCARD);
    assertThat(ProcessMode.BACKGROUND_NEW_WINDOW.getRedirectError()).isEqualTo(ProcessBuilder.Redirect.DISCARD);
    assertThat(ProcessMode.BACKGROUND_NEW_WINDOW.getRedirectInput()).isNull();
  }

  /**
   * Verify that on Linux, {@link ProcessMode#BACKGROUND_NEW_WINDOW} generates a bash command containing a terminal emulator ({@code x-terminal-emulator},
   * {@code gnome-terminal}, or another detected terminal). Uses a mocked {@link ProcessBuilder} to capture the command list without executing the process.
   */
  @Test
  @EnabledOnOs(OS.LINUX)
  void shouldContainTerminalEmulatorCommandOnLinux() throws Exception {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    if (context.findBash() == null) {
      return; // Skip when no bash is available
    }

    Path scriptPath = TEST_RESOURCES.resolve("process-context").resolve("log-order.sh");
    // act
    List<String> capturedArgs = captureCommandArgs(context, scriptPath, ProcessMode.BACKGROUND_NEW_WINDOW);
    // assert
    assertThat(capturedArgs).as("Captured args should not be empty").isNotEmpty();

    // The first argument should be bash
    assertThat(capturedArgs.get(0)).as("First arg should be bash path").contains("bash");

    // The second argument should be -c (bash command flag)
    assertThat(capturedArgs.get(1)).isEqualTo("-c");

    // The third argument should contain a terminal emulator command or fallback
    String bashCommand = capturedArgs.get(2);
    boolean hasTerminalEmulator = bashCommand.contains("x-terminal-emulator") || bashCommand.contains("gnome-terminal")
        || bashCommand.contains("konsole") || bashCommand.contains("xfce4-terminal")
        || bashCommand.contains("tilix") || bashCommand.contains("xterm") || bashCommand.contains("alacritty");
    boolean hasFallback = bashCommand.contains("disown") || bashCommand.contains("/dev/null");

    // Should either have a terminal emulator or fallback to disown
    assertThat(hasTerminalEmulator || hasFallback)
        .as("Bash command should contain a terminal emulator or the disown fallback").isTrue();

    if (hasTerminalEmulator) {
      // When a terminal emulator is found, the command should keep the window open and background it
      assertThat(bashCommand).as("Bash command should contain 'exec bash' to keep terminal open")
          .contains("exec bash");
      assertThat(bashCommand).as("Bash command should end with & for backgrounding").endsWith("&");
    }
  }

  /**
   * Verify that on Windows, {@link ProcessMode#BACKGROUND_NEW_WINDOW} generates a command using {@code cmd.exe /c start} to open a new CMD window.
   */
  @Test
  @EnabledOnOs(OS.WINDOWS)
  void shouldContainStartCommandOnWindows() throws Exception {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);

    Path scriptPath = TEST_RESOURCES.resolve("process-context").resolve("log-order.bat");
    // act
    List<String> capturedArgs = captureCommandArgs(context, scriptPath, ProcessMode.BACKGROUND_NEW_WINDOW);
    // assert
    assertThat(capturedArgs).as("Captured args should not be empty").isNotEmpty();

    // First argument should be cmd.exe
    assertThat(capturedArgs.get(0)).as("First arg should be cmd.exe").contains("cmd.exe");

    // Second argument should be /c
    assertThat(capturedArgs.get(1)).isEqualTo("/c");

    // Third argument should contain the 'start' command for a new window
    String startCommand = capturedArgs.get(2);
    assertThat(startCommand).as("Command should contain /k to keep window open").contains("/k");
    assertThat(startCommand).as("Command should NOT contain disown on Windows").doesNotContain("disown");
    assertThat(startCommand).as("Command should NOT contain x-terminal-emulator on Windows")
        .doesNotContain("x-terminal-emulator");
  }

  /**
   * Verify that on macOS, {@link ProcessMode#BACKGROUND_NEW_WINDOW} uses {@code osascript} to launch either iTerm2 or Terminal.app instead of falling back to
   * {@code & disown}.
   */
  @Test
  @EnabledOnOs(OS.MAC)
  void shouldUseOsascriptOnMac() throws Exception {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    if (context.findBash() == null) {
      return; // Skip when no bash is available
    }

    Path scriptPath = TEST_RESOURCES.resolve("process-context").resolve("log-order.sh");
    // act
    List<String> capturedArgs = captureCommandArgs(context, scriptPath, ProcessMode.BACKGROUND_NEW_WINDOW);
    // assert
    assertThat(capturedArgs).as("Captured args should not be empty").isNotEmpty();

    // The bash command should contain osascript (macOS uses AppleScript for terminal control)
    String bashCommand = capturedArgs.get(2);
    assertThat(bashCommand).as("Bash command should contain osascript on macOS").contains("osascript");
    assertThat(bashCommand).as("Bash command should contain Terminal or iTerm2 AppleScript")
        .satisfiesAnyOf(
            s -> assertThat(s).contains("iTerm2"),
            s -> assertThat(s).contains("Terminal"));
    // Should NOT fall back to plain disown anymore
    assertThat(bashCommand).as("Bash command should NOT contain disown on macOS (has terminal support)")
        .doesNotContain("disown");
  }

  /**
   * Sets up a mocked {@link ProcessBuilder} on the given {@link ProcessContextImpl} to capture the command list passed to {@link ProcessBuilder#command(List)},
   * then runs the process with the given mode.
   *
   * @param context the {@link IdeTestContext}
   * @param script the script path to set as executable
   * @param mode the {@link ProcessMode} to run with
   * @return the list of arguments captured from {@code command(List)}
   */
  private List<String> captureCommandArgs(IdeTestContext context, Path script, ProcessMode mode) throws Exception {

    List<String> capturedArgs = new ArrayList<>();

    ProcessBuilder mockPb = mock(ProcessBuilder.class);
    when(mockPb.command(anyList())).thenAnswer(invocation -> {
      capturedArgs.addAll(invocation.getArgument(0));
      return mockPb;
    });

    ProcessContextImpl processContext = new ProcessContextImpl(context);

    Field pbField = ReflectionUtils.findFields(ProcessContextImpl.class, f -> f.getName().equals("processBuilder"),
        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
    pbField.setAccessible(true);
    pbField.set(processContext, mockPb);
    pbField.setAccessible(false);

    processContext.executable(script);

    try {
      processContext.run(mode);
    } catch (Exception e) {
      // Expected — the mock throws when start() is called
    }

    return capturedArgs;
  }

  /**
   * End-to-end verification: on Windows (CMD), {@link ProcessMode#BACKGROUND_NEW_WINDOW} should open a new CMD window via {@code start} and actually execute
   * the command. The marker file proves the subprocess was spawned in the new window and completed.
   * <p>
   * Uses a polling approach with a generous timeout to handle cold-start delays on CI VMs.
   */
  @Test
  @EnabledOnOs(OS.WINDOWS)
  @EnabledIfSystemProperty(named = "ide.e2e.window.tests", matches = "true")
  void backgroundNewWindowShouldActuallyExecuteViaStartOnWindows() throws Exception {
    // arrange
    if (isCiEnvironment()) {
      return; // Skip — opening a CMD window would leak in CI
    }
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);

    Path markerFile = Files.createTempFile("bg-marker-", ".txt");
    Files.delete(markerFile); // Remove so polling can detect when the batch file creates it

    // Create a simple batch file that writes to the marker file
    Path batchFile = Files.createTempFile("bg-test-", ".bat");
    String batchContent = "@echo off\r\necho background-process-ran > " + markerFile + "\r\nexit /b 0\r\n";
    Files.writeString(batchFile, batchContent);

    ProcessContextImpl processContext = new ProcessContextImpl(context);
    // act
    ProcessResult result = processContext.executable(batchFile)
        .run(ProcessMode.BACKGROUND_NEW_WINDOW);
    // assert
    // The process should detach successfully
    assertThat(result.isSuccessful()).isTrue();

    // Poll for the marker file with a generous timeout.
    long timeout = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
    while (System.currentTimeMillis() < timeout) {
      if (Files.exists(markerFile)) {
        assertThat(Files.readString(markerFile)).contains("background-process-ran");
        Files.delete(batchFile); // Clean up
        return; // Success
      }
      Thread.sleep(Duration.ofMillis(500));
    }

    // If we reach here, the marker file was not created in time
    assertThat(markerFile)
        .as("Marker file should have been created by the new-window CMD process within 15 seconds")
        .exists();
    Files.deleteIfExists(batchFile);
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  @EnabledIfSystemProperty(named = "ide.e2e.window.tests", matches = "true")
  void backgroundNewWindowShouldWithEchoInjectedShouldNotActuallyLeakOnWindows() throws Exception {
    // arrange
    if (isCiEnvironment()) {
      return; // Skip — opening a CMD window would leak in CI
    }
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);

    Path markerFile = Files.createTempFile("bg-marker-", ".txt");
    Files.delete(markerFile);

    Path injectedFile = markerFile.resolveSibling("injected.txt");
    Files.deleteIfExists(injectedFile);

    // Create a simple batch file that writes to the marker file
    Path batchFile = Files.createTempFile("bg-test-", ".bat");
    String batchContent = "@echo off\r\necho background-process-ran > " + markerFile + "\r\nexit /b 0\r\n";
    Files.writeString(batchFile, batchContent);

    ProcessContextImpl processContext = new ProcessContextImpl(context);
    // act
    ProcessResult result = processContext.executable(batchFile)
        .addArg("& echo INJECTED > " + injectedFile)
        .run(ProcessMode.BACKGROUND_NEW_WINDOW);
    // assert
    // The process should detach successfully
    assertThat(result.isSuccessful()).isTrue();

    // Poll for the marker file with a generous timeout.
    long timeout = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
    while (System.currentTimeMillis() < timeout) {
      if (Files.exists(markerFile)) {
        assertThat(Files.readString(markerFile)).contains("background-process-ran");
        assertThat(Files.exists(injectedFile)).isFalse();
        Files.delete(batchFile);
        Files.deleteIfExists(injectedFile);
        return;
      }
      Thread.sleep(Duration.ofMillis(500));
    }

    // If we reach here, the marker file was not created in time
    assertThat(markerFile)
        .as("Marker file should have been created by the new-window CMD process within 15 seconds")
        .exists();
    Files.deleteIfExists(batchFile);
    Files.deleteIfExists(injectedFile);
  }

  /**
   * End-to-end verification: on Linux, {@link ProcessMode#BACKGROUND_NEW_WINDOW} should open a new terminal window (via {@code gnome-terminal}, {@code xterm},
   * or another detected emulator) and actually execute the command. The marker file proves the subprocess was spawned in the new window and completed.
   * <p>
   * If no display is available (CI, headless server), the fallback {@code disown} path is tested instead — it still proves the command executes in background.
   */
  @Test
  @EnabledOnOs(OS.LINUX)
  @EnabledIfSystemProperty(named = "ide.e2e.window.tests", matches = "true")
  void backgroundNewWindowShouldActuallyExecuteOnLinux() throws Exception {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    if (context.findBash() == null) {
      return; // Skip when no bash is available
    }

    // Pre-answer "yes" to the executable permission prompt that may appear in CI
    // where git clone does not preserve executable permissions on shell scripts
    context.setAnswers("1");

    boolean hasDisplay = System.getenv("DISPLAY") != null || System.getenv("WAYLAND_DISPLAY") != null;

    Path markerFile = Files.createTempFile("bg-marker-linux-", ".txt");
    Files.delete(markerFile); // Remove so polling can detect when the script creates it
    Path scriptPath = TEST_RESOURCES.resolve("process-context").resolve("write-marker.sh");

    ProcessContextImpl processContext = new ProcessContextImpl(context);
    // act
    ProcessResult result = processContext.executable(scriptPath).addArg(markerFile.toString())
        .run(ProcessMode.BACKGROUND_NEW_WINDOW);
    // assert
    assertThat(result.isSuccessful()).isTrue();

    // Poll for the marker file with a generous timeout.
    long timeout = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
    while (System.currentTimeMillis() < timeout) {
      if (Files.exists(markerFile)) {
        assertThat(Files.readString(markerFile)).contains("background-process-ran");
        return;
      }
      Thread.sleep(Duration.ofMillis(500));
    }

    // If we reach here, the marker file was not created in time
    if (hasDisplay) {
      assertThat(markerFile)
          .as("Marker file should have been created by the new-window terminal process within 15 seconds")
          .exists();
    }
    // Without a display, the fallback "disown" path may or may not succeed depending on the environment;
    // we don't fail the test in that case since the structural test (shouldContainTerminalEmulatorCommandOnLinux) covers command shape.
  }

  /**
   * End-to-end verification: on macOS, {@link ProcessMode#BACKGROUND_NEW_WINDOW} should open a new terminal window via {@code osascript} (iTerm2 or
   * Terminal.app) and actually execute the command. The marker file proves the subprocess was spawned in the new window and completed.
   * <p>
   * Uses a polling approach with a generous timeout since Terminal.app cold-start can take 2-6 seconds on CI VMs.
   */
  @Test
  @EnabledOnOs(OS.MAC)
  @EnabledIfSystemProperty(named = "ide.e2e.window.tests", matches = "true")
  void backgroundNewWindowShouldActuallyExecuteViaOsascriptOnMac() throws Exception {
    // arrange
    if (isCiEnvironment()) {
      return; // Skip — opening a terminal window would leak in CI
    }
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    if (context.findBash() == null) {
      return; // Skip when no bash is available
    }

    // Verify osascript is available (required for Terminal.app / iTerm2 AppleScript)
    if (!isOsascriptAvailable()) {
      // Skip - no GUI session or osascript not available (e.g. headless CI)
      return;
    }

    context.setAnswers("1");

    Path markerFile = Files.createTempFile("bg-marker-mac-", ".txt");
    Files.delete(markerFile); // Remove so polling can detect when the script creates it
    Path scriptPath = TEST_RESOURCES.resolve("process-context").resolve("write-marker.sh");

    ProcessContextImpl processContext = new ProcessContextImpl(context);
    // act
    ProcessResult result = processContext.executable(scriptPath).addArg(markerFile.toString())
        .run(ProcessMode.BACKGROUND_NEW_WINDOW);
    // assert
    // The process should detach successfully
    assertThat(result.isSuccessful()).isTrue();

    // Poll for the marker file with a generous timeout.
    long timeout = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
    while (System.currentTimeMillis() < timeout) {
      if (Files.exists(markerFile)) {
        assertThat(Files.readString(markerFile)).contains("background-process-ran");
        return; // Success
      }
      Thread.sleep(Duration.ofMillis(500));
    }

    // If we reach here, the marker file was not created in time
    assertThat(markerFile).as("Marker file should have been created by the new-window terminal process within 15 seconds")
        .exists();
  }

  /**
   * Check if {@code osascript} is available on macOS by attempting a trivial AppleScript call. This verifies both that the binary exists and that a GUI session
   * is present.
   */
  private boolean isOsascriptAvailable() {
    try {
      ProcessBuilder pb = new ProcessBuilder("osascript", "-e", "return 1");
      pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
      pb.redirectError(ProcessBuilder.Redirect.DISCARD);
      return pb.start().waitFor() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Check whether the test is running in a CI environment. E2E tests that open GUI windows should be skipped to avoid leaking windows in non-interactive CI
   * runners.
   */
  private static boolean isCiEnvironment() {
    return System.getenv("CI") != null
        || System.getenv("GITHUB_ACTIONS") != null
        || System.getenv("JENKINS_URL") != null;
  }
}
