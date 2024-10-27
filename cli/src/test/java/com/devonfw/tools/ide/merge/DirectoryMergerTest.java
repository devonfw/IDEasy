package com.devonfw.tools.ide.merge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * Test of {@link DirectoryMerger}.
 */
public class DirectoryMergerTest extends AbstractIdeContextTest {

  private static final String IDE_HOME = TEST_PROJECTS.resolve(PROJECT_BASIC).resolve("project").toAbsolutePath().toString().replace('\\', '/');

  private static final Prop JAVA_VERSION = new Prop("java.version", "1.11");

  private static final Prop JAVA_HOME = new Prop("java.home", IDE_HOME + "/software/java");

  private static final Prop THEME = new Prop("theme", "dark");

  private static final Prop UI = new Prop("ui", "classic");

  private static final Prop INDENTATION = new Prop("indentation", "2");

  private static final Prop THEME_HACKED = new Prop("theme", "light");

  private static final Prop UI_HACKED = new Prop("ui", "linux");

  private static final Prop INDENTATION_HACKED = new Prop("indentation", "4");

  private static final Prop JAVA_VERSION_HACKED = new Prop("java.version", "1.99");

  private static final Prop EDITOR = new Prop("editor", "vi");

  /**
   * Test of {@link DirectoryMerger}.
   *
   * @param workspaceDir the temporary folder to use as workspace for this test.
   * @throws Exception on error.
   */
  @Test
  public void testConfigurator(@TempDir Path workspaceDir) throws Exception {

    // arrange
    IdeContext context = newContext(PROJECT_BASIC, null, false);
    DirectoryMerger merger = context.getWorkspaceMerger();
    Path templates = Path.of("src/test/resources/templates");
    Path setup = templates.resolve(IdeContext.FOLDER_SETUP);
    Path update = templates.resolve(IdeContext.FOLDER_UPDATE);
    Path namePath = workspaceDir.resolve(".name");
    // to check overwrite for Text files
    Files.createFile(namePath);

    // act
    merger.merge(setup, update, context.getVariables(), workspaceDir);

    // assert
    Path mainPrefsFile = workspaceDir.resolve("main.prefs");
    Properties mainPrefs = PropertiesMerger.load(mainPrefsFile);
    assertThat(mainPrefs).containsOnly(JAVA_VERSION, JAVA_HOME, THEME, UI);
    Path jsonFolder = workspaceDir.resolve("json");
    assertThat(jsonFolder).isDirectory();
    assertThat(jsonFolder.resolve("settings.json")).hasContent("""
        {
            "java.home": "${IDE_HOME}/software/java",
            "tslint.autoFixOnSave": true,
            "object": {
                "bar": "${IDE_HOME}/bar",
                "array": [
                    "a",
                    "b",
                    "${IDE_HOME}"
                ],
                "foo": "${IDE_HOME}/foo"
            }
        }
        """.replace("${IDE_HOME}", IDE_HOME));
    assertThat(jsonFolder.resolve("update.json")).hasContent("""
        {
            "key": "value"
        }
        """);

    Path configFolder = workspaceDir.resolve("config");
    assertThat(configFolder).isDirectory();
    Path indentFile = configFolder.resolve("indent.properties");
    Properties indent = PropertiesMerger.load(indentFile);
    assertThat(indent).containsOnly(INDENTATION);
    assertThat(configFolder.resolve("layout.xml")).hasContent("""
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <layout>
          <left>navigator</left>
          <right>debugger</right>
          <top>editor</top>
          <bottom>console</bottom>
          <test path="${IDE_HOME}">${IDE_HOME}</test>
        </layout>
            """.replace("${IDE_HOME}", IDE_HOME));

    // and arrange
    EDITOR.apply(mainPrefs);
    JAVA_VERSION_HACKED.apply(mainPrefs);
    UI_HACKED.apply(mainPrefs);
    THEME_HACKED.apply(mainPrefs);
    INDENTATION_HACKED.apply(mainPrefs);
    PropertiesMerger.save(mainPrefs, mainPrefsFile);

    // act
    merger.merge(setup, update, context.getVariables(), workspaceDir);

    // assert
    mainPrefs = PropertiesMerger.load(mainPrefsFile);
    assertThat(mainPrefs).containsOnly(JAVA_VERSION, JAVA_HOME, THEME_HACKED, UI_HACKED, EDITOR, INDENTATION_HACKED);

    assertThat(namePath).hasContent("project - main\ntest");
  }

  private static class Prop implements Entry<String, String> {

    private final String key;

    private String value;

    private Prop(String key, String value) {

      super();
      this.key = key;
      this.value = value;
    }

    @Override
    public String getKey() {

      return this.key;
    }

    @Override
    public String getValue() {

      return this.value;
    }

    @Override
    public String setValue(String value) {

      throw new IllegalStateException(value);
    }

    public void apply(Properties properties) {

      properties.setProperty(this.key, this.value);
    }

    @Override
    public String toString() {

      return this.key + "=" + this.value;
    }

  }

}
