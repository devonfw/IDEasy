package com.devonfw.tools.ide.tool;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.tool.az.Azure;
import com.devonfw.tools.ide.url.model.file.UrlSecurityJsonFile;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/** Test of {@link ToolCommandlet} */
public class ToolCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link ToolCommandlet#securityRiskInteraction(VersionIdentifier)} where no safe version is available. But
   * there is a vulnerability that affects all versions. This vulnerability is then ignored, but the other
   * vulnerabilities are considered.
   */
  @Test
  public void testSecurityRiskInteractionAllVersionAffectedBySingleWarning() {

    // arrange
    Class<? extends ToolCommandlet> dummyTool = Azure.class;
    String[] answers = { "1", "2", "3" };
    IdeContext context = getContextForSecurityJsonTests(dummyTool, answers);

    ToolCommandlet tool = context.getCommandletManager().getCommandlet(dummyTool);
    UrlSecurityJsonFile securityFile = context.getUrls().getEdition(tool.getName(), tool.getEdition())
        .getSecurityJsonFile();

    securityFile.addSecurityWarning(VersionRange.of("(,)")); // should get ignored
    securityFile.addSecurityWarning(VersionRange.of("[0,11]")); // should get ignored
    securityFile.addSecurityWarning(VersionRange.of("[2,5]"));

    // act & assert
    // the current version is safe, so no interaction needed and no answer is consumed
    VersionIdentifier currentVersion = VersionIdentifier.of("1");
    assertThat(tool.securityRiskInteraction(currentVersion)).isEqualTo(currentVersion);
    assertThat(((IdeTestContext) context).level(IdeLogLevel.INTERACTION).getMessages()).isEmpty();

    // answer to the interaction is option 1
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("2"))).isEqualTo(VersionIdentifier.of("2"));
    List<String> interactions = ((IdeTestContext) context).level(IdeLogLevel.INTERACTION).getMessages();
    assertThat(interactions.get(0)).contains("which has one or more vulnerabilities");
    assertThat(interactions.get(1)).isEqualTo("Option 1: Stay with the current unsafe version (2).");
    assertThat(interactions.get(2)).isEqualTo("Option 2: Install the next safe version (6).");
    assertThat(interactions.get(3)).isEqualTo("Option 3: Install the latest version (9). This version is save.");
    assertThat(interactions.size()).isEqualTo(4);

    // answer to the interaction is option 2
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("2"))).isEqualTo(VersionIdentifier.of("6"));
    // answer to the interaction is option 3
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("2"))).isEqualTo(VersionIdentifier.of("*"));
  }

  /**
   * Test of {@link ToolCommandlet#securityRiskInteraction(VersionIdentifier)} where no safe version is available. Only
   * all the vulnerabilities considered together cover all versions and there is no single vulnerability that affects
   * all versions.
   */
  @Test
  public void testSecurityRiskInteractionAllVersionAffectedByMultipleWarning() {

    // arrange
    Class<? extends ToolCommandlet> dummyTool = Azure.class;
    IdeContext context = getContextForSecurityJsonTests(dummyTool);
    ToolCommandlet tool = context.getCommandletManager().getCommandlet(dummyTool);
    UrlSecurityJsonFile securityFile = context.getUrls().getEdition(tool.getName(), tool.getEdition())
        .getSecurityJsonFile();
    securityFile.addSecurityWarning(VersionRange.of("[1,5]"));
    securityFile.addSecurityWarning(VersionRange.of("[6,)"));

    // act & assert
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("6"))).isEqualTo(VersionIdentifier.of("6"));
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("1"))).isEqualTo(VersionIdentifier.of("1"));
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("9"))).isEqualTo(VersionIdentifier.of("9"));
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("*"))).isEqualTo(VersionIdentifier.of("*"));
    assertThat(((IdeTestContext) context).level(IdeLogLevel.INTERACTION).getMessages()).isEmpty();
    assertThat(((IdeTestContext) context).level(IdeLogLevel.WARNING).getMessages())
        .allMatch(message -> message.contains("There is no safe version available."));

  }

  /**
   * Test of {@link ToolCommandlet#securityRiskInteraction(VersionIdentifier)} where the set version is the latest but
   * vulnerable.
   */
  @Test
  public void testSecurityRiskInteractionCurrentIsLatest() {

    // arrange
    Class<? extends ToolCommandlet> dummyTool = Azure.class;
    String[] answers = { "1", "2" };
    IdeContext context = getContextForSecurityJsonTests(dummyTool, answers);
    ToolCommandlet tool = context.getCommandletManager().getCommandlet(dummyTool);
    UrlSecurityJsonFile securityFile = context.getUrls().getEdition(tool.getName(), tool.getEdition())
        .getSecurityJsonFile();
    securityFile.addSecurityWarning(VersionRange.of("[2,5]"));
    securityFile.addSecurityWarning(VersionRange.of("[7,9]"));

    // act & assert
    // answer to the interaction is option 1
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("*"))).isEqualTo(VersionIdentifier.of("*"));
    List<String> interactions = ((IdeTestContext) context).level(IdeLogLevel.INTERACTION).getMessages();
    assertThat(interactions.get(0)).contains("which has one or more vulnerabilities");
    assertThat(interactions.get(0)).contains("There are no updates available.");
    assertThat(interactions.get(1)).isEqualTo("Option 1: Stay with the current unsafe version (9).");
    assertThat(interactions.get(2)).isEqualTo("Option 2: Install the latest of all safe versions (6).");
    assertThat(interactions.size()).isEqualTo(3);

    // answer to the interaction is option 2
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("*"))).isEqualTo(VersionIdentifier.of("6"));
  }

  /**
   * Test of {@link ToolCommandlet#securityRiskInteraction(VersionIdentifier)} where there are no newer versions that
   * are safe, but there is a previous version that is safe.
   */
  @Test
  public void testSecurityRiskInteractionNextSafeIsNull() {

    // arrange
    Class<? extends ToolCommandlet> dummyTool = Azure.class;
    String[] answers = { "1", "2" };
    IdeContext context = getContextForSecurityJsonTests(dummyTool, answers);
    ToolCommandlet tool = context.getCommandletManager().getCommandlet(dummyTool);
    UrlSecurityJsonFile securityFile = context.getUrls().getEdition(tool.getName(), tool.getEdition())
        .getSecurityJsonFile();
    securityFile.addSecurityWarning(VersionRange.of("[3,3]"));
    securityFile.addSecurityWarning(VersionRange.of("[6,7]"));
    securityFile.addSecurityWarning(VersionRange.of("[8,)"));

    // act & assert
    // answer to the interaction is option 1
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("6"))).isEqualTo(VersionIdentifier.of("6"));
    List<String> interactions = ((IdeTestContext) context).level(IdeLogLevel.INTERACTION).getMessages();
    assertThat(interactions.get(0)).contains("which has one or more vulnerabilities");
    assertThat(interactions.get(0)).contains("All newer versions are also not safe.");
    assertThat(interactions.get(1)).isEqualTo("Option 1: Stay with the current unsafe version (6).");
    assertThat(interactions.get(2)).isEqualTo("Option 2: Install the latest of all safe versions (5).");
    assertThat(interactions.size()).isEqualTo(3);

    // answer to the interaction is option 2
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("6"))).isEqualTo(VersionIdentifier.of("5"));
  }

  /**
   * Test of {@link ToolCommandlet#securityRiskInteraction(VersionIdentifier)} where the next safe version is also the
   * latest.
   */
  @Test
  public void testSecurityRiskInteractionNextSafeIsLatest() {

    // arrange
    Class<? extends ToolCommandlet> dummyTool = Azure.class;
    String[] answers = { "1", "2" };
    IdeContext context = getContextForSecurityJsonTests(dummyTool, answers);
    ToolCommandlet tool = context.getCommandletManager().getCommandlet(dummyTool);
    UrlSecurityJsonFile securityFile = context.getUrls().getEdition(tool.getName(), tool.getEdition())
        .getSecurityJsonFile();
    securityFile.addSecurityWarning(VersionRange.of("[3,3]"));
    securityFile.addSecurityWarning(VersionRange.of("[6,7]"));
    securityFile.addSecurityWarning(VersionRange.of("[8,8]"));

    // act & assert
    // answer to the interaction is option 1
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("7"))).isEqualTo(VersionIdentifier.of("7"));
    List<String> interactions = ((IdeTestContext) context).level(IdeLogLevel.INTERACTION).getMessages();
    assertThat(interactions.get(0)).contains("which has one or more vulnerabilities");
    assertThat(interactions.get(0)).contains("Of the newer versions, only the latest is safe.");
    assertThat(interactions.get(1)).isEqualTo("Option 1: Stay with the current unsafe version (7).");
    assertThat(interactions.get(2)).isEqualTo("Option 2: Install the latest version (9). This version is save.");
    assertThat(interactions.size()).isEqualTo(3);

    // answer to the interaction is option 2
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("7"))).isEqualTo(VersionIdentifier.of("*"));
  }

  /**
   * Test of {@link ToolCommandlet#securityRiskInteraction(VersionIdentifier)} where the next safe version is also the
   * latest safe version, and the overall latest version is not safe.
   */
  @Test
  public void testSecurityRiskInteractionNextSafeIsLatestSafe() {

    // arrange
    Class<? extends ToolCommandlet> dummyTool = Azure.class;
    String[] answers = { "1", "2" };
    IdeContext context = getContextForSecurityJsonTests(dummyTool, answers);
    ToolCommandlet tool = context.getCommandletManager().getCommandlet(dummyTool);
    UrlSecurityJsonFile securityFile = context.getUrls().getEdition(tool.getName(), tool.getEdition())
        .getSecurityJsonFile();
    securityFile.addSecurityWarning(VersionRange.of("[3,3]"));
    securityFile.addSecurityWarning(VersionRange.of("[5,6]"));
    securityFile.addSecurityWarning(VersionRange.of("[8,9]"));

    // act & assert
    // answer to the interaction is option 1
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("5"))).isEqualTo(VersionIdentifier.of("5"));
    List<String> interactions = ((IdeTestContext) context).level(IdeLogLevel.INTERACTION).getMessages();
    assertThat(interactions.get(0)).contains("which has one or more vulnerabilities");
    assertThat(interactions.get(0))
        .contains("Of the newer versions, only version 7 is safe, which is however not the latest.");
    assertThat(interactions.get(1)).isEqualTo("Option 1: Stay with the current unsafe version (5).");
    assertThat(interactions.get(2)).isEqualTo("Option 2: Install the next safe version (7).");
    assertThat(interactions.size()).isEqualTo(3);

    // answer to the interaction is option 2
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("5"))).isEqualTo(VersionIdentifier.of("7"));
  }

  /**
   * Test of {@link ToolCommandlet#securityRiskInteraction(VersionIdentifier)} where the next safe version differs from
   * the latest safe, which is also the overall latest version.
   */
  @Test
  public void testSecurityRiskInteractionLatestSafeDiffersFromNextSafeButIsLatest() {

    // arrange
    Class<? extends ToolCommandlet> dummyTool = Azure.class;
    String[] answers = { "1", "2", "3" };
    IdeContext context = getContextForSecurityJsonTests(dummyTool, answers);
    ToolCommandlet tool = context.getCommandletManager().getCommandlet(dummyTool);
    UrlSecurityJsonFile securityFile = context.getUrls().getEdition(tool.getName(), tool.getEdition())
        .getSecurityJsonFile();
    securityFile.addSecurityWarning(VersionRange.of("[3,3]"));
    securityFile.addSecurityWarning(VersionRange.of("[5,6]"));
    securityFile.addSecurityWarning(VersionRange.of("[8,8]"));

    // act & assert
    // answer to the interaction is option 1
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("5"))).isEqualTo(VersionIdentifier.of("5"));
    List<String> interactions = ((IdeTestContext) context).level(IdeLogLevel.INTERACTION).getMessages();
    assertThat(interactions.get(0)).contains("which has one or more vulnerabilities");
    assertThat(interactions.get(1)).isEqualTo("Option 1: Stay with the current unsafe version (5).");
    assertThat(interactions.get(2)).isEqualTo("Option 2: Install the next safe version (7).");
    assertThat(interactions.get(3)).isEqualTo("Option 3: Install the latest version (9). This version is save.");
    assertThat(interactions.size()).isEqualTo(4);

    // answer to the interaction is option 2
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("5"))).isEqualTo(VersionIdentifier.of("7"));
    // answer to the interaction is option 3
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("5"))).isEqualTo(VersionIdentifier.of("*"));
  }

  /**
   * Test of {@link ToolCommandlet#securityRiskInteraction(VersionIdentifier)} where the next safe version differs from
   * the latest safe, and the overall latest version is not safe.
   */
  @Test
  public void testSecurityRiskInteractionLatestSafeDiffersFromNextSafeAndLatest() {

    // arrange
    Class<? extends ToolCommandlet> dummyTool = Azure.class;
    String[] answers = { "1", "2", "3" };
    IdeContext context = getContextForSecurityJsonTests(dummyTool, answers);
    ToolCommandlet tool = context.getCommandletManager().getCommandlet(dummyTool);
    UrlSecurityJsonFile securityFile = context.getUrls().getEdition(tool.getName(), tool.getEdition())
        .getSecurityJsonFile();
    securityFile.addSecurityWarning(VersionRange.of("[3,3]"));
    securityFile.addSecurityWarning(VersionRange.of("[6,6]"));
    securityFile.addSecurityWarning(VersionRange.of("[8,9]"));

    // act & assert
    // answer to the interaction is option 1
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("3"))).isEqualTo(VersionIdentifier.of("3"));
    List<String> interactions = ((IdeTestContext) context).level(IdeLogLevel.INTERACTION).getMessages();
    assertThat(interactions.get(0)).contains("which has one or more vulnerabilities");
    assertThat(interactions.get(1)).isEqualTo("Option 1: Stay with the current unsafe version (3).");
    assertThat(interactions.get(2)).isEqualTo("Option 2: Install the next safe version (4).");
    assertThat(interactions.get(3)).isEqualTo("Option 3: Install the latest of all safe versions (7).");
    assertThat(interactions.size()).isEqualTo(4);

    // answer to the interaction is option 2
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("3"))).isEqualTo(VersionIdentifier.of("4"));
    // answer to the interaction is option 3
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("3"))).isEqualTo(VersionIdentifier.of("7"));
  }

  /** Test of {@link ToolCommandlet#securityRiskInteraction(VersionIdentifier)} where set version is safe. */
  @Test
  public void testSecurityRiskInteractionCurrentVersionIsSafe() {

    // arrange
    Class<? extends ToolCommandlet> dummyTool = Azure.class;
    IdeContext context = getContextForSecurityJsonTests(dummyTool);
    ToolCommandlet tool = context.getCommandletManager().getCommandlet(dummyTool);
    UrlSecurityJsonFile securityFile = context.getUrls().getEdition(tool.getName(), tool.getEdition())
        .getSecurityJsonFile();
    securityFile.addSecurityWarning(VersionRange.of("[1,5]"));
    securityFile.addSecurityWarning(VersionRange.of("[7,8]"));

    // act & assert
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("6"))).isEqualTo(VersionIdentifier.of("6"));
    assertThat(tool.securityRiskInteraction(VersionIdentifier.of("9"))).isEqualTo(VersionIdentifier.of("9"));
    assertThat(((IdeTestContext) context).level(IdeLogLevel.INTERACTION).getMessages()).isEmpty();
  }

  /**
   * Creates the context and data for the tests of {@link ToolCommandlet#securityRiskInteraction(VersionIdentifier)}.
   *
   * @param dummyTool the dummy tool to be used for the tests. The
   *        {@link com.devonfw.tools.ide.url.model.folder.UrlVersion folders} representing the versions of the dummy
   *        tool are created here.
   * @param answers the answers to be used for the interaction in
   *        {@link ToolCommandlet#securityRiskInteraction(VersionIdentifier)}.
   * @return the {@link IdeTestContext} to be used for the tests.
   */
  private IdeContext getContextForSecurityJsonTests(Class<? extends ToolCommandlet> dummyTool, String... answers) {

    String path = "project/workspaces/foo-test/my-git-repo";
    // if I don't pass answers here I get: End of answers reached!
    IdeContext context = newContext("basic", path, true, answers);
    ToolCommandlet toolCommandlet = context.getCommandletManager().getCommandlet(dummyTool);
    Path editionPath = context.getUrlsPath().resolve(toolCommandlet.getName()).resolve(toolCommandlet.getEdition());
    context.getFileAccess().delete(editionPath); // I want to define my own versions for simplicity
    int numberOfVersions = 10;
    for (int i = 1; i < numberOfVersions; i++) {
      context.getFileAccess().mkdirs(editionPath.resolve(String.valueOf(i)));
    }
    return context;
  }
}
