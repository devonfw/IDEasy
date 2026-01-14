package com.devonfw.tools.ide.tool.npm;

import java.io.StringReader;
import java.io.StringWriter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.json.JsonMapping;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test of {@link NpmJs}.
 */
class NpmJsTest extends Assertions {

  /**
   * Test of {@link NpmJs} constructor and versions property.
   */
  @Test
  void testNpmJs() {
    NpmJsVersions versions = new NpmJsVersions();
    NpmJs obj = new NpmJs(versions);
    assertThat(obj.versions()).isEqualTo(versions);
  }

  @Test
  void testNpmJsMapping() throws Exception {
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
    NpmJsVersions versions = new NpmJsVersions();
    addVersion(versions, "1.0.0");
    addVersion(versions, "2.1.0");
    NpmJs obj = new NpmJs(versions);
    ObjectMapper mapper = JsonMapping.create();
    StringWriter sw = new StringWriter(1024);
    StringReader sr = new StringReader(json);
    // act
    mapper.writerWithDefaultPrettyPrinter().writeValue(sw, obj);
    NpmJs objFromJson = mapper.readValue(sr, NpmJs.class);
    // assert
    assertThat(objFromJson).isEqualTo(obj);
    assertThat(sw.toString()).isEqualTo(json);
  }

  private void addVersion(NpmJsVersions versions, String version) {
    NpmJsDist dist = new NpmJsDist("https://registry.npmjs.org/npm/-/npm-" + version + ".tgz");
    NpmJsVersion npmVersion = new NpmJsVersion(version, dist);
    versions.setDetails(version, npmVersion);
  }
}

