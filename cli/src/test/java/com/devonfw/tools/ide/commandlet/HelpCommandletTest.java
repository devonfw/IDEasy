package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;

/**
 * Integration test of {@link HelpCommandlet}.
 */
public class HelpCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link HelpCommandlet} does not require home.
   */
  @Test
  public void testThatHomeIsNotRequired() {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    // act
    HelpCommandlet help = new HelpCommandlet(context);
    // assert
    assertThat(help.isIdeHomeRequired()).isFalse();
  }

  /**
   * Test of {@link HelpCommandlet} run.
   */
  @Test
  public void testRun() {

    // arrange
    IdeTestContext context = IdeTestContext.of();
    context.getStartContext().setLocale(Locale.ROOT);
    HelpCommandlet help = new HelpCommandlet(context);

    // act
    help.run();
    // assert
    assertLogoMessage(context);
    assertThat(context).logAtSuccess().hasMessageContaining("Current version of IDE is");
    assertThat(context).logAtInfo().hasMessage("Usage: ide [option]* [[commandlet] [arg]*]");
    for (Commandlet cmd : context.getCommandletManager().getCommandlets()) {
      assertThat(context).log().hasMessageContaining(cmd.getName());
    }
    assertOptionLogMessages(context);
  }

  /**
   * Test of {@link HelpCommandlet} run with a Commandlet.
   */
  @Test
  public void testRunWithCommandlet() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getStartContext().setLocale(Locale.ROOT);
    HelpCommandlet help = context.getCommandletManager().getCommandlet(HelpCommandlet.class);
    help.commandlet.setValueAsString("mvn", context);
    // act
    help.run();
    // assert
    assertLogoMessage(context);
    assertThat(context).logAtInfo()
        .hasEntries("Usage: ide [option]* mvn [<args>*]", "Tool commandlet for Maven (Build-Tool).", "usage: mvn [options] [<goal(s)>] [<phase(s)>]");
    assertOptionLogMessages(context);
  }

  /**
   * Ensure that for every {@link Commandlet} and each of their {@link Property} a help text is defined.
   *
   * @param locale the {@link String} representation of the {@link Locale} to test. The empty {@link String} will be used for {@link Locale#ROOT}.
   */
  @ParameterizedTest
  @ValueSource(strings = { "", "de" })
  public void testEnsureAllNlsPropertiesPresent(String locale) throws IOException {

    // arrange
    IdeContext context = IdeTestContextMock.get();
    NlsBundle bundleRoot = new NlsBundle(context, Locale.ROOT);
    NlsBundle bundle = new NlsBundle(context, Locale.forLanguageTag(locale));
    SoftAssertions soft = new SoftAssertions();
    // act
    for (Commandlet commandlet : context.getCommandletManager().getCommandlets()) {
      String message = bundle.get(commandlet);
      soft.assertThat(message).doesNotStartWith("?");
      String detail = bundle.getDetail(commandlet);
      soft.assertThat(detail).doesNotStartWith("?");
      if (!locale.isEmpty()) {
        soft.assertThat(message).isNotEqualTo(bundleRoot.get(commandlet));
        soft.assertThat(detail).isNotEqualTo(bundleRoot.getDetail(commandlet));
      }
      for (Property<?> property : commandlet.getProperties()) {
        if (!(property instanceof KeywordProperty)) {
          message = bundle.get(commandlet, property);
          soft.assertThat(message).doesNotStartWith("?");
          if (!locale.isEmpty()) {
            soft.assertThat(message).isNotEqualTo(bundleRoot.get(commandlet, property));
          }
        }
      }
    }
    // assert
    soft.assertAll();
    // also ensure the resource bundle is sorted alphabetically
    Path bundlePath = Path.of("src/main/resources/nls");
    String filename = "Help.properties";
    if (!locale.isEmpty()) {
      filename = "Help_" + locale + ".properties";
    }
    Path bundleFile = bundlePath.resolve(filename);
    List<String> strings = Files.readAllLines(bundleFile).stream().map(s -> s.replaceAll("=.*", "")).toList();
    List<String> sorted = new ArrayList<>(strings);
    sorted.sort((s1, s2) -> {
      if (s1.startsWith(s2)) {
        return 1;
      } else if (s2.startsWith(s1)) {
        return -1;
      } else {
        return s1.compareTo(s2);
      }
    });
    assertThat(strings).isEqualTo(sorted);
  }

  /**
   * Assertion for the options that should be displayed.
   */
  private void assertOptionLogMessages(IdeTestContext context) {

    assertThat(context).logAtInfo().hasEntries(
        "--locale        the locale (e.g. '--locale=de' for German language).",
        "--skip-updates  disables tool updates if the configured versions match the installed versions.",
        "-b | --batch    enable batch mode (non-interactive).",
        "-d | --debug    enable debug logging.",
        "-f | --force    enable force mode.",
        "-o | --offline  enable offline mode (skip updates or git pull, fail downloads or git clone).",
        "-q | --quiet    disable info logging (only log success, warning or error).",
        "-t | --trace    enable trace logging.");
  }

  /**
   * Assertion for the IDE-Logo that should be displayed.
   */
  private void assertLogoMessage(IdeTestContext context) {

    assertThat(context).logAtInfo().hasMessage(IdeContext.LOGO);
  }
}
