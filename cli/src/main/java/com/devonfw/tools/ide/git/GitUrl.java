package com.devonfw.tools.ide.git;

/**
 * Handles parsing of git URLs.
 *
 * @param url the git url e.g. https://github.com/devonfw/ide-urls.git.
 * @param branch the branch name e.g. master.
 */
public record GitUrl(String url, String branch) {

  /** {@link #branch() Branch} @{@value }. */
  public static final String BRANCH_MAIN = "main";

  /** {@link #branch() Branch} @{@value }. */
  public static final String BRANCH_MASTER = "master";

  /**
   * The constructor.
   */
  public GitUrl {
    if (url.contains("#")) {
      String message = "Invalid git URL " + url;
      assert false : message;
    }
  }

  /**
   * Converts the Git URL based on the specified {@link GitUrlSyntax}.
   *
   * @param syntax the preferred {@link GitUrlSyntax} (SSH or HTTPS).
   * @return the converted {@link GitUrl} or the original if no conversion is required.
   */
  public GitUrl convert(GitUrlSyntax syntax) {
    return syntax.format(this);
  }

  @Override
  public String toString() {

    if (this.branch == null) {
      return this.url;
    }
    return this.url + "#" + this.branch;
  }

  /**
   * Extracts the project name from an git URL. For URLs like "https://github.com/devonfw/ide-urls.git" returns "ide-urls"
   *
   * @return the project name without ".git" extension
   */
  public String getProjectName() {

    int lastSlash = this.url.lastIndexOf('/');
    String path;
    if (lastSlash >= 0) {
      path = this.url.substring(lastSlash + 1);
    } else {
      path = this.url; // actually invalid URL
    }
    if (path.endsWith(".git")) {
      path = path.substring(0, path.length() - 4);
    }
    return path;
  }

  /**
   * @param gitUrl the {@link #toString() string representation} of a {@link GitUrl}. May contain a branch name as {@code «url»#«branch»}.
   * @return the parsed {@link GitUrl}.
   */
  public static GitUrl of(String gitUrl) {

    int hashIndex = gitUrl.indexOf('#');
    String url = gitUrl;
    String branch = null;
    if (hashIndex > 0) {
      url = gitUrl.substring(0, hashIndex);
      branch = gitUrl.substring(hashIndex + 1);
    }
    return new GitUrl(url, branch);
  }

  /**
   * @param gitUrl the git {@link #url() URL}.
   * @return a new instance of {@link GitUrl} with the given URL and {@link #BRANCH_MAIN}.
   */
  public static GitUrl ofMain(String gitUrl) {

    return new GitUrl(gitUrl, BRANCH_MAIN);
  }

  /**
   * @param gitUrl the git {@link #url() URL}.
   * @return a new instance of {@link GitUrl} with the given URL and {@link #BRANCH_MASTER}.
   */
  public static GitUrl ofMaster(String gitUrl) {

    return new GitUrl(gitUrl, BRANCH_MASTER);
  }
}
