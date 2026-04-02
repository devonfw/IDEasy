package com.devonfw.tools.ide.url.tool.java;

import java.util.Collection;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;

/**
 * URL updater for Java Azul (Zulu) edition.
 */
public class JavaAzulUrlUpdater extends JsonUrlUpdater<JavaAzulJsonObject, JavaAzulJsonVersion> {

  private static final String JAVA_AZUL_BASE_URL = "https://cdn.azul.com";
  private static final String JAVA_AZUL_VERSION_URL = "https://api.azul.com";

  @Override
  public String getTool() {
    return "java";
  }

  @Override
  protected String getEdition() {
    return "azul";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    super.addVersion(urlVersion);
    // https://cdn.azul.com/zulu/bin/zulu11.66.15-ca-jre11.0.20-solaris_x64.zip
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
        + "/metadata/v1/zulu/packages?availability_types=ca&release_status=both&page_size=1000&include_fields=java_package_features,release_status,support_term,os,arch,hw_bitness,abi,java_package_type,javafx_bundled,sha256_hash,cpu_gen,size,archive_type,certifications,lib_c_type,crac_supported&page=6&azul_com=true";
  }

  @Override
  protected Class<JavaAzulJsonObject> getJsonObjectType() {
    return JavaAzulJsonObject.class;
  }

  @Override
  protected Collection<JavaAzulJsonVersion> getVersionItems(JavaAzulJsonObject jsonObject) {
    return jsonObject.versions();
  }

  @Override
  public String getCpeVendor() {
    return "azul";
  }

  @Override
  public String getCpeProduct() {
    return "zulu";
  }
}
