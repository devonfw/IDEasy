package com.devonfw.tools.ide.url.updater.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test of {@link UrlStatusFile}.
 */
class UrlStatusFileTest extends Assertions {

  private static final String STATUS_JSON = """
      {
        "manual": false,
        "urls": {
          "111553425": {
            "success": {
              "timestamp": "2023-02-21T15:03:09.387386Z"
            },
            "error": {
              "timestamp": "2023-01-01T23:59:59.999999Z",
              "code": 500,
              "message": "core dumped"
            }
          }
        }
      }
      """;

  /**
   * Test that an existing status.json is loaded and its content is deserialized correctly.
   */
  @Test
  void testLoadExistingFile(@TempDir Path tempDir) throws IOException {

    // arrange
    Path path = tempDir.resolve(UrlStatusFile.STATUS_JSON);
    Files.writeString(path, STATUS_JSON);

    // act
    StatusJson statusJson = new UrlStatusFile(path).getStatusJson();

    // assert
    assertThat(statusJson.getUrls()).hasSize(1);
    UrlStatus urlStatus = statusJson.getUrls().values().iterator().next();
    UrlStatusState success = urlStatus.getSuccess();
    assertThat(success.getTimestamp()).isEqualTo(Instant.parse("2023-02-21T15:03:09.387386Z"));
    assertThat(success.getCode()).isNull();
    assertThat(success.getMessage()).isNull();
    UrlStatusState error = urlStatus.getError();
    assertThat(error.getTimestamp()).isEqualTo(Instant.parse("2023-01-01T23:59:59.999999Z"));
    assertThat(error.getCode()).isEqualTo(500);
    assertThat(error.getMessage()).isEqualTo("core dumped");
  }

  /**
   * Test that a missing status.json defaults to an empty {@link StatusJson}.
   */
  @Test
  void testLoadMissingFileDefaultsToEmpty(@TempDir Path tempDir) {

    // arrange
    Path path = tempDir.resolve(UrlStatusFile.STATUS_JSON);

    // act
    StatusJson statusJson = new UrlStatusFile(path).getStatusJson();

    // assert
    assertThat(statusJson.getUrls()).isEmpty();
    assertThat(statusJson.isManual()).isFalse();
  }

  /**
   * Test that {@link UrlStatusFile#save()} does not write a file when nothing was modified.
   */
  @Test
  void testSaveIsNoOpWhenUnmodified(@TempDir Path tempDir) {

    // arrange
    Path path = tempDir.resolve(UrlStatusFile.STATUS_JSON);
    UrlStatusFile statusFile = new UrlStatusFile(path);

    // act
    statusFile.save();

    // assert
    assertThat(path).doesNotExist();
  }

  /**
   * Test that {@link UrlStatusFile#delete()} removes the file from disk.
   */
  @Test
  void testDeleteRemovesFile(@TempDir Path tempDir) throws IOException {

    // arrange
    Path path = tempDir.resolve(UrlStatusFile.STATUS_JSON);
    Files.writeString(path, STATUS_JSON);
    UrlStatusFile statusFile = new UrlStatusFile(path);

    // act
    statusFile.delete();

    // assert
    assertThat(path).doesNotExist();
  }

}
