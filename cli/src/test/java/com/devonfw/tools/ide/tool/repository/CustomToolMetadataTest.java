package com.devonfw.tools.ide.tool.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.file.UrlChecksums;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link CustomToolMetadata}.
 */
public class CustomToolMetadataTest extends Assertions {

  /**
   * Test of {@link CustomToolMetadata}.
   */
  @Test
  public void testAgnostic() {

    // arrange
    String name = "jboss-eap";
    String version = "7.4.5.GA";
    String repositoryUrl = "https://host.domain.tld:8443/folder/repo/";
    String url = repositoryUrl + "jboss-eap/7.4.5.GA/jboss-eap-7.4.5.GA.tgz";
    UrlChecksums checksums = null;
    OperatingSystem os = null;
    SystemArchitecture arch = null;
    // act
    CustomToolMetadata tool = new CustomToolMetadata(name, version, os, arch, url, checksums, repositoryUrl);
    // assert
    assertThat(tool.getTool()).isEqualTo(name);
    assertThat(tool.getVersion()).isEqualTo(VersionIdentifier.of(version));
    assertThat(tool.getOs()).isEqualTo(os);
    assertThat(tool.getArch()).isEqualTo(arch);
    assertThat(tool.getUrl()).isEqualTo(url);
    assertThat(tool.getChecksums()).isEqualTo(checksums);
    assertThat(tool.getRepositoryUrl()).isEqualTo(repositoryUrl);
  }

  /**
   * Test of {@link CustomToolMetadata}.
   */
  @Test
  public void testSpecific() {

    // arrange
    String name = "firefox";
    String version = "70.0.1";
    String repositoryUrl = "https://host.domain.tld:8443/folder/repo";
    String url = repositoryUrl + "/firefox/70.0.1/firefox-70.0.1-windows.tgz";
    UrlChecksums checksums = null;
    OperatingSystem os = OperatingSystem.MAC;
    SystemArchitecture arch = SystemArchitecture.ARM64;
    // act
    CustomToolMetadata tool = new CustomToolMetadata(name, version, os, arch, url, checksums, repositoryUrl);
    // assert
    assertThat(tool.getTool()).isEqualTo(name);
    assertThat(tool.getVersion()).isEqualTo(VersionIdentifier.of(version));
    assertThat(tool.getOs()).isEqualTo(os);
    assertThat(tool.getArch()).isEqualTo(arch);
    assertThat(tool.getUrl()).isEqualTo(url);
    assertThat(tool.getChecksums()).isEqualTo(checksums);
    assertThat(tool.getRepositoryUrl()).isEqualTo(repositoryUrl);
  }


}
