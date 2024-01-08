package com.devonfw.tools.ide.environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.IdeTestContextMock;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test of {@link EnvironmentVariablesPropertiesFile}.
 */
@SuppressWarnings("javadoc")
class EnvironmentVariablesPropertiesFileTest extends Assertions {

  /**
   * Test of {@link EnvironmentVariablesPropertiesFile} including legacy support.
   */
  @Test
  public void testLoad() {

    // arrange
    AbstractEnvironmentVariables parent = null;
    Path propertiesFilePath = Path.of("src/test/resources/com/devonfw/tools/ide/env/var/devon.properties");
    EnvironmentVariablesType type = EnvironmentVariablesType.SETTINGS;
    // act
    EnvironmentVariablesPropertiesFile variables = new EnvironmentVariablesPropertiesFile(parent, type,
        propertiesFilePath, IdeTestContextMock.get());
    // assert
    assertThat(variables.getType()).isSameAs(type);
    assertThat(variables.get("MVN_VERSION")).isEqualTo("3.9.0");
    assertThat(variables.get("IDE_TOOLS")).isEqualTo("mvn, npm");
    assertThat(variables.get("CREATE_START_SCRIPTS")).isEqualTo("eclipse");
    assertThat(variables.get("KEY")).isEqualTo("value");
    assertThat(variables.getVariables()).hasSize(4);
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

    AbstractEnvironmentVariables parent = null;
    EnvironmentVariablesType type = EnvironmentVariablesType.SETTINGS;

    EnvironmentVariablesPropertiesFile variables = new EnvironmentVariablesPropertiesFile(parent, type,
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
}