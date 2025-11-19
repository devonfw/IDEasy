package com.devonfw.tools.ide.tool;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of extra tool functionality.
 */
public class ExtraToolTest extends AbstractIdeContextTest {

  /**
   * Test tool for extra tool functionality.
   */
  public static class TestTool extends LocalToolCommandlet {
    
    TestTool(IdeContext context) {
      super(context, "testtool", Set.of(Tag.RUNTIME));
    }
  }

  /**
   * Test {@link EnvironmentVariables#getExtraToolVersion(String)}.
   */
  @Test
  public void testGetExtraToolVersion() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("EXTRA_TESTTOOL_VERSION", "1.2.3", false);
    
    // act
    VersionIdentifier version = context.getVariables().getExtraToolVersion("testtool");
    
    // assert
    assertThat(version).isNotNull();
    assertThat(version.toString()).isEqualTo("1.2.3");
  }

  /**
   * Test {@link EnvironmentVariables#getExtraToolVersion(String)} when not set.
   */
  @Test
  public void testGetExtraToolVersionNotSet() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    
    // act
    VersionIdentifier version = context.getVariables().getExtraToolVersion("testtool");
    
    // assert
    assertThat(version).isNull();
  }

  /**
   * Test {@link EnvironmentVariables#getExtraToolEdition(String)}.
   */
  @Test
  public void testGetExtraToolEdition() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("EXTRA_TESTTOOL_EDITION", "testedi", false);
    
    // act
    String edition = context.getVariables().getExtraToolEdition("testtool");
    
    // assert
    assertThat(edition).isEqualTo("testedi");
  }

  /**
   * Test {@link EnvironmentVariables#getExtraToolEdition(String)} when not set.
   */
  @Test
  public void testGetExtraToolEditionNotSet() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    
    // act
    String edition = context.getVariables().getExtraToolEdition("testtool");
    
    // assert
    assertThat(edition).isNull();
  }

  /**
   * Test {@link EnvironmentVariables#getExtraToolVersionVariable(String)}.
   */
  @Test
  public void testGetExtraToolVersionVariable() {
    // act
    String variable = EnvironmentVariables.getExtraToolVersionVariable("testtool");
    
    // assert
    assertThat(variable).isEqualTo("EXTRA_TESTTOOL_VERSION");
  }

  /**
   * Test {@link EnvironmentVariables#getExtraToolEditionVariable(String)}.
   */
  @Test
  public void testGetExtraToolEditionVariable() {
    // act
    String variable = EnvironmentVariables.getExtraToolEditionVariable("testtool");
    
    // assert
    assertThat(variable).isEqualTo("EXTRA_TESTTOOL_EDITION");
  }

  /**
   * Test {@link LocalToolCommandlet#getExtraToolPath()}.
   */
  @Test
  public void testGetExtraToolPath() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    TestTool tool = new TestTool(context);
    
    // act
    assertThat(tool.getExtraToolPath()).isNotNull();
    assertThat(tool.getExtraToolPath().toString()).endsWith("software/extra/testtool");
  }

  /**
   * Test {@link LocalToolCommandlet#getExtraConfiguredVersion()}.
   */
  @Test
  public void testGetExtraConfiguredVersion() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("EXTRA_TESTTOOL_VERSION", "2.0.0", false);
    TestTool tool = new TestTool(context);
    
    // act
    VersionIdentifier version = tool.getExtraConfiguredVersion();
    
    // assert
    assertThat(version).isNotNull();
    assertThat(version.toString()).isEqualTo("2.0.0");
  }

  /**
   * Test {@link LocalToolCommandlet#getExtraConfiguredEdition()}.
   */
  @Test
  public void testGetExtraConfiguredEdition() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("EXTRA_TESTTOOL_EDITION", "special", false);
    TestTool tool = new TestTool(context);
    
    // act
    String edition = tool.getExtraConfiguredEdition();
    
    // assert
    assertThat(edition).isEqualTo("special");
  }

  /**
   * Test {@link LocalToolCommandlet#getExtraConfiguredEdition()} fallback to regular edition.
   */
  @Test
  public void testGetExtraConfiguredEditionFallback() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    context.getVariables().getByType(EnvironmentVariablesType.CONF).set("TESTTOOL_EDITION", "regular", false);
    TestTool tool = new TestTool(context);
    
    // act
    String edition = tool.getExtraConfiguredEdition();
    
    // assert
    assertThat(edition).isEqualTo("regular");
  }

  /**
   * Test {@link LocalToolCommandlet#isExtraToolSupported()} for regular tool.
   */
  @Test
  public void testIsExtraToolSupported() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    TestTool tool = new TestTool(context);
    
    // act & assert
    assertThat(tool.isExtraToolSupported()).isTrue();
  }

  /**
   * Test {@link LocalToolCommandlet#isExtraToolSupported()} for GraalVM.
   */
  @Test
  public void testIsExtraToolSupportedGraalVM() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    
    class GraalVMTool extends LocalToolCommandlet {
      GraalVMTool(IdeContext context) {
        super(context, "graalvm", Set.of(Tag.JAVA, Tag.RUNTIME));
      }
    }
    
    GraalVMTool tool = new GraalVMTool(context);
    
    // act & assert
    assertThat(tool.isExtraToolSupported()).isFalse();
  }

  /**
   * Test {@link LocalToolCommandlet#isExtraToolSupported()} for IDE tools.
   */
  @Test
  public void testIsExtraToolSupportedIDE() {
    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    
    class IDETool extends LocalToolCommandlet {
      IDETool(IdeContext context) {
        super(context, "eclipse", Set.of(Tag.IDE));
      }
    }
    
    IDETool tool = new IDETool(context);
    
    // act & assert
    assertThat(tool.isExtraToolSupported()).isFalse();
  }
}