package com.devonfw.tools.ide.url.updater;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.devonfw.tools.ide.maven.MavenMetadata;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * The MvnCrawler class is an abstract class that provides functionality for crawling Maven repositories.
 */
public abstract class MavenBasedUrlUpdater extends AbstractUrlUpdater {

  private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

  private final String mavenBaseRepoUrl;

  /**
   * The constructor.
   */
  public MavenBasedUrlUpdater() {

    super();
    this.mavenBaseRepoUrl = "https://repo1.maven.org/maven2/" + getMavenGroupIdPath() + "/" + getMavenArtifcatId()
        + "/";

  }

  /**
   * @return the maven groupId as path (e.g. "com/devonfw/cobigen").
   */
  protected abstract String getMavenGroupIdPath();

  /**
   * @return the maven artifactId (e.g. "cobigen-cli").
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

    return doGetVersionsFromMavenApi(this.mavenBaseRepoUrl + MAVEN_METADATA_XML);
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String version = urlVersion.getName();
    String url = this.mavenBaseRepoUrl + version + "/" + getMavenArtifcatId() + "-" + version + getExtension();
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
      MavenMetadata metaData = mapper.readValue(response, MavenMetadata.class);
      for (String version : metaData.getVersioning().getVersions()) {
        addVersion(version, versions);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to get version from " + url, e);
    }
    return versions;
  }

}
