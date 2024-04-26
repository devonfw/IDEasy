package com.devonfw.tools.ide.tool.mvn;

import com.devonfw.tools.ide.common.Tag;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ToolCommandlet} for <a href="https://maven.apache.org/">maven</a>.
 */
public class Mvn extends PluginBasedCommandlet {

  private static final String IDE_SETTINGS_GIT_URL = "https://github.com/devonfw/ide-settings.git";

  private final Path SETTINGS_SECURITY_FILE = this.context.getIdeHome().resolve("conf/.m2/settings-security.xml");

  private final Path SETTINGS_FILE = this.context.getConfPath().resolve(".m2/settings.xml");

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

    if (!Files.exists(SETTINGS_SECURITY_FILE)) {
      Step step = this.context.newStep("Create mvn settings security file at " + SETTINGS_SECURITY_FILE);
      try {
        createSettingsSecurityFile(SETTINGS_SECURITY_FILE);
        step.success();
      } finally {
        step.end();

      }
    }

    if (!Files.exists(SETTINGS_FILE)) {
      Step step = this.context.newStep("Create mvn settings file at " + SETTINGS_FILE);
      try {
        createSettingsFile(SETTINGS_FILE);
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
      Files.writeString(settingsSecurityFile, settingsSecurityXml);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create file " + settingsSecurityFile, e);
    }
  }

  private void createSettingsFile(Path settingsFile) {

    Path settingsTemplate = this.context.getSettingsPath().resolve("ide/conf/.m2/settings.xml");
    try {
      String content = Files.readString(settingsTemplate);

      if (!getSettingsGitUrl().equals(IDE_SETTINGS_GIT_URL)) {
        List<String> variables = findVariables(content);
        for (String variable : variables) {
          String secret = getEncryptedPassword(variable);
          content = content.replace("$[" + variable + "]", secret);
        }
      }
      Files.writeString(settingsFile, content);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write to file " + settingsFile, e);
    }
  }

  private String getEncryptedPassword(String variable) {

    String input = this.context.askForInput("Please enter secret value for variable: " + variable);

    ProcessContext pc = this.context.newProcess().executable("mvn");
    pc.addArgs("--encrypt-password", input);

    ProcessResult result = pc.run(ProcessMode.DEFAULT_CAPTURE);

    String encryptedPassword = result.getOut().toString();
    this.context.info("Encrypted as " + encryptedPassword);

    return encryptedPassword;
  }

  private List<String> findVariables(String content) {

    List<String> variables = new ArrayList<>();
    Pattern pattern = Pattern.compile("\\$\\[(\\w+)]");
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      variables.add(matcher.group(1));
    }
    return variables;
  }

  private String getSettingsGitUrl() {

    ProcessContext pc = this.context.newProcess().executable("git");
    pc.addArgs("-C", this.context.getSettingsPath(), "remote", "-v");
    ProcessResult result = pc.run(ProcessMode.DEFAULT_CAPTURE);
    for (String line : result.getOut()) {
      if (line.contains("(fetch)")) {
        return line.split("\\s+")[1]; // Extract the URL from the line
      }
    }
    throw new IllegalStateException("Failed to retrieve settings git URL.");
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
