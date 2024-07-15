package com.devonfw.tools.ide.github;

import com.devonfw.tools.ide.common.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a github tag ref.
 */
public class GithubTag implements JsonVersionItem {

  private String ref;

  /**
   * The constructor.
   */
  public GithubTag() {

    super();
  }

  /**
   * The constructor.
   *
   * @param ref the {@link #getRef() ref}.
   */
  public GithubTag(String ref) {

    super();
    this.ref = ref;
  }

  /**
   * @return the tag reference (e.g. "refs/tags/v1.0").
   */
  @JsonProperty("ref")
  public String getRef() {

    return this.ref;
  }

  /**
   * @param ref the new value of {@link #getRef()}.
   */
  @JsonProperty("ref")
  public void setRef(String ref) {

    this.ref = ref;
  }
}
