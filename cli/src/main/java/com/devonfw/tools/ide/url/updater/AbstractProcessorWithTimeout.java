package com.devonfw.tools.ide.url.updater;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.url.model.report.UrlFinalReport;
import com.devonfw.tools.ide.url.model.report.UrlUpdaterReport;

/**
 * Abstract base class for a processor that has a timeout and should cancel if the timeout is expired.
 */
public abstract class AbstractProcessorWithTimeout {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractProcessorWithTimeout.class);

  /** The {@link Instant} expiration time for the GitHub actions url-update job */
  private Instant expirationTime;

  /** The {@link UrlFinalReport} final report of url updates for monitoring */
  private UrlFinalReport urlFinalReport;

  /** The {@link UrlUpdaterReport} report instance to track data for the {@link UrlFinalReport} */
  private UrlUpdaterReport urlUpdaterReport;

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
   * @param urlFinalReport to collect {@link UrlUpdaterReport} instances for final output of reports. The init happens in UpdateInitiator.class.
   */
  public void setUrlFinalReport(UrlFinalReport urlFinalReport) {

    this.urlFinalReport = urlFinalReport;
  }

  /**
   * @return the {@link UrlFinalReport} representing the final report
   */
  public UrlFinalReport getUrlFinalReport() {

    if (this.urlFinalReport == null) {
      this.urlFinalReport = new UrlFinalReport();
    }
    return this.urlFinalReport;
  }

  /**
   * @param urlUpdaterReport to collect information during  url updating process
   */
  public void setUrlUpdaterReport(UrlUpdaterReport urlUpdaterReport) {

    this.urlUpdaterReport = urlUpdaterReport;
  }

  /**
   * @return the {@link UrlUpdaterReport} representing the report instance to collect failures successes
   */
  public UrlUpdaterReport getUrlUpdaterReport() {

    return urlUpdaterReport;
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
