package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.intellij.Intellij;

/**
 * Tests that a commandlet's declared options are recognized natively even in the multi-valued passthrough argument region of a tool commandlet (issues #2821
 * parsing, #2822 auto-completion). On tool commandlets the trailing arguments belong to the multi-valued {@code arguments} property, which takes over option
 * handling; a declared option such as {@code --project} must still be matched by the framework instead of being swallowed into that passthrough.
 */
class ToolCommandletDeclaredOptionTest extends AbstractIdeContextTest {

  /**
   * Tests that the {@code --project=<path>} option declared on a tool commandlet is consumed by the parser (setting
   * {@link AbstractIdeToolCommandlet#getOpenPath() the open path}) and is NOT swallowed into the multi-valued passthrough {@code arguments} property,
   * although it appears in the passthrough region.
   */
  @Test
  void testDeclaredOptionEqualsFormIsParsedNotSwallowed() {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    Path external = context.getIdeHome().resolve("external-project").normalize();
    intellij.reset();
    // act - drive the CLI parse seam for "ide intellij --project=<path>"
    CliArguments args = new CliArguments("intellij", "--project=" + external.toString());
    args.next(); // skip the start sentinel so the parser starts at the keyword, as Ideasy.run does
    context.apply(args, intellij);
    // assert - the declared option was parsed natively, not swallowed into the passthrough arguments
    assertThat(intellij.getOpenPath()).isEqualTo(external);
    assertThat(intellij.arguments.asList()).doesNotContain("--project=" + external.toString());
    assertThat(intellij.arguments.asList()).doesNotContain("--project");
  }

  /**
   * Tests that a bare {@code --project} flag (no value) in the passthrough region selects the current working directory via
   * {@link AbstractIdeToolCommandlet#getOpenPath()} and is likewise NOT swallowed into the passthrough {@code arguments} property.
   */
  @Test
  void testBareDeclaredOptionIsParsedAndSelectsCwd() {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    intellij.reset();
    // act - drive the CLI parse seam for "ide intellij --project"
    CliArguments args = new CliArguments("intellij", "--project");
    args.next();
    context.apply(args, intellij);
    // assert - the bare flag selects the current working directory and was consumed, not passed to the IDE
    assertThat(intellij.getOpenPath()).isEqualTo(context.getCwd());
    assertThat(intellij.arguments.asList()).doesNotContain("--project");
  }

  /**
   * Tests the end-of-options token: a {@code --project} option appearing AFTER a literal {@code --} must NOT be recognized as a declared option - it is
   * passed through to the tool verbatim as a passthrough argument, even though it looks like an option. This guards the "care needed to not break the
   * {@code --} end-of-options token" requirement of issue #2821.
   */
  @Test
  void testOptionAfterEndOfOptionsTokenIsPassedThroughVerbatim() {

    // arrange
    IdeTestContext context = newContext("intellij");
    Intellij intellij = context.getCommandletManager().getCommandlet(Intellij.class);
    intellij.reset();
    // act - drive the CLI parse seam for "ide intellij -- --project"
    CliArguments args = new CliArguments("intellij", "--", "--project");
    args.next();
    context.apply(args, intellij);
    // assert - after "--" nothing is option-matched: no declared option is consumed and the token is kept for the IDE
    assertThat(intellij.project.getValue()).isNull();
    assertThat(intellij.arguments.asList()).contains("--project");
  }

  /**
   * Tests auto-completion of the declared {@code --project} option name in the passthrough region (issue #2822): {@code ide complete intellij --project}
   * offers the {@code --project} option instead of nothing.
   */
  @Test
  void testCompleteDeclaredOptionNameInPassthroughRegion() {

    // arrange
    IdeTestContext context = newContext("intellij");
    String[] argsArray = { "intellij", "--project" };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    // act
    List<String> candidates = context.complete(args, new CompletionCandidateCollectorDefault(context), false)
        .stream().map(CompletionCandidate::text).toList();
    // assert - the declared option is offered (it was previously swallowed by the passthrough and not completed)
    assertThat(candidates).contains("--project");
  }

  /**
   * Tests auto-completion of the {@code --project=<path>} value in the passthrough region (issue #2822): after {@code --project=} the folder candidates of
   * the directory being completed are offered via the {@link com.devonfw.tools.ide.property.FolderProperty} folder completion, instead of nothing.
   */
  @Test
  void testCompleteDeclaredOptionFolderValueInPassthroughRegion() {

    // arrange - complete an absolute path prefix of an existing folder so the folder completion has a deterministic target
    IdeTestContext context = newContext("intellij");
    Path target = context.getWorkspacePath();
    Path parent = target.getParent();
    String name = target.getFileName().toString();
    String prefix = name.substring(0, Math.min(3, name.length()));
    String[] argsArray = { "intellij", "--project=" + parent.resolve(prefix).toString() };
    CliArguments args = CliArguments.ofCompletion(argsArray);
    // act
    List<String> candidates = context.complete(args, new CompletionCandidateCollectorDefault(context), false)
        .stream().map(CompletionCandidate::text).toList();
    // assert - the existing folder is offered as a --project value, prefixed with the option name
    assertThat(candidates).anySatisfy(c -> assertThat(c).startsWith("--project=").contains(target.toString()));
  }
}
