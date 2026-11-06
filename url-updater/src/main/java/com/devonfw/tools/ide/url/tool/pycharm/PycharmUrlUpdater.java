package com.devonfw.tools.ide.url.tool.pycharm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.url.tool.intellij.IntellijJsonObject;
import com.devonfw.tools.ide.url.tool.intellij.IntellijJsonRelease;
import com.devonfw.tools.ide.url.updater.IdeaBasedUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link IdeaBasedUrlUpdater} base class for Pycharm.
 */
public class PycharmUrlUpdater extends IdeaBasedUrlUpdater {

  private static final String JSON_URL = "products?code=PCP%2CPCC&release.type=release";
  private static final VersionIdentifier LAST_SEPARATE_VERSION = VersionIdentifier.of("2025.2");
  private static final List<String> EDITIONS = List.of("professional", "pycharm");
  protected static final ObjectMapper MAPPER = JsonMapping.createWithReflectionSupportForUrlUpdaters();

  /**
   * The Constructor.
   */
  public PycharmUrlUpdater() {
    super();
  }

  /**
   * Package-private constructor used for testing {@link PycharmUrlUpdater}.
   *
   * @param versionBaseUrl mock url used as version base.
   */
  PycharmUrlUpdater(String versionBaseUrl) {
    super(versionBaseUrl);
  }

  @Override
  public String getTool() {

    return "pycharm";
  }

  @Override
  public List<String> getEditions() {
    return EDITIONS;
  }

  @Override
  protected IntellijJsonObject getJsonObjectFromResponse(String response, String edition) throws JsonProcessingException {
    IntellijJsonObject[] jsonObjects = MAPPER.readValue(response, IntellijJsonObject[].class);
    moveUnifiedReleases(jsonObjects);
    return jsonObjects[EDITIONS.indexOf(edition)];
  }

  /**
   * This function moves releases later than 2025.2, which are unified releases of Pycharm, but are still distributed internally as professional editions to the
   * community edition releases in the JSON objects, so that they are correctly recognized as community edition releases by the rest of the code.
   *
   * @param jsonObjects the array of JSON objects parsed from the response, which contains one object for the professional edition and one for the community
   *     edition. The function modifies this array in-place, so that after execution, all unified releases are moved to the community edition JSON object.
   */
  private void moveUnifiedReleases(IntellijJsonObject[] jsonObjects) {

    IntellijJsonObject professional = jsonObjects[EDITIONS.indexOf("professional")];
    IntellijJsonObject community = jsonObjects[EDITIONS.indexOf("pycharm")];
    List<IntellijJsonRelease> movedReleases = new ArrayList<>();
    Iterator<IntellijJsonRelease> iterator = professional.releases().iterator();
    while (iterator.hasNext()) {
      IntellijJsonRelease release = iterator.next();
      if (isUnifiedRelease(release)) {
        movedReleases.add(release);
        iterator.remove();
      }
    }
    community.releases().addAll(0, movedReleases);
  }

  /**
   * This function determines, whether a given Pycharm release is a unified release, meaning that there is no separate community edition available. This is the
   * case for all releases greater than 2025.2.
   *
   * @param release the Pycharm JSON release to check.
   * @return {@code true} if the release is a unified release, {@code false} otherwise.
   */
  private boolean isUnifiedRelease(IntellijJsonRelease release) {

    return VersionIdentifier.of(release.version()).isGreater(LAST_SEPARATE_VERSION);
  }

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + "/" + JSON_URL;
  }

  @Override
  public String getCpeVendor() {
    return "jetbrains";
  }

  @Override
  public String getCpeProduct() {
    return "pycharm";
  }
}
