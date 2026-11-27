package com.devonfw.tools.ide.url.updater;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.url.tool.intellij.IntellijJsonObject;
import com.devonfw.tools.ide.url.tool.intellij.IntellijJsonRelease;

/**
 * Test of {@link IdeaBasedUrlUpdater}.
 */
class IdeaBasedUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /** A concrete {@link IdeaBasedUrlUpdater} used to test the merged handling of the JetBrains registry response. */
  private static final class TestIdeaBasedUrlUpdater extends IdeaBasedUrlUpdater {

    @Override
    public String getTool() {

      return null;
    }

    @Override
    protected String doGetVersionUrl() {

      return null;
    }
  }

  /**
   * Test that the releases of all the former editions returned by the JetBrains registry are merged into a single JSON object (deduplicated by version), so
   * that the versions which are only listed for one of them are still collected.
   */
  @Test
  void testGetJsonObjectFromResponseMergesReleasesOfAllEditions() throws Exception {

    String response = """
        [
          {
            "releases": [
              {
                "version": "2025.2.6.2",
                "downloads": {}
              },
              {
                "version": "2025.2.6.1",
                "downloads": {}
              },
              {
                "version": "2024.3.0",
                "downloads": {}
              }
            ]
          },
          {
            "releases": [
              {
                "version": "2025.2.6.1",
                "downloads": {}
              },
              {
                "version": "2024.3.0",
                "downloads": {}
              }
            ]
          }
        ]
        """;

    TestIdeaBasedUrlUpdater updater = new TestIdeaBasedUrlUpdater();

    IntellijJsonObject merged = updater.getJsonObjectFromResponse(response, "");

    assertThat(merged.releases()).extracting(IntellijJsonRelease::version).containsExactly("2025.2.6.2", "2025.2.6.1", "2024.3.0");
  }
}
