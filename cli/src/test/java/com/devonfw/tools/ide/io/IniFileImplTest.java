package com.devonfw.tools.ide.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.io.ini.IniFile;
import com.devonfw.tools.ide.io.ini.IniFileImpl;
import com.devonfw.tools.ide.io.ini.IniSection;

/**
 * Test of {@link IniFileImpl}
 */
public class IniFileImplTest extends AbstractIdeContextTest {

  String iniContent = """
      ; this is an ini file!
      filetype = ini file
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

  private IniFile getIniFile(IdeContext context) throws IOException {
    Path file = Files.createTempFile("test", "ini");
    Files.writeString(file, iniContent);
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
  public void testGetSectionNames() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniFile iniFile = getIniFile(context);

    String[] expectedSections = { "filter \"lfs\"", "credential", "credential.details", "core", "last section" };

    // act
    String[] sections = iniFile.getSectionNames();

    // assert
    assertThat(sections).isEqualTo(expectedSections);
  }


  /**
   * test of {@link IniFileImpl#removeSection(String)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testRemoveSection() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniFile iniFile = getIniFile(context);
    String[] expectedSections = { "filter \"lfs\"", "credential", "credential.details", "core" };
    String sectionToRemove = "last section";

    // act
    iniFile.removeSection(sectionToRemove);
    String[] sections = iniFile.getSectionNames();

    // assert
    assertThat(sections).isEqualTo(expectedSections);
  }

  /**
   * test of {@link IniFileImpl#getSection(String)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testGetSection() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniFile iniFile = getIniFile(context);
    String sectionName = "credential";
    List<String> expectedPropertyKeys = new LinkedList<>();
    expectedPropertyKeys.add("helper");

    // act
    IniSection section = iniFile.getSection(sectionName);
    IniSection missingSection = iniFile.getSection("missing section");

    // assert
    assertThat(section.getName()).isEqualTo(sectionName);
    assertThat(section.getPropertyKeys()).isEqualTo(expectedPropertyKeys);
    assertThat(missingSection).isNull();
  }

  /**
   * test of {@link IniFileImpl#getOrCreateSection(String)}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testGetOrCreateSection() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniFile iniFile = getIniFile(context);
    String sectionName = "credential";
    String newSectionName = "missing section";
    List<String> expectedPropertyKeys = new LinkedList<>();
    expectedPropertyKeys.add("helper");
    String expectedHelperValue = "store";
    String expectedNewFileContent = iniContent + "[missing section]";

    // act
    IniSection section = iniFile.getOrCreateSection(sectionName);
    IniSection newSection = iniFile.getOrCreateSection("[" + newSectionName + "]");

    // assert
    assertThat(section.getName()).isEqualTo(sectionName);
    assertThat(section.getPropertyKeys()).isEqualTo(expectedPropertyKeys);
    assertThat(section.getPropertyValue("helper")).isEqualTo(expectedHelperValue);

    assertThat(newSection.getName()).isEqualTo(newSectionName);
    assertThat(newSection.getPropertyKeys()).isEmpty();

    assertThat(iniFile.toString()).isEqualTo(expectedNewFileContent);
  }

  /**
   * test of {@link IniFileImpl#toString()}
   *
   * @throws IOException if the temporary ini file couldn't be created
   */
  @Test
  public void testToString() throws IOException {
    // arrange
    IdeContext context = IdeTestContextMock.get();
    IniFile iniFile = getIniFile(context);

    // act
    String stringFromPath = iniFile.toString();

    // assert
    assertThat(stringFromPath).isEqualTo(iniContent);
  }
}
