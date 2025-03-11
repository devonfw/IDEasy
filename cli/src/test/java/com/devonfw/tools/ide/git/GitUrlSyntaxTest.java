package com.devonfw.tools.ide.git;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.variable.IdeVariables;


/**
 * Test class for verifying the behavior of Git URL conversions
 */
public class GitUrlSyntaxTest extends AbstractIdeContextTest {

  /**
   * Tests the reading of property PREFERRED_GIT_PROTOCOL from ide.properties is done successfully
   */
  @Test
  public void testPreferredGitProtocolIsReadAsSsh() {
    // Read the PREFERRED_GIT_PROTOCOL from the config file
    IdeTestContext context = newContext("git");
    GitUrlSyntax preferredProtocol = IdeVariables.PREFERRED_GIT_PROTOCOL.get(context);

    // Check if the configuration value is correctly set to SSH
    assertThat(GitUrlSyntax.SSH).isEqualTo(preferredProtocol);
  }

  /**
   * Tests the conversion of a Git URL from HTTPS to SSH protocol.
   * <p>
   * Given a Git URL in HTTPS format, this test ensures that it is correctly converted to the SSH format using the {@link GitUrl#convert(GitUrlSyntax)} method.
   */
  @Test
  public void testConvertGitUrlFromHttpsToSsh() {
    String url = "https://testgitdomain.com/devonfw/IDEasy.git";
    GitUrl gitUrl = new GitUrl(url, null);

    // Use the convert method with GitUrlSyntax enum
    GitUrl convertedGitUrl = gitUrl.convert(GitUrlSyntax.SSH);

    String expectedSshUrl = "git@testgitdomain.com:devonfw/IDEasy.git";
    assertThat(convertedGitUrl.url()).isEqualTo(expectedSshUrl);
  }

  /**
   * Tests the conversion of a Git URL from SSH to HTTPS protocol.
   * <p>
   * Given a Git URL in SSH format, this test ensures that it is correctly converted to the HTTPS format using the {@link GitUrl#convert(GitUrlSyntax)} method.
   */
  @Test
  public void testConvertGitUrlFromSshToHttps() {
    String url = "git@testgitdomain.com:devonfw/IDEasy.git";
    GitUrl gitUrl = new GitUrl(url, null);

    // Use the convert method with GitUrlSyntax enum
    GitUrl convertedGitUrl = gitUrl.convert(GitUrlSyntax.HTTPS);

    String expectedHttpsUrl = "https://testgitdomain.com/devonfw/IDEasy.git";
    assertThat(convertedGitUrl.url()).isEqualTo(expectedHttpsUrl);
  }

  /**
   * Tests that when a Git URL is in HTTPS format and points to the github.com domain, it remains in the original format and is not converted to SSH.
   * <p>
   * This test ensures that the Git URL for github.com stays in HTTPS format, even if SSH is specified as the preferred protocol.
   */
  @Test
  public void testConvertGitUrlGitHubDomain() {
    String url = "https://github.com/devonfw/IDEasy.git";
    GitUrl gitUrl = new GitUrl(url, null);

    // Attempt to convert to SSH, but it should remain in HTTPS format for github.com
    GitUrl convertedGitUrl = gitUrl.convert(GitUrlSyntax.SSH);

    // The URL should remain unchanged in HTTPS format
    assertThat(convertedGitUrl.url()).isEqualTo(url);
  }
}
