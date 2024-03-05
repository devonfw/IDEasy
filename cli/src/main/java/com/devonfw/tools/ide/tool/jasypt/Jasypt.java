package com.devonfw.tools.ide.tool.jasypt;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.mvn.Mvn;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * {@link ToolCommandlet} for <a href="http://www.jasypt.org/">Jasypt</a>, The java library which allows to add basic
 * encryption capabilities with minimum effort.
 */
public class Jasypt extends LocalToolCommandlet {

  private static final String USAGE_INFO = """
      Jasypt encryption tool
      Usage:
       encrypt  <masterpassword>  <secret>             encrypt a secret with a master-password
       decrypt  <masterpassword>  <secret>             decrypt an encrypted secret with a master-password
      """;

  public static final String CLASS_NAME_ENCRYPTION = "org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI";

  public static final String CLASS_NAME_DECRYPTION = "org.jasypt.intf.cli.JasyptPBEStringDecryptionCLI";

  public static final String ALGORITHM = "algorithm=PBEWITHHMACSHA512ANDAES_256";

  public static final String GENERATOR_CLASS_NAME = "ivGeneratorClassName=org.jasypt.iv.RandomIvGenerator";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Jasypt(IdeContext context) {

    super(context, "jasypt", Set.of(Tag.JAVA));
  }

  @Override
  public void run() {

    String[] args = this.arguments.asArray();

    if (args.length == 0) {
      this.context.info(USAGE_INFO);
    } else if (args.length == 3 && args[0].equals("encrypt")) {
      doJasypt(CLASS_NAME_ENCRYPTION, args);
    } else if (args.length == 3 && args[0].equals("decrypt")) {
      doJasypt(CLASS_NAME_DECRYPTION, args);
    } else {
      this.context.warning("Unknown arguments");
      this.context.info(USAGE_INFO);
    }
  }

  @Override
  public boolean doInstall(boolean silent) {

    getCommandlet(Java.class).install();
   /* if (Files.notExists(resolveJasyptJarPath())) {
      installJasyptArtifact();
    }*/
    return super.doInstall(silent);
  }

  @Override
  public void postInstall() {

    super.postInstall();

    if (Files.notExists(resolveJasyptJarPath())) {
      installJasyptArtifact();
      this.context.debug("installing jasypt artifact at:");
    }
    this.context.debug("post install ");
  }

  private void installJasyptArtifact() {

    Mvn mvn = getCommandlet(Mvn.class);
    mvn.install();
    mvn.runTool(null, "org.apache.maven.plugins:maven-dependency-plugin:3.1.2:get",
        "-Dartifact=org.jasypt:jasypt:" + getInstalledVersion().toString());
  }

  private void doJasypt(String className, String[] args) {

    Java java = getCommandlet(Java.class);
    java.runTool(null, "-cp", resolveJasyptJarPath().toString(), className, ALGORITHM, GENERATOR_CLASS_NAME,
        "password=" + args[1], "input=" + args[2]);
  }

  private Path resolveJasyptJarPath() {

    Path m2Repo = context.getVariables().getPath("M2_REPO");
    String installedVersion = getInstalledVersion().toString();
    return m2Repo.resolve("org").resolve("jasypt").resolve("jasypt").resolve(installedVersion)
        .resolve("jasypt-" + installedVersion + ".jar");
  }

}


