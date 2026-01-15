package com.devonfw.tools.ide.url.updater;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.devonfw.tools.ide.tool.mvn.MvnMetadata;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * The MvnCrawler class is an abstract class that provides functionality for crawling Maven repositories.
 */
public abstract class MavenBasedUrlUpdater extends AbstractUrlUpdater {

  private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

  /**
   * The constructor.
   */
  public MavenBasedUrlUpdater() {

    super();
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://repo1.maven.org/maven2";
  }

  private String getDownloadArtifactUrl() {

    return getDownloadBaseUrl() + "/" + getMavenGroupIdPath() + "/" + getMavenArtifcatId() + "/";
  }

  /**
   * @return the maven groupId as path.
   */
  protected abstract String getMavenGroupIdPath();

  /**
   * @return the maven artifactId.
   */
  protected abstract String getMavenArtifcatId();

  /**
   * @return the artifact extension including the dot (e.g. ".jar").
   */
  protected String getExtension() {

    return ".jar";
  }

  @Override
  protected Set<String> getVersions() {

    return doGetVersionsFromMavenApi(getDownloadArtifactUrl() + MAVEN_METADATA_XML);
  }

  @Override
  protected String getVersionBaseUrl() {

    return getDownloadBaseUrl();
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String version = urlVersion.getName();
    String url = getDownloadArtifactUrl() + version + "/" + getMavenArtifcatId() + "-" + version + getExtension();
    doAddVersion(urlVersion, url);
  }

  /**
   * Gets the versions from the Maven API.
   *
   * @param url The Url of the metadata.xml file
   * @return The versions.
   */
  private Set<String> doGetVersionsFromMavenApi(String url) {

    Set<String> versions = new HashSet<>();
    try {
      String response = doGetResponseBodyAsString(url);
      XmlMapper mapper = new XmlMapper();
      MvnMetadata metaData = mapper.readValue(response, MvnMetadata.class);
      for (String version : metaData.getVersioning().getVersions()) {
        if (isValidVersion(version)) {
          addVersion(version, versions);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to get version from " + url, e);
    }
    return versions;
  }

  /**
   * Subclasses should override this method to enforce version validation.
   *
   * @param version the version of the artifact.
   * @return true as default implementation.
   */
  protected boolean isValidVersion(String version) {

    return true;
  }

}
