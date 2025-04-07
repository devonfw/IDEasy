package com.devonfw.tools.ide.url.updater;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.io.HttpErrorResponse;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.file.UrlChecksum;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFile;
import com.devonfw.tools.ide.url.model.file.UrlFile;
import com.devonfw.tools.ide.url.model.file.UrlStatusFile;
import com.devonfw.tools.ide.url.model.file.json.StatusJson;
import com.devonfw.tools.ide.url.model.file.json.UrlStatus;
import com.devonfw.tools.ide.url.model.file.json.UrlStatusState;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.model.report.UrlUpdaterReport;
import com.devonfw.tools.ide.util.DateTimeUtil;
import com.devonfw.tools.ide.util.HexUtil;

/**
 * Abstract base implementation of {@link UrlUpdater}. Contains methods for retrieving response bodies from URLs, updating tool versions, and checking if
 * download URLs work.
 */
public abstract class AbstractUrlUpdater extends AbstractProcessorWithTimeout implements UrlUpdater {

  /** <a href="https://nvd.nist.gov/products/cpe">CPE</a> stands for Common Platform Enumeration. */
  public static final String CPE = "CPE";

  private static final Duration VERSION_RECHECK_DELAY = Duration.ofDays(3);

  private static final Duration DAYS_UNTIL_DELETION_OF_BROKEN_URL = Duration.ofDays(14);

  /** {@link OperatingSystem#WINDOWS}. */
  protected static final OperatingSystem WINDOWS = OperatingSystem.WINDOWS;

  /** {@link OperatingSystem#MAC}. */
  protected static final OperatingSystem MAC = OperatingSystem.MAC;

  /** {@link OperatingSystem#LINUX}. */
  protected static final OperatingSystem LINUX = OperatingSystem.LINUX;

  /** {@link SystemArchitecture#X64}. */
  protected static final SystemArchitecture X64 = SystemArchitecture.X64;

  /** {@link SystemArchitecture#ARM64}. */
  protected static final SystemArchitecture ARM64 = SystemArchitecture.ARM64;

  /** List of URL file names dependent on OS which need to be checked for existence */
  private static final Set<String> URL_FILENAMES_PER_OS = Set.of("linux_x64.urls", "mac_arm64.urls", "mac_x64.urls",
      "windows_x64.urls");

  /** List of URL file name independent of OS which need to be checked for existence */
  private static final Set<String> URL_FILENAMES_OS_INDEPENDENT = Set.of("urls");

  /** The {@link HttpClient} for HTTP requests. */
  protected final HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();

  private static final Logger logger = LoggerFactory.getLogger(AbstractUrlUpdater.class);

  /**
   * The constructor.
   */
  public AbstractUrlUpdater() {
    super();
  }

  /**
   * @return the name of the {@link UrlTool tool} handled by this updater.
   */
  protected abstract String getTool();

  /**
   * @return the name of the {@link UrlEdition edition} handled by this updater.
   */
  protected String getEdition() {

    return getTool();
  }

  /**
   * @return the names of the {@link UrlEdition editions} handled by this updater.
   */
  protected List<String> getEditions() {

    return List.of(getEdition());
  }

  /**
   * @param edition the edition of the tool
   * @return the combination of {@link #getTool() tool} and edition but simplified if both are equal.
   */
  protected final String getToolWithEdition(String edition) {

    String tool = getTool();
    if (tool.equals(edition)) {
      return tool;
    }
    return tool + "/" + edition;
  }

  /**
   * @return the combination of {@link #getTool() tool} and {@link #getEdition() edition} but simplified if both are equal.
   */
  protected final String getToolWithEdition() {

    String tool = getTool();
    String edition = getEdition();
    if (tool.equals(edition)) {
      return tool;
    }
    return tool + "/" + edition;
  }

  /**
   * @return the vendor of the tool as specified in the {@link #CPE}.
   */
  public String getCpeVendor() {
    return getTool();
  }

  /**
   * @return the product name of the tool as specified in the {@link #CPE}.
   */
  public String getCpeProduct() {
    return getTool();
  }

  /**
   * @return the edition as specified in the {@link #CPE}.
   */
  public String getCpeEdition() {

    return getTool();
  }

  /**
   * @param version the {@link UrlVersion#getName() version} to map to the format or syntax used in the {@link #CPE}.
   * @return the version as specified in the {@link #CPE}.
   */
  public String mapUrlVersionToCpeVersion(String version) {

    return version;
  }

  /**
   * This method is only used as fallback if the passed version is not in the expected format or syntax of {@link #mapUrlVersionToCpeVersion(String)}. This
   * doesn't have to be inverse of {@link #mapUrlVersionToCpeVersion(String)}. It must only be sufficient to get the correct
   * {@link com.devonfw.tools.ide.version.VersionRange} from the matched vulnerable software.
   *
   * @return the mapped version as specified in the {@link #CPE} to the version as specified by the directory name in the url repository.
   */
  public String mapCpeVersionToUrlVersion(String version) {

    return version;
  }

  /**
   * Retrieves the response body from a given URL.
   *
   * @param url the URL to retrieve the response body from.
   * @return a string representing the response body.
   * @throws IllegalStateException if the response body could not be retrieved.
   */
  protected String doGetResponseBodyAsString(String url) {

    try {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
      HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        return response.body();
      }
      throw new IllegalStateException("Unexpected response code " + response.statusCode() + ":" + response.body());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to retrieve response body from url: " + url, e);
    }
  }

  /**
   * @param url the URL of the download file.
   * @return the {@link InputStream} of response body.
   */
  protected HttpResponse<InputStream> doGetResponseAsStream(String url) {

    try {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
      return this.client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to retrieve response from url: " + url, e);
    }
  }


  /**
   * Updates a tool version with the given arguments (OS independent).
   *
   * @param urlVersion the {@link UrlVersion} with the {@link UrlVersion#getName() version-number} to process.
   * @param downloadUrl the URL of the download for the tool.
   * @return {@code true} if the version was successfully added, {@code false} otherwise.
   */
  protected boolean doAddVersion(UrlVersion urlVersion, String downloadUrl) {

    return doAddVersion(getEdition(), urlVersion, downloadUrl, null);
  }

  /**
   * Updates a tool version with the given arguments (OS independent).
   *
   * @param edition the edition of the tool.
   * @param urlVersion the {@link UrlVersion} with the {@link UrlVersion#getName() version-number} to process.
   * @param downloadUrl the URL of the download for the tool.
   * @return {@code true} if the version was successfully added, {@code false} otherwise.
   */
  protected boolean doAddVersion(String edition, UrlVersion urlVersion, String downloadUrl) {

    return doAddVersion(edition, urlVersion, downloadUrl, null);
  }


  /**
   * Updates a tool version with the given arguments.
   *
   * @param urlVersion the {@link UrlVersion} with the {@link UrlVersion#getName() version-number} to process.
   * @param downloadUrl the URL of the download for the tool.
   * @param os the {@link OperatingSystem} for the tool (can be null).
   * @return {@code true} if the version was successfully added, {@code false} otherwise.
   */
  protected boolean doAddVersion(UrlVersion urlVersion, String downloadUrl, OperatingSystem os) {

    return doAddVersion(getEdition(), urlVersion, downloadUrl, os, null);
  }

  /**
   * Updates a tool version with the given arguments.
   *
   * @param edition the edition of the tool.
   * @param urlVersion the {@link UrlVersion} with the {@link UrlVersion#getName() version-number} to process.
   * @param downloadUrl the URL of the download for the tool.
   * @param os the {@link OperatingSystem} for the tool (can be null).
   * @return {@code true} if the version was successfully added, {@code false} otherwise.
   */
  protected boolean doAddVersion(String edition, UrlVersion urlVersion, String downloadUrl, OperatingSystem os) {

    return doAddVersion(edition, urlVersion, downloadUrl, os, null);
  }

  /**
   * Updates a tool version with the given arguments.
   *
   * @param urlVersion the {@link UrlVersion} with the {@link UrlVersion#getName() version-number} to process.
   * @param downloadUrl the URL of the download for the tool.
   * @param os the {@link OperatingSystem} for the tool (can be null).
   * @param architecture the optional {@link SystemArchitecture}.
   * @return {@code true} if the version was successfully added, {@code false} otherwise.
   */
  protected boolean doAddVersion(UrlVersion urlVersion, String downloadUrl, OperatingSystem os,
      SystemArchitecture architecture) {

    return doAddVersion(getEdition(), urlVersion, downloadUrl, os, architecture, "");
  }

  /**
   * Updates a tool version with the given arguments.
   *
   * @param edition the edition of the tool.
   * @param urlVersion the {@link UrlVersion} with the {@link UrlVersion#getName() version-number} to process.
   * @param downloadUrl the URL of the download for the tool.
   * @param os the {@link OperatingSystem} for the tool (can be null).
   * @param architecture the optional {@link SystemArchitecture}.
   * @return {@code true} if the version was successfully added, {@code false} otherwise.
   */
  protected boolean doAddVersion(String edition, UrlVersion urlVersion, String downloadUrl, OperatingSystem os,
      SystemArchitecture architecture) {

    return doAddVersion(edition, urlVersion, downloadUrl, os, architecture, "");
  }


  /**
   * Updates a tool version with the given arguments.
   *
   * @param urlVersion the {@link UrlVersion} with the {@link UrlVersion#getName() version-number} to process.
   * @param url the URL of the download for the tool.
   * @param os the optional {@link OperatingSystem}.
   * @param architecture the optional {@link SystemArchitecture}.
   * @param checksum the existing checksum (e.g. from JSON metadata) or the empty {@link String} if not available and computation needed.
   * @return {@code true} if the version was successfully added, {@code false} otherwise.
   */
  protected boolean doAddVersion(UrlVersion urlVersion, String url, OperatingSystem os, SystemArchitecture architecture, String checksum) {
    return doAddVersion(getEdition(), urlVersion, url, os, architecture, checksum);
  }

  /**
   * Updates a tool version with the given arguments.
   *
   * @param edition the edition of the tool.
   * @param urlVersion the {@link UrlVersion} with the {@link UrlVersion#getName() version-number} to process.
   * @param url the URL of the download for the tool.
   * @param os the optional {@link OperatingSystem}.
   * @param architecture the optional {@link SystemArchitecture}.
   * @param checksum the existing checksum (e.g. from JSON metadata) or the empty {@link String} if not available and computation needed.
   * @return {@code true} if the version was successfully added, {@code false} otherwise.
   */
  protected boolean doAddVersion(String edition, UrlVersion urlVersion, String url, OperatingSystem os, SystemArchitecture architecture,
      String checksum) {

    UrlStatusFile status = urlVersion.getStatus();
    if ((status != null) && status.getStatusJson().isManual()) {
      return true;
    }
    String version = urlVersion.getName();
    url = url.replace("${version}", version);
    String major = urlVersion.getVersionIdentifier().getStart().getDigits();
    url = url.replace("${major}", major);
    if (os != null) {
      url = url.replace("${os}", os.toString());
    }
    if (architecture != null) {
      url = url.replace("${arch}", architecture.toString());
    }
    url = url.replace("${edition}", edition);

    return doAddVersionUrlIfNewAndValid(edition, url, urlVersion, os, architecture, checksum);
  }

  /**
   * @param response the {@link HttpResponse}.
   * @return {@code true} if success, {@code false} otherwise.
   */
  protected boolean isSuccess(HttpResponse<?> response) {

    if (response == null) {
      return false;
    }
    return response.statusCode() == 200;
  }

  /**
   * Checks if the download file checksum is still valid
   *
   * @param checksum the newly computed checksum.
   * @param urlChecksum the {@link UrlChecksum} to compare.
   * @param toolWithEdition the tool/edition information for logging.
   * @param version the tool version.
   * @param url the URL the checksum belongs to.
   * @return {@code true} if update of checksum was successful, {@code false} otherwise.
   */
  private static boolean isChecksumStillValid(String checksum, UrlChecksum urlChecksum,
      String toolWithEdition, String version, String url) {

    String existingChecksum = urlChecksum.getChecksum();

    if ((existingChecksum != null) && !existingChecksum.equals(checksum)) {
      logger.error("For tool {} and version {} the download URL {} results in checksum {} but expected {}.",
          toolWithEdition, version, url, checksum, existingChecksum);
      return false;
    } else {
      urlChecksum.setChecksum(checksum);
    }
    return true;
  }

  /**
   * Checks if the content type is valid (not of type text)
   *
   * @param url the URL to check.
   * @param toolWithEdition the tool/edition information for logging.
   * @param version the tool version.
   * @param response the {@link HttpResponse}.
   * @return {@code true} if the content type is not of type text, {@code false} otherwise.
   */
  private boolean isValidDownload(String url, String toolWithEdition, String version, HttpResponse<?> response) {

    if (isSuccess(response)) {
      String contentType = response.headers().firstValue("content-type").orElse("undefined");
      boolean isValidContentType = isValidContentType(contentType);
      if (!isValidContentType) {
        logger.error("For toolWithEdition {} and version {} the download has an invalid content type {} for URL {}", toolWithEdition, version,
            contentType, url);
        return false;
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Checks if the content type was not of type text (this method is required because {@link com.devonfw.tools.ide.url.tool.pip.PipUrlUpdater} returns text and
   * needs to be overridden)
   * <p>
   * See: <a href="https://github.com/devonfw/ide/issues/1343">#1343</a> for reference.
   *
   * @param contentType String of the content type
   * @return {@code true} if the content type is not of type text, {@code false} otherwise.
   */
  protected boolean isValidContentType(String contentType) {

    return !contentType.startsWith("text");
  }

  /**
   * Checks the download URL by checksum or by downloading the file and generating the checksum from it
   *
   * @param edition the edition of the tool.
   * @param url the URL of the download to check.
   * @param urlVersion the {@link UrlVersion} where to store the collected information like status and checksum.
   * @param os the {@link OperatingSystem}
   * @param architecture the {@link SystemArchitecture}
   * @param checksum the existing checksum (e.g. from JSON metadata) or the empty {@link String} if not available and computation needed.
   * @return {@code true} if the download was checked successfully, {@code false} otherwise.
   */
  private boolean doAddVersionUrlIfNewAndValid(String edition, String url, UrlVersion urlVersion, OperatingSystem os,
      SystemArchitecture architecture, String checksum) {

    UrlDownloadFile urlDownloadFile = urlVersion.getUrls(os, architecture);
    if (urlDownloadFile != null) {
      if (urlDownloadFile.getUrls().contains(url)) {
        logger.debug("Skipping add of already existing URL {}", url);
        return false;
      }
    }
    HttpResponse<?> response = doCheckDownloadViaHeadRequest(url);
    int statusCode = response.statusCode();
    String toolWithEdition = getToolWithEdition(edition);
    String version = urlVersion.getName();

    boolean success = isValidDownload(url, toolWithEdition, version, response);

    boolean update = false;

    if (success) {
      UrlChecksum urlChecksum = null;
      if (urlDownloadFile != null) {
        urlChecksum = urlVersion.getChecksum(urlDownloadFile.getName());
        if (urlChecksum != null) {
          logger.warn("Checksum is already existing for: {}, skipping.", url);
          update = true;
        }
      }
      if (checksum == null || checksum.isEmpty()) {
        String contentType = response.headers().firstValue("content-type").orElse("undefined");
        checksum = doGenerateChecksum(doGetResponseAsStream(url), url, edition, version, contentType);
      }
      // we only use getOrCreate here to avoid creating empty file if doGenerateChecksum fails
      if (urlChecksum == null) {
        if (urlDownloadFile == null) {
          urlDownloadFile = urlVersion.getOrCreateUrls(os, architecture);
        }
        urlChecksum = urlVersion.getOrCreateChecksum(urlDownloadFile.getName());
      }
      success = isChecksumStillValid(checksum, urlChecksum, toolWithEdition, version, url);
      if (success) {
        urlDownloadFile.addUrl(url);
      }
    }

    doUpdateStatusJson(success, statusCode, edition, urlVersion, url, urlDownloadFile, update);

    urlVersion.save();

    return success;
  }

  /**
   * @param response the {@link HttpResponse}.
   * @param url the download URL
   * @param edition the edition of the tool
   * @param version the {@link UrlVersion version} identifier.
   * @return checksum of input stream as hex string
   */
  private String doGenerateChecksum(HttpResponse<InputStream> response, String url, String edition, String version,
      String contentType) {

    logger.info("Computing checksum for download with URL {}", url);
    try (InputStream inputStream = response.body()) {
      MessageDigest md = MessageDigest.getInstance(UrlChecksum.HASH_ALGORITHM);

      byte[] buffer = new byte[8192];
      int bytesRead;
      long size = 0;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        md.update(buffer, 0, bytesRead);
        size += bytesRead;
      }
      if (size == 0) {
        throw new IllegalStateException("Download empty for " + url);
      }
      byte[] digestBytes = md.digest();
      String checksum = HexUtil.toHexString(digestBytes);
      logger.info(
          "For tool {} and version {} we received {} bytes with content-type {} and computed SHA256 {} from URL {}",
          getToolWithEdition(edition), version, Long.valueOf(size), contentType, checksum, url);
      return checksum;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read body of download " + url, e);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("No such hash algorithm " + UrlChecksum.HASH_ALGORITHM, e);
    }
  }

  /**
   * Checks if a download URL works and if the file is available for download.
   *
   * @param url the URL to check.
   * @return the {@link HttpResponse} to the HEAD request.
   */
  protected HttpResponse<?> doCheckDownloadViaHeadRequest(String url) {

    URI uri = null;
    HttpRequest request = null;
    try {
      uri = URI.create(url);
      request = HttpRequest.newBuilder().uri(uri)
          .method("HEAD", HttpRequest.BodyPublishers.noBody()).timeout(Duration.ofSeconds(5)).build();

      return this.client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      logger.error("Failed to perform HEAD request of URL {}", url, e);
      return new HttpErrorResponse(e, request, uri);
    }
  }

  /**
   * Creates or refreshes the status JSON file for a given UrlVersion instance based on the URLRequestResult of checking if a download URL works.
   *
   * @param success - {@code true} on successful HTTP response, {@code false} otherwise.
   * @param statusCode the HTTP status code of the response.
   * @param urlVersion the {@link UrlVersion} instance to create or refresh the status JSON file for.
   * @param url the checked download URL.
   * @param downloadFile the {@link UrlDownloadFile} where the given {@code url} comes from.
   * @param update - {@code true} in case the URL was updated (verification), {@code false} otherwise (version/URL initially added).
   */
  @SuppressWarnings("null") // Eclipse is too stupid to check this
  private void doUpdateStatusJson(boolean success, int statusCode, String edition, UrlVersion urlVersion, String url, UrlDownloadFile downloadFile,
      boolean update) {

    UrlStatusFile urlStatusFile = urlVersion.getStatus();
    boolean forceCreation = (success || update);
    if ((urlStatusFile == null) && forceCreation) {
      urlStatusFile = urlVersion.getOrCreateStatus();
    }
    StatusJson statusJson = null;
    UrlStatus status = null;
    UrlStatusState errorStatus = null;
    Instant errorTimestamp = null;
    UrlStatusState successStatus = null;
    Instant successTimestamp = null;
    if (urlStatusFile != null) {
      statusJson = urlStatusFile.getStatusJson();
      status = statusJson.getStatus(url, forceCreation);
      if (status != null) {
        errorStatus = status.getError();
        if (errorStatus != null) {
          errorTimestamp = errorStatus.getTimestamp();
        }
        successStatus = status.getSuccess();
        if (successStatus != null) {
          successTimestamp = successStatus.getTimestamp();
        }
      }
    }
    Integer code = Integer.valueOf(statusCode);
    String version = urlVersion.getName();
    String tool = getToolWithEdition(edition);
    boolean modified = false;

    if (success) {
      boolean setSuccess = !update || (successStatus == null);

      if (errorStatus != null) {
        // we avoid git diff overhead by only updating success timestamp if last check was an error
        setSuccess = DateTimeUtil.isAfter(errorTimestamp, successTimestamp);
      }

      if (setSuccess) {
        status.setSuccess(new UrlStatusState());
        modified = true;
      }

      logger.info("For tool {} and version {} the download verification succeeded with status code {} for URL {}.", tool,
          version, code, url);
      getUrlUpdaterReport().incrementVerificationSuccess();
    } else {
      if (status != null) {
        if (errorStatus == null) {
          modified = true;
        } else {
          if (!Objects.equals(code, errorStatus.getCode())) {
            logger.warn("For tool {} and version {} the error status-code changed from {} to {} for URL {}.", tool,
                version, code, errorStatus.getCode(), url);
            modified = true;
          } else if (isErrorCodeForAutomaticUrlRemoval(code)) {
            boolean urlBroken;
            Instant ts = successTimestamp;
            if (ts == null) {
              ts = errorTimestamp;
            }
            if (ts == null) {
              urlBroken = true; // this code should never be reached, but if it does something is very wrong
            } else {
              Duration errorDuration = Duration.between(ts, Instant.now());
              // if more than this number of days the download URL is broken, we delete it
              urlBroken = isGreaterThan(errorDuration, DAYS_UNTIL_DELETION_OF_BROKEN_URL);
              if (!urlBroken) {
                logger.info("Leaving broken URL since error could be temporary - error duration {} for URL {}", errorDuration, url);
              }
            }
            if (urlBroken) {
              removeUrl(url, downloadFile, tool, version, code, urlStatusFile, status);
            }
          } else {
            // we avoid git diff overhead by only updating error timestamp if last check was a success
            if (DateTimeUtil.isAfter(successTimestamp, errorTimestamp)) {
              modified = true;
            }
          }
        }
        if (modified) {
          errorStatus = new UrlStatusState();
          errorStatus.setCode(code);
          status.setError(errorStatus);
        }
      }
      logger.warn("For tool {} and version {} the download verification failed with status code {} for URL {}.", tool,
          version, code, url);

      getUrlUpdaterReport().incrementVerificationFailure();
    }
    if (modified) {
      urlStatusFile.setStatusJson(statusJson); // hack to set modified (better solution welcome)
    }
    if (status != null) {
      assert !status.checkEmpty() : "Invalid status!";
    }
  }

  private static void removeUrl(String url, UrlDownloadFile downloadFile, String tool, String version, Integer code, UrlStatusFile urlStatusFile,
      UrlStatus status) {
    logger.warn("For tool {} and version {} the the URL {} is broken (status code {}) for a long time and will be removed.", tool,
        version, code, url);
    downloadFile.removeUrl(url);
    if (downloadFile.getUrls().isEmpty()) {
      Path downloadPath = downloadFile.getPath();
      logger.warn("For tool {} and version {} all URLs have been removed so the download file {} will be removed.", tool,
          version, downloadPath);
      downloadFile.delete();
      UrlChecksum urlChecksum = downloadFile.getParent().getChecksum(downloadFile.getName());
      if (urlChecksum == null) {
        logger.warn("Was missing checksum file for {}", downloadFile.getPath());
      } else {
        urlChecksum.delete();
      }
    } else {
      downloadFile.save();
    }
    StatusJson statusJson = urlStatusFile.getStatusJson();
    statusJson.remove(url);
    if (statusJson.getUrls().isEmpty()) {
      urlStatusFile.delete();
    } else {
      urlStatusFile.setStatusJson(statusJson);
    }
  }

  private static boolean isErrorCodeForAutomaticUrlRemoval(Integer code) {
    return Integer.valueOf(404).equals(code);
  }

  private boolean isGreaterThan(Duration errorDuration, Duration daysUntilDeletionOfBrokenUrl) {

    int delta = errorDuration.compareTo(daysUntilDeletionOfBrokenUrl);
    // delta: 1 = greater, 0 = equal, -1 = less
    return (delta > 0);
  }

  /**
   * @return Set of URL file names (dependency on OS file names can be overridden with isOsDependent())
   */
  protected Set<String> getUrlFilenames() {

    if (isOsDependent()) {
      return URL_FILENAMES_PER_OS;
    } else {
      return URL_FILENAMES_OS_INDEPENDENT;
    }
  }

  /**
   * Checks if we are dependent on OS URL file names, can be overridden to disable OS dependency
   *
   * @return {@code true} if we want to check for missing OS URL file names, {@code false} if not.
   */
  protected boolean isOsDependent() {

    return true;
  }

  /**
   * Checks if an OS URL file name was missing in {@link UrlVersion}
   *
   * @param urlVersion the {@link UrlVersion} to check
   * @return {@code true} if an OS type was missing, {@code false} if not.
   */
  public boolean isMissingOs(UrlVersion urlVersion) {

    Set<String> childNames = urlVersion.getChildNames();
    Set<String> osTypes = getUrlFilenames();
    // invert result of containsAll to avoid negative condition
    return !childNames.containsAll(osTypes);
  }

  /**
   * Updates the tool's versions in the URL repository.
   *
   * @param urlRepository the {@link UrlRepository} to update
   */
  @Override
  public void update(UrlRepository urlRepository) {

    UrlTool tool = urlRepository.getOrCreateChild(getTool());
    for (String edition : getEditions()) {
      UrlEdition urlEdition = tool.getOrCreateChild(edition);
      setUrlUpdaterReport(new UrlUpdaterReport(tool.getName(), urlEdition.getName()));
      updateExistingVersions(urlEdition);
      Set<String> versions = getVersions();
      String toolWithEdition = getToolWithEdition(edition);
      logger.info("For tool {} we found the following versions : {}", toolWithEdition, versions);

      for (String version : versions) {

        if (isTimeoutExpired()) {
          break;
        }

        UrlVersion urlVersion = urlEdition.getChild(version);
        if (urlVersion == null || isMissingOs(urlVersion)) {
          try {
            urlVersion = urlEdition.getOrCreateChild(version);
            addVersion(urlVersion);
            urlVersion.save();
            getUrlUpdaterReport().incrementAddVersionSuccess();
            logger.info("For tool {} we add version {}.", toolWithEdition, version);
          } catch (Exception e) {
            logger.error("For tool {} we failed to add version {}.", toolWithEdition, version, e);
            getUrlUpdaterReport().incrementAddVersionFailure();
          }
        }
      }
      getUrlFinalReport().addUrlUpdaterReport(getUrlUpdaterReport());
    }
  }

  /**
   * Update existing versions of the tool in the URL repository.
   *
   * @param edition the {@link UrlEdition} to update
   */
  protected void updateExistingVersions(UrlEdition edition) {

    // since Java collections do not support modification while iterating, we need to create a copy
    String[] existingVersions = edition.getChildNames().toArray(i -> new String[i]);
    for (String version : existingVersions) {
      UrlVersion urlVersion = edition.getChild(version);
      if (urlVersion != null) {
        UrlStatusFile urlStatusFile = urlVersion.getOrCreateStatus();
        StatusJson statusJson = urlStatusFile.getStatusJson();
        if (statusJson.isManual()) {
          logger.info("For tool {} the version {} is set to manual, hence skipping update", getToolWithEdition(edition.getName()),
              version);
        } else {
          updateExistingVersion(edition.getName(), version, urlVersion, statusJson, urlStatusFile);
          if (urlVersion.getChildren().isEmpty()) {
            logger.warn("Finally deleting broken or disappeared version {}", urlVersion.getPath());
            urlVersion.delete();
          } else {
            urlVersion.save();
          }
        }
      }
    }
  }

  private void updateExistingVersion(String edition, String version, UrlVersion urlVersion, StatusJson statusJson,
      UrlStatusFile urlStatusFile) {

    String toolWithEdition = getToolWithEdition(edition);
    Instant now = Instant.now();
    // since Java collections do not support modification while iterating, we need to create a copy
    Collection<UrlFile<?>> urlFiles = new ArrayList<>(urlVersion.getChildren());
    for (UrlFile<?> child : urlFiles) {
      if (child instanceof UrlDownloadFile urlDownloadFile) {
        // since Java collections do not support modification while iterating, we need to create a copy
        Set<String> urls = new HashSet<>(urlDownloadFile.getUrls());
        for (String url : urls) {
          if (shouldVerifyDownloadUrl(url, statusJson, toolWithEdition, now)) {
            HttpResponse<?> response = doCheckDownloadViaHeadRequest(url);
            doUpdateStatusJson(isSuccess(response), response.statusCode(), edition, urlVersion, url, urlDownloadFile, true);
          }
        }
      }
    }
    urlStatusFile.cleanup();
    if (statusJson.getUrls().isEmpty()) {
      urlStatusFile.delete();
    } else {
      urlStatusFile.save();
    }
  }

  private boolean shouldVerifyDownloadUrl(String url, StatusJson statusJson, String toolWithEdition, Instant now) {

    UrlStatus urlStatus = statusJson.getOrCreateUrlStatus(url);
    UrlStatusState success = urlStatus.getSuccess();
    if (success != null) {
      Instant timestamp = success.getTimestamp();
      if (timestamp != null) {
        Integer delta = DateTimeUtil.compareDuration(timestamp, now, VERSION_RECHECK_DELAY);
        if ((delta != null) && (delta.intValue() <= 0)) {
          logger.debug("For tool {} the URL {} has already been checked recently on {}", toolWithEdition, url, timestamp);
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Finds all currently available versions of the {@link UrlEdition tool edition}.
   *
   * @return the {@link Set} with all current versions.
   */
  protected abstract Set<String> getVersions();

  /**
   * @param version the original version (e.g. "v1.0").
   * @return the transformed version (e.g. "1.0") or {@code null} to filter and omit the given version.
   */
  protected String mapVersion(String version) {

    String prefix = getVersionPrefixToRemove();
    if ((prefix != null) && version.startsWith(prefix)) {
      version = version.substring(prefix.length());
    }
    String vLower = version.toLowerCase(Locale.ROOT);
    if (vLower.contains("alpha") || vLower.contains("beta") || vLower.contains("dev") || vLower.contains("snapshot")
        || vLower.contains("preview") || vLower.contains("test") || vLower.contains("tech-preview") //
        || vLower.contains("-pre") || vLower.startsWith("ce-") || vLower.contains("-next") || vLower.contains("-rc")
        // vscode nonsense
        || vLower.startsWith("bad") || vLower.contains("vsda-") || vLower.contains("translation/") || vLower.contains(
        "-insiders")) {
      return null;
    }
    return version;
  }

  /**
   * @return the optional version prefix that has to be removed (e.g. "v").
   */
  protected String getVersionPrefixToRemove() {

    return null;
  }

  /**
   * @param version the version to add (e.g. "1.0").
   * @param versions the {@link Collection} with the versions to collect.
   * @return {@code true} if the version has been added to the collection.
   */
  protected final boolean addVersion(String version, Collection<String> versions) {

    String mappedVersion = getMappedVersion(version);
    if (mappedVersion == null) {
      return false;
    }
    boolean added = versions.add(mappedVersion);
    if (!added) {
      logger.warn("Duplicate version {}", mappedVersion);
    }
    return added;
  }

  /**
   * Higher level method for {@link #mapVersion(String)} with additional logging.
   *
   * @param version the version to {@link #mapVersion(String) map}.
   * @return the mapped version or {@code null} if the version was filtered and shall be ignored.
   * @see #mapVersion(String)
   */
  private String getMappedVersion(String version) {
    String mappedVersion = mapVersion(version);
    if ((mappedVersion == null) || mappedVersion.isBlank()) {
      logger.debug("Filtered version {}", version);
      return null;
    } else if (!version.equals(mappedVersion)) {
      logger.debug("Mapped version {} to {}", version, mappedVersion);
    }
    return mappedVersion;
  }

  /**
   * Updates the version of a given URL version.
   *
   * @param urlVersion the {@link UrlVersion} to be updated
   */
  protected abstract void addVersion(UrlVersion urlVersion);

}
