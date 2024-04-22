package com.devonfw.tools.ide.step;

import java.util.concurrent.Callable;

/**
 * Interface for a {@link Step} of the process. Allows to split larger processes into smaller steps that are traced and
 * measured. At the end you can get a report with the hierarchy of all steps and their success/failure status, duration
 * in absolute and relative numbers to gain transparency.<br> The typical use should follow this pattern:
 *
 * <pre>
 * Step step = context.{@link com.devonfw.tools.ide.context.IdeContext#newStep(String) newStep}("My step description");
 * try {
 *   // ... do something ...
 *   step.{@link #success(String) success}("Did something successfully.");
 * } catch (Exception e) {
 *   step.{@link #error(Throwable, String)}(e, "Failed to do something.");
 * } finally {
 *   step.{@link #end() end()};
 * }
 * </pre>
 */
public interface Step {

  /** Empty object array for no parameters. */
  Object[] NO_PARAMS = new Object[0];

  /**
   * @return the name of this {@link Step} as given to constructor.
   */
  String getName();

  /**
   * @return the duration of this {@link Step} from construction to {@link #success()} or {@link #end()}. Will be
   * {@code 0} if not {@link #end() ended}.
   */
  long getDuration();

  /**
   * @return {@code Boolean#TRUE} if this {@link Step} has {@link #success() succeeded}, {@code Boolean#FALSE} if the
   * {@link Step} has {@link #end() ended} without {@link #success() success} and {@code null} if the {@link Step} is
   * still running.
   */
  Boolean getSuccess();

  /**
   * @return {@code true} if this step completed {@link #success() successfully}, {@code false} otherwise.
   */
  default boolean isSuccess() {

    return Boolean.TRUE.equals(getSuccess());
  }

  /**
   * @return {@code true} if this step {@link #end() ended} without {@link #success() success} e.g. with an
   * {@link #error(String) error}, {@code false} otherwise.
   */
  default boolean isFailure() {

    return Boolean.FALSE.equals(getSuccess());
  }

  /**
   * @return {@code true} if this step is silent and not logged by default, {@code false} otherwise (default).
   */
  boolean isSilent();

  /**
   * Should be called to end this {@link Step} {@link #getSuccess() successfully}. May be called only once.
   */
  default void success() {

    success(null);
  }

  /**
   * Should be called to end this {@link Step} {@link #getSuccess() successfully}. May be called only once.
   *
   * @param message the explicit message to log as success.
   */
  default void success(String message) {

    success(message, (Object[]) null);
  }

  /**
   * Should be called to end this {@link Step} {@link #getSuccess() successfully}. May be called only once.
   *
   * @param message the explicit message to log as success.
   * @param args the optional arguments to fill as placeholder into the {@code message}.
   */
  void success(String message, Object... args);

  /**
   * Ensures this {@link Step} is properly ended. Has to be called from a finally block.
   */
  void end();

  /**
   * Should be called to end this {@link Step} as {@link #isFailure() failure} with an explicit error message. May be
   * called only once.
   *
   * @param message the explicit message to log as error.
   */
  default void error(String message) {

    error(null, message);
  }

  /**
   * Should be called to end this {@link Step} as {@link #isFailure() failure} with an explicit error message and/or
   * {@link Throwable exception}. May be called only once.
   *
   * @param message the explicit message to log as error.
   * @param args the optional arguments to fill as placeholder into the {@code message}.
   */
  default void error(String message, Object... args) {

    error(null, message, args);
  }

  /**
   * Should be called to end this {@link Step} as {@link #isFailure() failure} with an explicit error message and/or
   * {@link Throwable exception}. May be called only once.
   *
   * @param error the catched {@link Throwable}.
   */
  default void error(Throwable error) {

    error(error, false);
  }

  /**
   * Should be called to end this {@link Step} as {@link #isFailure() failure} with an explicit error message and/or
   * {@link Throwable exception}. May be called only once.
   *
   * @param error the catched {@link Throwable}.
   * @param suppress to suppress the error logging (if error will be rethrown and duplicated error messages shall be
   * avoided).
   */
  default void error(Throwable error, boolean suppress) {

    assert (error != null);
    error(error, suppress, null, (Object[]) null);
  }

  /**
   * Should be called to end this {@link Step} as {@link #isFailure() failure} with an explicit error message and/or
   * {@link Throwable exception}. May be called only once.
   *
   * @param error the catched {@link Throwable}. May be {@code null} if only a {@code message} is provided.
   * @param message the explicit message to log as error.
   */
  default void error(Throwable error, String message) {

    error(error, message, (Object[]) null);
  }

  /**
   * Should be called to end this {@link Step} as {@link #isFailure() failure} with an explicit error message and/or
   * {@link Throwable exception}. May be called only once.
   *
   * @param error the catched {@link Throwable}. May be {@code null} if only a {@code message} is provided.
   * @param message the explicit message to log as error.
   * @param args the optional arguments to fill as placeholder into the {@code message}.
   */
  default void error(Throwable error, String message, Object... args) {

    error(error, false, message, args);
  }

  /**
   * Should be called to end this {@link Step} as {@link #isFailure() failure} with an explicit error message and/or
   * {@link Throwable exception}. May be called only once.
   *
   * @param error the catched {@link Throwable}. May be {@code null} if only a {@code message} is provided.
   * @param suppress to suppress the error logging (if error will be rethrown and duplicated error messages shall be
   * avoided).
   * @param message the explicit message to log as error.
   * @param args the optional arguments to fill as placeholder into the {@code message}.
   */
  void error(Throwable error, boolean suppress, String message, Object... args);

  /**
   * @return the parent {@link Step} or {@code null} if there is no parent.
   */
  Step getParent();

  /**
   * @param i the index of the requested parameter. Should be in the range from {@code 0} to
   * <code>{@link #getParameterCount()}-1</code>.
   * @return the parameter at the given index {@code i} or {@code null} if no such parameter exists.
   */
  Object getParameter(int i);

  /**
   * @return the number of {@link #getParameter(int) parameters}.
   */
  int getParameterCount();

  /**
   * @param stepCode the {@link Runnable} to {@link Runnable#run() execute} for this {@link Step}.
   */
  default void run(Runnable stepCode) {

    try {
      stepCode.run();
      if (getSuccess() == null) {
        success();
      }
    } catch (RuntimeException | Error e) {
      error(e);
      throw e;
    } finally {
      end();
    }
  }

  /**
   * @param stepCode the {@link Callable} to {@link Callable#call() execute} for this {@link Step}.
   * @param <R> type of the return value.
   * @return the value returned from {@link Callable#call()}.
   */
  default <R> R call(Callable<R> stepCode) {

    try {
      R result = stepCode.call();
      if (getSuccess() == null) {
        success();
      }
      return result;
    } catch (Throwable e) {
      error(e);
      if (e instanceof RuntimeException re) {
        throw re;
      } else if (e instanceof Error error) {
        throw error;
      } else {
        throw new IllegalStateException(e);
      }
    } finally {
      end();
    }
  }

}