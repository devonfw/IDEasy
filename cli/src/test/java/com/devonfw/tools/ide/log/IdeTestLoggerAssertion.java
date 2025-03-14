package com.devonfw.tools.ide.log;

import java.util.ArrayList;
import java.util.Arrays;
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
   * @return this assertion itself for fluent API calls.
   */
  public IdeTestLoggerAssertion hasMessage(String message) {

    return fulfillsPredicate(e -> e.message().equals(message), PredicateMode.MATCH_ONE, "Could not find log message equal to '" + message + "'");
  }

  /**
   * @param message the {@link String} expected to be {@link String#contains(CharSequence) contained} in a
   *     {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message}.
   * @return this assertion itself for fluent API calls.
   */
  public IdeTestLoggerAssertion hasMessageContaining(String message) {

    return fulfillsPredicate(e -> e.message().contains(message), PredicateMode.MATCH_ONE, "Could not find log message containing '" + message + "'");
  }

  /**
   * @param message the {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message} that is not expected and should not have been logged.
   * @return this assertion itself for fluent API calls.
   */
  public IdeTestLoggerAssertion hasNoMessage(String message) {

    return fulfillsPredicate(e -> !e.message().equals(message), PredicateMode.MATCH_ALL, "No log message should be equal to '" + message + "'");
  }

  /**
   * @param message the {@link String} expected not be {@link String#contains(CharSequence) contained} in any
   *     {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message}.
   * @return this assertion itself for fluent API calls.
   */
  public IdeTestLoggerAssertion hasNoMessageContaining(String message) {

    return fulfillsPredicate(e -> !e.message().contains(message), PredicateMode.MATCH_ALL, "No log message should contain '" + message + "'");
  }

  /**
   * Asserts that no {@link IdeLogEntry} has an {@link IdeLogEntry#error() error} containing a {@link Throwable} (exception).
   *
   * @return this assertion itself for fluent API calls.
   */
  public IdeTestLoggerAssertion hasNoEntryWithException() {

    return fulfillsPredicate(e -> e.error() == null, PredicateMode.MATCH_ALL, "No log message should have an exception");
  }

  /**
   * @param messages the expected {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message}s in order.
   * @return this assertion itself for fluent API calls.
   */
  public IdeTestLoggerAssertion hasEntries(String... messages) {

    assert (this.level != null);
    IdeLogEntry[] entries = new IdeLogEntry[messages.length];
    int i = 0;
    for (String message : messages) {
      entries[i++] = new IdeLogEntry(this.level, message);
    }
    return hasEntries(false, entries);
  }

  /**
   * @param expectedEntries the expected {@link com.devonfw.tools.ide.log.IdeLogEntry log entries} in order.
   * @return this assertion itself for fluent API calls.
   */
  public IdeTestLoggerAssertion hasEntries(IdeLogEntry... expectedEntries) {

    return hasEntries(false, expectedEntries);
  }

  /**
   * @param expectedEntries the expected {@link com.devonfw.tools.ide.log.IdeLogEntry log entries} to be logged in order without any other log statement in
   *     between them.
   * @return this assertion itself for fluent API calls.
   */
  public IdeTestLoggerAssertion hasEntriesWithNothingElseInBetween(IdeLogEntry... expectedEntries) {

    return hasEntries(true, expectedEntries);
  }

  private IdeTestLoggerAssertion hasEntries(boolean nothingElseInBetween, IdeLogEntry... expectedEntries) {
    assert (expectedEntries.length > 0);
    
    List<IdeLogEntry> remainingEntries = new ArrayList<>(Arrays.asList(expectedEntries));

    for (IdeLogEntry entry : this.entries) {
      remainingEntries.removeIf(expectedEntry -> expectedEntry.matches(entry));
      if (remainingEntries.isEmpty()) {
        return this;
      }
    }

    StringBuilder error = new StringBuilder(4096);
    error.append("The following expected log entries were not found:\n");
    for (IdeLogEntry entry : remainingEntries) {
      appendEntry(error, entry);
    }
    error.append("\nActual logs:\n");
    for (IdeLogEntry entry : this.entries) {
      appendEntry(error, entry);
    }

    Assertions.fail(error.toString());
    return this;
  }

  private static void appendEntry(StringBuilder sb, IdeLogEntry entry) {
    sb.append(entry.level());
    sb.append(":");
    sb.append(entry.message());
    sb.append('\n');
  }

  private IdeTestLoggerAssertion fulfillsPredicate(Predicate<IdeLogEntry> predicate, PredicateMode mode, String errorMessage) {

    if (this.level != null) {
      errorMessage = errorMessage + " on level " + this.level;
    }
    for (IdeLogEntry entry : entries) {
      if ((this.level == null) || (this.level == entry.level())) {
        if (predicate.test(entry)) {
          if (mode == PredicateMode.MATCH_ONE) {
            return this;
          }
        } else if (mode == PredicateMode.MATCH_ALL) {
          Assertions.fail(errorMessage + "\nFound unexpected log entry: " + entry);
          return this;
        }
      }
    }
    if (mode == PredicateMode.MATCH_ONE) {
      Assertions.fail(errorMessage); // no log entry matched by predicate
    }
    return this;
  }

}
