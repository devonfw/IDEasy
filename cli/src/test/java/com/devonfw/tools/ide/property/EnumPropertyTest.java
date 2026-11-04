package com.devonfw.tools.ide.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.commandlet.UpgradeCommandlet;
import com.devonfw.tools.ide.commandlet.UpgradeMode;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link EnumProperty}.
 */
class EnumPropertyTest {

  /**
   * Enum for testing EnumProperties
   */
  private enum TestEnum {
    ELEMENTZERO, ELEMENTONE, ELEMENTTWO;
  }

  @Test
  void testGetValueType() {
    EnumProperty<TestEnum> enumProp = new EnumProperty<>("", false, "", TestEnum.class);
    assertThat(enumProp.getValueType()).isEqualTo(TestEnum.class);
  }

  @Test
  void testParse() {
    IdeContext context = new IdeTestContext();
    EnumProperty<TestEnum> enumProp = new EnumProperty<>("", false, "", TestEnum.class);

    assertThat(enumProp.parse("elementzero", context)).isEqualTo(TestEnum.ELEMENTZERO);
    assertThrows(IllegalArgumentException.class, () -> enumProp.parse(null, context));
    assertThrows(IllegalArgumentException.class, () -> enumProp.parse("element-not-in-enum", context));
  }

  @Test
  void testCompleteValue() {
    IdeContext context = new IdeTestContext();
    String[] expectedCandidates = { "elementzero", "elementone", "elementtwo" };
    String input = "ele";
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    EnumProperty<TestEnum> enumProp = new EnumProperty<>("", false, "", TestEnum.class);
    enumProp.completeValue(input, context, new ContextCommandlet(), collector);

    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).containsExactly(expectedCandidates);
  }

  @ParameterizedTest
  @CsvSource({ "'',stable unstable snapshot", "u,unstable", "s,stable snapshot", "st,stable" })
  void testUpgradeModeCompletion(String input, String expected) {
    // arrange
    IdeTestContext context = new IdeTestContext();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);
    EnumProperty<UpgradeMode> property = new EnumProperty<>("--mode", false, null, UpgradeMode.class);

    // act
    property.completeValue(input, context, new UpgradeCommandlet(context), collector);

    // assert
    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text))
        .containsExactly(expected.split(" "));
  }

  /**
   * Test that no completion candidates are suggested for an unknown prefix.
   */
  @Test
  void testUpgradeModeCompletionNoMatch() {
    // arrange
    IdeContext context = new IdeTestContext();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);
    EnumProperty<UpgradeMode> property = new EnumProperty<>("--mode", false, null, UpgradeMode.class);

    // act
    property.completeValue("ss", context, new UpgradeCommandlet(context), collector);

    // assert
    assertThat(collector.getCandidates()).isEmpty();
  }
}
