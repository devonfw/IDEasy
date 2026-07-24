package com.devonfw.tools.ide.tool.python;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.uv.Uv;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link PythonRepository}.
 */
public class PythonRepositoryTest extends AbstractIdeContextTest {

  /** JSON same structure as produced by {@code uv python list --all-versions --output-format json}. */
  private static final String UV_PYTHON_LIST_JSON = """
      [
        {"version": "3.14.6", "implementation": "cpython", "os": "macos", "arch": "x86_64"},
        {"version": "3.13.14", "implementation": "cpython", "os": "macos", "arch": "x86_64"},
        {"version": "3.11.4",  "implementation": "cpython", "os": "macos", "arch": "x86_64"},
        {"version": "7.3.17",  "implementation": "pypy",    "os": "macos", "arch": "x86_64"}
      ]
      """;

  private PythonRepository newRepository(IdeTestContext context) {

    return new PythonRepository(context) {
      @Override
      protected List<PythonUvListEntry> fetchUvPythonList() {
        return context.getCommandletManager().getCommandlet(Uv.class).parsePythonListJson(List.of(UV_PYTHON_LIST_JSON));
      }
    };
  }

  @Test
  public void testGetSortedVersionsComesFromUvAndDropsNonCpython() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    PythonRepository repository = newRepository(context);

    // act
    List<VersionIdentifier> versions = repository.getSortedVersions("python", "python", null);

    // assert
    assertThat(versions).containsExactly(
        VersionIdentifier.of("3.14.6"),
        VersionIdentifier.of("3.13.14"),
        VersionIdentifier.of("3.11.4"));
    // PyPy must not leak in as a python version
    assertThat(versions).doesNotContain(VersionIdentifier.of("7.3.17"));
  }

  @Test
  public void testResolveVersionPatternFindsPlatformSpecificBuild() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    PythonRepository repository = newRepository(context);

    // act
    VersionIdentifier resolved = repository.resolveVersion("python", "python", VersionIdentifier.of("3.14*"), null);

    // assert
    assertThat(resolved).isEqualTo(VersionIdentifier.of("3.14.6"));
  }

  @Test
  public void testGetSortedEditionsIsToolNameOnly() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    PythonRepository repository = newRepository(context);

    // act & assert
    assertThat(repository.getSortedEditions("python")).containsExactly("python");
  }
}
