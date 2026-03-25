package com.devonfw.tools.ide.url.tool.java;

import java.util.Collection;

import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;

public class JavaAzulUrlUpdater extends JsonUrlUpdater<JavaAzulJsonObject, JavaAzulJsonVersion> {

  private static final String JAVA_AZUL_BASE_URL = "https://cdn.azul.com";
  private static final String JAVA_AZUL_VERSION_URL = "https://api.azul.com";

  @Override
  public String getTool() {
    return "java-azul";
  }

  @Override
  public String mapVersion(String version) {
    return super.mapVersion(version);
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
    return getDownloadBaseUrl()
        + "https://api.azul.com/metadata/v1/zulu/packages?availability_types=ca&release_status=both&page_size=1000&include_fields=java_package_features,release_status,support_term,os,arch,hw_bitness,abi,java_package_type,javafx_bundled,sha256_hash,cpu_gen,size,archive_type,certifications,lib_c_type,crac_supported&page=6&azul_com=true";
  }

  @Override
  protected Class<JavaAzulJsonObject> getJsonObjectType() {
    return JavaAzulJsonObject.class;
  }

  @Override
  protected Collection<JavaAzulJsonVersion> getVersionItems(JavaAzulJsonObject jsonObject) {
    return jsonObject.versions();
  }
}
