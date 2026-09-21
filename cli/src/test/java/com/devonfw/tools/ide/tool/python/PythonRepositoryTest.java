package com.devonfw.tools.ide.tool.python;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link PythonRepository}.
 */
public class PythonRepositoryTest extends AbstractIdeContextTest {

  /** Canned {@code uv python list} entries (cpython plus a pypy that must be filtered out). */
  private static final List<PythonUvListEntry> ENTRIES = List.of(
      new PythonUvListEntry("3.14.6", "cpython"),
      new PythonUvListEntry("3.13.14", "cpython"),
      new PythonUvListEntry("3.11.4", "cpython"),
      new PythonUvListEntry("7.3.17", "pypy"));

  /**
   * Creates a {@link PythonRepository} whose {@code uv python list} interaction is stubbed via the {@link PythonRepository#fetchUvPythonList()} seam.
   *
   * @param context the test {@link IdeContext}.
   * @return the {@link PythonRepository}.
   */
  private PythonRepository repositoryWithVersions(IdeTestContext context) {

    return new PythonRepository(context) {
      @Override
      protected List<PythonUvListEntry> fetchUvPythonList() {

        return ENTRIES;
      }
    };
  }

  @Test
  public void testGetSortedVersionsComesFromUvAndDropsNonCpython() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);

    // act
    List<VersionIdentifier> versions = repositoryWithVersions(context).getSortedVersions("python", "python", null);

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

    // act
    VersionIdentifier resolved = repositoryWithVersions(context).resolveVersion("python", "python", VersionIdentifier.of("3.14*"), null);

    // assert
    assertThat(resolved).isEqualTo(VersionIdentifier.of("3.14.6"));
  }

  @Test
  public void testGetSortedVersionsIsEmptyWhenUvNotInstalled() {

    // arrange: a plain context where uv is not installed. The real fetchUvPythonList() hits the isInstalled() guard and yields nothing.
    IdeTestContext context = newContext(PROJECT_BASIC);
    PythonRepository repository = new PythonRepository(context);

    // act
    List<VersionIdentifier> versions = repository.getSortedVersions("python", "python", null);

    // assert: no candidates (a warning is logged instead) and no (auto-)install was triggered
    assertThat(versions).isEmpty();
  }

  @Test
  public void testGetSortedEditionsIsToolNameOnly() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    PythonRepository repository = new PythonRepository(context);

    // act & assert
    assertThat(repository.getSortedEditions("python")).containsExactly("python");
  }
}
