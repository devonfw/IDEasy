package com.devonfw.tools.ide.log;

/**
 * Abstract base implementation of {@link IdeSubLogger}.
 */
public abstract class AbstractIdeSubLogger implements IdeSubLogger {

  /** @see #getLevel() */
  protected final IdeLogLevel level;

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   */
  public AbstractIdeSubLogger(IdeLogLevel level) {

    super();
    this.level = level;
  }

  @Override
  public IdeLogLevel getLevel() {

    return this.level;
  }

  @Override
  public String toString() {

    return getClass().getSimpleName() + "@" + this.level;
  }

}
