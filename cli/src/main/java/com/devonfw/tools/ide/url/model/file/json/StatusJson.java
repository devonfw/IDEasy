package com.devonfw.tools.ide.url.model.file.json;

import java.util.LinkedHashMap;
import java.util.Map;

import com.devonfw.tools.ide.url.model.file.UrlStatusFile;

/**
 * Java model class representing a "status.json" file.
 *
 * @see UrlStatusFile
 */
public class StatusJson {
  private boolean manual;

  private Map<Integer, UrlStatus> urls;

  /**
   * The constructor.
   */
  public StatusJson() {

    this.manual = false;
    this.urls = new LinkedHashMap<>();
  }

  /**
   * @return {@code true} if this file has been created manually and the containing version folder shall be ignored by
   *         the automatic update process, {@code false} otherwise.
   */
  public boolean isManual() {

    return this.manual;
  }

  /**
   * @param manual the new value of {@link #isManual()}.
   */
  public void setManual(boolean manual) {

    this.manual = manual;
  }

  /**
   * @return the {@link Map} with the {@link UrlStatus} objects. The {@link Map#keySet() keys} are the
   *         {@link String#hashCode() hash-codes} of the URLs.
   */
  public Map<Integer, UrlStatus> getUrls() {

    return this.urls;
  }

  /**
   * @param urlStatuses the new value of {@link #getUrls()}.
   */
  public void setUrls(Map<Integer, UrlStatus> urlStatuses) {

    this.urls = urlStatuses;
  }

  /**
   * @param url the URL to get or create the {@link UrlStatus} for.
   * @return the existing {@link UrlStatus} for the given URL or a new {@link UrlStatus} associated with the given URL.
   */
  public UrlStatus getOrCreateUrlStatus(String url) {

    Integer key = Integer.valueOf(url.hashCode());
    return this.urls.computeIfAbsent(key, hash -> new UrlStatus());
  }
}