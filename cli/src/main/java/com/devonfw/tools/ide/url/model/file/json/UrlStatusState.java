package com.devonfw.tools.ide.url.model.file.json;

import java.time.Instant;

/**
 * Class for the state information of an URL status check.
 *
 * @see UrlStatus#getSuccess()
 * @see UrlStatus#getError()
 */
public final class UrlStatusState {

  private Instant timestamp;

  private Integer code;

  private String message;

  /**
   * The constructor.
   */
  public UrlStatusState() {

    this.timestamp = Instant.now();
  }

  /**
   * @return the {@link Instant} when this status was matched for the last time.
   */
  public Instant getTimestamp() {

    return this.timestamp;
  }

  /**
   * @param timestamp the new value of {@link #getTimestamp()}.
   */
  public void setTimestamp(Instant timestamp) {

    this.timestamp = timestamp;
  }

  /**
   * @return the HTTP status code (e.g. 404) or {@code null} if undefined (200 is omitted for success).
   */
  public Integer getCode() {

    return this.code;
  }

  /**
   * @param code new value of {@link #getCode()}.
   */
  public void setCode(Integer code) {

    this.code = code;
  }

  /**
   * @return the optional HTTP status message (e.g. error message).
   */
  public String getMessage() {

    return this.message;
  }

  /**
   * @param message the new value of {@link #getMessage()}.
   */
  public void setMessage(String message) {

    this.message = message;
  }

  @Override
  public String toString() {

    return getClass().getSimpleName() + "@" + this.timestamp
        + ((this.message == null || this.message.isEmpty()) ? "" : ":" + this.message);
  }
}