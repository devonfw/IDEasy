package com.devonfw.tools.ide.tool.custom;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeTestContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeSlf4jContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link CustomToolsJsonMapper}.
 */
public class CustomToolsJsonMapperTest extends Assertions {

  @Test
  public void testReadCustomToolsFromJson() {
    // arrange
    Path testPath = Path.of("src/test/resources/customtools");
    // act
    CustomToolsJson customToolsJson = CustomToolsJsonMapper.loadJson(testPath.resolve("ide-custom-tools.json"));
    // assert
    assertThat(customToolsJson.url()).isEqualTo("https://some-file-server.company.com/projects/my-project");
    // assert that custom tools content matches to json file
    assertThat(customToolsJson.tools()).containsExactly(new CustomToolJson("jboss-eap", "7.1.4.GA", true, true,
            null),
        new CustomToolJson("firefox", "70.0.1", false, false,
            "https://some-file-server.company.com/projects/my-project2"));
  }

  @Test
  public void testReadCustomToolsFromLegacyConfig() {
    // arrange
    IdeContext context = new IdeTestContext();
    String legacyProperties = "(jboss-eap:7.1.4.GA:all:https://host.tld/projects/my-project firefox:70.0.1:all:)";
    // act
    CustomToolsJson customToolsJson = CustomToolsJsonMapper.parseCustomToolsFromLegacyConfig(legacyProperties, context);
    // assert
    assertThat(customToolsJson.url()).isEqualTo("https://host.tld/projects/my-project");
    assertThat(customToolsJson.tools()).containsExactly(new CustomToolJson("jboss-eap", "7.1.4.GA", true, true,
            null),
        new CustomToolJson("firefox", "70.0.1", true, true, ""));
  }

  @Test
  public void testReadEmptyCustomToolsFromLegacyConfig() {
    // arrange
    IdeContext context = new IdeTestContext();
    String legacyProperties = "()";
    // act
    CustomToolsJson customToolsJson = CustomToolsJsonMapper.parseCustomToolsFromLegacyConfig(legacyProperties, context);
    // assert
    assertThat(customToolsJson).isNull();
  }

  @Test
  public void testReadFaultyCustomToolsFromLegacyConfig() {
    // arrange
    IdeContext context = new IdeTestContext();
    String legacyProperties = "(jboss-eap:7.1.4.GA:all)";
    // act
    CustomToolsJson customToolsJson = CustomToolsJsonMapper.parseCustomToolsFromLegacyConfig(legacyProperties, context);
    // assert
    assertThat(customToolsJson).isNull();
  }

  /**
   * Tests the convert of a {@link CustomToolsJson} with different os and arch agnostic settings to a proper {@link CustomToolMetadata}.
   */
  @Test
  public void testProperConvertFromCustomToolsJsonToCustomToolMetaData() {

    // arrange
    AbstractIdeTestContext context = new IdeSlf4jContext(Path.of(""));
    context.setSystemInfo(SystemInfoMock.LINUX_X64);
    String name = "jboss-eap";
    String version = "7.4.5.GA";
    String repositoryUrl = "https://host.domain.tld:8443/folder/repo/";
    String url = repositoryUrl + "jboss-eap/7.4.5.GA/jboss-eap-7.4.5.GA.tgz";
    OperatingSystem os = null;
    SystemArchitecture arch = null;

    String name1 = "firefox";
    String version1 = "70.0.1";
    String repositoryUrl1 = "https://host.domain.tld:8443/folder/repo/";
    String checkOsArchUrl = repositoryUrl1 + "firefox/70.0.1/firefox-70.0.1-linux-x64.tgz";
    OperatingSystem os1 = OperatingSystem.LINUX;
    SystemArchitecture arch1 = SystemArchitecture.X64;

    CustomToolJson customToolJson = new CustomToolJson(name, version, true, true, repositoryUrl);
    CustomToolJson customToolJsonWithOs = new CustomToolJson(name1, version1, false, false, repositoryUrl1);
    List<CustomToolJson> customToolJsonList = new ArrayList<>();
    customToolJsonList.add(customToolJson);
    customToolJsonList.add(customToolJsonWithOs);
    CustomToolsJson customToolsJson = new CustomToolsJson(repositoryUrl, customToolJsonList);
    // act
    List<CustomToolMetadata> customToolMetadata = CustomToolsJsonMapper.convert(customToolsJson, context);
    // assert
    assertThat(customToolMetadata.get(0).getTool()).isEqualTo(name);
    assertThat(customToolMetadata.get(0).getVersion()).isEqualTo(VersionIdentifier.of(version));
    assertThat(customToolMetadata.get(0).getOs()).isEqualTo(os);
    assertThat(customToolMetadata.get(0).getArch()).isEqualTo(arch);
    assertThat(customToolMetadata.get(0).getUrl()).isEqualTo(url);
    assertThat(customToolMetadata.get(0).getChecksums()).isNull();
    assertThat(customToolMetadata.get(0).getRepositoryUrl()).isEqualTo(repositoryUrl);

    assertThat(customToolMetadata.get(1).getTool()).isEqualTo(name1);
    assertThat(customToolMetadata.get(1).getVersion()).isEqualTo(VersionIdentifier.of(version1));
    assertThat(customToolMetadata.get(1).getOs()).isEqualTo(os1);
    assertThat(customToolMetadata.get(1).getArch()).isEqualTo(arch1);
    // assert that url was properly created
    assertThat(customToolMetadata.get(1).getUrl()).isEqualTo(checkOsArchUrl);
    assertThat(customToolMetadata.get(1).getChecksums()).isNull();
    assertThat(customToolMetadata.get(1).getRepositoryUrl()).isEqualTo(repositoryUrl1);
  }
}
