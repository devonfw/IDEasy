package com.devonfw.tools.ide.url.updater.docker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for docker-desktop.
 */
public class DockerDesktopUrlUpdater extends WebsiteUrlUpdater {
  @Override
  protected String getTool() {

    return "docker";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String version = urlVersion.getName().replaceAll("\\.", "");
    // get Code for version
    String body = doGetResponseBodyAsString("https://docs.docker.com/desktop/release-notes/");
    String regex = "<h2 id=\"" + version + "\".*?https://desktop\\.docker\\.com.*?(\\d{5})\\.exe(?!.<h2 id=)";
    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
    Matcher matcher = pattern.matcher(body);
    String code;
    if (matcher.find()) {
      code = matcher.group(1);
      boolean success = doAddVersion(urlVersion,
          "https://desktop.docker.com/win/main/amd64/" + code + "/Docker%20Desktop%20Installer.exe", WINDOWS);
      if (!success) {
        return;
      }
      doAddVersion(urlVersion, "https://desktop.docker.com/mac/main/amd64/" + code + "/Docker.dmg", MAC, X64);
      doAddVersion(urlVersion, "https://desktop.docker.com/mac/main/arm64/" + code + "/Docker.dmg", MAC, ARM64);
    } else {
      // For the latest version, there is no code in the url.
      // TODO but that means that the implementation is wrong as the URL will then change later and is therefore
      // unstable as it is always pointing to the latest version so all newly added versions will always have the same
      // URL pointing to the latest version what is dead wrong...
      // instead we could add a manual version called "latest" with these URLs but we can not provide checksum files...
      boolean success = doAddVersion(urlVersion,
          "https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe", WINDOWS);
      if (!success) {
        return;
      }
      doAddVersion(urlVersion, "https://desktop.docker.com/mac/main/amd64/Docker.dmg", MAC, X64);
      doAddVersion(urlVersion, "https://desktop.docker.com/mac/main/arm64/Docker.dmg", MAC, ARM64);
    }

  }

  @Override
  protected String getVersionUrl() {

    return "https://docs.docker.com/desktop/release-notes/";
  }

  @Override
  protected Pattern getVersionPattern() {

    return Pattern.compile("(4\\.\\d+\\.\\d+)");
  }
}
