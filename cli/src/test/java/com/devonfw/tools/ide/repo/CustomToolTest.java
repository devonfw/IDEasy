package com.devonfw.tools.ide.repo;

import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.version.VersionIdentifier;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link CustomTool}.
 */
public class CustomToolTest extends Assertions {

  /**
   * Test of {@link CustomTool}.
   */
  @Test
  public void testAgnostic() {

    // arrange
    String name = "jboss-eap";
    VersionIdentifier version = VersionIdentifier.of("7.4.5.GA");
    String repositoryUrl = "https://host.domain.tld:8443/folder/repo";
    String checksum = "4711";
    boolean osAgnostic = true;
    boolean archAgnostic = true;
    // act
    CustomTool tool = new CustomTool(name, version, osAgnostic, archAgnostic, repositoryUrl, checksum, null);
    // assert
    assertThat(tool.getTool()).isEqualTo(name);
    assertThat(tool.getVersion()).isSameAs(version);
    assertThat(tool.isOsAgnostic()).isEqualTo(osAgnostic);
    assertThat(tool.isArchAgnostic()).isEqualTo(archAgnostic);
    assertThat(tool.getRepositoryUrl()).isEqualTo(repositoryUrl);
    assertThat(tool.getUrl()).isEqualTo(
        "https://host.domain.tld:8443/folder/repo/jboss-eap/7.4.5.GA/jboss-eap-7.4.5.GA.tgz");
    assertThat(tool.getChecksum()).isEqualTo(checksum);
  }

  /**
   * Test of {@link CustomTool}.
   */
  @Test
  public void testSpecific() {

    // arrange
    String name = "firefox";
    VersionIdentifier version = VersionIdentifier.of("70.0.1");
    String repositoryUrl = "https://host.domain.tld:8443/folder/repo";
    String checksum = "4711";
    boolean osAgnostic = false;
    boolean archAgnostic = true;
    // act
    CustomTool tool = new CustomTool(name, version, osAgnostic, archAgnostic, repositoryUrl, checksum,
        SystemInfoMock.WINDOWS_X64);
    // assert
    assertThat(tool.getTool()).isEqualTo(name);
    assertThat(tool.getVersion()).isSameAs(version);
    assertThat(tool.isOsAgnostic()).isEqualTo(osAgnostic);
    assertThat(tool.isArchAgnostic()).isEqualTo(archAgnostic);
    assertThat(tool.getRepositoryUrl()).isEqualTo(repositoryUrl);
    assertThat(tool.getUrl()).isEqualTo(
        "https://host.domain.tld:8443/folder/repo/firefox/70.0.1/firefox-70.0.1-windows.tgz");
    assertThat(tool.getChecksum()).isEqualTo(checksum);
  }

}
