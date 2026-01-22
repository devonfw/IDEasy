package com.devonfw.tools.ide.tool.custom;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link CustomToolRepository}.
 */
class CustomToolRepositoryTest extends Assertions {

  @Test
  void testRepositoryId() {

    // arrange
    String name = "jboss-eap";
    String version = "7.4.5.GA";
    String repositoryUrl = "https://host.domain.tld:port/folder/räpö$itöry+name/ochn%C3%B6n%F6";

    // act
    CustomTool tool = new CustomTool(name, version, false, false, repositoryUrl);

    // assert
    assertThat(tool.name()).isEqualTo(name);
    assertThat(tool.version()).isSameAs(version);
    assertThat(tool.url()).isEqualTo(repositoryUrl);
  }

}
