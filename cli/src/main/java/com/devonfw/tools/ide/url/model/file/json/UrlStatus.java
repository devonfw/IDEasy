package com.devonfw.tools.ide.url.model.file.json;

/**
 * Status information for a specific (download) URL.
 */
public class UrlStatus {

  private UrlStatusState success;

  private UrlStatusState error;

  /**
   * The constructor.
   */
  public UrlStatus() {

    super();
  }

  /**
   * @return the {@link UrlStatusState} of the last success.
   */
  public UrlStatusState getSuccess() {

    return this.success;
  }

  /**
   * @param success the new value of {@link #getSuccess()}.
   */
  public void setSuccess(UrlStatusState success) {

    this.success = success;
  }

  /**
   * @return the {@link UrlStatusState} of the last error or {@code null} if no error has ever occurred.
   */
  public UrlStatusState getError() {

    return this.error;
  }

  /**
   * @param error the new value of {@link #getError()}.
   */
  public void setError(UrlStatusState error) {

    this.error = error;
  }
}
