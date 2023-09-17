package com.devonfw.tools.ide.url.updater.python;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Java object to represent the JSON of a single download file of a Python release. Mapping just the needed properties
 */
public class PythonFile {

  @JsonProperty("filename")
  private String filename;

  @JsonProperty("arch")
  private String arch;

  @JsonProperty("platform")
  private String platform;

  @JsonProperty("download_url")
  private String download_url;

  /**
   * @return the filename of the download.
   */
  public String getFilename() {

    return this.filename;
  }

  /**
   * @return the operating system of the download.
   */
  public String getPlatform() {

    return this.platform;
  }

  /**
   * @return the system architecture of the download.
   */
  public String getArch() {

    return this.arch;
  }

  /**
   * @return the URL of the download.
   */
  public String getDownloadUrl() {

    return this.download_url;
  }
}
