package com.devonfw.tools.ide.merge;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementation of {@link FileMerger} for Text files.
 */
public class TextMerger extends FileMerger {

  /**
   * The constructor.
   *
   * @param context the {@link #context}.
   */
  public TextMerger(IdeContext context) {

    super(context);
  }

  @Override
  public void merge(Path setup, Path update, EnvironmentVariables variables, Path workspace) {

    Path template = update;
    if (!Files.exists(template)) {
      template = setup;
      assert Files.exists(template);
      if (Files.exists(workspace)) {
        return; // setup is only applied for initial setup if workspace file does not yet exist
      }
    }
    StringBuilder inputBuffer = new StringBuilder();
    try (BufferedReader reader = Files.newBufferedReader(template)) {
      String line;
      String resolvedValue;
      while ((line = reader.readLine()) != null) {
        resolvedValue = variables.resolve(line, workspace.getFileName());
        inputBuffer.append(resolvedValue);
        inputBuffer.append('\n');
      }
    } catch (IOException e) {
      throw new IllegalStateException("Could not read text file: " + workspace, e);
    }
    try {
      Files.write(workspace, inputBuffer.toString().getBytes());
    } catch (IOException e) {
      throw new IllegalStateException("Could not write to text file: " + workspace, e);
    }

  }

  @Override
  public void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path update) {

  }
}
