package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.androidstudio.AndroidStudio;
import com.devonfw.tools.ide.tool.intellij.Intellij;
import com.devonfw.tools.ide.tool.pycharm.Pycharm;

/**
 * Test of the workspace contract that {@link IdeaBasedIdeToolCommandlet} shares between all JetBrains based IDEs (see #2531).
 * <p>
 * The individual IDEs are tested in their own test classes; what is verified here is that the shared behaviour holds for <em>every</em> JetBrains IDE. This
 * matters most for {@code android-studio}, whose metadata folder name (derived from the tool name, {@code .android-studio}) and configuration file name
 * (derived from the product prefix, {@code studio.properties}) come from two different sources.
 */
class IdeaBasedIdeToolCommandletTest extends AbstractIdeContextTest {

  /**
   * @param tool the name of the JetBrains IDE tool.
   * @param configurationFileName the name of the IDE configuration file that IDEasy generates for it.
   */
  @ParameterizedTest
  @CsvSource({ "intellij,idea.properties", "android-studio,studio.properties", "pycharm,pycharm.properties" })
  void testWorkspaceRedirectsAndExcludes(String tool, String configurationFileName) {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    IdeaBasedIdeToolCommandlet commandlet = newCommandlet(tool).apply(context);
    Path workspaceFolder = context.getWorkspacePath();
    Path ideMetadata = context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve(tool).resolve(context.getWorkspaceName());

    // act
    Map<Path, Path> redirects = commandlet.getWorkspaceRedirects(workspaceFolder);
    Set<Path> excludes = commandlet.getWorkspaceExcludes(workspaceFolder);

    // assert
    // the IDE metadata template is merged out of the workspace ...
    assertThat(redirects).containsExactly(Map.entry(workspaceFolder.resolve("." + tool), ideMetadata));
    // ... and the configuration file template is not merged at all since IDEasy generates that file itself
    assertThat(excludes).containsExactly(workspaceFolder.resolve(configurationFileName));
  }

  private static Function<IdeContext, IdeaBasedIdeToolCommandlet> newCommandlet(String tool) {

    return switch (tool) {
      case "intellij" -> Intellij::new;
      case "android-studio" -> AndroidStudio::new;
      case "pycharm" -> Pycharm::new;
      default -> throw new IllegalArgumentException(tool);
    };
  }
}
