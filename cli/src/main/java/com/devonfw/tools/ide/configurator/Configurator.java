package com.devonfw.tools.ide.configurator;

import java.io.File;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Supplier;

import com.devonfw.tools.ide.configurator.merge.DirectoryMerger;
import com.devonfw.tools.ide.configurator.merge.PropertiesMerger;
import com.devonfw.tools.ide.configurator.resolve.VariableResolver;
import com.devonfw.tools.ide.configurator.resolve.VariableResolverImpl;
import com.devonfw.tools.ide.locking.EclipseWorkspaceLockChecker;
import com.devonfw.tools.ide.logging.Log;

/**
 * Class to create and update workspaces.
 *
 * @author trippl
 */
public class Configurator {

  /**
   * The {@link java.io.File#getName() name} of the {@link java.io.File#isDirectory() folder} with the configuration
   * templates for the initial setup of an workspace.
   */
  public static final String FOLDER_SETUP = "setup";

  /**
   * The {@link java.io.File#getName() name} of the {@link java.io.File#isDirectory() folder} with the configuration
   * templates for the update of an workspace.
   */
  public static final String FOLDER_UPDATE = "update";

  /**
   * The systems file separator character.
   */
  public static final String FILE_SEPARATOR = System.getProperty("file.separator");

  /** The directory where java was executed from. */
  public static final String CURRENT_WORKING_DIRECTORY = cwd();

  private static final String OPTION_VARIABLES = "-v";

  private static final String OPTION_WORKSPACE = "-w";

  private static final String OPTION_TEMPLATES = "-t";

  private static final String OPTION_UPDATE = "-u";

  private static final String OPTION_INVERSE = "-i";

  private static final String OPTION_EXTEND = "-x";

  private final DirectoryMerger merger;

  private VariableResolver resolver;

  private File workspaceFolder;

  private File setupFolder;

  private File updateFolder;

  /**
   * The constructor.
   */
  public Configurator() {

    this.merger = new DirectoryMerger();
  }

  private static String cwd() {

    String cwd = System.getProperty("user.dir");
    if (System.getProperty("os.name").toLowerCase(Locale.US).contains("win")) {
      cwd = cwd.replace("\\", "/");
    }
    return cwd;
  }

  /**
   * Creates or updates the workspace.
   */
  private void createOrUpdateWorkspace() {

    this.merger.merge(this.setupFolder, this.updateFolder, this.resolver, this.workspaceFolder);
  }

  /**
   * Saves changes in the workspace files back into the update files.
   *
   * @param saveNewProperties - specifies if new properties are saved as well.
   */
  private void saveChangesInWorkspace(boolean saveNewProperties) {

    this.merger.inverseMerge(this.workspaceFolder, this.resolver, saveNewProperties, this.updateFolder);
  }

  /**
   * Creates a {@link VariableResolverImpl} with the replacement patterns specified by the file at the
   * replacementPatternsPath and the given regEx to find variables to resolve.
   *
   * @param variablesFile - path to the replacement patterns file.
   * @return the created resolver.
   */
  private VariableResolver createResolver(File variablesFile) {

    Properties variables = PropertiesMerger.loadIfExists(variablesFile);
    putVariable(variables, VariableResolver.VARIABLE_DEVON_IDE_HOME, CURRENT_WORKING_DIRECTORY);
    putVariable(variables, VariableResolver.VARIABLE_WORKSPACE_PATH, this.workspaceFolder.getPath());
    putSystemProperty(variables, "java.home");
    putEnvironmentVariable(variables, "JAVA_HOME", () -> CURRENT_WORKING_DIRECTORY + "/software/java");
    putEnvironmentVariable(variables, "ECLIPSE_HOME", () -> CURRENT_WORKING_DIRECTORY + "/software/eclipse");
    putEnvironmentVariable(variables, "SETTINGS_PATH", () -> CURRENT_WORKING_DIRECTORY + "/settings");
    return new VariableResolverImpl(variables);
  }

  private static void putSystemProperty(Properties properties, String key) {

    putVariable(properties, key, System.getProperty(key));
  }

  private static void putEnvironmentVariable(Properties properties, String key, Supplier<String> fallback) {

    String value = System.getenv(key);
    if ((value == null) || value.isEmpty()) {
      if (fallback != null) {
        value = fallback.get();
      }
    }
    putVariable(properties, key, value);
  }

  private static void putVariable(Properties properties, String key, String value) {

    if ((value != null) && !value.isEmpty()) {
      if (value.startsWith("file:")) {
        value = value.substring(5);
      }
      value = VariableResolverImpl.normalizePath(value);
      properties.put(key, value);
      Log.debug("Variable '" + key + "' = " + value);
    } else {
      Log.info("Variable '" + key + "' is undefined");
    }
  }

  /**
   * @see #main(String[])
   * @param args the command-line arguments.
   * @return the {@link System#exit(int) exit-code}.
   */
  public int run(String... args) {

    logCall(args);
    File variablesFile = null;
    File templatesFolder = null;
    String command = null;
    Args arguments = new Args(args);
    while (arguments.hasNext()) {
      String arg = arguments.next();
      if (OPTION_VARIABLES.equals(arg)) {
        variablesFile = arguments.nextFile(variablesFile);
      } else if (OPTION_WORKSPACE.equals(arg)) {
        this.workspaceFolder = arguments.nextFile(this.workspaceFolder);
      } else if (OPTION_TEMPLATES.equals(arg)) {
        templatesFolder = arguments.nextFile(templatesFolder);
      } else if (OPTION_UPDATE.equals(arg) || OPTION_INVERSE.equals(arg) || OPTION_EXTEND.equals(arg)) {
        if (command != null) {
          if (command.equals(arg)) {
            Log.warn("Duplicate option '" + arg + "'.");
          } else {
            fail("Conflicting commands. Can not do both '" + command + "' and '" + arg + "'!");
            return -1;
          }
        }
        command = arg;
      }
    }
    if (!verifyFolder(this.workspaceFolder, "workspace")) {
      return -1;
    } else if (!verifyFolder(templatesFolder, "templates") || (templatesFolder == null)) {
      return -1;
    } else if (command == null) {
      command = OPTION_UPDATE;
      Log.warn("Missing command option. Using update (" + command + ") as fallback.");
    }
    try {
      if (templatesFolder.getParentFile().getName().equals("eclipse")) {
        File lockfile = new File(this.workspaceFolder, ".metadata/.lock");
        if (EclipseWorkspaceLockChecker.isLocked(lockfile)) {
          System.err.println("Your workspace is locked at " + lockfile);
          return 1;
        }
      }
      this.resolver = createResolver(variablesFile);
      this.setupFolder = new File(templatesFolder, FOLDER_SETUP);
      this.updateFolder = new File(templatesFolder, FOLDER_UPDATE);

      File parentFile = templatesFolder.getParentFile();
      String ide = parentFile.getName();
      if ("workspace".equals(ide)) {
        parentFile = parentFile.getParentFile();
        ide = parentFile.getName();
      }
      String prefix = ide + "-";
      if (OPTION_UPDATE.equals(command)) {
        Log.init(prefix + "update");
        Log.debug("Starting setup/update of workspace...");
        createOrUpdateWorkspace();
      } else if (OPTION_INVERSE.equals(command)) {
        Log.init(prefix + "inverse-merge");
        Log.debug("Merging changes of workspace back to settings ...");
        saveChangesInWorkspace(false);
      } else if (OPTION_EXTEND.equals(command)) {
        Log.init(prefix + "inverse-merge-add");
        Log.debug("Merging changes of workspace back to settings (adding new properties)...");
        saveChangesInWorkspace(true);
      } else {
        throw new IllegalStateException(command);
      }
      return 0;
    } catch (Exception e) {
      Log.err("Configurator failed: " + e.getMessage(), e);
      e.printStackTrace();
      return -1;
    }
  }

  private static boolean verifyFolder(File folder, String name) {

    if (folder == null) {
      fail("No " + name + " folder configured.");
      return false;
    } else if (!folder.isDirectory()) {
      fail("The " + name + " folder " + folder.getAbsolutePath() + " does not exist.");
      return false;
    }
    return true;
  }

  /**
   * Runs the application.
   *
   * @param args the command-line arguments.
   */
  public static void main(String[] args) {

    Configurator configurator = new Configurator();
    int exitCode = configurator.run(args);
    System.exit(exitCode);
  }

  private static void usage() {

    Log.info("USAGE: [-v <variables-file>] -w <workspace-folder> -t <templates-folder> -u|-i");
    Log.info(
        "  -v <variables-file>:   specifies the properties file to use for replacements of variables in templates.");
    Log.info("  -w <workspace-folder>: specifies the folder containing the workspace to manage.");
    Log.info(
        "  -t <templates-folder>: specifies the folder containing the templates to setup and update the workspace.");
    Log.info("  -u:                    operation to create or update the workspace.");
    Log.info(
        "  -i:                    operation to do the inverse logic and map back the workspace changes into the update templates.");
  }

  private static void fail(String message) {

    Log.err(message);
    usage();
  }

  private static void logCall(String[] args) {

    StringBuilder buffer = new StringBuilder();
    buffer.append(Configurator.class.getName());
    for (String arg : args) {
      buffer.append(' ');
      buffer.append(arg);
    }
    Log.debug(buffer.toString());
  }

}
