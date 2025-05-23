package com.devonfw.tools.ide.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;

/**
 * Test of {@link IniParserImpl}
 */
public class IniParserImplTest extends AbstractIdeContextTest {

  String iniContent = """
      [filter "lfs"]
      \trequired = true
      \tclean = git-lfs clean -- %f
      \tsmudge = git-lfs smudge -- %f
      [credential]
      \thelper = store
      [core]
      \tsshCommand = C:/Windows/System32/OpenSSH/ssh.exe
      \tlongpaths = false
      [last section]
      \trequired = false
      """;

  private Path getIniFile() throws IOException {
    Path file = Files.createTempFile("test", "ini");
    Files.writeString(file, iniContent);
    return file;
  }

  /**
   * test of {@link IniParserImpl#getSections()}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testGetSections() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniParserImpl parserFromString = new IniParserImpl(iniContent, context);
    IniParserImpl parserFromPath = new IniParserImpl(getIniFile(), context);
    String[] expectedSections = { "filter \"lfs\"", "credential", "core", "last section" };

    // act
    String[] sectionsFromString = parserFromString.getSections();
    String[] sectionsFromPath = parserFromPath.getSections();

    // assert
    assertThat(sectionsFromString).isEqualTo(expectedSections);
    assertThat(sectionsFromPath).isEqualTo(expectedSections);
  }

  /**
   * test of {@link IniParserImpl#getPropertiesBySection(String)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testGetPropertiesBySection() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniParserImpl parserFromString = new IniParserImpl(iniContent, context);
    IniParserImpl parserFromPath = new IniParserImpl(getIniFile(), context);
    HashMap<String, String> expectedProperties = new LinkedHashMap<>();
    expectedProperties.put("required", "true");
    expectedProperties.put("clean", "git-lfs clean -- %f");
    expectedProperties.put("smudge", "git-lfs smudge -- %f");
    String section = "filter \"lfs\"";

    // act
    HashMap<String, String> propertiesFromString = parserFromString.getPropertiesBySection(section);
    HashMap<String, String> propertiesFromPath = parserFromPath.getPropertiesBySection(section);

    // assert
    assertThat(propertiesFromString).isEqualTo(expectedProperties);
    assertThat(propertiesFromPath).isEqualTo(expectedProperties);
  }

  /**
   * test of {@link IniParserImpl#getPropertyValue(String, String)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testGetPropertyValue() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniParserImpl parserFromString = new IniParserImpl(iniContent, context);
    IniParserImpl parserFromPath = new IniParserImpl(getIniFile(), context);
    String section = "last section";
    String property = "required";
    String expectedValue = "false";

    // act
    String valueFromString = parserFromString.getPropertyValue(section, property);
    String valueFromPath = parserFromPath.getPropertyValue(section, property);

    // assert
    assertThat(valueFromString).isEqualTo(expectedValue);
    assertThat(valueFromPath).isEqualTo(expectedValue);
  }

  /**
   * test of {@link IniParserImpl#removeSection(String)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testRemoveSection() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniParserImpl parserFromString = new IniParserImpl(iniContent, context);
    IniParserImpl parserFromPath = new IniParserImpl(getIniFile(), context);
    String[] expectedSections = { "filter \"lfs\"", "credential", "core" };
    String sectionToRemove = "last section";

    // act
    parserFromString.removeSection(sectionToRemove);
    parserFromPath.removeSection(sectionToRemove);
    String[] sectionsFromString = parserFromString.getSections();
    String[] sectionsFromPath = parserFromPath.getSections();

    // assert
    assertThat(sectionsFromString).isEqualTo(expectedSections);
    assertThat(sectionsFromPath).isEqualTo(expectedSections);
  }

  /**
   * test of {@link IniParserImpl#removeProperty(String, String)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testRemoveProperty() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniParserImpl parserFromString = new IniParserImpl(iniContent, context);
    IniParserImpl parserFromPath = new IniParserImpl(getIniFile(), context);
    HashMap<String, String> expectedProperties = new LinkedHashMap<>();
    expectedProperties.put("required", "true");
    expectedProperties.put("clean", "git-lfs clean -- %f");
    String section = "filter \"lfs\"";
    String propertyToRemove = "smudge";

    // act
    parserFromString.removeProperty(section, propertyToRemove);
    parserFromPath.removeProperty(section, propertyToRemove);
    HashMap<String, String> propertiesFromString = parserFromString.getPropertiesBySection(section);
    HashMap<String, String> propertiesFromPath = parserFromPath.getPropertiesBySection(section);

    // assert
    assertThat(propertiesFromString).isEqualTo(expectedProperties);
    assertThat(propertiesFromPath).isEqualTo(expectedProperties);
  }


  /**
   * test of {@link IniParserImpl#addSection(String)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testAddSection() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniParserImpl parserFromString = new IniParserImpl(iniContent, context);
    IniParserImpl parserFromPath = new IniParserImpl(getIniFile(), context);
    String existingSection = "core";
    String newSection = "testSection";

    // act
    parserFromString.addSection(existingSection);
    parserFromString.addSection(newSection);
    parserFromPath.addSection(existingSection);
    parserFromPath.addSection(newSection);

    // assert
    assertThat(parserFromString.getPropertiesBySection(existingSection).size()).isEqualTo(2);
    assertThat(parserFromString.getSections()).contains(newSection);
    assertThat(parserFromPath.getPropertiesBySection(existingSection).size()).isEqualTo(2);
    assertThat(parserFromPath.getSections()).contains(newSection);
  }

  /**
   * test of {@link IniParserImpl#setProperty(String, String, String)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testSetProperty() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniParserImpl parserFromString = new IniParserImpl(iniContent, context);
    IniParserImpl parserFromPath = new IniParserImpl(getIniFile(), context);
    String section = "last section";
    String existingProperty = "required";
    String newProperty = "newProperty";
    String newValue = "someValue";

    // act
    parserFromString.setProperty(section, existingProperty, newValue);
    parserFromString.setProperty(section, newProperty, newValue);
    parserFromPath.setProperty(section, existingProperty, newValue);
    parserFromPath.setProperty(section, newProperty, newValue);

    // assert
    assertThat(parserFromString.getPropertyValue(section, existingProperty)).isEqualTo(newValue);
    assertThat(parserFromString.getPropertyValue(section, newProperty)).isEqualTo(newValue);
    assertThat(parserFromPath.getPropertyValue(section, existingProperty)).isEqualTo(newValue);
    assertThat(parserFromPath.getPropertyValue(section, newProperty)).isEqualTo(newValue);
  }

  /**
   * test of {@link IniParserImpl#write(Path)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testWrite() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniParserImpl parserFromString = new IniParserImpl(iniContent, context);
    IniParserImpl parserFromPath = new IniParserImpl(getIniFile(), context);
    Path fileFromString = Files.createTempFile("testFromString", "ini");
    Path fileFromPath = Files.createTempFile("testFromPath", "ini");
    FileAccess fileAccess = context.getFileAccess();

    // act
    parserFromString.write(fileFromString);
    parserFromPath.write(fileFromPath);
    String contentFromString = fileAccess.readFileContent(fileFromString);
    String contentFromPath = fileAccess.readFileContent(fileFromPath);

    // assert
    assertThat(contentFromString).isEqualTo(iniContent);
    assertThat(contentFromPath).isEqualTo(iniContent);
  }

  /**
   * test of {@link IniParserImpl#toString()}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testToString() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniParserImpl parserFromString = new IniParserImpl(iniContent, context);
    IniParserImpl parserFromPath = new IniParserImpl(getIniFile(), context);

    // act
    String stringFromString = parserFromString.toString();
    String stringFromPath = parserFromPath.toString();

    // assert
    assertThat(stringFromString).isEqualTo(iniContent);
    assertThat(stringFromPath).isEqualTo(iniContent);
  }
}
