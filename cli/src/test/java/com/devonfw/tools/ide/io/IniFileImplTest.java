package com.devonfw.tools.ide.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.io.ini.IniFile;
import com.devonfw.tools.ide.io.ini.IniFileImpl;
import com.devonfw.tools.ide.io.ini.IniSection;

/**
 * Test of {@link IniFileImpl}
 */
class IniFileImplTest extends AbstractIdeContextTest {

  String iniContent = """
      [filter "lfs"]
      \trequired = true
      \tclean = git-lfs clean -- %f
      \tsmudge = git-lfs smudge -- %f
      \t[credential]
      \t ; I am a comment inside of a section!
      \thelper = store
      
      
      \t[credential.details]
      # this comment uses another comment symbol
      \t\tmode = strict
      [core]
      \t; core elements
      \tsshCommand = C:/Windows/System32/OpenSSH/ssh.exe
      \tlongpaths = false
      [last section]
      \trequired = false
      \t\tindentation = different
      """;

  String iniContentWithInitialProperties = """
      ; this is an ini file!
      filetype = ini file
      """ + iniContent;

  private IniFile getIniFile(IdeContext context, String content) throws IOException {
    Path file = Files.createTempFile("test", "ini");
    Files.writeString(file, content);
    FileAccess fileAccess = new FileAccessImpl(context);
    IniFileImpl iniFile = new IniFileImpl();
    fileAccess.readIniFile(file, iniFile);
    return iniFile;
  }

  /**
   * test of {@link IniFileImpl#getSectionNames()}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  void testGetSectionNames() throws IOException {
    // arrange
    IdeTestContext context = new IdeTestContext();
    IniFile iniFileA = getIniFile(context, iniContentWithInitialProperties);
    IniFile iniFIleB = getIniFile(context, iniContent);

    String[] expectedSections = { "filter \"lfs\"", "credential", "credential.details", "core", "last section" };

    // act
    String[] sectionsA = iniFileA.getSectionNames();
    String[] sectionsB = iniFIleB.getSectionNames();

    // assert
    assertThat(sectionsA).isEqualTo(expectedSections);
    assertThat(sectionsB).isEqualTo(expectedSections);
  }


  /**
   * test of {@link IniFileImpl#removeSection(String)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  void testRemoveSection() throws IOException {
    // arrange
    IdeContext context = new IdeTestContext();
    IniFile iniFileA = getIniFile(context, iniContentWithInitialProperties);
    IniFile iniFileB = getIniFile(context, iniContent);
    String[] expectedSections = { "filter \"lfs\"", "credential", "credential.details", "core" };
    String sectionToRemove = "last section";

    // act
    iniFileA.removeSection(sectionToRemove);
    iniFileB.removeSection(sectionToRemove);
    String[] sectionsA = iniFileA.getSectionNames();
    String[] sectionsB = iniFileB.getSectionNames();

    // assert
    assertThat(sectionsA).isEqualTo(expectedSections);
    assertThat(sectionsB).isEqualTo(expectedSections);
  }

  /**
   * test of {@link IniFileImpl#getSection(String)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  void testGetSection() throws IOException {
    // arrange
    IdeContext context = new IdeTestContext();
    IniFile iniFileA = getIniFile(context, iniContentWithInitialProperties);
    IniFile iniFileB = getIniFile(context, iniContent);
    String sectionName = "credential";
    List<String> expectedPropertyKeys = new LinkedList<>();
    expectedPropertyKeys.add("helper");

    // act
    IniSection sectionA = iniFileA.getSection(sectionName);
    IniSection missingSectionA = iniFileA.getSection("missing section");
    IniSection sectionB = iniFileB.getSection(sectionName);
    IniSection missingSectionB = iniFileB.getSection("missing section");

    // assert
    assertThat(sectionA.getName()).isEqualTo(sectionName);
    assertThat(sectionA.getPropertyKeys()).isEqualTo(expectedPropertyKeys);
    assertThat(missingSectionA).isNull();
    assertThat(sectionB.getName()).isEqualTo(sectionName);
    assertThat(sectionB.getPropertyKeys()).isEqualTo(expectedPropertyKeys);
    assertThat(missingSectionB).isNull();
  }

  /**
   * test of {@link IniFileImpl#getOrCreateSection(String)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  void testGetOrCreateSection() throws IOException {
    // arrange
    IdeContext context = new IdeTestContext();
    IniFile iniFileA = getIniFile(context, iniContentWithInitialProperties);
    IniFile iniFileB = getIniFile(context, iniContent);
    String sectionName = "credential";
    String newSectionName = "missing section";
    List<String> expectedPropertyKeys = new LinkedList<>();
    expectedPropertyKeys.add("helper");
    String expectedHelperValue = "store";
    String addedContent = "[missing section]\n";
    String expectedNewFileContentA = iniContentWithInitialProperties + addedContent;
    String expectedNewFileContentB = iniContent + addedContent;

    // act
    IniSection sectionA = iniFileA.getOrCreateSection(sectionName);
    IniSection newSectionA = iniFileA.getOrCreateSection("[" + newSectionName + "]");
    IniSection sectionB = iniFileB.getOrCreateSection(sectionName);
    IniSection newSectionB = iniFileB.getOrCreateSection("[" + newSectionName + "]");

    // assert
    assertThat(sectionA.getName()).isEqualTo(sectionName);
    assertThat(sectionA.getPropertyKeys()).isEqualTo(expectedPropertyKeys);
    assertThat(sectionA.getPropertyValue("helper")).isEqualTo(expectedHelperValue);
    assertThat(sectionB.getName()).isEqualTo(sectionName);
    assertThat(sectionB.getPropertyKeys()).isEqualTo(expectedPropertyKeys);
    assertThat(sectionB.getPropertyValue("helper")).isEqualTo(expectedHelperValue);

    assertThat(newSectionA.getName()).isEqualTo(newSectionName);
    assertThat(newSectionA.getPropertyKeys()).isEmpty();
    assertThat(newSectionB.getName()).isEqualTo(newSectionName);
    assertThat(newSectionB.getPropertyKeys()).isEmpty();

    assertThat(iniFileA.toString()).isEqualTo(expectedNewFileContentA);
    assertThat(iniFileB.toString()).isEqualTo(expectedNewFileContentB);
  }

  /**
   * test of {@link IniFileImpl#toString()}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  void testToString() throws IOException {
    // arrange
    IdeContext context = new IdeTestContext();
    IniFile iniFileA = getIniFile(context, iniContentWithInitialProperties);
    IniFile iniFileB = getIniFile(context, iniContent);

    // act
    String stringFromPathA = iniFileA.toString();
    String stringFromPathB = iniFileB.toString();

    // assert
    assertThat(stringFromPathA).isEqualTo(iniContentWithInitialProperties);
    assertThat(stringFromPathB).isEqualTo(iniContent);
  }

  /**
   * test of {@link IniSection#toString()} to check that linebreaks are inserted correctly
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  void testAddProperty() throws IOException {
    // arrange
    IdeContext context = new IdeTestContext();
    IniFile iniFileB = getIniFile(context, iniContent);
    String expectedContent = "variable = value\n" + iniContent;

    // act
    iniFileB.getInitialSection().setProperty("variable", "value");

    // assert
    assertThat(iniFileB.toString()).isEqualTo(expectedContent);
  }
}
