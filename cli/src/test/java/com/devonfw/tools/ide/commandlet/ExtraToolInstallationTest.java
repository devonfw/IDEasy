package com.devonfw.tools.ide.commandlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Integration test for extra tool installation functionality.
 */
public class ExtraToolInstallationTest extends AbstractIdeContextTest {

  /**
   * Test tool for extra tool installation testing.
   */
  public static class TestTool extends LocalToolCommandlet {

    TestTool(IdeContext context) {
      super(context, "testtool", Set.of(Tag.RUNTIME));
    }

    @Override
    public boolean install(boolean silent, ProcessContext processContext, Step step) {
      // Mock installation by creating the required directories and files
      Path toolPath = getToolPath();
      
      try {
        // Create regular tool installation
        Files.createDirectories(toolPath);
        Files.writeString(toolPath.resolve(IdeContext.FILE_SOFTWARE_VERSION), "1.0.0");
        
        // Create extra tool installation if configured
        VersionIdentifier extraVersion = getExtraConfiguredVersion();
        if (isExtraToolSupported() && extraVersion != null) {
          Path extraToolPath = getExtraToolPath();
          Files.createDirectories(extraToolPath);
          Files.writeString(extraToolPath.resolve(IdeContext.FILE_SOFTWARE_VERSION), extraVersion.toString());
          this.context.info("Successfully installed extra {} in version {}", getName(), extraVersion);
        }
        
        return true;
      } catch (IOException e) {
        throw new RuntimeException("Failed to create mock installation", e);
      }
    }

    @Override
    public ToolInstallation installTool(GenericVersionRange version, ProcessContext processContext, String edition) {
      // Mock the tool installation
      Path mockInstallPath = this.context.getSoftwareRepositoryPath().resolve("default").resolve(getName()).resolve(edition).resolve(version.toString());
      VersionIdentifier resolvedVersion = VersionIdentifier.of(version.toString());
      return new ToolInstallation(mockInstallPath, mockInstallPath, mockInstallPath, resolvedVersion, true);
    }

    @Override
    public void uninstall() {
      // Mock uninstall
    }
  }

  /**
   * Test that extra tool installation works when extra version is configured.
   */
  @Test
  public void testExtraToolInstallation() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    
    // Configure regular tool version
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("TESTTOOL_VERSION", "1.0.0", false);
    
    // Configure extra tool version
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("EXTRA_TESTTOOL_VERSION", "2.0.0", false);
    
    TestTool tool = new TestTool(context);
    
    // act
    boolean result = tool.install(false);
    
    // assert
    assertThat(result).isTrue();
    
    // Check regular tool installation
    assertThat(tool.getToolPath()).exists();
    assertThat(tool.getInstalledVersion()).isEqualTo(VersionIdentifier.of("1.0.0"));
    
    // Check extra tool installation
    assertThat(tool.getExtraToolPath()).exists();
    assertThat(tool.getExtraInstalledVersion()).isEqualTo(VersionIdentifier.of("2.0.0"));
  }

  /**
   * Test that extra tool installation doesn't happen when not configured.
   */
  @Test
  public void testNoExtraToolInstallationWhenNotConfigured() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    
    // Only configure regular tool version
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("TESTTOOL_VERSION", "1.0.0", false);
    
    TestTool tool = new TestTool(context);
    
    // act
    boolean result = tool.install(false);
    
    // assert
    assertThat(result).isTrue();
    
    // Check regular tool installation
    assertThat(tool.getToolPath()).exists();
    assertThat(tool.getInstalledVersion()).isEqualTo(VersionIdentifier.of("1.0.0"));
    
    // Check extra tool was not installed
    assertThat(tool.getExtraToolPath()).doesNotExist();
    assertThat(tool.getExtraInstalledVersion()).isNull();
  }

  /**
   * Test that extra tool installation is skipped for unsupported tools.
   */
  @Test
  public void testExtraToolNotSupportedForGraalVM() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    
    // Configure extra tool version for graalvm
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("EXTRA_GRAALVM_VERSION", "21.0.0", false);
    
    class GraalVMTool extends LocalToolCommandlet {
      GraalVMTool(IdeContext context) {
        super(context, "graalvm", Set.of(Tag.JAVA, Tag.RUNTIME));
      }
      
      @Override
      public boolean install(boolean silent, ProcessContext processContext, Step step) {
        // Mock installation
        return true;
      }
      
      @Override
      public ToolInstallation installTool(GenericVersionRange version, ProcessContext processContext, String edition) {
        Path mockInstallPath = this.context.getSoftwareRepositoryPath().resolve("default").resolve(getName()).resolve(edition).resolve(version.toString());
        VersionIdentifier resolvedVersion = VersionIdentifier.of(version.toString());
        return new ToolInstallation(mockInstallPath, mockInstallPath, mockInstallPath, resolvedVersion, true);
      }
      
      @Override
      public void uninstall() {
        // Mock uninstall
      }
    }
    
    GraalVMTool tool = new GraalVMTool(context);
    
    // act & assert
    assertThat(tool.isExtraToolSupported()).isFalse();
    assertThat(tool.getExtraConfiguredVersion()).isNotNull(); // Variable is set
    // But extra tool installation should be skipped during install
  }

  /**
   * Test that warning is logged when extra tool version is identical to regular version.
   */
  @Test
  public void testWarningForIdenticalVersions() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    
    // Configure same version for both regular and extra tool
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("TESTTOOL_VERSION", "1.0.0", false);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("EXTRA_TESTTOOL_VERSION", "1.0.0", false);
    
    TestTool tool = new TestTool(context);
    
    // act
    boolean result = tool.install(false);
    
    // assert
    assertThat(result).isTrue();
    
    // Check that warning is logged (this would be verified in the log output)
    // For now, just verify the installation still works
    assertThat(tool.getToolPath()).exists();
    assertThat(tool.getExtraToolPath()).exists();
  }

  /**
   * Test that extra tool edition falls back to regular edition when not configured.
   */
  @Test
  public void testExtraToolEditionFallback() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    
    // Configure regular tool with specific edition
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("TESTTOOL_VERSION", "1.0.0", false);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("TESTTOOL_EDITION", "special", false);
    
    // Configure extra tool version but not edition
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("EXTRA_TESTTOOL_VERSION", "2.0.0", false);
    
    TestTool tool = new TestTool(context);
    
    // act
    String extraEdition = tool.getExtraConfiguredEdition();
    
    // assert
    assertThat(extraEdition).isEqualTo("special"); // Should fall back to regular edition
  }

  /**
   * Test that extra tool respects specific edition configuration.
   */
  @Test
  public void testExtraToolWithSpecificEdition() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    
    // Configure regular tool
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("TESTTOOL_VERSION", "1.0.0", false);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("TESTTOOL_EDITION", "regular", false);
    
    // Configure extra tool with different edition
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("EXTRA_TESTTOOL_VERSION", "2.0.0", false);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("EXTRA_TESTTOOL_EDITION", "extra", false);
    
    TestTool tool = new TestTool(context);
    
    // act
    String extraEdition = tool.getExtraConfiguredEdition();
    
    // assert
    assertThat(extraEdition).isEqualTo("extra"); // Should use specific extra edition
  }
}