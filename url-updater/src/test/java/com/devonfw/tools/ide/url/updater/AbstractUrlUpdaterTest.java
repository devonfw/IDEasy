package com.devonfw.tools.ide.url.updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.Assertions;

import com.devonfw.tools.ide.url.model.file.UrlStatusFile;
import com.devonfw.tools.ide.url.model.file.json.StatusJson;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Base class for tests of {@link com.devonfw.tools.ide.url.updater.AbstractUrlUpdater} and its subclasses.
 */
public class AbstractUrlUpdaterTest extends Assertions {

  /** A placeholder body which gets returned by WireMock for tool downloads. */
  public static final String DOWNLOAD_CONTENT = "aBody";

  /** The SHA256 checksum of {@link #DOWNLOAD_CONTENT}. */
  public static final String SHA_256 = "de08da1685e537e887fbbe1eb3278fed38aff9da5d112d96115150e8771a0f30";

  /** The {@link Path} to the integration test data for URL updaters. */
  public static final Path PATH_INTEGRATION_TEST = Path.of("src/test/resources/integrationtest");

  /**
   * Reads the content of the given file and replaces the placeholder "${testbaseurl}" with the actual base URL.
   *
   * @param file the {@link Path} to the file to read.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} providing the base URL.
   * @return the resolved file content.
   */
  protected static String readAndResolve(Path file, WireMockRuntimeInfo wmRuntimeInfo) {

    try {
      String payload = Files.readString(file);
      return payload.replace("${testbaseurl}", wmRuntimeInfo.getHttpBaseUrl());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * @param urlRepository {@link UrlRepository} to use
   * @param tool the tool.
   * @param edition the edition.
   * @param version the version.
   * @return the {@link StatusJson}.
   */
  protected StatusJson retrieveStatusJson(UrlRepository urlRepository, String tool, String edition, String version) {

    UrlTool urlTool = new UrlTool(urlRepository, tool);
    UrlEdition urlEdition = new UrlEdition(urlTool, edition);
    UrlVersion urlVersion = new UrlVersion(urlEdition, version);
    UrlStatusFile urlStatusFile = new UrlStatusFile(urlVersion);
    urlStatusFile.load(false);
    return urlStatusFile.getStatusJson();
  }

  protected void assertUrlVersionAgnostic(Path urlVersionFolder) {

    assertUrlVersion(urlVersionFolder, OsAndArchitecture.AGNOSTIC.getFilenames());
  }

  protected void assertUrlVersionOs(Path urlVersionFolder) {

    assertUrlVersion(urlVersionFolder, OsAndArchitecture.OS.getFilenames());
  }

  protected void assertUrlVersionOsX64(Path urlVersionFolder) {

    assertUrlVersion(urlVersionFolder, OsAndArchitecture.OS_X64.getFilenames());
  }

  protected void assertUrlVersionOsX64MacArm(Path urlVersionFolder) {

    assertUrlVersion(urlVersionFolder, OsAndArchitecture.OS_X64_MAC_ARM.getFilenames());
  }

  protected void assertUrlVersionOsDefArch(Path urlVersionFolder) {

    assertUrlVersion(urlVersionFolder, OsAndArchitecture.OS_DEF_ARCH.getFilenames());
  }

  protected void assertUrlVersionOsArch(Path urlVersionFolder) {

    assertUrlVersion(urlVersionFolder, OsAndArchitecture.OS_ARCH.getFilenames());
  }

  protected void assertUrlVersion(Path urlVersionFolder, List<String> platforms) {

    assertThat(urlVersionFolder.resolve("status.json")).exists();
    for (String platform : platforms) {
      assertUrlVersionFile(urlVersionFolder, platform);
    }
  }

  protected void assertUrlVersionFile(Path urlVersionFolder, String platform) {

    String filename = "urls";
    if (!platform.isEmpty()) {
      filename = platform + ".urls";
    }
    assertThat(urlVersionFolder.resolve(filename)).exists().content().contains("http://localhost:");
    assertThat(urlVersionFolder.resolve(filename + ".sha256")).exists().hasContent(SHA_256);
  }
}
