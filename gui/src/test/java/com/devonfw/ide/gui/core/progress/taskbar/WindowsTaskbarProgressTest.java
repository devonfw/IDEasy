package com.devonfw.ide.gui.core.progress.taskbar;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.nio.charset.StandardCharsets.UTF_16LE;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Test of {@link WindowsTaskbarProgress}. It creates hidden native windows (of the system window class {@code STATIC}) to have a window of the current process
 * with a known title, since the headless JavaFX test environment does not create native windows.
 */
@EnabledOnOs(OS.WINDOWS)
class WindowsTaskbarProgressTest extends Assertions {

  private static final Linker LINKER = Linker.nativeLinker();

  private static final SymbolLookup USER32 = SymbolLookup.libraryLookup("user32.dll", Arena.global());

  /**
   * Verifies that a top-level window of the current process is found by its title.
   */
  @Test
  void shouldFindWindowOfCurrentProcessByTitle() throws Throwable {

    // arrange
    String title = newUniqueTitle();
    MemorySegment window = createWindow(title);
    try {
      // act
      MemorySegment foundWindow = WindowsTaskbarProgress.findWindow(title);

      // assert
      assertThat(foundWindow.address()).isEqualTo(window.address());
    } finally {
      destroyWindow(window);
    }
  }

  /**
   * Verifies that no window is found for a title that no window has.
   */
  @Test
  void shouldNotFindWindowWithUnknownTitle() {

    // act
    MemorySegment foundWindow = WindowsTaskbarProgress.findWindow(newUniqueTitle());

    // assert
    assertThat(foundWindow.address()).isZero();
  }

  /**
   * Verifies that displaying the progress of an existing window works without errors. Whether the taskbar actually shows it cannot be checked, but this
   * exercises the calls of the native taskbar interface.
   */
  @Test
  void shouldDisplayProgressOfExistingWindow() throws Throwable {

    // arrange
    String title = newUniqueTitle();
    MemorySegment window = createWindow(title);
    try {
      WindowsTaskbarProgress taskbarProgress = new WindowsTaskbarProgress(() -> title);

      // act + assert
      assertThatCode(() -> displayAllStates(taskbarProgress)).doesNotThrowAnyException();
    } finally {
      destroyWindow(window);
    }
  }

  /**
   * Verifies that the progress is silently not displayed if the window cannot be found.
   */
  @Test
  void shouldIgnoreProgressIfWindowIsNotFound() {

    // arrange
    String title = newUniqueTitle();
    WindowsTaskbarProgress taskbarProgress = new WindowsTaskbarProgress(() -> title);

    // act + assert
    assertThatCode(() -> displayAllStates(taskbarProgress)).doesNotThrowAnyException();
  }

  private static void displayAllStates(TaskbarProgress taskbarProgress) {

    taskbarProgress.setProgress(0.5);
    taskbarProgress.setIndeterminate();
    taskbarProgress.setProgress(1.0);
    taskbarProgress.clear();
  }

  private static String newUniqueTitle() {

    return "IDEasy " + WindowsTaskbarProgressTest.class.getSimpleName() + " " + UUID.randomUUID();
  }

  private static MemorySegment createWindow(String title) throws Throwable {

    MethodHandle createWindowEx = LINKER.downcallHandle(USER32.findOrThrow("CreateWindowExW"), FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS, ADDRESS,
        JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment windowClass = arena.allocateFrom("STATIC", UTF_16LE);
      MemorySegment windowName = arena.allocateFrom(title, UTF_16LE);
      // invisible top-level window without position, size, parent, menu or instance
      MemorySegment window = (MemorySegment) createWindowEx.invokeExact(0, windowClass, windowName, 0, 0, 0, 0, 0, MemorySegment.NULL,
          MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL);
      assertThat(window.address()).as("handle of the created window").isNotZero();
      return window;
    }
  }

  private static void destroyWindow(MemorySegment window) throws Throwable {

    MethodHandle destroyWindow = LINKER.downcallHandle(USER32.findOrThrow("DestroyWindow"), FunctionDescriptor.of(JAVA_INT, ADDRESS));
    int result = (int) destroyWindow.invokeExact(window);
    assertThat(result).as("result of DestroyWindow").isNotZero();
  }
}
