package com.devonfw.tools.ide.url.tool.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;

/**
 * Tests JSON response parsing in {@link JavaAzulUrlUpdater}.
 */
class JavaAzulUrlUpdaterJsonParsingTest {

  @Test
  void shouldParseRootArrayResponse() throws Exception {

    // given
    String response = "[{\"java_version\":[17,0,14],\"openjdk_build_number\":7}]";
    TestableJavaAzulUrlUpdater updater = new TestableJavaAzulUrlUpdater();

    // when
    JavaAzulJsonObject jsonObject = updater.parse(response);

    // then
    assertThat(jsonObject.versions()).hasSize(1);
    assertThat(jsonObject.versions().getFirst().version()).isEqualTo("17.0.14_7");
  }

  @Test
  void shouldParseLegacyRootObjectResponse() throws Exception {

    // given
    String response = "{\"versions\":[{\"java_version\":[21,0,3],\"openjdk_build_number\":9}]}";
    TestableJavaAzulUrlUpdater updater = new TestableJavaAzulUrlUpdater();

    // when
    JavaAzulJsonObject jsonObject = updater.parse(response);

    // then
    assertThat(jsonObject.versions()).hasSize(1);
    assertThat(jsonObject.versions().getFirst().version()).isEqualTo("21.0.3_9");
  }

  @Test
  void shouldFormatFourPartVersion() throws Exception {

    // given
    String response = "[{\"java_version\":[17,0,14,1],\"openjdk_build_number\":1}]";
    TestableJavaAzulUrlUpdater updater = new TestableJavaAzulUrlUpdater();

    // when
    JavaAzulJsonObject jsonObject = updater.parse(response);

    // then
    assertThat(jsonObject.versions()).hasSize(1);
    assertThat(jsonObject.versions().getFirst().version()).isEqualTo("17.0.14.1_1");
  }

  @Test
  void shouldNotThrowForJsonItemAddVersionPath(@TempDir Path tempDir) throws Exception {

    // given
    String response = "[{\"java_version\":[17,0,4],\"openjdk_build_number\":8}]";
    TestableJavaAzulUrlUpdater updater = new TestableJavaAzulUrlUpdater();
    JavaAzulJsonVersion jsonVersion = updater.parse(response).versions().getFirst();
    UrlRepository repository = UrlRepository.load(tempDir);
    UrlVersion urlVersion = repository.getOrCreateChild("java").getOrCreateChild("azul").getOrCreateChild("17.0.4_8");

    // then
    assertThatNoException().isThrownBy(() -> updater.add(urlVersion, jsonVersion));
  }

  @Test
  void shouldMapLinuxX86ToLinuxX64AndUseChecksum(@TempDir Path tempDir) throws Exception {

    // given
    String response =
        "[{\"java_version\":[17,0,14],\"openjdk_build_number\":7,\"download_url\":\"https://cdn.azul.com/zulu/bin/zulu17.56.15-ca-crac-jdk17.0.14-linux_x64.tar.gz\",\"os\":\"linux\",\"arch\":\"x86\",\"sha256_hash\":\"abc123\"}]";
    TestableJavaAzulUrlUpdater updater = new TestableJavaAzulUrlUpdater();
    JavaAzulJsonVersion jsonVersion = updater.parse(response).versions().getFirst();
    UrlVersion urlVersion = UrlRepository.load(tempDir).getOrCreateChild("java").getOrCreateChild("azul").getOrCreateChild("17.0.14_7");

    // when
    updater.add(urlVersion, jsonVersion);

    // then
    assertThat(updater.addedUrls).contains(
        "https://cdn.azul.com/zulu/bin/zulu17.56.15-ca-crac-jdk17.0.14-win_x64.zip",
        "https://cdn.azul.com/zulu/bin/zulu17.56.15-ca-crac-jdk17.0.14-win_x64.msi",
        "https://cdn.azul.com/zulu/bin/zulu17.56.15-ca-crac-jdk17.0.14-linux_x64.tar.gz",
        "https://cdn.azul.com/zulu/bin/zulu17.56.15-ca-crac-jdk17.0.14-linux_aarch64.tar.gz",
        "https://cdn.azul.com/zulu/bin/zulu17.56.15-ca-crac-jdk17.0.14-macosx_x64.tar.gz",
        "https://cdn.azul.com/zulu/bin/zulu17.56.15-ca-crac-jdk17.0.14-macosx_aarch64.tar.gz");
    assertThat(updater.lastOs).isEqualTo(OperatingSystem.MAC);
    assertThat(updater.lastArchitecture).isEqualTo(SystemArchitecture.ARM64);
  }

  @Test
  void shouldSkipNonMatchingPackagePattern(@TempDir Path tempDir) throws Exception {

    // given
    String response =
        "[{\"java_version\":[17,0,14],\"openjdk_build_number\":7,\"download_url\":\"https://cdn.azul.com/zulu/bin/zulu17.56.15-ca-crac-jdk17.0.14-linux_ppc64.tar.gz\",\"os\":\"linux\",\"arch\":\"ppc64\"}]";
    TestableJavaAzulUrlUpdater updater = new TestableJavaAzulUrlUpdater();
    JavaAzulJsonVersion jsonVersion = updater.parse(response).versions().getFirst();
    UrlVersion urlVersion = UrlRepository.load(tempDir).getOrCreateChild("java").getOrCreateChild("azul").getOrCreateChild("17.0.14_7");

    // when
    updater.add(urlVersion, jsonVersion);

    // then
    assertThat(updater.addInvoked).isFalse();
  }

  private static class TestableJavaAzulUrlUpdater extends JavaAzulUrlUpdater {

    private boolean addInvoked;
    private final List<String> addedUrls = new ArrayList<>();
    private OperatingSystem lastOs;
    private SystemArchitecture lastArchitecture;

    private JavaAzulJsonObject parse(String response) throws Exception {

      return getJsonObjectFromResponse(response, "azul");
    }

    private void add(UrlVersion urlVersion, JavaAzulJsonVersion jsonVersion) {

      addVersion(urlVersion, jsonVersion);
    }

    @Override
    protected boolean doAddVersion(UrlVersion urlVersion, String url, OperatingSystem os, SystemArchitecture architecture) {

      this.addInvoked = true;
      this.addedUrls.add(url);
      this.lastOs = os;
      this.lastArchitecture = architecture;
      return true;
    }
  }

}







