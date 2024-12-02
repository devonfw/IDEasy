package com.devonfw.tools.ide.cli;

import java.nio.file.Path;

import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link CliException} that is thrown if further processing requires network but the user if offline.
 */
public final class CliOfflineException extends CliException {

  /**
   * The constructor.
   */
  public CliOfflineException() {

    super("You are offline but network connection is required to perform the operation.", ProcessResult.OFFLINE);
  }

  /**
   * The constructor.
   *
   * @param message the {@link #getMessage() message}.
   */
  private CliOfflineException(String message) {
    super(message, ProcessResult.OFFLINE);
  }

  /**
   * Factory method, which is called, when trying to download via a URL
   *
   * @param url the url, which the software should be downloaded from.
   * @return A {@link CliOfflineException} with an informative message.
   */
  public static CliOfflineException ofDownloadViaUrl(String url) {
    return new CliOfflineException("You are offline and cannot download from URL " + url);
  }

  /**
   * Factory method, which is called, when trying to download via tool name, edition and version
   *
   * @param tool the name of the tool, we want to download.
   * @param edition the edition of the tool, we want to download.
   * @param version the {@link VersionIdentifier} of the tool, we want to download.
   * @return A {@link CliOfflineException} with an informative message.
   */
  public static CliOfflineException ofDownloadOfTool(String tool, String edition, VersionIdentifier version) {
    return new CliOfflineException("Not able to download tool " + tool + " in edition " + edition + " with version " + version + " because we are offline");
  }

  /**
   * Factory method, which is called, when just a purpose is given.
   *
   * @param purpose the purpose, which the internet connection serves.
   * @return A {@link CliOfflineException} with an informative message.
   */
  public static CliOfflineException ofPurpose(String purpose) {
    return new CliOfflineException("You are offline but Internet access is required for " + purpose);
  }

  /**
   * Factory method, which is called, when a clone is performed in offline mode
   *
   * @param url the url, in which the clone should be executed.
   * @param repository the path, where the repository should be cloned to.
   * @return A {@link CliOfflineException} with an informative message.
   */
  public static CliOfflineException ofClone(String url, Path repository) {
    return new CliOfflineException("Could not clone " + url + " to " + repository + " because you are offline.");
  }

}
