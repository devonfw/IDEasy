package com.devonfw.tools.ide.github;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link GithubTags}.
 */
class GithubTagsTest extends Assertions {

  /**
   * Test adding and retrieving tags from the GithubTags collection.
   */
  @Test
  void testAddAndRetrieveTags() {
    GithubTags tags = new GithubTags();
    GithubTag tag1 = new GithubTag("refs/tags/v1.0.0");
    GithubTag tag2 = new GithubTag("refs/tags/v2.0.0");
    tags.add(tag1);
    tags.add(tag2);
    assertThat(tags).hasSize(2);
    assertThat(tags.get(0).version()).isEqualTo("v1.0.0");
    assertThat(tags.get(1).version()).isEqualTo("v2.0.0");
  }

  /**
   * Test that a new GithubTags collection is empty.
   */
  @Test
  void testEmptyTags() {
    GithubTags tags = new GithubTags();
    assertThat(tags).isEmpty();
  }
}
