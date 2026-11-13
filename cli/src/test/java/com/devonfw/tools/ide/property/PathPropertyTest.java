package com.devonfw.tools.ide.property;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Tests value auto-completion of a {@link PathProperty} for an empty value (the {@code --project=} form, issue #2822) where the path has no file name.
 */
class PathPropertyTest extends Assertions {

  @Test
  void testCompleteValueWithEmptyValueCompletesFolder() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    FolderProperty property = new FolderProperty("--project", false, null, false);
    CompletionCandidateCollectorDefault collector = new CompletionCandidateCollectorDefault(context);
    // act - an empty value (a bare "--project=" the user is completing) must complete the current folder instead of crashing
    // assert - pre-fix this threw an NPE because Path.of("").getFileName() is null
    assertThatCode(() -> property.completeValue("", context, null, collector)).doesNotThrowAnyException();
  }
}
