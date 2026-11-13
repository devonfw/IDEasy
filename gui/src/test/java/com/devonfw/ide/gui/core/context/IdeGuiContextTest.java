package com.devonfw.ide.gui.core.context;

import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.cli.CliAbortException;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.environment.IdeSystem;
import com.devonfw.tools.ide.environment.IdeSystemTestImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerNone;

/**
 * Tests that GUI questions abort when input is required, preserving default answers and batch behavior.
 */
public class IdeGuiContextTest extends Assertions {

  @TempDir
  Path workingDirectory;

  private IdeGuiContext context;

  private IdeStartContextImpl startContext;

  @BeforeEach
  void setUp() {

    IdeSystemTestImpl testSystem = new IdeSystemTestImpl();
    testSystem.getProperties().setProperty("user.home", this.workingDirectory.toString());
    this.startContext = new IdeStartContextImpl(IdeLogLevel.INFO, IdeLogListenerNone.INSTANCE);
    this.context = new IdeGuiContext(this.startContext, this.workingDirectory, new TaskManager()) {

      private boolean inputRequested;

      @Override
      public IdeSystem getSystem() {

        return testSystem;
      }

      @Override
      protected String readLine() {

        // Bound the regression test even if the implementation starts returning empty input again.
        assertThat(this.inputRequested).as("Unavailable GUI input must not be polled repeatedly").isFalse();
        this.inputRequested = true;
        return super.readLine();
      }
    };
  }

  @Test
  void testQuestionAbortsWithoutRetryingUnavailableInput() {

    assertThatExceptionOfType(CliAbortException.class)
        .isThrownBy(() -> this.context.question(new String[] { "yes", "no" }, "Continue?"));
  }

  @Test
  void testQuestionWithSingleOptionKeepsDefaultAnswer() {

    assertThat(this.context.question(new String[] { "onlyOption" }, "Which option?"))
        .isEqualTo("onlyOption");
  }

  @Test
  void testQuestionInBatchModeAbortsWithoutForce() {

    this.startContext.setBatchMode(true);

    assertThatExceptionOfType(CliAbortException.class)
        .isThrownBy(() -> this.context.question(new String[] { "yes", "no" }, "Continue?"));
  }

  @Test
  void testQuestionInForcedBatchModeKeepsFirstOption() {

    this.startContext.setBatchMode(true);
    this.startContext.setForceMode(true);

    assertThat(this.context.question(new String[] { "yes", "no" }, "Continue?")).isEqualTo("yes");
  }

  @Test
  void testTextInputKeepsDefaultAnswer() {

    assertThat(this.context.askForInput("Enter a value", "defaultValue")).isEqualTo("defaultValue");
  }

  @Test
  void testSecretInputKeepsDefaultAnswer() {

    assertThat(this.context.askForSecret("Enter a secret", "defaultSecret")).isEqualTo("defaultSecret");
  }
}
