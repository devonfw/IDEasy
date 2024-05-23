package com.devonfw.tools.ide.tool.jasypt;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.property.PasswordProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * {@link ToolCommandlet} for <a href="http://www.jasypt.org/">Jasypt</a>, The java library which allows to add basic encryption capabilities with minimum
 * effort.
 */
public class Jasypt extends LocalToolCommandlet {

  final EnumProperty<JasyptCommand> command;

  final PasswordProperty masterPassword;

  final PasswordProperty secret;

  private static final String CLASS_NAME_ENCRYPTION = "org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI";

  private static final String CLASS_NAME_DECRYPTION = "org.jasypt.intf.cli.JasyptPBEStringDecryptionCLI";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Jasypt(IdeContext context) {

    super(context, "jasypt", Set.of(Tag.JAVA, Tag.ENCRYPTION));

    this.command = add(new EnumProperty<>("", true, "command", JasyptCommand.class));
    this.masterPassword = add(new PasswordProperty("", true, "masterPassword"));
    this.secret = add(new PasswordProperty("", true, "secret"));
  }

  @Override
  protected void initProperties() {

    // Empty on purpose
  }

  @Override
  public boolean doInstall(boolean silent) {

    getCommandlet(Java.class).install();

    return super.doInstall(silent);
  }

  @Override
  protected boolean isExtract() {

    return false;
  }

  @Override
  public void run() {

    Path toolPath = getToolPath();
    if (!toolPath.toFile().exists()) {
      super.install(true);
    }

    JasyptCommand command = this.command.getValue();
    switch (command) {
      case ENCRYPT:
        runJasypt(CLASS_NAME_ENCRYPTION);
        break;
      case DECRYPT:
        runJasypt(CLASS_NAME_DECRYPTION);
        break;

      default:
    }
  }

  private void runJasypt(String className) {

    List<String> arguments = new ArrayList<>(
        Arrays.asList("-cp", resolveJasyptJarPath().toString(), className, "password=" + this.masterPassword.getValue(), "input=" + this.secret.getValue()));

    String jasyptOpts = this.context.getVariables().get("JASYPT_OPTS");
    if (jasyptOpts != null && !jasyptOpts.trim().isEmpty()) {
      String[] jasyptOptions = jasyptOpts.split("\\s+");

      arguments.addAll(Arrays.asList(jasyptOptions));
    }

    Java java = getCommandlet(Java.class);
    java.runTool(null, arguments.toArray(new String[0]));
  }

  private Path resolveJasyptJarPath() {

    Path toolPath = this.getToolPath();
    String installedVersion = getInstalledVersion().toString();
    return toolPath.resolve("jasypt-" + installedVersion + ".jar");
  }
}
