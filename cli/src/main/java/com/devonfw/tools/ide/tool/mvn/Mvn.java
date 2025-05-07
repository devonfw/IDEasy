package com.devonfw.tools.ide.tool.mvn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.variable.VariableSyntax;

/**
 * {@link ToolCommandlet} for <a href="https://maven.apache.org/">maven</a>.
 */
public class Mvn extends PluginBasedCommandlet {

  /**
   * The name of the mvn folder
   */
  public static final String MVN_CONFIG_FOLDER = "mvn";

  /**
   * The name of the m2 repository
   */
  public static final String MVN_CONFIG_LEGACY_FOLDER = ".m2";

  /** The name of the settings-security.xml */
  public static final String SETTINGS_SECURITY_FILE = "settings-security.xml";

  /**
   * The name of the settings.xml
   */
  public static final String SETTINGS_FILE = "settings.xml";

  private static final String DOCUMENTATION_PAGE_CONF = "https://github.com/devonfw/IDEasy/blob/main/documentation/conf.adoc";

  private static final String ERROR_SETTINGS_FILE_MESSAGE =
      "Failed to create settings file at: {}. For further details see:\n" + DOCUMENTATION_PAGE_CONF;

  private static final String ERROR_SETTINGS_SECURITY_FILE_MESSAGE =
      "Failed to create settings security file at: {}. For further details see:\n" + DOCUMENTATION_PAGE_CONF;

  private static final VariableSyntax VARIABLE_SYNTAX = VariableSyntax.SQUARE;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Mvn(IdeContext context) {

    super(context, "mvn", Set.of(Tag.JAVA, Tag.BUILD));
  }

  @Override
  public void postInstall() {

    // locate templates...
    Path templatesConfMvnFolder = getMavenTemplatesFolder();
    if (templatesConfMvnFolder == null) {
      return;
    }
    // locate real config...
    boolean legacy = templatesConfMvnFolder.getFileName().toString().equals(MVN_CONFIG_LEGACY_FOLDER);
    Path mvnConfigPath = getMavenConfFolder(legacy);

    Path settingsSecurityFile = mvnConfigPath.resolve(SETTINGS_SECURITY_FILE);
    createSettingsSecurityFile(settingsSecurityFile);

    Path settingsFile = mvnConfigPath.resolve(SETTINGS_FILE);
    createSettingsFile(settingsFile, settingsSecurityFile, templatesConfMvnFolder.resolve(SETTINGS_FILE));
  }

  private void createSettingsSecurityFile(Path settingsSecurityFile) {

    if (Files.exists(settingsSecurityFile)) {
      return; // file already exists, nothing to do...
    }
    try (Step step = this.context.newStep("Create mvn settings security file at " + settingsSecurityFile)) {
      SecureRandom secureRandom = new SecureRandom();
      byte[] randomBytes = new byte[20];

      secureRandom.nextBytes(randomBytes);
      String base64String = Base64.getEncoder().encodeToString(randomBytes);

      ProcessResult result = runTool(ProcessMode.DEFAULT_CAPTURE, ProcessErrorHandling.LOG_WARNING, this.context.newProcess(), "--encrypt-master-password",
          base64String);
      IdeSubLogger logger = this.context.level(IdeLogLevel.WARNING);
      String encryptedMasterPassword = result.getSingleOutput(logger);

      String settingsSecurityXml =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<settingsSecurity>\n" + "  <master>" + encryptedMasterPassword + "</master>\n"
              + "</settingsSecurity>";
      try {
        Files.writeString(settingsSecurityFile, settingsSecurityXml);
        step.success();
      } catch (IOException e) {
        step.error(e, ERROR_SETTINGS_SECURITY_FILE_MESSAGE, settingsSecurityFile);
      }
    }
  }

  private void createSettingsFile(Path settingsFile, Path settingsSecurityFile, Path settingsTemplateFile) {

    if (Files.exists(settingsFile) || !Files.exists(settingsSecurityFile)) {
      return;
    }
    if (!Files.exists(settingsTemplateFile)) {
      this.context.warning("Missing maven settings template at {}. ", settingsTemplateFile);
      return;
    }
    try (Step step = this.context.newStep("Create mvn settings file at " + settingsFile)) {
      try {
        String content = Files.readString(settingsTemplateFile);

        GitContext gitContext = this.context.getGitContext();

        String gitSettingsUrl = gitContext.retrieveGitUrl(this.context.getSettingsPath());

        if (gitSettingsUrl == null) {
          this.context.warning("Failed to determine git remote URL for settings folder.");
        } else if (!gitSettingsUrl.equals(GitContext.DEFAULT_SETTINGS_GIT_URL)) {
          Set<String> variables = findVariables(content);
          for (String variable : variables) {
            String secret = getEncryptedPassword(variable);
            content = content.replace(VARIABLE_SYNTAX.create(variable), secret);
          }
        }
        Files.createDirectories(settingsFile.getParent());
        Files.writeString(settingsFile, content);
        step.success();
      } catch (IOException e) {
        step.error(e, ERROR_SETTINGS_FILE_MESSAGE, settingsFile);
      }
    }
  }

  private String getEncryptedPassword(String variable) {

    String input = this.context.askForInput("Please enter secret value for variable " + variable + ":");

    ProcessResult result = runTool(ProcessMode.DEFAULT_CAPTURE, ProcessErrorHandling.LOG_WARNING, this.context.newProcess(), "--encrypt-password", input,
        getSettingsSecurityProperty());

    IdeSubLogger logger = this.context.level(IdeLogLevel.WARNING);
    String encryptedPassword = result.getSingleOutput(logger);
    this.context.info("Encrypted as " + encryptedPassword);

    return encryptedPassword;
  }

  private Set<String> findVariables(String content) {

    Set<String> variables = new LinkedHashSet<>();
    Matcher matcher = VARIABLE_SYNTAX.getPattern().matcher(content);
    while (matcher.find()) {
      String variableName = VARIABLE_SYNTAX.getVariable(matcher);
      variables.add(variableName);
    }
    return variables;
  }

  @Override
  public boolean installPlugin(ToolPluginDescriptor plugin, Step step, ProcessContext pc) {

    Path mavenPlugin = this.getToolPath().resolve("lib/ext/" + plugin.name() + ".jar");
    this.context.getFileAccess().download(plugin.url(), mavenPlugin);

    if (Files.exists(mavenPlugin)) {
      this.context.success("Successfully added {} to {}", plugin.name(), mavenPlugin.toString());
      step.success();
      return true;
    } else {
      step.error("Plugin {} has wrong properties\n" //
          + "Please check the plugin properties file in {}", mavenPlugin.getFileName(), mavenPlugin.toAbsolutePath());
      return false;
    }
  }

  @Override
  public String getToolHelpArguments() {

    return "-h";
  }

  /**
   * @return the {@link Path} to the folder with the maven configuration templates.
   */
  public Path getMavenTemplatesFolder() {

    Path templatesFolder = this.context.getSettingsTemplatePath();
    if (templatesFolder == null) {
      return null;
    }
    Path templatesConfFolder = templatesFolder.resolve(IdeContext.FOLDER_CONF);
    Path templatesConfMvnFolder = templatesConfFolder.resolve(MVN_CONFIG_FOLDER);
    if (!Files.isDirectory(templatesConfMvnFolder)) {
      Path templatesConfMvnLegacyFolder = templatesConfFolder.resolve(MVN_CONFIG_LEGACY_FOLDER);
      if (!Files.isDirectory(templatesConfMvnLegacyFolder)) {
        this.context.warning("No maven templates found neither in {} nor in {} - configuration broken", templatesConfMvnFolder,
            templatesConfMvnLegacyFolder);
        return null;
      }
      templatesConfMvnFolder = templatesConfMvnLegacyFolder;
    }
    return templatesConfMvnFolder;
  }

  /**
   * @param legacy - {@code true} to enforce legacy fallback creation, {@code false} otherwise.
   * @return the {@link Path} to the maven configuration folder (where "settings.xml" can be found).
   */
  public Path getMavenConfFolder(boolean legacy) {

    Path confPath = this.context.getConfPath();
    Path mvnConfigFolder = confPath.resolve(MVN_CONFIG_FOLDER);
    if (!Files.isDirectory(mvnConfigFolder)) {
      Path mvnConfigLegacyFolder = confPath.resolve(Mvn.MVN_CONFIG_LEGACY_FOLDER);
      if (Files.isDirectory(mvnConfigLegacyFolder)) {
        mvnConfigFolder = mvnConfigLegacyFolder;
      } else {
        if (legacy) {
          mvnConfigFolder = mvnConfigLegacyFolder;
        }
        this.context.getFileAccess().mkdirs(mvnConfigFolder);
      }
    }
    return mvnConfigFolder;
  }

  /**
   * @return the maven arguments (MVN_ARGS).
   */
  public String getMavenArgs() {
    Path mavenConfFolder = this.context.getMavenConfigurationFolder();
    Path mvnSettingsFile = mavenConfFolder.resolve(Mvn.SETTINGS_FILE);
    Path settingsSecurityFile = mavenConfFolder.resolve(SETTINGS_SECURITY_FILE);
    if (!Files.exists(mvnSettingsFile) && !Files.exists(settingsSecurityFile)) {
      return null;
    }
    String settingsPath = mvnSettingsFile.toString();
    return "-s " + settingsPath + " " + getSettingsSecurityProperty();
  }

  private String getSettingsSecurityProperty() {
    return "-Dsettings.security=" + this.context.getMavenConfigurationFolder().resolve(SETTINGS_SECURITY_FILE).toString().replace("\\", "\\\\");
  }

  /**
   * @return the {@link Path} to the local maven repository.
   */
  public Path getLocalRepository() {
    return IdeVariables.M2_REPO.get(this.context);
  }

  /**
   * @param artifact the {@link MvnArtifact}.
   */
  public void downloadArtifact(MvnArtifact artifact) {

    this.context.newStep("Download artifact " + artifact).run(() -> {
      runTool("dependency:get", "-Dartifact=" + artifact.getKey());
    });
  }

  /**
   * @param artifact the {@link MvnArtifact}.
   * @return the {@link Path} to the {@link MvnArtifact} that was downloaded if not already present.
   */
  public Path getOrDownloadArtifact(MvnArtifact artifact) {

    Path artifactPath = getLocalRepository().resolve(artifact.getPath());
    if (!Files.exists(artifactPath)) {
      downloadArtifact(artifact);
      assert (Files.exists(artifactPath));
    }
    return artifactPath;
  }
}
