package com.devonfw.tools.ide.step;

/**
 * Interface for a {@link Step} of the processing. Allows to split larger processing into smaller steps that are traced
 * and measured. At the end you can get a report with the hierarchy of all steps and their success/failure status,
 * duration in absolute and relative numbers to gain transparency.
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
   *         {@code 0} if not {@link #end() ended}.
   */
  long getDuration();

  /**
   * @return {@code Boolean#TRUE} if this {@link Step} has {@link #success() succeeded}, {@code Boolean#FALSE} if the
   *         {@link Step} has {@link #end() ended} without {@link #success() success} and {@code null} if the
   *         {@link Step} is still running.
   */
  Boolean getSuccess();

  /**
   * @return {@code true} if this step completed {@link #success() successfully}, {@code false} otherwise.
   */
  default boolean isSuccess() {

    return Boolean.TRUE.equals(getSuccess());
  }

  /**
   * @return {@code true} if this step completed {@link #success() successfully}, {@code false} otherwise.
   */
  default boolean isFailure() {

    return Boolean.FALSE.equals(getSuccess());
  }

  /**
   * Should be called to end this {@link Step} {@link #getSuccess() successfully}. May be called only once.
   */
  void success();

  /**
   * Ensures this {@link Step} is properly ended. Has to be called from a finally block.
   */
  void end();

  /**
   * @return the parent {@link Step} or {@code null} if there is no parent.
   */
  Step getParent();

  /**
   * @param i the index of the requested parameter. Should be in the range from {@code 0} to
   *        <code>{@link #getParameterCount()}-1</code>.
   * @return the parameter at the given index {@code i} or {@code null} if no such parameter exists.
   */
  Object getParameter(int i);

  /**
   * @return the number of {@link #getParameter(int) parameters}.
   */
  int getParameterCount();

}