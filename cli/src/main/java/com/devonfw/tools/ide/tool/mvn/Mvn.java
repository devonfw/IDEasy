package com.devonfw.tools.ide.tool.mvn;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.GitContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.variable.VariableSyntax;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;

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

  /**
   * The name of the settings.xml
   */
  public static final String SETTINGS_FILE = "settings.xml";

  private static final String SETTINGS_SECURITY_FILE = "settings-security.xml";

  private static final String DOCUMENTATION_PAGE_CONF = "https://github.com/devonfw/IDEasy/blob/main/documentation/conf.adoc";

  private static final String ERROR_SETTINGS_FILE_MESSAGE =
          "Failed to create settings file at: {}. For further details see:\n" + DOCUMENTATION_PAGE_CONF;

  private static final String ERROR_SETTINGS_SECURITY_FILE_MESSAGE =
          "Failed to create settings security file at: {}. For further details see:\n" + DOCUMENTATION_PAGE_CONF;
  public static final VariableSyntax VARIABLE_SYNTAX = VariableSyntax.SQUARE;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Mvn(IdeContext context) {

    super(context, "mvn", Set.of(Tag.JAVA, Tag.BUILD));
  }

  @Override
  public boolean install(boolean silent) {

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  public void postInstall() {

    // locate templates...
    boolean legacy = false;
    boolean hasMvnTemplates = true;
    Path settingsFolder = this.context.getSettingsPath();
    Path templatesFolder = settingsFolder.resolve(IdeContext.FOLDER_TEMPLATES);
    if (!Files.isDirectory(templatesFolder)) {
      Path templatesFolderLegacy = settingsFolder.resolve(IdeContext.FOLDER_LEGACY_TEMPLATES);
      if (Files.isDirectory(templatesFolderLegacy)) {
        templatesFolder = templatesFolderLegacy;
      } else {
        this.context.warning("No maven templates found. Neither in {} nor in {} - configuration broken", templatesFolder, templatesFolderLegacy);
        hasMvnTemplates = false;
      }
    }
    Path templatesConfFolder = templatesFolder.resolve(IdeContext.FOLDER_CONF);
    Path templatesConfMvnFolder = templatesConfFolder.resolve(MVN_CONFIG_FOLDER);
    if (hasMvnTemplates) {
      if (!Files.isDirectory(templatesConfMvnFolder)) {
        Path templatesConfMvnLegacyFolder = templatesConfFolder.resolve(MVN_CONFIG_LEGACY_FOLDER);
        if (Files.isDirectory(templatesConfMvnLegacyFolder)) {
          templatesConfMvnFolder = templatesConfMvnLegacyFolder;
          legacy = true;
        } else {
          this.context.warning("No maven templates found. Neither in {} nor in {} - configuration broken", templatesConfMvnFolder, templatesConfMvnLegacyFolder);
          hasMvnTemplates = false;
        }
      }
    }
    // locate real config...
    Path confPath = this.context.getConfPath();
    Path mvnConfigPath = confPath.resolve(MVN_CONFIG_FOLDER);
    if (!Files.isDirectory(mvnConfigPath) && legacy) {
      mvnConfigPath = confPath.resolve(MVN_CONFIG_LEGACY_FOLDER);
    }
    this.context.getFileAccess().mkdirs(mvnConfigPath);

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

      ProcessContext pc = this.context.newProcess().executable("mvn");
      pc.addArgs("--encrypt-master-password", base64String);

      ProcessResult result = pc.run(ProcessMode.DEFAULT_CAPTURE);

      String encryptedMasterPassword = result.getOut().toString();

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
        } else if (!gitSettingsUrl.equals(gitContext.DEFAULT_SETTINGS_GIT_URL)) {
          Set<String> variables = findVariables(content);
          for (String variable : variables) {
            String secret = getEncryptedPassword(variable, settingsSecurityFile);
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

  private String getEncryptedPassword(String variable, Path settingsSecurityFile) {

    String input = this.context.askForInput("Please enter secret value for variable " + variable + ":");

    ProcessContext pc = this.context.newProcess().executable("mvn");
    pc.addArgs("--encrypt-password", input);
    ProcessResult result = pc.run(ProcessMode.DEFAULT_CAPTURE);

    String encryptedPassword = result.getOut().toString();
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
  public void installPlugin(PluginDescriptor plugin) {

    Path mavenPlugin = this.getToolPath().resolve("lib/ext/" + plugin.getName() + ".jar");
    this.context.getFileAccess().download(plugin.getUrl(), mavenPlugin);
    if (Files.exists(mavenPlugin)) {
      this.context.success("Successfully added {} to {}", plugin.getName(), mavenPlugin.toString());
    } else {
      this.context.warning("Plugin {} has wrong properties\n" //
              + "Please check the plugin properties file in {}", mavenPlugin.getFileName(), mavenPlugin.toAbsolutePath());
    }
  }
}
