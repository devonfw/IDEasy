package com.devonfw.tools.ide.tool.jasypt;

import java.nio.file.Path;
import java.util.Scanner;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;
import java.nio.file.Paths;

public class Jasypt extends LocalToolCommandlet {

  public Jasypt(IdeContext context) {

    super(context, "jasypt", Set.of(Tag.JAVA));
/*    Scanner scanner = new Scanner(System.in);

    // Prompt the user to enter input
    System.out.println("Please enter your input:");

    // Read input from stdin
    String input = scanner.nextLine();

    // Print the input back to the user
    System.out.println("You entered: " + input);

    // Close the scanner
    scanner.close();
    System.out.println("hello jasypt " + input);*/
  }

  @Override
  public boolean doInstall(boolean silent) {

    getCommandlet(Java.class).install();
    return super.doInstall(silent);
  }

  @Override
  public void run() {
    String[] arguments = this.arguments.asArray();
    System.out.println("arguments are: " + arguments);

    Path m2Repo = context.getVariables().getPath("M2_REPO");

    // Assuming getInstalledVersion() returns the installed version dynamically
    String installedVersion = getInstalledVersion().toString();

    // Append the installed version and the JAR file to the path
    Path jasyptJar = m2Repo.resolve(Paths.get("org", "jasypt", "jasypt", installedVersion, "jasypt-" + installedVersion + ".jar"));
    System.out.println("jar file is is :" + jasyptJar);
    System.out.println("version :" + this.getInstalledVersion() + "edition :" + this.getInstalledEdition());

    // Define the master password and secret
    String masterPassword = "marco";
    String secret = "bello";

    // Create an array of strings to represent the command arguments
    String[] commandArgs = {
        "-cp",
        jasyptJar.toString(),
        "org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI",
        "algorithm=PBEWITHHMACSHA512ANDAES_256",
        "ivGeneratorClassName=org.jasypt.iv.RandomIvGenerator",
        "password=" + masterPassword,
        "input=" + secret
    };

    // Print the command arguments
    for (String arg : commandArgs) {
      System.out.println(arg);
    }




    Java java = getCommandlet(Java.class);

    java.runTool(null, commandArgs);

  }
}


