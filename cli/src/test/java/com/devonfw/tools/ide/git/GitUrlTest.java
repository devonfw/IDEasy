package com.devonfw.tools.ide.git;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link GitUrl}.
 */
class GitUrlTest extends Assertions {

  /** Test {@link GitUrl#of(String)} with url having branch. */
  @Test
  void testOfUrlWithBranch() {

    // arrange
    String url = "https://github.com/devonfw/IDEasy.git";
    String branch = "feature/xyz";
    String urlWithBranch = url + "#" + branch;
    // act
    GitUrl gitUrl = GitUrl.of(urlWithBranch);
    // assert
    assertThat(gitUrl.url()).isEqualTo(url);
    assertThat(gitUrl.branch()).isEqualTo(branch);
    assertThat(gitUrl).hasToString(urlWithBranch);
  }

  /** Test {@link GitUrl#of(String)} with url having no branch. */
  @Test
  void testOfUrlWithoutBranch() {

    // arrange
    String url = "https://github.com/devonfw/IDEasy.git";
    // act
    GitUrl gitUrl = GitUrl.of(url);
    // assert
    assertThat(gitUrl.url()).isEqualTo(url);
    assertThat(gitUrl.branch()).isNull();
    assertThat(gitUrl).hasToString(url);
  }

  /** Test {@link GitUrl#GitUrl(String, String)} with invalid URL. */
  @Test
  void testInvalidUrl() {

    // arrange
    String url = "invalid#url";
    String branch = null;
    // act
    assertThatThrownBy(() -> {
      new GitUrl(url, branch);
    }).isInstanceOf(AssertionError.class).hasMessage("Invalid git URL " + url);
  }

}
