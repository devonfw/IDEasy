package com.devonfw.tools.ide.tool.jasypt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.property.PasswordProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;

/**
 * {@link ToolCommandlet} for <a href="http://www.jasypt.org/">Jasypt</a>, The java library which allows to add basic encryption capabilities with minimum
 * effort.
 */
public class Jasypt extends LocalToolCommandlet {

  /** {@link EnumProperty} for the command (encrypt or decrypt) */
  public final EnumProperty<JasyptCommand> command;

  /** {@link PasswordProperty} for the master password */
  public final PasswordProperty masterPassword;

  /** {@link PasswordProperty} for the secret to be encrypted or decrypted */
  public final PasswordProperty secret;

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
  protected void installDependencies() {

    // TODO create jasypt/jasypt/dependencies.json file in ide-urls and delete this method
    getCommandlet(Java.class).install();
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
        Arrays.asList("-cp", resolveJasyptJarPath().toString(), className, "password=" + this.masterPassword.getValue(),
            "input=" + this.secret.getValue()));

    String jasyptOpts = this.context.getVariables().get("JASYPT_OPTS");
    if (jasyptOpts != null && !jasyptOpts.trim().isEmpty()) {
      String[] jasyptOptions = jasyptOpts.split("\\s+");

      arguments.addAll(Arrays.asList(jasyptOptions));
    }

    String javaHome = System.getenv("JAVA_HOME");
    ProcessBuilder processBuilder = new ProcessBuilder("java", "-version");
    processBuilder.environment().put("JAVA_HOME",javaHome);

    try{
      Process process = processBuilder.start();
      int exitCode = process.waitFor();
      System.out.println("Process ended with exit code: " + exitCode);
    }
    catch(IOException | InterruptedException e){
        e.printStackTrace();
    }
  }

  private Path resolveJasyptJarPath() {

    Path toolPath = this.getToolPath();
    String installedVersion = getInstalledVersion().toString();
    return toolPath.resolve("jasypt-" + installedVersion + ".jar");
  }

  @Override
  public void printHelp(NlsBundle bundle) {

    this.context.info(
        "To get detailed help about the usage of the jasypt CLI tools, see http://www.jasypt.org/cli.html#");
  }
}
