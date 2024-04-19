package com.devonfw.tools.ide.tool.mvn;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.java.Java;

/**
 * {@link ToolCommandlet} for <a href="https://maven.apache.org/">maven</a>.
 */
public class Mvn extends PluginBasedCommandlet {

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
  public void runTool(ProcessMode processMode, VersionIdentifier toolVersion, String... args) {

    Path settingsSecurityFile = this.context.getIdeHome().resolve("conf/.m2/test.xml");
    if (!Files.exists(settingsSecurityFile)) {
      mvnSettingsSecurity(settingsSecurityFile);
    }

    this.context.warning(getSettingsGitUrl());

    Path mvnSettings = this.context.getConfPath().resolve(".m2/settings_test.xml");

    Path settingsTemplate = this.context.getSettingsPath().resolve("devon/conf/.m2/settings.xml");
    try {
      String content = Files.readString(settingsTemplate);
      this.context.info(content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    String input = this.context.askForInput("please input", "INPUT");
    this.context.info(input);

    this.context.warning(getEncryptedPassword("ciao"));

    this.context.warning(mvnSettings.toString());

    //    ProcessContext pc = this.context.newProcess();
    //
    //    pc.executable("mvn");
    //
    //    pc.addArgs("--help");
    //
    //    ProcessResult processResult = pc.run(ProcessMode.DEFAULT_CAPTURE);
    //
    //    //String line = processResult.getOut().get(0);
    //
    //    this.context.warning(processResult.getOut().toString());

  }

  private String getEncryptedPassword(String variable) {

    String input = this.context.askForInput("Please enter secret value for variable: " + variable);

    ProcessContext pc = this.context.newProcess().executable("mvn");
    pc.addArgs("--encrypt-password", input);

    ProcessResult result = pc.run(ProcessMode.DEFAULT_CAPTURE);

    return result.getOut().toString();
    
  }

  private void mvnSettingsSecurity(Path settingsSecurityFile) {

    SecureRandom secureRandom = new SecureRandom();

    byte[] randomBytes = new byte[20];
    secureRandom.nextBytes(randomBytes);

    String base64String = Base64.getEncoder().encodeToString(randomBytes);

    ProcessContext pc = this.context.newProcess().executable("mvn");
    pc.addArgs("--encrypt-master-password", base64String);

    ProcessResult processResult = pc.run(ProcessMode.DEFAULT_CAPTURE);

    String encryptedMasterPassword = processResult.getOut().toString();

    String settingsSecurityXml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<settingsSecurity>\n" + "  <master>" + encryptedMasterPassword + "</master>\n"
            + "</settingsSecurity>";
    try {
      Files.writeString(settingsSecurityFile, settingsSecurityXml);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    System.out.println(base64String);
  }

  private String getSettingsGitUrl() {

    ProcessContext pc = this.context.newProcess().executable("git");
    pc.addArgs("-C", this.context.getSettingsPath(), "remote", "-v");
    ProcessResult result = pc.run(ProcessMode.DEFAULT_CAPTURE);
    if (result.isSuccessful()) {
      for (String line : result.getOut()) {
        if (line.contains("(fetch)")) {
          // Extract the URL from the line
          return line.split("\\s+")[1];
        }
      }
    }
    return null;
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

    Path mavenPlugin = this.context.getSoftwarePath().resolve(this.tool).resolve("lib/ext/" + plugin.getName() + ".jar");
    this.context.getFileAccess().download(plugin.getUrl(), mavenPlugin);
    if (Files.exists(mavenPlugin)) {
      this.context.success("Successfully added {} to {}", plugin.getName(), mavenPlugin.toString());
    } else {
      this.context.warning("Plugin {} has wrong properties\n" //
          + "Please check the plugin properties file in {}", mavenPlugin.getFileName(), mavenPlugin.toAbsolutePath());
    }
  }
}
