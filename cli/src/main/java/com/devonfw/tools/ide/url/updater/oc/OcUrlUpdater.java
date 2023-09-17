package com.devonfw.tools.ide.url.updater.oc;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for oc (openshift CLI).
 */
public class OcUrlUpdater extends WebsiteUrlUpdater {
  @Override
  protected String getTool() {

    return "oc";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = "https://mirror.openshift.com/pub/openshift-v4/clients/ocp/${version}/openshift-client-";
    doAddVersion(urlVersion, baseUrl + "windows-${version}.zip", WINDOWS);
    doAddVersion(urlVersion, baseUrl + "linux-${version}.tar.gz", LINUX);
    doAddVersion(urlVersion, baseUrl + "mac-${version}.tar.gz", MAC);

  }

  @Override
  protected String getVersionUrl() {

    return "https://mirror.openshift.com/pub/openshift-v4/clients/ocp/";
  }

  @Override
  protected Pattern getVersionPattern() {

    return Pattern.compile("(\\d\\.\\d\\.\\d*)");
  }
}
