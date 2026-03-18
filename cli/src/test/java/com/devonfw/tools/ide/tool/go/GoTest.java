package com.devonfw.tools.ide.tool.go;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfoMock;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;

/**
 * Test of {@link Go}.
 */
class GoTest extends AbstractIdeContextTest {

  private static final String PROJECT_GO = "go";

  private static final String GO_VERSION = "1.22.4";

  @ParameterizedTest
  @ValueSource(strings = { "windows", "linux", "mac" })
  void testGoInstallInvokesBootstrapFromSrcForAllPlatforms(String os) {

    // arrange
    IdeTestContext context = newContext(PROJECT_GO);
    context.setSystemInfo(SystemInfoMock.of(os));
    GoSpy go = new GoSpy(context);

    // act
    go.install();

    // assert
    assertThat(go.bootstrapCalled).isTrue();
    assertThat(go.bootstrapScript).isNotNull();
    assertThat(go.bootstrapScript.getFileName().toString()).isEqualTo("make.bash");
    assertThat(go.bootstrapWorkingDir).isNotNull();
    assertThat(go.bootstrapWorkingDir.getFileName().toString()).isEqualTo("src");
    assertThat(context.getSoftwarePath().resolve("go/.ide.software.version")).exists().hasContent(GO_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed go in version " + GO_VERSION);
  }

  @Test
  void testGoInstallSkipsBootstrapWhenScriptMissing() {

    // arrange
    IdeTestContext context = newContext(PROJECT_GO);
    context.getFileAccess().delete(context.getIdeRoot().resolve("repository/go/go/default/src/make.bash"));
    GoSpy go = new GoSpy(context);

    // act
    go.install();

    // assert
    assertThat(go.bootstrapCalled).isFalse();
    assertThat(context).logAtDebug().hasMessageContaining("No Go bootstrap script found");
  }

  @Test
  void testGoInstallPrefersRootBootstrapScriptOverSrc() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_GO);
    Path rootMakeBash = context.getIdeRoot().resolve("repository/go/go/default/make.bash");
    Files.writeString(rootMakeBash, "#!/usr/bin/env bash\nexit 0\n");
    GoSpy go = new GoSpy(context);

    // act
    go.install();

    // assert
    assertThat(go.bootstrapCalled).isTrue();
    assertThat(go.bootstrapWorkingDir).isNotNull();
    assertThat(go.bootstrapWorkingDir.getFileName().toString()).isEqualTo(GO_VERSION);
    assertThat(go.bootstrapScript).isEqualTo(go.bootstrapWorkingDir.resolve("make.bash"));
  }

  @Test
  void testGoInstallUsesMakeBatOnWindowsWhenNoMakeBash() throws IOException {

    // arrange
    IdeTestContext context = newContext(PROJECT_GO);
    context.setSystemInfo(SystemInfoMock.of("windows"));
    context.getFileAccess().delete(context.getIdeRoot().resolve("repository/go/go/default/src/make.bash"));
    Path makeBat = context.getIdeRoot().resolve("repository/go/go/default/src/make.bat");
    Files.writeString(makeBat, "@echo off\r\nexit /b 0\r\n");
    GoSpy go = new GoSpy(context);

    // act
    go.install();

    // assert
    assertThat(go.bootstrapCalled).isTrue();
    assertThat(go.bootstrapScript).isNotNull();
    assertThat(go.bootstrapScript.getFileName().toString()).isEqualTo("make.bat");
  }

  @Test
  void testRunGoBootstrapScriptUsesBashOnWindows() {

    // arrange
    IdeTestContext context = mock(IdeTestContext.class);
    ProcessContext process = mock(ProcessContext.class);
    when(context.getSystemInfo()).thenReturn(SystemInfoMock.of("windows"));
    when(context.findBashRequired()).thenReturn(Path.of("C:/tools/git/bin/bash.exe"));
    when(context.newProcess()).thenReturn(process);
    when(process.executable(Path.of("C:/tools/git/bin/bash.exe"))).thenReturn(process);
    when(process.directory(Path.of("C:/go/src"))).thenReturn(process);
    when(process.addArgs("./make.bash")).thenReturn(process);
    when(process.run(ProcessMode.DEFAULT)).thenReturn(successResult());
    Go go = new Go(context);

    // act
    go.runGoBootstrapScript(Path.of("C:/go/src/make.bash"), Path.of("C:/go/src"));

    // assert
    verify(process).executable(Path.of("C:/tools/git/bin/bash.exe"));
    verify(process).directory(Path.of("C:/go/src"));
    verify(process).addArgs("./make.bash");
    verify(process).run(ProcessMode.DEFAULT);
  }

  @ParameterizedTest
  @ValueSource(strings = { "linux", "mac" })
  void testRunGoBootstrapScriptRunsScriptDirectlyOnUnix(String os) {

    // arrange
    IdeTestContext context = mock(IdeTestContext.class);
    ProcessContext process = mock(ProcessContext.class);
    Path script = Path.of("/tmp/go/src/make.bash");
    Path workingDir = Path.of("/tmp/go/src");
    when(context.getSystemInfo()).thenReturn(SystemInfoMock.of(os));
    when(context.newProcess()).thenReturn(process);
    when(process.executable(script)).thenReturn(process);
    when(process.directory(workingDir)).thenReturn(process);
    when(process.run(ProcessMode.DEFAULT)).thenReturn(successResult());
    Go go = new Go(context);

    // act
    go.runGoBootstrapScript(script, workingDir);

    // assert
    verify(process).executable(script);
    verify(process).directory(workingDir);
    verify(process, never()).addArg("./make.bash");
    verify(process).run(ProcessMode.DEFAULT);
  }

  private ProcessResult successResult() {

    return new ProcessResultImpl("test", "test", ProcessResult.SUCCESS, true, Collections.emptyList());
  }

  private static class GoSpy extends Go {

    private boolean bootstrapCalled;

    private Path bootstrapScript;

    private Path bootstrapWorkingDir;

    private GoSpy(IdeTestContext context) {

      super(context);
    }

    @Override
    protected void runGoBootstrapScript(Path makeBash, Path workingDir) {

      this.bootstrapCalled = true;
      this.bootstrapScript = makeBash;
      this.bootstrapWorkingDir = workingDir;
    }
  }
}
