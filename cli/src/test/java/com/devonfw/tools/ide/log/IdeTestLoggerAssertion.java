package com.devonfw.tools.ide.log;

import java.util.List;
import java.util.function.Predicate;

import org.assertj.core.api.Assertions;

/**
 * Assertion for {@link IdeLogEntry log entries}.
 */
public class IdeTestLoggerAssertion {

  private final List<IdeLogEntry> entries;

  private final IdeLogLevel level;

  public IdeTestLoggerAssertion(List<IdeLogEntry> entries, IdeLogLevel level) {
    super();
    this.entries = entries;
    this.level = level;
  }

  /**
   * @param message the expected {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message}.
   */
  public void hasMessage(String message) {

    fulfillsPredicate(e -> e.message().equals(message), PredicateMode.MATCH_ONE, "Could not find log message equal to '" + message + "'");
  }

  /**
   * @param message the {@link String} expected to be {@link String#contains(CharSequence) contained} in a
   * {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message}.
   */
  public void hasMessageContaining(String message) {

    fulfillsPredicate(e -> e.message().contains(message), PredicateMode.MATCH_ONE, "Could not find log message containing '" + message + "'");
  }

  /**
   * @param message the {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message} that is not expected and should not have been logged.
   */
  public void hasNoMessage(String message) {

    fulfillsPredicate(e -> !e.message().equals(message), PredicateMode.MATCH_ALL, "No log message should be equal to '" + message + "'");
  }

  /**
   * @param message the {@link String} expected not be {@link String#contains(CharSequence) contained} in any
   * {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message}.
   */
  public void hasNoMessageContaining(String message) {

    fulfillsPredicate(e -> !e.message().contains(message), PredicateMode.MATCH_ALL, "No log message should contain '" + message + "'");
  }

  /**
   * @param messages the expected {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message}s in order.
   */
  public void hasEntries(String... messages) {

    assert (this.level != null);
    IdeLogEntry[] entries = new IdeLogEntry[messages.length];
    int i = 0;
    for (String message : messages) {
      entries[i++] = new IdeLogEntry(this.level, message);
    }
    hasEntries(false, entries);
  }

  /**
   * @param expectedEntries the expected {@link com.devonfw.tools.ide.log.IdeLogEntry log entries} in order.
   */
  public void hasEntries(IdeLogEntry... expectedEntries) {

    hasEntries(false, expectedEntries);
  }

  /**
   * @param expectedEntries the expected {@link com.devonfw.tools.ide.log.IdeLogEntry log entries} to be logged in order without any other log statement in
   * between them.
   */
  public void hasEntriesWithNothingElseInBetween(IdeLogEntry... expectedEntries) {

    hasEntries(true, expectedEntries);
  }

  private void hasEntries(boolean nothingElseInBetween, IdeLogEntry... expectedEntries) {

    assert (expectedEntries.length > 0);
    int i = 0;
    int max = 0;
    for (IdeLogEntry entry : this.entries) {
      if (expectedEntries[i].matches(entry)) {
        i++;
      } else {
        if (nothingElseInBetween) {
          i = 0;
        } else if (expectedEntries[0].matches(entry)) {
          i = 1;
        }
      }
      if (i == expectedEntries.length) {
        return;
      }
      if (i > max) {
        max = i;
      }
    }
    StringBuilder error = new StringBuilder(4096);
    if (max > 0) {
      error.append("Found expected log entries:\n");
      for (i = 0; i < max; i++) {
        appendEntry(error, expectedEntries[i]);
      }
    }
    error.append("\nThe first entry that was not matching from a block of ");
    error.append(expectedEntries.length);
    error.append(" expected log-entries ");
    if (nothingElseInBetween) {
      error.append("with nothing else logged in between ");
    }
    error.append("was:\n");
    appendEntry(error, expectedEntries[max]);
    error.append("\nIn the logs of this test:\n");
    for (IdeLogEntry entry : this.entries) {
      appendEntry(error, entry);
    }
    Assertions.fail(error.toString());
  }

  private static void appendEntry(StringBuilder sb, IdeLogEntry entry) {
    sb.append(entry.level());
    sb.append(":");
    sb.append(entry.message());
    sb.append('\n');
  }

  private void fulfillsPredicate(Predicate<IdeLogEntry> predicate, PredicateMode mode, String errorMessage) {

    if (this.level != null) {
      errorMessage = errorMessage + " on level " + this.level;
    }
    for (IdeLogEntry entry : entries) {
      if ((this.level == null) || (this.level == entry.level())) {
        if (predicate.test(entry)) {
          if (mode == PredicateMode.MATCH_ONE) {
            return;
          }
        } else if (mode == PredicateMode.MATCH_ALL) {
          Assertions.fail(errorMessage + "\nFound unexpected log entry: " + entry);
          return;
        }
      }
    }
    if (mode == PredicateMode.MATCH_ONE) {
      Assertions.fail(errorMessage); // no log entry matched by predicate
    }
  }

}
