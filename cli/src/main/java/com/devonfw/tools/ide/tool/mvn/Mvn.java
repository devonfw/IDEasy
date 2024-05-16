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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ToolCommandlet} for <a href="https://maven.apache.org/">maven</a>.
 */
public class Mvn extends PluginBasedCommandlet {

  /** The name of the mvn folder */
  public static final String MVN_CONFIG_FOLDER = "mvn";

  /** The name of the settings.xml */
  public static final String SETTINGS_FILE = "settings.xml";

  private static final String M2_CONFIG_FOLDER = ".m2";

  private static final String SETTINGS_SECURITY_FILE = "settings-security.xml";

  private static final String DOCUMENTATION_PAGE_CONF = "https://github.com/devonfw/IDEasy/blob/main/documentation/conf.adoc";

  private static final String ERROR_SETTINGS_FILE_MESSAGE =
      "Failed to create settings file at: {}. The settings file will not be set. For further details see: " + DOCUMENTATION_PAGE_CONF;

  private static final String ERROR_SETTINGS_SECURITY_FILE_MESSAGE =
      "Failed to create settings security file at: {}. The settings security file will not be set. For further details see: " + DOCUMENTATION_PAGE_CONF;

  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\[(\\w+)]");

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

    Path settingsSecurityFile = this.context.getConfPath().resolve(MVN_CONFIG_FOLDER).resolve(SETTINGS_SECURITY_FILE);
    if (!Files.exists(settingsSecurityFile)) {
      Step step = this.context.newStep("Create mvn settings security file at " + settingsSecurityFile);
      try {
        createSettingsSecurityFile(settingsSecurityFile);
        step.success();
      } finally {
        step.end();
      }
    }

    Path settingsFile = this.context.getConfPath().resolve(MVN_CONFIG_FOLDER).resolve(SETTINGS_FILE);
    if (!Files.exists(settingsFile) && Files.exists(settingsSecurityFile)) {
      Step step = this.context.newStep("Create mvn settings file at " + settingsFile);
      try {
        createSettingsFile(settingsFile, settingsSecurityFile);
        step.success();
      } finally {
        step.end();
      }
    }
  }

  private void createSettingsSecurityFile(Path settingsSecurityFile) {

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
      Files.createDirectories(settingsSecurityFile.getParent());
      Files.writeString(settingsSecurityFile, settingsSecurityXml);
    } catch (IOException e) {
      this.context.warning(ERROR_SETTINGS_SECURITY_FILE_MESSAGE, settingsSecurityFile);
    }
  }

  private void createSettingsFile(Path settingsFile, Path settingsSecurityFile) {

    Path settingsTemplate = this.context.getSettingsPath().resolve(IdeContext.FOLDER_TEMPLATES).resolve(IdeContext.FOLDER_CONF).resolve(MVN_CONFIG_FOLDER)
        .resolve(SETTINGS_FILE);
    if (!Files.exists(settingsTemplate)) {
      settingsTemplate = this.context.getSettingsPath().resolve(IdeContext.FOLDER_LEGACY_TEMPLATES).resolve(IdeContext.FOLDER_CONF).resolve(M2_CONFIG_FOLDER)
          .resolve(SETTINGS_FILE);
      if (!Files.exists(settingsTemplate)) {
        this.context.warning(SETTINGS_FILE + " template not found in settings folder. " + ERROR_SETTINGS_FILE_MESSAGE, settingsFile);
        return;
      }
    }
    try {
      String content = Files.readString(settingsTemplate);

      GitContext gitContext = this.context.getGitContext();

      String gitSettingsUrl = gitContext.retrieveGitUrl(this.context.getSettingsPath());

      if (gitSettingsUrl == null) {
        this.context.warning(ERROR_SETTINGS_FILE_MESSAGE, settingsFile);
        return;
      }

      if (!gitSettingsUrl.equals(gitContext.DEFAULT_SETTINGS_GIT_URL)) {
        Set<String> variables = findVariables(content);
        for (String variable : variables) {
          String secret = getEncryptedPassword(variable, settingsSecurityFile);
          content = content.replace("$[" + variable + "]", secret);
        }
      }
      Files.createDirectories(settingsFile.getParent());
      Files.writeString(settingsFile, content);
    } catch (IOException e) {
      this.context.warning(ERROR_SETTINGS_FILE_MESSAGE, settingsFile);
    }
  }

  private String getEncryptedPassword(String variable, Path settingsSecurityFile) {

    String input = this.context.askForInput("Please enter secret value for variable: " + variable);

    ProcessContext pc = this.context.newProcess().executable("mvn");
    pc.addArgs("--encrypt-password", input, "-Dsettings.security=" + settingsSecurityFile);

    ProcessResult result = pc.run(ProcessMode.DEFAULT_CAPTURE);

    String encryptedPassword = result.getOut().toString();
    this.context.info("Encrypted as " + encryptedPassword);

    return encryptedPassword;
  }

  private Set<String> findVariables(String content) {

    Set<String> variables = new LinkedHashSet<>();
    Matcher matcher = VARIABLE_PATTERN.matcher(content);
    while (matcher.find()) {
      String variableName = matcher.group(1);
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
