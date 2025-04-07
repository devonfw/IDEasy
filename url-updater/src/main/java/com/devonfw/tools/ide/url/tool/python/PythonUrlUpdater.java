package com.devonfw.tools.ide.url.tool.python;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link JsonUrlUpdater} for Python
 */
public class PythonUrlUpdater extends JsonUrlUpdater<PythonJsonObject, PythonRelease> {

  /**
   * The base Url of the Python versions Json
   */
  private final String VERSION_BASE_URL = "https://raw.githubusercontent.com";

  private final static String VERSION_FILENAME = "actions/python-versions/main/versions-manifest.json";

  final static ObjectMapper MAPPER = JsonMapping.create();

  private static final Logger logger = LoggerFactory.getLogger(PythonUrlUpdater.class);

  @Override
  protected String getTool() {

    return "python";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion, PythonRelease release) {

    String version = release.version();

    try {
      for (PythonFile download : release.files()) {
        if (download.getPlatform().equals("win32") && download.getArch().equals("x64")) {
          doAddVersion(urlVersion, download.getDownloadUrl(), WINDOWS, X64);
        } else if (download.getPlatform().equals("linux") && download.getArch().equals("x64")) {
          doAddVersion(urlVersion, download.getDownloadUrl(), LINUX, X64);
        } else if (download.getPlatform().equals("darwin") && download.getArch().equals("arm64")) {
          doAddVersion(urlVersion, download.getDownloadUrl(), MAC, ARM64);
        } else {
          logger.info("Unknown architecture for tool {} version {} and download {}.", getToolWithEdition(), version,
              download.getDownloadUrl());
        }
      }
      urlVersion.save();
    } catch (Exception exp) {
      logger.error("For tool {} we failed to add version {}.", getToolWithEdition(), version, exp);
    }
  }

  /**
   * @return String of version base Url
   */
  protected String getVersionBaseUrl() {

    return this.VERSION_BASE_URL;
  }

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + "/" + VERSION_FILENAME;
  }

  @Override
  public String getCpeVendor() {
    return "python";
  }

  @Override
  public String getCpeProduct() {
    return "python";
  }

  @Override
  protected Class<PythonJsonObject> getJsonObjectType() {

    return PythonJsonObject.class;
  }

  @Override
  protected PythonJsonObject getJsonObjectFromResponse(String response, String edition) throws JsonProcessingException {
    PythonRelease[] res = MAPPER.readValue(response, PythonRelease[].class);
    PythonJsonObject jsonObj = new PythonJsonObject();
    jsonObj.setReleases(List.of(res));
    return jsonObj;
  }

  @Override
  protected Collection<PythonRelease> getVersionItems(PythonJsonObject jsonObject) {

    return jsonObject.getReleases();
  }

}
