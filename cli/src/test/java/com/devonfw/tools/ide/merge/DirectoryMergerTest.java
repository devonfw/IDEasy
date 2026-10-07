package com.devonfw.tools.ide.merge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.io.FileAccess;

/**
 * Test of {@link DirectoryMerger}.
 */
class DirectoryMergerTest extends AbstractIdeContextTest {

  private static final String IDE_HOME = TEST_PROJECTS.resolve(PROJECT_BASIC).resolve("project").toAbsolutePath().toString().replace('\\', '/');

  private static final Path WORKSPACE = Path.of("/ide/workspaces/main");

  private static final Path JDK_TABLE_TEMPLATE = Path.of(".intellij/config/options/jdk.table.xml");

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

  /**
   * Tests that {@link DirectoryMerger#resolveMergeTarget(Path, Path, Map, Set)} predicts exactly what
   * {@link DirectoryMerger#merge(Path, Path, EnvironmentVariables, Path, Map, Set)} actually does. Both apply the redirects and excludes per path segment, but
   * they do so in two separate loops - this test is what fails if those ever drift apart (see #2531, where the extra SDK import relies on the prediction).
   *
   * @param workspaceDir the temporary folder to use as workspace for this test.
   * @throws Exception on error.
   */
  @Test
  void testMergeWritesWhereResolveMergeTargetPredicts(@TempDir Path workspaceDir) throws Exception {

    // arrange
    IdeContext context = newContext(PROJECT_BASIC, null, false);
    DirectoryMerger merger = context.getWorkspaceMerger();
    Path templates = Path.of("src/test/resources/templates");
    Path setup = templates.resolve(IdeContext.FOLDER_SETUP);
    Path update = templates.resolve(IdeContext.FOLDER_UPDATE);
    Path redirected = workspaceDir.resolve("outside");
    // redirect a whole folder and exclude a single file, so both mechanisms are exercised
    Map<Path, Path> redirects = Map.of(workspaceDir.resolve("config"), redirected);
    Set<Path> excludes = Set.of(workspaceDir.resolve("main.prefs"));

    // act
    merger.merge(setup, update, context.getVariables(), workspaceDir, redirects, excludes);

    // assert
    Set<Path> templatePaths = new HashSet<>();
    for (Path templateFolder : List.of(setup, update)) {
      try (Stream<Path> files = Files.walk(templateFolder)) {
        files.filter(Files::isRegularFile).map(templateFolder::relativize).forEach(templatePaths::add);
      }
    }
    assertThat(templatePaths).isNotEmpty();
    for (Path templatePath : templatePaths) {
      Optional<Path> predicted = DirectoryMerger.resolveMergeTarget(workspaceDir, templatePath, redirects, excludes);
      if (predicted.isEmpty()) {
        assertThat(workspaceDir.resolve(templatePath)).as("excluded template %s must not be merged anywhere", templatePath).doesNotExist();
      } else {
        assertThat(predicted.get()).as("template %s must be merged where resolveMergeTarget predicts", templatePath).exists();
      }
    }
    // sanity check that the arrangement really exercised both mechanisms
    assertThat(redirected.resolve("merge.xml")).exists();
    assertThat(workspaceDir.resolve("config")).doesNotExist();
    assertThat(workspaceDir.resolve("main.prefs")).doesNotExist();
  }

  /**
   * Test of {@link DirectoryMerger#resolveMergeTarget(Path, Path, Map, Set)} without any redirects or excludes.
   */
  @Test
  void testResolveMergeTargetWithoutRedirectsOrExcludes() {

    // act
    Optional<Path> target = DirectoryMerger.resolveMergeTarget(WORKSPACE, JDK_TABLE_TEMPLATE, Map.of(), Set.of());

    // assert
    assertThat(target).contains(WORKSPACE.resolve(JDK_TABLE_TEMPLATE));
  }

  /**
   * Test of {@link DirectoryMerger#resolveMergeTarget(Path, Path, Map, Set)} with a redirect of a parent folder that has to apply to all its descendants.
   */
  @Test
  void testResolveMergeTargetRedirectsDescendantsOfRedirectedFolder() {

    // arrange
    Path metadata = Path.of("/ide/.ide/intellij/main");
    Map<Path, Path> redirects = Map.of(WORKSPACE.resolve(".intellij"), metadata);

    // act
    Optional<Path> target = DirectoryMerger.resolveMergeTarget(WORKSPACE, JDK_TABLE_TEMPLATE, redirects, Set.of());

    // assert
    assertThat(target).contains(metadata.resolve("config/options/jdk.table.xml"));
  }

  /**
   * Test of {@link DirectoryMerger#resolveMergeTarget(Path, Path, Map, Set)} with a redirect of the file itself.
   */
  @Test
  void testResolveMergeTargetRedirectsSingleFile() {

    // arrange
    Path generated = Path.of("/ide/.ide/intellij/main/idea.properties");
    Map<Path, Path> redirects = Map.of(WORKSPACE.resolve("idea.properties"), generated);

    // act
    Optional<Path> target = DirectoryMerger.resolveMergeTarget(WORKSPACE, Path.of("idea.properties"), redirects, Set.of());

    // assert
    assertThat(target).contains(generated);
  }

  /**
   * Test of {@link DirectoryMerger#resolveMergeTarget(Path, Path, Map, Set)} with the file itself being excluded.
   */
  @Test
  void testResolveMergeTargetExcludesFile() {

    // arrange
    Set<Path> excludes = Set.of(WORKSPACE.resolve("idea.properties"));

    // act
    Optional<Path> target = DirectoryMerger.resolveMergeTarget(WORKSPACE, Path.of("idea.properties"), Map.of(), excludes);

    // assert
    assertThat(target).isEmpty();
  }

  /**
   * Test of {@link DirectoryMerger#resolveMergeTarget(Path, Path, Map, Set)} with an ancestor folder being excluded, which has to apply to all its
   * descendants.
   */
  @Test
  void testResolveMergeTargetExcludesDescendantsOfExcludedFolder() {

    // arrange
    Set<Path> excludes = Set.of(WORKSPACE.resolve(".intellij"));

    // act
    Optional<Path> target = DirectoryMerger.resolveMergeTarget(WORKSPACE, JDK_TABLE_TEMPLATE, Map.of(), excludes);

    // assert
    assertThat(target).isEmpty();
  }

  /**
   * Test of {@link DirectoryMerger#resolveMergeTarget(Path, Path, Map, Set)} rejecting an absolute path.
   */
  @Test
  void testResolveMergeTargetRejectsAbsolutePath() {

    // arrange
    // toAbsolutePath so this also holds on Windows, where a path without a drive letter is not absolute
    Path absolute = WORKSPACE.toAbsolutePath().resolve("idea.properties");

    // act & assert
    assertThatThrownBy(() -> DirectoryMerger.resolveMergeTarget(WORKSPACE, absolute, Map.of(), Set.of()))
        .isInstanceOf(IllegalArgumentException.class);
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
