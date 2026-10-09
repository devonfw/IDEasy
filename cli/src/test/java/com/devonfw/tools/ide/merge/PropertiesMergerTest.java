package com.devonfw.tools.ide.merge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.io.FileAccess;

/**
 * Test of {@link PropertiesMerger}.
 */
class PropertiesMergerTest extends AbstractIdeContextTest {

  /**
   * Test of {@link PropertiesMerger#doMerge(Path, Path, EnvironmentVariables, Path)} with existing workspace and update file.
   *
   * @param workspaceDir the temporary folder to use as workspace for this test.
   * @throws Exception on error.
   */
  @Test
  void testMergeWithExistingWorkspaceAndExistingUpdate(@TempDir Path workspaceDir) throws Exception {
    //arrange
    IdeContext context= newContext(PROJECT_BASIC,null,false);
      PropertiesMerger propertiesMerger= new PropertiesMerger(context);
      Path workspace= workspaceDir.resolve("workspace.prefs");
      Files.writeString(workspace, "theme=dark\n");
      Path update = workspaceDir.resolve("update.prefs");
      Files.writeString(update, "ui=linux\n");
      Path setup = workspaceDir.resolve("setup.prefs");
      Files.writeString(setup, "editor=vi\n");
      //act
      propertiesMerger.doMerge(setup, update, context.getVariables(), workspace);
      //assert
    Properties result= context.getFileAccess().readProperties(workspace);
    assertThat(result).containsEntry("theme", "dark");
    assertThat(result).containsEntry("ui", "linux");
  }
  @Test
  void testMergeWithExistingWorkspaceAndMissingUpdate(@TempDir Path workspaceDir)
      throws Exception {

    // arrange
    IdeContext context = newContext(PROJECT_BASIC, null, false);
    PropertiesMerger propertiesMerger = new PropertiesMerger(context);

    Path workspace = workspaceDir.resolve("workspace.prefs");
    Files.writeString(workspace, "theme=light\n");

    Path setup = workspaceDir.resolve("setup.prefs");
    Files.writeString(setup, "theme=dark\n");

    Path update = workspaceDir.resolve("missing-update.prefs");

    String before = Files.readString(workspace);

    // act
    propertiesMerger.doMerge(
        setup,
        update,
        context.getVariables(),
        workspace);

    // assert
    String after = Files.readString(workspace);

    assertThat(after).isEqualTo(before);
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

    //assert
    Properties result = fileAccess.readProperties(workspaceMain);
    assertThat(result).containsEntry("theme", "dark").containsEntry("ui", "classic").containsEntry("java.version", "1.11");
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
    assertThat(result).containsEntry("theme", "light").containsEntry("ui", "linux").containsEntry("editor", "vi")
        .containsEntry("java.home", "${IDE_HOME}/software/java");
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
    workspaceProperties.setProperty("java.version", "1.99");
    workspaceProperties.setProperty("theme", "light");
    fileAccess.writeProperties(workspaceProperties, workspaceMain);
    //act
    propertiesMerger.inverseMerge(workspaceMain, environmentVariables, false, updateMain);
    //assert
    Properties result = fileAccess.readProperties(updateMain);
    assertThat(result).containsEntry("java.version", "1.99").containsEntry("java.home", "${IDE_HOME}/software/java").doesNotContainKey("theme");
  }
  @Test
  void nonexistentWorkspace(@TempDir Path workspaceDir)throws Exception{
    IdeContext  context = newContext(PROJECT_BASIC, null, false);
    PropertiesMerger propertiesMerger= new PropertiesMerger(context);
    Path workspaceMain = workspaceDir.resolve("missing.prefs");
    Path updateMain= workspaceDir.resolve("update.prefs");
    Files.createFile(updateMain);
    assertThatCode(() -> propertiesMerger.inverseMerge(workspaceMain, context.getVariables(), false, updateMain)).doesNotThrowAnyException();
    assertThat(updateMain).isEmptyFile();
  }
  @Test
  void missingFilesTest(@TempDir Path workspaceDir)throws Exception{
    IdeContext context = newContext(PROJECT_BASIC, null, false);
    PropertiesMerger propertiesMerger= new PropertiesMerger(context);
    Path workspaceMain = workspaceDir.resolve("workspace.prefs");
    Files.createFile(workspaceMain);
    Path updateMain= workspaceDir.resolve("missing-update.prefs");
    assertThatCode(()->propertiesMerger.inverseMerge(workspaceMain, context.getVariables(), true, updateMain)).doesNotThrowAnyException();
  }
  @Test
  void fileUnchangedTest(@TempDir Path workspaceDir)throws Exception{
    //arrange
    IdeContext context = newContext(PROJECT_BASIC, null, false);
    PropertiesMerger propertiesMerger= new PropertiesMerger(context);
    Path workspaceMain = workspaceDir.resolve("workspace.prefs");
    Path updateMain= workspaceDir.resolve("update.prefs");
    Files.writeString(workspaceMain, "");
    Files.writeString(updateMain, "theme=dark\n");
    long sizeBefore=Files.size(updateMain);
    //act
    propertiesMerger.inverseMerge(workspaceMain, context.getVariables(), true, updateMain);
    //assert
    assertThat(Files.size(updateMain)).isEqualTo(sizeBefore);
  }
  @Test
  void testUpgrade(@TempDir Path tempDir) throws Exception {
    IdeContext context = newContext(PROJECT_BASIC, null, false);
    PropertiesMerger propertiesMerger= new PropertiesMerger(context);
    Path file = tempDir.resolve("test.properties");
    Files.writeString(file, "old");
    boolean modified= propertiesMerger.doUpgrade(file);
    assertThat(modified).isFalse();
    assertThat(Files.readString(file)).isEqualTo("old");

  }
}
