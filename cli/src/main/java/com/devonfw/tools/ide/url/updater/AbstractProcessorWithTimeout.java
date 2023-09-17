package com.devonfw.tools.ide.url.updater;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for a processor that has a timeout and should cancel if the timeout is expired.
 */
public abstract class AbstractProcessorWithTimeout {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractProcessorWithTimeout.class);

  /** The {@link Instant} expiration time for the GitHub actions url-update job */
  private Instant expirationTime;

  /**
   * @param expirationTime to set for the GitHub actions url-update job
   */
  public void setExpirationTime(Instant expirationTime) {

    this.expirationTime = expirationTime;
  }

  /**
   * @return the {@link Instant} representing the timeout when to expire and stop further processing.
   */
  public Instant getExpirationTime() {

    return this.expirationTime;
  }

  /**
   * Checks if the timeout was expired.
   *
   * @return boolean true if timeout was expired, false if not
   */
  public boolean isTimeoutExpired() {

    if (this.expirationTime == null) {
      return false;
    }

    if (Instant.now().isAfter(this.expirationTime)) {
      LOG.warn("Expiration time of timeout was reached, cancelling update process.");
      return true;
    }

    return false;
  }

}
