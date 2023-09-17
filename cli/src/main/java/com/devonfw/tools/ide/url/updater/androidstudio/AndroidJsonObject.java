package com.devonfw.tools.ide.url.updater.androidstudio;

import com.devonfw.tools.ide.common.JsonObject;

/**
 * {@link JsonObject} for Android Studio content.
 */
public class AndroidJsonObject implements JsonObject {

  private AndroidJsonContent content;

  /**
   * @return the {@link AndroidJsonContent}.
   */
  public AndroidJsonContent getContent() {

    return this.content;
  }
}
