package com.devonfw.tools.ide.git;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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

  /** Test {@link GitUrl#GitUrl(String, String)} with invalid URL (containing #). */
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

  /** Test {@link GitUrl#GitUrl(String, String)} with invalid URL (not starting with http, https or ssh). */
  @Test
  void testUrlWithoutHttpsOrSsh() {
    // arrange
    String url = "htps:/url-with-typo";
    String branch = null;
    // act and assert
    assertThatThrownBy(() -> {
      new GitUrl(url, branch);
    }).isInstanceOf(AssertionError.class).hasMessage("Invalid git URL - has to start with https, http or ssh: " + url);
  }

  @Test
  void testValidURL() {
    // arrange
    String url = "https://some-valid.url";
    String branch = null;
    // act and assert
    assertDoesNotThrow(() -> new GitUrl(url, branch));
  }

}
