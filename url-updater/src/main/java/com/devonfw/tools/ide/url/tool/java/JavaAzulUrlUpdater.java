package com.devonfw.tools.ide.url.tool.java;

import java.util.Arrays;
import java.util.Collection;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * URL updater for Java Azul (Zulu) edition.
 */
public class JavaAzulUrlUpdater extends JsonUrlUpdater<JavaAzulJsonObject, JavaAzulJsonVersion> {

  private static final String JAVA_AZUL_BASE_URL = "https://cdn.azul.com";
  private static final String JAVA_AZUL_VERSION_URL = "https://api.azul.com";
  private static final ObjectMapper MAPPER = JsonMapping.createWithReflectionSupportForUrlUpdaters();

  @Override
  public String getTool() {
    return "java";
  }

  @Override
  protected String getEdition() {
    return "azul";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion, JavaAzulJsonVersion pkg) {

    String url = pkg.getDownloadUrl();
    if (url == null || url.isBlank()) {
      return;
    }

    int lastDash = url.lastIndexOf('-');
    if (lastDash < 0) {
      return;
    }
    String prefix = url.substring(0, lastDash + 1);
    doAddVersion(urlVersion, prefix + "win_x64.zip", WINDOWS, X64);
    doAddVersion(urlVersion, prefix + "win_x64.msi", WINDOWS, X64);
    doAddVersion(urlVersion, prefix + "linux_x64.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, prefix + "linux_aarch64.tar.gz", LINUX, ARM64);
    doAddVersion(urlVersion, prefix + "macosx_x64.tar.gz", MAC, X64);
    doAddVersion(urlVersion, prefix + "macosx_aarch64.tar.gz", MAC, ARM64);
  }

  @Override
  public String getCpeVendor() {
    return "azul";
  }

  @Override
  public String getCpeProduct() {
    return "zulu";
  }

  @Override
  protected String getDownloadBaseUrl() {
    return JAVA_AZUL_BASE_URL;
  }

  @Override
  protected String getVersionBaseUrl() {
    return JAVA_AZUL_VERSION_URL;
  }

  @Override
  protected String doGetVersionUrl() {
    return getVersionBaseUrl()
        + "/metadata/v1/zulu/packages"
        + "?availability_types=CA"
        + "&java_package_type=jdk"
        + "&release_status=ga"
        + "&latest=true"
        + "&os=linux"
        + "&arch=x86"
        + "&archive_type=tar.gz"
        + "&javafx_bundled=false"
        + "&crac_supported=false"
        + "&page_size=100";
  }

  @Override
  protected Class<JavaAzulJsonObject> getJsonObjectType() {
    return JavaAzulJsonObject.class;
  }

  @Override
  protected JavaAzulJsonObject getJsonObjectFromResponse(String response, String edition) throws JsonProcessingException {

    JsonNode rootNode = MAPPER.readTree(response);
    if (rootNode.isArray()) {
      JavaAzulJsonVersion[] versions = MAPPER.treeToValue(rootNode, JavaAzulJsonVersion[].class);
      return new JavaAzulJsonObject(Arrays.asList(versions));
    }
    return MAPPER.treeToValue(rootNode, JavaAzulJsonObject.class);
  }

  @Override
  protected Collection<JavaAzulJsonVersion> getVersionItems(JavaAzulJsonObject jsonObject) {
    return jsonObject.versions();
  }
}
