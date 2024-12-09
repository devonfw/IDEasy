package com.devonfw.tools.ide.io;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link IdeProgressBar} used for tests.
 */
public class IdeProgressBarTestImpl extends AbstractIdeProgressBar {

  /** Starting time of a {@link IdeProgressBar}. */
  private final Instant start;

  /** Ending time of an {@link IdeProgressBar}. */
  private Instant end;

  /** The total span of an {@link IdeProgressBar}. */
  private long total;

  /** The list of events of an {@link IdeProgressBar}. */
  private final List<ProgressEvent> eventList;

  /**
   * The constructor.
   *
   * @param title the {@link #getTitle() title}.
   * @param maxSize the {@link #getMaxSize() maximum size}.
   * @param unitName the {@link #getUnitName() unit name}.
   * @param unitSize the {@link #getUnitSize() unit size}.
   */
  public IdeProgressBarTestImpl(String title, long maxSize, String unitName, long unitSize) {
    super(title, maxSize, unitName, unitSize);
    this.start = Instant.now();
    this.eventList = new ArrayList<>();
  }

  @Override
  protected void doStepBy(long stepSize, long currentProgress) {
    this.total = currentProgress;
    this.eventList.add(new ProgressEvent(stepSize));
  }

  @Override
  protected void doStepTo(long stepPosition) {
    this.total = stepPosition;
    this.eventList.add(new ProgressEvent(stepPosition));
  }

  @Override
  public void close() {
    super.close();
    if (this.end == null) {
      this.end = Instant.now();
    }

    if (getMaxSize() != -1) {
      assert this.total == getMaxSize();
    }
  }

  /**
   * @return the list of {@link ProgressEvent}s.
   */
  public List<ProgressEvent> getEventList() {

    return this.eventList;
  }

  /**
   * A progress event providing data about the event.
   */
  public static class ProgressEvent {

    /** The timestamp of the event */
    private final Instant timestamp;

    /** The step size of the event */
    private final long stepSize;

    /**
     * The constructor of the event.
     *
     * @param stepSize The step size of the event.
     */
    public ProgressEvent(long stepSize) {

      this.timestamp = Instant.now();
      this.stepSize = stepSize;
    }

    /**
     * @return the timestamp of the event.
     */
    public Instant getTimestamp() {

      return this.timestamp;
    }

    /**
     * @return the step size of the event.
     */
    public long getStepSize() {

      return this.stepSize;
    }
  }

}
