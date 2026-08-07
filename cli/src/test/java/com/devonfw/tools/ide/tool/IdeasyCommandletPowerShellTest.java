package com.devonfw.tools.ide.tool;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

class IdeasyCommandletPowerShellTest extends AbstractIdeContextTest {

  private IdeasyCommandlet commandlet;

  @BeforeEach
  void setUp() {
    IdeTestContext context = newContext(PROJECT_BASIC);
    this.commandlet = new IdeasyCommandlet(context);
  }

  @Test
  void testInstallIntoMissingPowerShellProfile() {
    // act
    List<String> result =
        this.commandlet.modifyPowerShellProfileLines(null, true);

    // assert
    assertThat(result).containsExactly(
        IdeasyCommandlet.POWERSHELL_CODE_SOURCE_FUNCTIONS);
  }

  @Test
  void testInstallIntoEmptyPowerShellProfile() {
    // arrange
    List<String> lines = List.of();

    // act
    List<String> result =
        this.commandlet.modifyPowerShellProfileLines(lines, true);

    // assert
    assertThat(result).containsExactly(
        IdeasyCommandlet.POWERSHELL_CODE_SOURCE_FUNCTIONS);
  }

  @Test
  void testInstallPreservesExistingPowerShellProfileContent() {
    // arrange
    List<String> lines = List.of(
        "Set-Alias ll Get-ChildItem",
        "$env:TEST = \"value\"");

    // act
    List<String> result =
        this.commandlet.modifyPowerShellProfileLines(lines, true);

    // assert
    assertThat(result).containsExactly(
        "Set-Alias ll Get-ChildItem",
        "$env:TEST = \"value\"",
        IdeasyCommandlet.POWERSHELL_CODE_SOURCE_FUNCTIONS);
  }

  @Test
  void testInstallDoesNotAddPowerShellEntryTwice() {
    // arrange
    List<String> lines = List.of(
        "Set-Alias ll Get-ChildItem",
        IdeasyCommandlet.POWERSHELL_CODE_SOURCE_FUNCTIONS);

    // act
    List<String> result =
        this.commandlet.modifyPowerShellProfileLines(lines, true);

    // assert
    assertThat(result).containsExactly(
        "Set-Alias ll Get-ChildItem",
        IdeasyCommandlet.POWERSHELL_CODE_SOURCE_FUNCTIONS);
  }

  @Test
  void testInstallRecognizesPowerShellEntryWithWhitespace() {
    // arrange
    List<String> lines = List.of(
        "Set-Alias ll Get-ChildItem",
        "  " + IdeasyCommandlet.POWERSHELL_CODE_SOURCE_FUNCTIONS + "  ");

    // act
    List<String> result =
        this.commandlet.modifyPowerShellProfileLines(lines, true);

    // assert
    assertThat(result).containsExactly(
        "Set-Alias ll Get-ChildItem",
        "  " + IdeasyCommandlet.POWERSHELL_CODE_SOURCE_FUNCTIONS + "  ");
  }

  @Test
  void testUninstallRemovesPowerShellEntry() {
    // arrange
    List<String> lines = List.of(
        "Set-Alias ll Get-ChildItem",
        IdeasyCommandlet.POWERSHELL_CODE_SOURCE_FUNCTIONS,
        "$env:TEST = \"value\"");

    // act
    List<String> result =
        this.commandlet.modifyPowerShellProfileLines(lines, false);

    // assert
    assertThat(result).containsExactly(
        "Set-Alias ll Get-ChildItem",
        "$env:TEST = \"value\"");
  }

  @Test
  void testUninstallRemovesPowerShellEntryWithWhitespace() {
    // arrange
    List<String> lines = List.of(
        "Set-Alias ll Get-ChildItem",
        "  " + IdeasyCommandlet.POWERSHELL_CODE_SOURCE_FUNCTIONS + "  ",
        "$env:TEST = \"value\"");

    // act
    List<String> result =
        this.commandlet.modifyPowerShellProfileLines(lines, false);

    // assert
    assertThat(result).containsExactly(
        "Set-Alias ll Get-ChildItem",
        "$env:TEST = \"value\"");
  }

  @Test
  void testUninstallPreservesProfileWithoutIdeasyEntry() {
    // arrange
    List<String> lines = List.of(
        "Set-Alias ll Get-ChildItem",
        "$env:TEST = \"value\"");

    // act
    List<String> result =
        this.commandlet.modifyPowerShellProfileLines(lines, false);

    // assert
    assertThat(result).containsExactlyElementsOf(lines);
  }

  @Test
  void testUninstallHandlesMissingPowerShellProfile() {
    // act
    List<String> result =
        this.commandlet.modifyPowerShellProfileLines(null, false);

    // assert
    assertThat(result).isEmpty();
  }

  @Test
  void testUninstallRemovesAllDuplicatePowerShellEntries() {
    // arrange
    List<String> lines = List.of(
        IdeasyCommandlet.POWERSHELL_CODE_SOURCE_FUNCTIONS,
        "Set-Alias ll Get-ChildItem",
        IdeasyCommandlet.POWERSHELL_CODE_SOURCE_FUNCTIONS);

    // act
    List<String> result =
        this.commandlet.modifyPowerShellProfileLines(lines, false);

    // assert
    assertThat(result).containsExactly(
        "Set-Alias ll Get-ChildItem");
  }
}
