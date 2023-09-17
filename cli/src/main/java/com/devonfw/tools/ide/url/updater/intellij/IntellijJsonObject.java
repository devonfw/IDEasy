package com.devonfw.tools.ide.url.updater.intellij;

import java.util.List;

import com.devonfw.tools.ide.common.JsonObject;

/**
 * {@link JsonObject} for Intellij content.
 */
public class IntellijJsonObject implements JsonObject {

  private List<IntellijJsonRelease> releases;

  /**
   * @return the {@link List} of the {@link IntellijJsonRelease}s.
   */
  public List<IntellijJsonRelease> getReleases() {

    return this.releases;
  }

}
