package com.devonfw.tools.ide.log;

import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link IdeSubLogger} for testing that collects all messages and allows to check if an expected
 * message was logged.
 */
public class IdeTestLogger extends AbstractIdeSubLogger {

  private final List<String> messages;

  /**
   * The constructor.
   *
   * @param level the {@link #getLevel() log-level}.
   */
  public IdeTestLogger(IdeLogLevel level) {

    super(level);
    this.messages = new LinkedList<>();
  }

  @Override
  public void log(String message) {

    this.messages.add(message);
  }

  /**
   * @return the {@link List} of messages that have been logged for test assertions.
   */
  public List<String> getMessages() {

    return this.messages;
  }

  @Override
  public boolean isEnabled() {

    return true;
  }

}
