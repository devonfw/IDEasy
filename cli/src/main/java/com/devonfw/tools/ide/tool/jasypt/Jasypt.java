package com.devonfw.tools.ide.tool.jasypt;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.property.PasswordProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

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

    // avoid generic multi-valued arguments
  }

  @Override
  protected boolean isExtract() {

    return false;
  }

  @Override
  public ProcessResult runTool(ProcessMode processMode, ProcessErrorHandling errorHandling, ProcessContext pc, String... args) {

    return runJasypt(determineJasyptMainClass(), pc, processMode);
  }

  private String determineJasyptMainClass() {
    JasyptCommand command = this.command.getValue();
    return switch (command) {
      case ENCRYPT -> CLASS_NAME_ENCRYPTION;
      case DECRYPT -> CLASS_NAME_DECRYPTION;
    };
  }

  private ProcessResult runJasypt(String className, ProcessContext pc, ProcessMode processMode) {

    pc = pc.executable("java").addArgs("-cp", resolveJasyptJarPath().toString(), className, "password=" + this.masterPassword.getValue(),
        "input=" + this.secret.getValue());

    String jasyptOpts = this.context.getVariables().get("JASYPT_OPTS");
    if (jasyptOpts != null) {
      jasyptOpts = jasyptOpts.trim();
      if (!jasyptOpts.isEmpty()) {
        for (String opt : jasyptOpts.split("\\s+")) {
          pc = pc.addArg(opt);
        }
      }
    }

    return pc.run(processMode);
  }

  private Path resolveJasyptJarPath() {

    Path toolPath = this.getToolPath();
    String installedVersion = getInstalledVersion().toString();
    return toolPath.resolve("jasypt-" + installedVersion + ".jar");
  }

}
