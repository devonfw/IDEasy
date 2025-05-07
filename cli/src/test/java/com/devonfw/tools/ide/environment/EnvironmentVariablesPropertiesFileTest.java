package com.devonfw.tools.ide.environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link EnvironmentVariablesPropertiesFile}.
 */
@SuppressWarnings("javadoc")
class EnvironmentVariablesPropertiesFileTest extends AbstractIdeContextTest {

  private static final Path ENV_VAR_PATH = Path.of("src/test/resources/com/devonfw/tools/ide/environment/var/");
  private static final EnvironmentVariablesType TYPE = EnvironmentVariablesType.SETTINGS;

  /**
   * Test of {@link EnvironmentVariablesPropertiesFile} including legacy support.
   */
  @Test
  public void testLoad() {

    // arrange
    Path propertiesFilePath = ENV_VAR_PATH.resolve("devon.properties");
    IdeTestContext context = new IdeTestContext();
    // act
    EnvironmentVariablesPropertiesFile variables = new EnvironmentVariablesPropertiesFile(null, TYPE,
        propertiesFilePath, context);
    // assert
    assertThat(variables.getType()).isSameAs(TYPE);
    assertThat(variables.get("MVN_VERSION")).isEqualTo("3.9.0");
    assertThat(variables.get("IDE_TOOLS")).isEqualTo("mvn, npm");
    assertThat(variables.get("CREATE_START_SCRIPTS")).isEqualTo("eclipse");
    assertThat(variables.get("KEY")).isEqualTo("value");
    assertThat(variables.get("IDE_MIN_VERSION")).isEqualTo("2024.11.001");
    assertThat(variables.getToolVersion("java")).isEqualTo(VersionIdentifier.LATEST);
    assertThat(variables.getVariables()).hasSize(7);
    assertThat(context).log(IdeLogLevel.WARNING)
        .hasEntries("Duplicate variable definition MVN_VERSION with old value 'undefined' and new value '3.9.0' in " + propertiesFilePath,
            "Both legacy variable MAVEN_VERSION and official variable MVN_VERSION are configured in " + propertiesFilePath
                + " - ignoring legacy variable declaration!",
            "Variable JAVA_VERSION is configured with empty value, please fix your configuration.");
  }

  @Test
  void testSave(@TempDir Path tempDir) throws Exception {

    // arrange
    List<String> linesToWrite = new ArrayList<>();
    linesToWrite.add("# first comment");
    linesToWrite.add("# second comment");
    linesToWrite.add("var0=0");
    linesToWrite.add("var1=1");
    linesToWrite.add("var2=2");
    linesToWrite.add("export var3=3");
    linesToWrite.add("export var4=4");
    linesToWrite.add("export var5=5");
    linesToWrite.add("export var6=6");
    linesToWrite.add("# third comment");
    linesToWrite.add("var7=7");
    linesToWrite.add("var8=8");
    linesToWrite.add("# 4th comment");
    linesToWrite.add("# 5th comment");
    linesToWrite.add("var9=9");

    Path propertiesFilePath = tempDir.resolve("test.properties");
    Files.write(propertiesFilePath, linesToWrite, StandardOpenOption.CREATE_NEW);
    // check if this writing was correct
    List<String> lines = Files.readAllLines(propertiesFilePath);
    assertThat(lines).containsExactlyElementsOf(linesToWrite);

    EnvironmentVariablesPropertiesFile variables = new EnvironmentVariablesPropertiesFile(null, TYPE,
        propertiesFilePath, IdeTestContextMock.get());

    // act
    variables.set("var5", "5", true);
    variables.set("var1", "1.0", false);
    variables.set("var10", "10", false);
    variables.set("var11", "11", true);
    variables.set("var3", "3", false);
    variables.set("var7", "7", true);
    variables.set("var6", "6.0", true);
    variables.set("var4", "4.0", false);
    variables.set("var8", "8.0", true);

    variables.save();

    // assert
    List<String> linesAfterSave = new ArrayList<>();
    linesAfterSave.add("# first comment");
    linesAfterSave.add("# second comment");
    linesAfterSave.add("var0=0");
    linesAfterSave.add("var1=1.0");
    linesAfterSave.add("var2=2");
    linesAfterSave.add("var3=3");
    linesAfterSave.add("var4=4.0");
    linesAfterSave.add("export var5=5");
    linesAfterSave.add("export var6=6.0");
    linesAfterSave.add("# third comment");
    linesAfterSave.add("export var7=7");
    linesAfterSave.add("export var8=8.0");
    linesAfterSave.add("# 4th comment");
    linesAfterSave.add("# 5th comment");
    linesAfterSave.add("var9=9");
    linesAfterSave.add("var10=10");
    linesAfterSave.add("export var11=11");

    lines = Files.readAllLines(propertiesFilePath);
    assertThat(lines).containsExactlyElementsOf(linesAfterSave);
  }

  @Test
  void testSaveWithMissingParentFilePath(@TempDir Path tempDir) throws Exception {
    // arrange
    Path propertiesFilePath = tempDir.resolve("test.properties");

    EnvironmentVariablesPropertiesFile variables = new EnvironmentVariablesPropertiesFile(null, TYPE,
        propertiesFilePath, IdeTestContextMock.get());

    // act
    variables.set("var1", "1.0", false);
    variables.set("var2", "2", true);

    variables.save();

    // assert
    List<String> linesAfterSave = new ArrayList<>();
    linesAfterSave.add("export var2=2");
    linesAfterSave.add("var1=1.0");

    List<String> lines = Files.readAllLines(propertiesFilePath);
    assertThat(lines).containsExactlyElementsOf(linesAfterSave);
  }

}
