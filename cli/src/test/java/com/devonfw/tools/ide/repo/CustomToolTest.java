package com.devonfw.tools.ide.repo;

import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.version.VersionIdentifier;

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

  @Test
  public void testReadCustomToolsFromJson() {
    // arrange
    IdeContext context = new IdeTestContext();
    Path testPath = Path.of("src/test/resources/customtools");
    // act
    CustomToolsJson customToolsJson = CustomToolRepositoryImpl.readCustomToolsFromJson(context, testPath.resolve("custom-tools.json"));
    // assert
    assertThat(customToolsJson.url()).isEqualTo("https://some-file-server.company.com/projects/my-project");
    assertThat(customToolsJson.tools().get(0).getUrl()).isEqualTo(
        "https://some-file-server.company.com/projects/my-project/jboss-eap/7.1.4.GA/jboss-eap-7.1.4.GA.tgz");
    assertThat(customToolsJson.tools().get(1).getUrl()).isEqualTo(
        "https://some-file-server.company.com/projects/my-project2/firefox/70.0.1/firefox-70.0.1-windows-x64.tgz");
  }

  @Test
  public void testReadCustomToolsFromLegacyConfig() {
    // arrange
    IdeContext context = new IdeTestContext();
    String legacyProperties = "DEVON_IDE_CUSTOM_TOOLS=(jboss-eap:7.1.4.GA:all:https://host.tld/projects/my-project firefox:70.0.1:all:https://host.tld/projects/my-project2)";
    // act
    CustomToolsJson customToolsJson = CustomToolsJson.retrieveCustomToolsFromLegacyConfig(legacyProperties, context);
    // assert
    assertThat(customToolsJson.url()).isEqualTo("https://host.tld/projects/my-project");
    assertThat(customToolsJson.tools().get(0).getUrl()).isEqualTo(
        "https://host.tld/projects/my-project/jboss-eap/7.1.4.GA/jboss-eap-7.1.4.GA.tgz");
    assertThat(customToolsJson.tools().get(1).getUrl()).isEqualTo(
        "https://host.tld/projects/my-project2/firefox/70.0.1/firefox-70.0.1.tgz");
  }

  @Test
  public void testReadEmptyCustomToolsFromLegacyConfig() {
    // arrange
    IdeContext context = new IdeTestContext();
    String legacyProperties = "DEVON_IDE_CUSTOM_TOOLS=()";
    // act
    CustomToolsJson customToolsJson = CustomToolsJson.retrieveCustomToolsFromLegacyConfig(legacyProperties, context);
    // assert
    assertThat(customToolsJson).isNull();
  }

  @Test
  public void testReadFaultyCustomToolsFromLegacyConfig() {
    // arrange
    IdeContext context = new IdeTestContext();
    String legacyProperties = "DEVON_IDE_CUSTOM_TOOLS=(jboss-eap:7.1.4.GA:all)";
    // act
    CustomToolsJson customToolsJson = CustomToolsJson.retrieveCustomToolsFromLegacyConfig(legacyProperties, context);
    // assert
    assertThat(customToolsJson).isNull();
  }
}
