package com.devonfw.tools.ide.merge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Properties;

import com.devonfw.tools.ide.environment.EnvironmentVariables;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;

/**
 * Test of {@link DirectoryMerger}.
 */
class DirectoryMergerTest extends AbstractIdeContextTest {

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
  void testConfigurator(@TempDir Path workspaceDir) throws Exception {

    // arrange
    IdeContext context = newContext(PROJECT_BASIC, null, false);
    DirectoryMerger merger = context.getWorkspaceMerger();
    PropertiesMerger propertiesMerger = new PropertiesMerger(context);
    Path templates = Path.of("src/test/resources/templates");
    Path setup = templates.resolve(IdeContext.FOLDER_SETUP);
    Path update = templates.resolve(IdeContext.FOLDER_UPDATE);
    Path namePath = workspaceDir.resolve(".name");
    // to check overwrite for Text files
    Files.createFile(namePath);
    FileAccess fileAccess = context.getFileAccess();

    // act
    merger.merge(setup, update, context.getVariables(), workspaceDir);

    // assert
    Path mainPrefsFile = workspaceDir.resolve("main.prefs");
    Properties mainPrefs = fileAccess.readProperties(mainPrefsFile);
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
    Properties indent = fileAccess.readProperties(indentFile);
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
    fileAccess.writeProperties(mainPrefs, mainPrefsFile);

    // act
    merger.merge(setup, update, context.getVariables(), workspaceDir);

    // assert
    mainPrefs = fileAccess.readProperties(mainPrefsFile);
    assertThat(mainPrefs).containsOnly(JAVA_VERSION, JAVA_HOME, THEME_HACKED, UI_HACKED, EDITOR, INDENTATION_HACKED);

    assertThat(namePath).hasContent("project - main\ntest");
  }

  @Test
  void testMergeTwoPropertyFiles(@TempDir Path workspaceDir){
    //arrange
    IdeContext context = newContext(PROJECT_BASIC, null, false);
    DirectoryMerger merger = context.getWorkspaceMerger();

    Path templates = Path.of("src/test/resources/templates");
    Path setup = templates.resolve(IdeContext.FOLDER_SETUP);
    Path update = templates.resolve(IdeContext.FOLDER_UPDATE);
    merger.merge(setup, update, context.getVariables(), workspaceDir);

  FileAccess fileAccess = context.getFileAccess();
  Path workspaceMain = workspaceDir.resolve("main.prefs");
  Path updateMain= workspaceDir.resolve("main.prefs");
  Properties workspaceProperties= fileAccess.readProperties(workspaceMain);

    //act
    workspaceProperties.setProperty("theme", "dark");
    workspaceProperties.setProperty("ui", "linux");
    fileAccess.writeProperties(workspaceProperties, workspaceMain);

    //assert
    Properties result= fileAccess.readProperties(updateMain);
    assertThat(result).containsEntry("theme", "dark").containsEntry("ui", "linux");
  }

@Test
void testInverseMergeRestoresOriginal(@TempDir Path workspaceDir)throws Exception{
  //arrange
    IdeContext context = newContext(PROJECT_BASIC, null, false);
  DirectoryMerger merger = context.getWorkspaceMerger();
  PropertiesMerger propertiesMerger= new PropertiesMerger(context);
  FileAccess fileAccess = context.getFileAccess();
  Path templates = Path.of("src/test/resources/templates");
  Path setup = templates.resolve(IdeContext.FOLDER_SETUP);
  Path update = templates.resolve(IdeContext.FOLDER_UPDATE);
  merger.merge(setup, update, context.getVariables(), workspaceDir);
  EnvironmentVariables environmentVariables = context.getVariables();


  Path workspaceMain = workspaceDir.resolve("main.prefs");
  Path updateMain= workspaceDir.resolve("update-main.prefs");
  Files.copy(update.resolve("main.prefs"), updateMain);
  Properties workspaceProperties= fileAccess.readProperties(workspaceMain);
  workspaceProperties.setProperty("theme", "light");
  workspaceProperties.setProperty("ui", "linux");
  workspaceProperties.setProperty("editor", "vi");
  //act
  fileAccess.writeProperties(workspaceProperties, workspaceMain);
  propertiesMerger.inverseMerge(workspaceMain, environmentVariables, true, updateMain);
  //assert
  Properties result= fileAccess.readProperties(updateMain);
  assertThat(result).containsKey("theme").containsKey("ui").containsKey("editor");
}
@Test
void testConflictResolution(@TempDir Path workspaceDir)throws Exception{
    //arrange
  IdeContext context = newContext(PROJECT_BASIC, null, false);
  DirectoryMerger merger = context.getWorkspaceMerger();
  PropertiesMerger propertiesMerger= new PropertiesMerger(context);
  FileAccess fileAccess = context.getFileAccess();
  Path templates = Path.of("src/test/resources/templates");
  Path setup = templates.resolve(IdeContext.FOLDER_SETUP);
  Path update = templates.resolve(IdeContext.FOLDER_UPDATE);
  merger.merge(setup, update, context.getVariables(), workspaceDir);
  EnvironmentVariables environmentVariables = context.getVariables();


  Path workspaceMain = workspaceDir.resolve("main.prefs");
  Path updateMain= workspaceDir.resolve("update-main.prefs");
  Files.copy(update.resolve("main.prefs"), updateMain);
  Properties workspaceProperties= fileAccess.readProperties(workspaceMain);
  workspaceProperties.setProperty("theme", "light");
  workspaceProperties.setProperty("ui", "linux");
fileAccess.writeProperties(workspaceProperties, workspaceMain);
//act
  propertiesMerger.inverseMerge(workspaceMain, environmentVariables, false, updateMain);
  Properties result= fileAccess.readProperties(updateMain);
  assertThat(result).containsKey("theme").containsKey("ui");
}
@Test
void nonexistentWorkspace(@TempDir Path workspaceDir)throws Exception{
  IdeContext  context = newContext(PROJECT_BASIC, null, false);
  PropertiesMerger propertiesMerger= new PropertiesMerger(context);
  Path workspaceMain = workspaceDir.resolve("missing.prefs");
  Path updateMain= workspaceDir.resolve("update.prefs");
  Files.createFile(updateMain);
  assertThatCode(()->propertiesMerger.inverseMerge(workspaceMain, context.getVariables(), false, updateMain));
}
@Test
void misstingFilesTest(@TempDir Path workspaceDir)throws Exception{
    IdeContext context = newContext(PROJECT_BASIC, null, false);
  PropertiesMerger propertiesMerger= new PropertiesMerger(context);
  Path workspaceMain = workspaceDir.resolve("workspace.prefs");
  Files.createFile(workspaceMain);
  Path updateMain= workspaceDir.resolve("missing-update.prefs");
  assertThatCode(()->propertiesMerger.inverseMerge(workspaceMain, context.getVariables(), true, updateMain)).doesNotThrowAnyException();
  }
  @Test
  void fileUnchangedTest(@TempDir Path workspaceDir)throws Exception{
    IdeContext context = newContext(PROJECT_BASIC, null, false);
    PropertiesMerger propertiesMerger= new PropertiesMerger(context);
    Path workspaceMain = workspaceDir.resolve("workspace.prefs");
    Path updateMain= workspaceDir.resolve("update.prefs");
    Files.writeString(workspaceMain, "");
    Files.writeString(updateMain, "theme=dark\n");
    long sizeBefore=Files.size(updateMain);
    propertiesMerger.inverseMerge(workspaceMain, context.getVariables(), true, updateMain);
    assertThat(Files.size(updateMain)).isEqualTo(sizeBefore);
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
