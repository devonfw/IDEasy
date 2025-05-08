package com.devonfw.tools.security;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.url.model.file.UrlSecurityFile;

public class UrlSecurityFileTest extends Assertions {

  @Test
  public void testReadSecurityFile() {
    //assign
    AbstractIdeContext context;
    UrlSecurityFile securityFile = context.getUrls().getEdition(tool, edition).getSecurityFile();
    //act
    //assert
  }
}
