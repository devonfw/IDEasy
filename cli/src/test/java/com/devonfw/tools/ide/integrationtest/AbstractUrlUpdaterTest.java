package com.devonfw.tools.ide.integrationtest;

import org.assertj.core.api.Assertions;

import com.devonfw.tools.ide.url.model.file.UrlStatusFile;
import com.devonfw.tools.ide.url.model.file.json.StatusJson;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;

public class AbstractUrlUpdaterTest extends Assertions {

  /**
   * @param urlRepository {@link UrlRepository} to use
   * @param toolName String of tool name
   * @param editionName String of edition name
   * @param versionName String of version name
   * @return {@link StatusJson}
   */
  protected StatusJson retrieveStatusJson(UrlRepository urlRepository, String toolName, String editionName,
      String versionName) {

    UrlTool urlTool = new UrlTool(urlRepository, toolName);
    UrlEdition urlEdition = new UrlEdition(urlTool, editionName);
    UrlVersion urlVersion = new UrlVersion(urlEdition, versionName);
    UrlStatusFile urlStatusFile = new UrlStatusFile(urlVersion);
    urlStatusFile.load(false);
    return urlStatusFile.getStatusJson();
  }

}
