package com.devonfw.tools.ide.tool.kubectl;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.tool.EditionAndVersion;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link KubeCtl}.
 */
class KubeCtlTest extends AbstractIdeContextTest {

  private ProcessContext processContext;

  /**
   * Creates a minimal {@link IdeTestContext} that returns the given output when {@code kubectl version --client} is executed.
   *
   * @param kubectlVersionClientOutput the output lines of {@code kubectl version --client}.
   * @return the {@link IdeTestContext}.
   */
  private IdeTestContext newContextWithKubectlOutput(String... kubectlVersionClientOutput) {

    this.processContext = Mockito.mock(ProcessContext.class);
    Mockito.when(this.processContext.runAndGetOutput("kubectl", "version", "--client")).thenReturn(List.of(kubectlVersionClientOutput));
    return new IdeTestContext(Path.of("/"), null) {
      @Override
      protected ProcessContext createProcessContext() {
        return KubeCtlTest.this.processContext;
      }
    };
  }

  /**
   * Verifies that the installed version of KubeCtl is determined by running {@code kubectl version --client} (and not by delegating to
   * {@link com.devonfw.tools.ide.tool.docker.Docker}). This is the behavior that regressed when the logic was moved to the protected
   * {@code computeInstalledEditionAndVersion()} hook, which {@link com.devonfw.tools.ide.tool.DelegatingToolCommandlet} never invokes.
   */
  @Test
  void testGetInstalledVersionComesFromKubectlVersionClient() {

    // arrange
    IdeTestContext context = newContextWithKubectlOutput("Client Version: v1.29.3", "Kustomize Version: v5.0.1");
    KubeCtl kubeCtl = new KubeCtl(context) {
      @Override
      protected boolean isCommandAvailable(String command) {
        return true;
      }
    };

    // act
    EditionAndVersion editionAndVersion = kubeCtl.getInstalledEditionAndVersion();

    // assert: version is parsed from the kubectl output and edition is the kubectl tool itself, not the Docker delegate
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("kubectl");
    assertThat(editionAndVersion.version()).isEqualTo(VersionIdentifier.of("1.29.3"));
    // the version must have come from invoking "kubectl version --client"
    Mockito.verify(this.processContext).runAndGetOutput("kubectl", "version", "--client");
  }

  /**
   * Verifies that the edition is still reported (the tool name) while the version is {@code null} when the output of
   * {@code kubectl version --client} does not contain a recognizable client version.
   */
  @Test
  void testGetInstalledVersionIsNullWhenNoVersionFoundInOutput() {

    // arrange
    IdeTestContext context = newContextWithKubectlOutput("some unexpected output without version");
    KubeCtl kubeCtl = new KubeCtl(context) {
      @Override
      protected boolean isCommandAvailable(String command) {
        return true;
      }
    };

    // act
    EditionAndVersion editionAndVersion = kubeCtl.getInstalledEditionAndVersion();

    // assert
    assertThat(editionAndVersion).isNotNull();
    assertThat(editionAndVersion.edition()).isEqualTo("kubectl");
    assertThat(editionAndVersion.version()).isNull();
    Mockito.verify(this.processContext).runAndGetOutput("kubectl", "version", "--client");
  }

}
