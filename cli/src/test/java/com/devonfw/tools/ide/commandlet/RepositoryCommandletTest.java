package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test of {@link RepositoryCommandlet}
 */
class RepositoryCommandletTest extends AbstractIdeContextTest {

  @Test
  public void runWithSpecifiedRepository() {

    //arrange
    String path = "workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext("basic", path, true);
    RepositoryCommandlet repositoryCommandlet = context.getCommandletManager().getCommandlet(RepositoryCommandlet.class);
    repositoryCommandlet.repository.setValueAsString("repo", context);
    //act
    repositoryCommandlet.run();
    //assert




  }

}