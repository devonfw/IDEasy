package com.devonfw.tools.ide.configurator;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Map.Entry;
import java.util.Properties;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.configurator.merge.PropertiesMerger;

/**
 * Test of {@link Configurator}.
 */
public class ConfiguratorTest extends Assertions {

  private static final String DEVON_IDE_HOME = new File("").getAbsolutePath().replace("\\", "/");

  private static final Prop JAVA_VERSION = new Prop("java.version", "1.11");

  private static final Prop JAVA_HOME = new Prop("java.home", DEVON_IDE_HOME + "/software/java");

  private static final Prop THEME = new Prop("theme", "dark");

  private static final Prop UI = new Prop("ui", "classic");

  private static final Prop INDENTATION = new Prop("indentation", "2");

  private static final Prop THEME_HACKED = new Prop("theme", "light");

  private static final Prop UI_HACKED = new Prop("ui", "linux");

  private static final Prop INDENTATION_HACKED = new Prop("indentation", "4");

  private static final Prop JAVA_VERSION_HACKED = new Prop("java.version", "1.99");

  private static final Prop EDITOR = new Prop("editor", "vi");

  /**
   * Test of {@link Configurator}.
   *
   * @throws Exception on error.
   */
  @Test
  public void testConfigurator() throws Exception {

    // given
    Configurator configurator = new Configurator();
    String tmp = System.getProperty("java.io.tmpdir");
    File tmpDir = new File(tmp);
    File workspaceDir = createUniqueFolder(tmpDir, ".test.workspace");
    String clientEnvHome = DEVON_IDE_HOME;

    // when
    int exitCode = configurator.run("-t", "src/test/resources/templates", "-w", workspaceDir.getAbsolutePath(), "-u");

    // then
    assertThat(exitCode).isEqualTo(0);
    File mainPrefsFile = new File(workspaceDir, "main.prefs");
    Properties mainPrefs = PropertiesMerger.load(mainPrefsFile);
    assertThat(mainPrefs).containsOnly(JAVA_VERSION, JAVA_HOME, THEME, UI);
    File jsonFolder = new File(workspaceDir, "json");
    assertThat(jsonFolder).isDirectory();
    assertThat(new File(jsonFolder, "settings.json")).hasContent("\n" // this newline is rather a bug of JSON-P impl
        + "{\n" //
        + "    \"java.home\": \"" + DEVON_IDE_HOME + "/software/java\",\n" //
        + "    \"tslint.autoFixOnSave\": true,\n" //
        + "    \"object\": {\n" //
        + "        \"bar\": \"" + DEVON_IDE_HOME + "/bar\",\n" //
        + "        \"array\": [\n" //
        + "            \"a\",\n" //
        + "            \"b\",\n" //
        + "            \"" + DEVON_IDE_HOME + "\"\n" //
        + "        ],\n" //
        + "        \"foo\": \"" + DEVON_IDE_HOME + "/foo\"\n" //
        + "    }\n" //
        + "}");
    assertThat(new File(jsonFolder, "update.json")).hasContent("\n" // this newline is rather a bug of JSON-P impl
        + "{\n" //
        + "    \"key\": \"value\"\n" //
        + "}");

    File configFolder = new File(workspaceDir, "config");
    assertThat(configFolder).isDirectory();
    File indentFile = new File(configFolder, "indent.properties");
    Properties indent = PropertiesMerger.load(indentFile);
    assertThat(indent).containsOnly(INDENTATION);
    assertThat(new File(configFolder, "layout.xml")).hasContent("" //
        + "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" // here a newline would be reasonable...
        + "<layout>\n" //
        + "  <left>navigator</left>\n" //
        + "  <right>debugger</right>\n" //
        + "  <top>editor</top>\n" //
        + "  <bottom>console</bottom>\n" //
        + "  <test path=\"" + clientEnvHome + "\">" + clientEnvHome + "</test>\n" //
        + "</layout>");

    // and after
    EDITOR.apply(mainPrefs);
    JAVA_VERSION_HACKED.apply(mainPrefs);
    UI_HACKED.apply(mainPrefs);
    THEME_HACKED.apply(mainPrefs);
    INDENTATION_HACKED.apply(mainPrefs);
    PropertiesMerger.save(mainPrefs, mainPrefsFile);

    // when
    configurator = new Configurator();
    exitCode = configurator.run("-t", "src/test/resources/templates", "-w", workspaceDir.getAbsolutePath(), "-u");

    // then
    mainPrefs = PropertiesMerger.load(mainPrefsFile);
    assertThat(mainPrefs).containsOnly(JAVA_VERSION, JAVA_HOME, THEME_HACKED, UI_HACKED, EDITOR, INDENTATION_HACKED);

    // finally cleanup
    delete(workspaceDir.toPath());
  }

  private File createUniqueFolder(File tmpDir, String string) {

    File folder = new File(tmpDir, string);
    File uniqueFolder = new File(folder, Instant.now().toString().replace(':', '_'));
    boolean success = uniqueFolder.mkdirs();
    assert success : uniqueFolder.getAbsolutePath();
    return uniqueFolder;
  }

  private static void delete(Path path) throws IOException {

    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {

        if (e != null) {
          throw e;
        }
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
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
