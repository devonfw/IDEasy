package com.devonfw.tools.ide.tool.python;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link Python}.
 */
public class PythonTest extends AbstractIdeContextTest {

  /**
   * Test that Python tool can be created and configured.
   */
  @Test
  public void testPython() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    Python python = new Python(context);
    
    // act & assert
    assertThat(python.getName()).isEqualTo("python");
    assertThat(python.getToolPath()).isNotNull();
  }

  /**
   * Test that postInstall creates proper symlinks on Windows.
   */
  @Test
  public void testPostInstallCreatesSymlinksOnWindows() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    SystemInfo systemInfo = SystemInfoMock.of("windows");
    context.setSystemInfo(systemInfo);
    Python python = new Python(context);
    
    // Create mock Python installation structure
    Path toolPath = python.getToolPath();
    context.getFileAccess().mkdirs(toolPath);
    
    // Create Scripts folder with pip.exe
    Path scriptsPath = toolPath.resolve("Scripts");
    context.getFileAccess().mkdirs(scriptsPath);
    
    // Create a mock pip.exe file
    Path pipExe = scriptsPath.resolve("pip.exe");
    context.getFileAccess().writeFileContent("mock pip executable", pipExe);
    
    // Create main python.exe 
    Path pythonExe = toolPath.resolve("python.exe");
    context.getFileAccess().writeFileContent("mock python executable", pythonExe);
    
    // act
    python.postInstall();
    
    // assert
    // On Windows, bin should be a symlink to Scripts
    Path binPath = toolPath.resolve("bin");
    assertThat(binPath).exists();
    
    // Scripts/python.exe should be a symlink to ../python.exe
    Path scriptsPythonExe = scriptsPath.resolve("python.exe");
    assertThat(scriptsPythonExe).exists();
  }

  /**
   * Test that postInstall does nothing on non-Windows systems.
   */
  @Test
  public void testPostInstallDoesNothingOnNonWindows() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    SystemInfo systemInfo = SystemInfoMock.of("linux");
    context.setSystemInfo(systemInfo);
    Python python = new Python(context);
    
    // Create mock Python installation structure
    Path toolPath = python.getToolPath();
    context.getFileAccess().mkdirs(toolPath);
    
    // Create Scripts folder with pip.exe
    Path scriptsPath = toolPath.resolve("Scripts");
    context.getFileAccess().mkdirs(scriptsPath);
    
    // Create a mock pip.exe file
    Path pipExe = scriptsPath.resolve("pip.exe");
    context.getFileAccess().writeFileContent("mock pip executable", pipExe);
    
    // Create main python.exe 
    Path pythonExe = toolPath.resolve("python.exe");
    context.getFileAccess().writeFileContent("mock python executable", pythonExe);
    
    // act
    python.postInstall();
    
    // assert
    // On Linux/Mac, bin should NOT be created
    Path binPath = toolPath.resolve("bin");
    assertThat(binPath).doesNotExist();
  }
}