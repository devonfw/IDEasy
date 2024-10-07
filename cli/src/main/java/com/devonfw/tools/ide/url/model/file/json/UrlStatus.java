package com.devonfw.tools.ide.url.model.file.json;

/**
 * Status information for a specific (download) URL.
 */
public class UrlStatus {

  private UrlStatusState success;

  private UrlStatusState error;

  private transient boolean stillUsed;

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

  /**
   * @return {@code true} if entirely empty, {@code false} otherwise.
   */
  public boolean checkEmpty() {

    return (this.error == null) && (this.success == null);
  }

  /**
   * ATTENTION: This is not a standard getter (isStillUsed()) since otherwise Jackson will write it to JSON.
   *
   * @return {@code true} if still {@link StatusJson#getOrCreateUrlStatus(String) used}, {@code false} otherwise (if the entire version has been processed, it
   *     will be removed via {@link StatusJson#cleanup()}.
   */
  public boolean checkStillUsed() {

    return this.stillUsed;
  }

  /**
   * Sets {@link #checkStillUsed()} to {@code true}.
   */
  void markSillUsed() {

    this.stillUsed = true;
  }

}
