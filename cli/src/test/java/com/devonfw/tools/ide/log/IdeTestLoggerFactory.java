package com.devonfw.tools.ide.log;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Factory for {@link IdeTestLogger}.
 */
public class IdeTestLoggerFactory implements Function<IdeLogLevel, IdeSubLogger> {

  private final List<IdeLogEntry> entries;

  /**
   * The constructor.
   */
  public IdeTestLoggerFactory() {
    super();
    this.entries = new ArrayList<>(512);
  }

  @Override
  public IdeSubLogger apply(IdeLogLevel ideLogLevel) {

    return new IdeTestLogger(ideLogLevel, this.entries);
  }

  /**
   * @return the {@link List} of {@link IdeLogEntry} that have been logged for test assertions.
   */
  public List<IdeLogEntry> getEntries() {

    return this.entries;
  }

}
