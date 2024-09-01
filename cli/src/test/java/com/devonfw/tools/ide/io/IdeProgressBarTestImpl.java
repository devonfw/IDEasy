package com.devonfw.tools.ide.io;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link IdeProgressBar} used for tests.
 */
public class IdeProgressBarTestImpl implements IdeProgressBar {

  /** Starting time of a {@link IdeProgressBar}. */
  private final Instant start;

  /** Ending time of an {@link IdeProgressBar}. */
  private Instant end;

  /** The task name of an {@link IdeProgressBar}. */
  private final String name;

  /** The total span of an {@link IdeProgressBar}. */
  private long total;

  /** The maximum length of an {@link IdeProgressBar}. */
  private final long max;

  /** The list of events of an {@link IdeProgressBar}. */
  private final List<ProgressEvent> eventList;

  /**
   * The constructor.
   *
   * @param name the task name.
   * @param max maximum length of the bar.
   */
  public IdeProgressBarTestImpl(String name, long max) {

    this.start = Instant.now();
    this.name = name;
    this.max = max;
    this.eventList = new ArrayList<>();
  }

  @Override
  public void stepBy(long stepSize) {

    this.total += stepSize;
    this.eventList.add(new ProgressEvent(stepSize));
  }

  @Override
  public long getCurrent() {
    return this.total;
  }

  @Override
  public void close() {

    if (this.end == null) {
      this.end = Instant.now();
    }
    assert this.total <= this.max;
  }

  /**
   * @return the list of {@link ProgressEvent}s.
   */
  public List<ProgressEvent> getEventList() {

    return this.eventList;
  }

  /**
   * @return the maximum length of a bar.
   */
  public long getMaxSize() {

    return this.max;
  }

  /**
   * @return the total length of a bar.
   */
  public long getTotalSize() {

    return this.total;
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
