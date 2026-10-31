package com.devonfw.tools.ide.url.tool.gcloud;

import java.util.Collection;
import java.util.List;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link JsonUrlUpdater} for the Google Cloud CLI using the latest rapid-channel manifest.
 */
public class GCloudUrlUpdater extends JsonUrlUpdater<GCloudJsonVersion, GCloudJsonVersion> {

  private static final String DOWNLOAD_BASE_URL = "https://dl.google.com";

  private static final VersionIdentifier MIN_GCLOUD_VID = VersionIdentifier.of("299.0.0");
  private static final VersionIdentifier MIN_ARM_GCLOUD_VID = VersionIdentifier.of("366.0.0");

  /**
   * The Constructor.
   */
  public GCloudUrlUpdater() {

    super(DOWNLOAD_BASE_URL, DOWNLOAD_BASE_URL);
  }

  /**
   * Package-private constructor used for testing {@link GCloudUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  GCloudUrlUpdater(String baseUrl) {

    super(baseUrl, baseUrl);
  }

  @Override
  public String getTool() {

    return "gcloud";
  }

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + "/dl/cloudsdk/channels/rapid/components-2.json";
  }

  @Override
  protected Class<GCloudJsonVersion> getJsonObjectType() {

    return GCloudJsonVersion.class;
  }

  @Override
  protected Collection<GCloudJsonVersion> getVersionItems(GCloudJsonVersion jsonObject) {

    return List.of(jsonObject);
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    String baseUrl = getDownloadBaseUrl() + "/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${version}-";
    if (vid.compareVersion(MIN_GCLOUD_VID).isGreater()) {
      doAddVersion(urlVersion, baseUrl + "windows-x86_64-bundled-python.zip", WINDOWS);
      doAddVersion(urlVersion, baseUrl + "linux-x86_64.tar.gz", LINUX);
      doAddVersion(urlVersion, baseUrl + "darwin-x86_64.tar.gz", MAC);
      if (vid.compareVersion(MIN_ARM_GCLOUD_VID).isGreater()) {
        doAddVersion(urlVersion, baseUrl + "linux-arm.tar.gz", LINUX, ARM64);
        doAddVersion(urlVersion, baseUrl + "darwin-arm.tar.gz", MAC, ARM64);
      }
    }
  }


}
