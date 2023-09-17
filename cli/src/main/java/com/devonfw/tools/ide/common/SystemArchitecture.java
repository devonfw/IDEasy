package com.devonfw.tools.ide.common;

/**
 * Enum with the supported architectures.
 */
public enum SystemArchitecture {
  /** x86 architecture (intel/AMD) with 64-Bit (a.k.a. "x86_64" or "amd64"). */
  X64("x64"),

  /** ARM architecture with 64-Bit (a.k.a. "aarch64") including Apple M1/2 CPU. */
  ARM64("arm64");

  private final String title;

  private SystemArchitecture(String title) {

    this.title = title;
  }

  @Override
  public String toString() {

    return this.title;
  }

  /**
   * @param title the {@link #toString() string representation} of the requested {@link SystemArchitecture}.
   * @return the according {@link SystemArchitecture} or {@code null} if none matches.
   */
  public static SystemArchitecture of(String title) {

    for (SystemArchitecture os : values()) {
      if (os.toString().equals(title)) {
        return os;
      }
    }
    return null;
  }

  /**
   * @param architecture the {@link SystemArchitecture}.
   * @return the given {@link SystemArchitecture} or {@link #X64} if {@code null} was given.
   */
  public static SystemArchitecture orDefault(SystemArchitecture architecture) {

    if (architecture == null) {
      return X64;
    }
    return architecture;
  }
}
