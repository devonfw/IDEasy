package com.devonfw.tools.ide.tool.jasypt;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.property.PasswordProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.mvn.Mvn;

/**
 * {@link ToolCommandlet} for <a href="http://www.jasypt.org/">Jasypt</a>, The java library which allows to add basic
 * encryption capabilities with minimum effort.
 */
public class Jasypt extends LocalToolCommandlet {

  private final EnumProperty<JasyptCommand> command;

  private final PasswordProperty masterPassword;
  private final PasswordProperty secret;


  private static final String CLASS_NAME_ENCRYPTION = "org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI";

  private static final String CLASS_NAME_DECRYPTION = "org.jasypt.intf.cli.JasyptPBEStringDecryptionCLI";

  private static final String ALGORITHM = "algorithm=PBEWITHHMACSHA512ANDAES_256";

  private static final String GENERATOR_CLASS_NAME = "ivGeneratorClassName=org.jasypt.iv.RandomIvGenerator";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Jasypt(IdeContext context) {


    super(context, "jasypt", Set.of(Tag.JAVA, Tag.ENCRYPTION));



    this.command = add(new EnumProperty<>("", true, "command", JasyptCommand.class));
    this.masterPassword=add(new PasswordProperty("", true, "masterPassword"));
    this.secret=add(new PasswordProperty("", true, "secret"));
    add(this.arguments);

  }

  @Override
  protected void initProperties() {

    // Empty on purpose
  }

  @Override
  public void run() {

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



//    String[] args = this.arguments.asArray();
//
//    if (args.length == 0) {
//      this.context.info(USAGE_INFO);
//    } else if (args.length == 3 && args[0].equals("encrypt")) {
//      runJasypt(CLASS_NAME_ENCRYPTION, args);
//    } else if (args.length == 3 && args[0].equals("decrypt")) {
//      runJasypt(CLASS_NAME_DECRYPTION, args);
//    } else {
//      this.context.warning("Unknown arguments");
//      this.context.info(USAGE_INFO);
//    }
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
  public void postInstall() {

    super.postInstall();

//    if (Files.notExists(resolveJasyptJarPath())) {
//      installJasyptArtifact();
//    }
  }

  private void installJasyptArtifact() {

    Mvn mvn = getCommandlet(Mvn.class);
    mvn.install();
    this.context.debug("installing jasypt artifact in: " + resolveJasyptJarPath());
    mvn.runTool(null, "org.apache.maven.plugins:maven-dependency-plugin:3.1.2:get",
        "-Dartifact=org.jasypt:jasypt:" + getInstalledVersion().toString());
  }

  private void runJasypt(String className) {

    Java java = getCommandlet(Java.class);

    //this.context.getVariables().set("JASYPT_OPTS", "algorithm=PBEWITHHMACSHA512ANDAES_256", false);


//    EnvironmentVariables variables = this.context.getVariables();
//    EnvironmentVariables typeVariables = variables.getByType(EnvironmentVariablesType.CONF);
//    typeVariables.set("JASYPT_OPTS", "algorithm=PBEWITHHMACSHA512ANDAES_256", false);
//    typeVariables.save();

    String[] jasyptOptions = this.context.getVariables().get("JASYPT_OPTS").split(" ");
    //String jasyptAlgorithm2 = IdeVariables.JASYPT_OPTS.get(context);
    //String jaspaglo3 = IdeVariables.get("JASYPT_OPTS").getDefaultValueAsString(context);



    java.runTool(null, "-cp", resolveJasyptJarPath().toString(), className, jasyptOptions[0], jasyptOptions[1],
        "password=" + this.masterPassword.getValue(), "input=" + this.secret.getValue());
  }

  private Path resolveJasyptJarPath() {

    Path toolPath = this.getToolPath();
    String installedVersion = getInstalledVersion().toString();
    return toolPath.resolve("jasypt-" + installedVersion + ".jar");
  }

}
