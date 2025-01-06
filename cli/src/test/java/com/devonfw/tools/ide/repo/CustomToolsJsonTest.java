package com.devonfw.tools.ide.repo;

import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeTestContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeSlf4jContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link CustomToolsJson}.
 */
public class CustomToolsJsonTest extends Assertions {

  @Test
  public void testReadCustomToolsFromJson() {
    // arrange
    AbstractIdeTestContext context = new IdeSlf4jContext(Path.of(""));
    SystemInfo systemInfo = SystemInfoMock.of("linux");
    context.setSystemInfo(systemInfo);
    Path testPath = Path.of("src/test/resources/customtools");
    // act
    CustomToolsJson customToolsJson = CustomToolRepositoryImpl.readCustomToolsFromJson(context, testPath.resolve("ide-custom-tools.json"));
    // assert
    assertThat(customToolsJson.url()).isEqualTo("https://some-file-server.company.com/projects/my-project");
    assertThat(customToolsJson.tools().get(0).getUrl()).isEqualTo(
        "https://some-file-server.company.com/projects/my-project/jboss-eap/7.1.4.GA/jboss-eap-7.1.4.GA.tgz");
    assertThat(customToolsJson.tools().get(1).getUrl()).isEqualTo(
        "https://some-file-server.company.com/projects/my-project2/firefox/70.0.1/firefox-70.0.1-linux-x64.tgz");
  }

  @Test
  public void testReadCustomToolsFromLegacyConfig() {
    // arrange
    IdeContext context = new IdeTestContext();
    String legacyProperties = "(jboss-eap:7.1.4.GA:all:https://host.tld/projects/my-project firefox:70.0.1:all:https://host.tld/projects/my-project2)";
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
    String legacyProperties = "()";
    // act
    CustomToolsJson customToolsJson = CustomToolsJson.retrieveCustomToolsFromLegacyConfig(legacyProperties, context);
    // assert
    assertThat(customToolsJson).isNull();
  }

  @Test
  public void testReadFaultyCustomToolsFromLegacyConfig() {
    // arrange
    IdeContext context = new IdeTestContext();
    String legacyProperties = "(jboss-eap:7.1.4.GA:all)";
    // act
    CustomToolsJson customToolsJson = CustomToolsJson.retrieveCustomToolsFromLegacyConfig(legacyProperties, context);
    // assert
    assertThat(customToolsJson).isNull();
  }
}
