package com.devonfw.tools.ide.npm;

import java.io.StringReader;
import java.io.StringWriter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.json.JsonPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test of {@link NpmJsonObject}.
 */
class NpmJsonObjectTest extends Assertions {

  /**
   * Test of {@link NpmJsonObject} constructor and versions property.
   */
  @Test
  void testNpmJsonObject() {
    NpmJsonVersions versions = new NpmJsonVersions();
    NpmJsonObject obj = new NpmJsonObject(versions);
    assertThat(obj.versions()).isEqualTo(versions);
  }

  @Test
  void testNpmJsonObjectMapping() throws Exception {
    // arrange
    String json = """
        {
          "versions": {
            "1.0.0": {
              "version": "1.0.0",
              "dist": {
                "tarball": "https://registry.npmjs.org/npm/-/npm-1.0.0.tgz"
              }
            },
            "2.1.0": {
              "version": "2.1.0",
              "dist": {
                "tarball": "https://registry.npmjs.org/npm/-/npm-2.1.0.tgz"
              }
            }
          }
        }""";
    NpmJsonVersions versions = new NpmJsonVersions();
    addVersion(versions, "1.0.0");
    addVersion(versions, "2.1.0");
    NpmJsonObject obj = new NpmJsonObject(versions);
    ObjectMapper mapper = JsonMapping.create();
    StringWriter sw = new StringWriter(1024);
    StringReader sr = new StringReader(json);
    // act
    mapper.writer(new JsonPrettyPrinter()).writeValue(sw, obj);
    NpmJsonObject objFromJson = mapper.readValue(sr, NpmJsonObject.class);
    // assert
    assertThat(objFromJson).isEqualTo(obj);
    assertThat(sw.toString()).isEqualTo(json);
  }

  private void addVersion(NpmJsonVersions versions, String version) {
    NpmJsonDist dist = new NpmJsonDist("https://registry.npmjs.org/npm/-/npm-" + version + ".tgz");
    NpmJsonVersion npmVersion = new NpmJsonVersion(version, dist);
    versions.setDetails(version, npmVersion);
  }
}

