package com.devonfw.ide.gui.core.progress.taskbar;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static java.nio.charset.StandardCharsets.UTF_16LE;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link TaskbarProgress} for Microsoft Windows. It uses the
 * <a href="https://learn.microsoft.com/en-us/windows/win32/api/shobjidl_core/nn-shobjidl_core-itaskbarlist3">ITaskbarList3</a> COM interface of the Windows
 * shell via the Java Foreign Function and Memory API. As JavaFX does not expose the native handle of a window, the window is looked up by its title among the
 * top-level windows of the current process.
 * <p>
 * The native resources are initialized on first use. If that fails (e.g. because the window cannot be found), a warning is logged once and no progress is
 * displayed.
 */
public class WindowsTaskbarProgress implements TaskbarProgress {

  private static final Logger LOG = LoggerFactory.getLogger(WindowsTaskbarProgress.class);

  /** CLSID of the TaskbarList COM class implementing ITaskbarList3. */
  private static final String CLSID_TASKBAR_LIST = "56FDF344-FD6D-11d0-958A-006097C9A090";

  /** IID of the ITaskbarList3 COM interface. */
  private static final String IID_TASKBAR_LIST3 = "EA1AFB91-9E28-4B86-90E9-9E9F8A5EEFAF";

  /** Size of a GUID (CLSID or IID) in bytes. */
  private static final long GUID_SIZE = 16;

  private static final int COINIT_APARTMENTTHREADED = 0x2;

  private static final int CLSCTX_INPROC_SERVER = 0x1;

  /** HRESULT of CoInitializeEx if COM is already initialized on the current thread with another concurrency model, which works for us as well. */
  private static final int RPC_E_CHANGED_MODE = 0x80010106;

  /** TBPFLAG of ITaskbarList3::SetProgressState to remove the progress. */
  private static final int TBPF_NOPROGRESS = 0x0;

  /** TBPFLAG of ITaskbarList3::SetProgressState for an indeterminate progress. */
  private static final int TBPF_INDETERMINATE = 0x1;

  // indices of the methods in the vtable of ITaskbarList3 that extends ITaskbarList2 (1 method), ITaskbarList (5 methods) and IUnknown (3 methods)
  private static final int RELEASE = 2;

  private static final int HR_INIT = 3;

  private static final int SET_PROGRESS_VALUE = 9;

  private static final int SET_PROGRESS_STATE = 10;

  /** Total passed to ITaskbarList3::SetProgressValue, so the progress is displayed with a resolution of 0.1%. */
  private static final long PROGRESS_TOTAL = 1000;

  private final Supplier<String> windowTitle;

  private boolean initialized;

  /** ITaskbarList3::SetProgressValue bound to the taskbar list and window, or {@code null} if not available. */
  private MethodHandle progressValueSetter;

  /** ITaskbarList3::SetProgressState bound to the taskbar list and window, or {@code null} if not available. */
  private MethodHandle progressStateSetter;

  /**
   * The constructor.
   *
   * @param windowTitle the {@link Supplier} of the title of the window whose taskbar button shall display the progress. It is called on first use, when the
   *     window has to be showing already.
   */
  public WindowsTaskbarProgress(Supplier<String> windowTitle) {

    this.windowTitle = windowTitle;
  }

  @Override
  public void setProgress(double progress) {

    if (isAvailable()) {
      long completed = Math.round(Math.clamp(progress, 0.0, 1.0) * PROGRESS_TOTAL);
      try {
        // this also switches the taskbar button from no or an indeterminate progress to a normal progress
        logFailure("SetProgressValue", (int) invoke(this.progressValueSetter, completed, PROGRESS_TOTAL));
      } catch (RuntimeException e) {
        disable(e);
      }
    }
  }

  @Override
  public void setIndeterminate() {

    setProgressState(TBPF_INDETERMINATE);
  }

  @Override
  public void clear() {

    setProgressState(TBPF_NOPROGRESS);
  }

  private void setProgressState(int state) {

    if (isAvailable()) {
      try {
        logFailure("SetProgressState", (int) invoke(this.progressStateSetter, state));
      } catch (RuntimeException e) {
        disable(e);
      }
    }
  }

  private boolean isAvailable() {

    if (!this.initialized) {
      this.initialized = true;
      try {
        initialize();
      } catch (RuntimeException e) {
        LOG.warn("Cannot display the progress of running tasks in the Windows taskbar: {}", e.getMessage(), e);
      }
    }
    return this.progressStateSetter != null;
  }

  private void initialize() {

    String title = this.windowTitle.get();
    MemorySegment window = findWindow(title);
    if (window.address() == 0) {
      throw new IllegalStateException("No window with the title '" + title + "' was found.");
    }
    MemorySegment taskbarList = createTaskbarList();
    int result = (int) invoke(comMethod(taskbarList, HR_INIT, FunctionDescriptor.of(JAVA_INT, ADDRESS)));
    if (result < 0) {
      invoke(comMethod(taskbarList, RELEASE, FunctionDescriptor.of(JAVA_INT, ADDRESS)));
      throw new IllegalStateException("ITaskbarList3::HrInit failed with HRESULT 0x" + Integer.toHexString(result));
    }
    this.progressValueSetter = MethodHandles.insertArguments(
        comMethod(taskbarList, SET_PROGRESS_VALUE, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG, JAVA_LONG)), 0, window);
    this.progressStateSetter = MethodHandles.insertArguments(
        comMethod(taskbarList, SET_PROGRESS_STATE, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT)), 0, window);
  }

  private void disable(RuntimeException error) {

    this.progressValueSetter = null;
    this.progressStateSetter = null;
    LOG.warn("Failed to display the progress of running tasks in the Windows taskbar, so it is not displayed anymore: {}", error.getMessage(), error);
  }

  /**
   * @param title the title of the window to find.
   * @return the handle of the top-level window of the current process with the given title or {@link MemorySegment#NULL} if there is no such window.
   */
  static MemorySegment findWindow(String title) {

    Linker linker = Linker.nativeLinker();
    SymbolLookup user32 = SymbolLookup.libraryLookup("user32.dll", Arena.global());
    MethodHandle findWindowEx = linker.downcallHandle(user32.findOrThrow("FindWindowExW"),
        FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
    MethodHandle getWindowThreadProcessId = linker.downcallHandle(user32.findOrThrow("GetWindowThreadProcessId"),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    long currentProcessId = ProcessHandle.current().pid();
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment windowName = arena.allocateFrom(title, UTF_16LE);
      MemorySegment processId = arena.allocate(JAVA_INT);
      MemorySegment window = MemorySegment.NULL;
      while (true) {
        // without a parent window, FindWindowExW searches the top-level windows following the given one (also those of other processes)
        window = (MemorySegment) invoke(findWindowEx, MemorySegment.NULL, window, MemorySegment.NULL, windowName);
        if (window.address() == 0) {
          return MemorySegment.NULL;
        }
        int threadId = (int) invoke(getWindowThreadProcessId, window, processId);
        if ((threadId != 0) && (Integer.toUnsignedLong(processId.get(JAVA_INT, 0)) == currentProcessId)) {
          return window;
        }
      }
    }
  }

  private static MemorySegment createTaskbarList() {

    Linker linker = Linker.nativeLinker();
    SymbolLookup ole32 = SymbolLookup.libraryLookup("ole32.dll", Arena.global());
    MethodHandle coInitializeEx = linker.downcallHandle(ole32.findOrThrow("CoInitializeEx"), FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    MethodHandle coCreateInstance = linker.downcallHandle(ole32.findOrThrow("CoCreateInstance"),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));
    // COM is never uninitialized, since the taskbar list is used on this thread until the application exits
    int result = (int) invoke(coInitializeEx, MemorySegment.NULL, COINIT_APARTMENTTHREADED);
    if ((result < 0) && (result != RPC_E_CHANGED_MODE)) {
      throw new IllegalStateException("CoInitializeEx failed with HRESULT 0x" + Integer.toHexString(result));
    }
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment taskbarList = arena.allocate(ADDRESS);
      result = (int) invoke(coCreateInstance, guid(arena, CLSID_TASKBAR_LIST), MemorySegment.NULL, CLSCTX_INPROC_SERVER, guid(arena, IID_TASKBAR_LIST3),
          taskbarList);
      if (result < 0) {
        throw new IllegalStateException("CoCreateInstance of ITaskbarList3 failed with HRESULT 0x" + Integer.toHexString(result));
      }
      return taskbarList.get(ADDRESS, 0);
    }
  }

  /**
   * @param comObject the pointer to the COM object.
   * @param index the index of the method in the vtable of the COM object.
   * @param descriptor the {@link FunctionDescriptor} of the method including the leading pointer to the COM object ("this").
   * @return the {@link MethodHandle} of the method with the COM object already bound as first argument.
   */
  private static MethodHandle comMethod(MemorySegment comObject, int index, FunctionDescriptor descriptor) {

    MemorySegment vtable = comObject.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
    MemorySegment method = vtable.reinterpret(ADDRESS.byteSize() * (index + 1)).getAtIndex(ADDRESS, index);
    return MethodHandles.insertArguments(Linker.nativeLinker().downcallHandle(method, descriptor), 0, comObject);
  }

  /**
   * @param arena the {@link Arena} to allocate the GUID in.
   * @param value the GUID in its common string representation.
   * @return the GUID struct in native memory.
   */
  private static MemorySegment guid(Arena arena, String value) {

    UUID uuid = UUID.fromString(value);
    long high = uuid.getMostSignificantBits();
    MemorySegment guid = arena.allocate(GUID_SIZE, Long.BYTES);
    guid.set(JAVA_INT, 0, (int) (high >>> 32)); // Data1
    guid.set(JAVA_SHORT, 4, (short) (high >>> 16)); // Data2
    guid.set(JAVA_SHORT, 6, (short) high); // Data3
    guid.set(JAVA_LONG.withOrder(ByteOrder.BIG_ENDIAN), 8, uuid.getLeastSignificantBits()); // Data4 is a byte array
    return guid;
  }

  private static Object invoke(MethodHandle function, Object... arguments) {

    try {
      return function.invokeWithArguments(arguments);
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      // native functions do not throw checked exceptions, so this should never happen
      throw new IllegalStateException(e);
    }
  }

  private static void logFailure(String method, int result) {

    if (result < 0) {
      // e.g. if the taskbar button of the window is not created yet, so just ignore it, the next update will display the progress
      LOG.debug("ITaskbarList3::{} failed with HRESULT 0x{}", method, Integer.toHexString(result));
    }
  }
}
