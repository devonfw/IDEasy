package com.devonfw.tools.ide.github;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link GithubReleases}.
 */
class GithubReleasesTest extends Assertions {

  /**
   * Test adding and retrieving releases from the GithubReleases collection.
   */
  @Test
  void testAddAndRetrieveReleases() {
    // arrange
    GithubReleases releases = new GithubReleases();
    GithubRelease release1 = new GithubRelease("1.0.0");
    GithubRelease release2 = new GithubRelease("2.0.0");

    // act
    releases.add(release1);
    releases.add(release2);

    // assert
    assertThat(releases).hasSize(2);
    assertThat(releases.get(0).version()).isEqualTo("1.0.0");
    assertThat(releases.get(1).version()).isEqualTo("2.0.0");
  }

  /**
   * Test that a new GithubReleases collection is empty.
   */
  @Test
  void testEmptyReleases() {
    // arrange
    GithubReleases releases = new GithubReleases();

    // assert
    assertThat(releases).isEmpty();
  }
}
