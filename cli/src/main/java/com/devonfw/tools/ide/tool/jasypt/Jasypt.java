package com.devonfw.tools.ide.tool.jasypt;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;

import java.nio.file.Path;
import java.util.Scanner;
import java.util.Set;

public class Jasypt extends LocalToolCommandlet {

  public Jasypt(IdeContext context) {

    super(context, "jasypt", Set.of(Tag.JAVA));
  }

  @Override
  public boolean doInstall(boolean silent) {

    getCommandlet(Java.class).install();
    return super.doInstall(silent);
  }

  private void doJasypt(String className, String[] args) {

    Scanner scanner = new Scanner(System.in);
    this.context.info("Enter masterpassword: ");
    String masterpassword = scanner.nextLine();
    this.context.info("Enter secret to encrypt/decrypt: ");
    String secret = scanner.nextLine();
    scanner.close();

    Path m2Repo = context.getVariables().getPath("M2_REPO");
    String installedVersion = getInstalledVersion().toString();
    Path jasyptJar = m2Repo.resolve("org").resolve("jasypt").resolve("jasypt").resolve(installedVersion)
        .resolve("jasypt-" + installedVersion + ".jar");

    String[] commandArgs = { "-cp", jasyptJar.toString(), className, "algorithm=PBEWITHHMACSHA512ANDAES_256",
        "ivGeneratorClassName=org.jasypt.iv.RandomIvGenerator", "password=" + masterpassword, "input=" + secret };

    Java java = getCommandlet(Java.class);

    java.runTool(null, commandArgs);
  }

  @Override
  public void run() {

    String[] args = this.arguments.asArray();

    if (args.length == 0) {
      this.context.info("Jasypt encryption tool");
      this.context.info("Arguments:");
      this.context.info(" encrypt              encrypt a secret with a master-password");
      this.context.info(" decrypt              decrypt an encrypted secret with a master-password");
    } else if (args[0].equals("encrypt")) {
      doJasypt("org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI", args);
    } else if (args[0].equals("decrypt")) {
      doJasypt("org.jasypt.intf.cli.JasyptPBEStringDecryptionCLI", args);
    } else {
      this.context.warning("Unknown argument " + args[0]);
    }
  }
}


