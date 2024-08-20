package com.devonfw.tools.ide.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.commandlet.ContextCommandlet;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;

class EnumPropertyTest {

  /**
   * Enum for testing EnumProperties
   */
  private enum TestEnum {
    ELEMENTZERO, ELEMENTONE, ELEMENTTWO;
  }

  @Test
  public void testGetValueType() {
    EnumProperty enumProp = new EnumProperty("", false, "", TestEnum.class);
    assertThat(enumProp.getValueType()).isEqualTo(TestEnum.class);
  }

  @Test
  public void testParse() {
    IdeContext context = IdeTestContextMock.get();
    EnumProperty enumProp = new EnumProperty("", false, "", TestEnum.class);

    assertThat(enumProp.parse("elementzero", context)).isEqualTo(TestEnum.ELEMENTZERO);
    assertThrows(IllegalArgumentException.class, () -> enumProp.parse(null, context));
    assertThrows(IllegalArgumentException.class, () -> enumProp.parse("element-not-in-enum", context));
  }

  @Test
  public void testCompleteValue() {
    IdeContext context = IdeTestContextMock.get();
    String[] expectedCandidates = { "elementzero", "elementone", "elementtwo" };
    String input = "ele";
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);

    EnumProperty enumProp = new EnumProperty("", false, "", TestEnum.class);
    enumProp.completeValue(input, context, new ContextCommandlet(), collector);

    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text)).containsExactly(expectedCandidates);
  }
}
