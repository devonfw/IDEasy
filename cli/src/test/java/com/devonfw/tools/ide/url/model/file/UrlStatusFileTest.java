package com.devonfw.tools.ide.url.model.file;

import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.url.model.AbstractUrlModelTest;
import com.devonfw.tools.ide.url.model.file.json.StatusJson;
import com.devonfw.tools.ide.url.model.file.json.UrlStatus;
import com.devonfw.tools.ide.url.model.file.json.UrlStatusState;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;

/**
 * Test of {@link UrlStatusFile}.
 */
public class UrlStatusFileTest extends AbstractUrlModelTest {

  /**
   * Test of {@link UrlStatusFile#getStatusJson()}.
   */
  @Test
  public void testReadJson() {

    // given
    UrlRepository repo = newRepo();
    UrlTool tool = repo.getChild("docker");
    UrlEdition edition = tool.getChild("rancher");
    UrlVersion version = edition.getChild("1.6.2");
    // when
    UrlStatusFile status = version.getOrCreateStatus();
    StatusJson statusJson = status.getStatusJson();
    // then
    Assertions.assertThat(statusJson.getUrls()).hasSize(1);
    UrlStatus urlStatus = statusJson.getUrls().values().iterator().next();
    UrlStatusState success = urlStatus.getSuccess();
    Assertions.assertThat(success.getTimestamp()).isEqualTo(Instant.parse("2023-02-21T15:03:09.387386Z"));
    Assertions.assertThat(success.getCode()).isNull();
    Assertions.assertThat(success.getMessage()).isNull();
    UrlStatusState error = urlStatus.getError();
    Assertions.assertThat(error.getTimestamp()).isEqualTo(Instant.parse("2023-01-01T23:59:59.999999Z"));
    Assertions.assertThat(error.getCode()).isEqualTo(500);
    Assertions.assertThat(error.getMessage()).isEqualTo("core dumped");
  }

}
