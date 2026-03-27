package com.devonfw.tools.ide.tool.gui;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.mvn.Mvn;
import com.devonfw.tools.ide.tool.mvn.MvnBasedLocalToolCommandlet;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * {@link MvnBasedLocalToolCommandlet} to launch the IDEasy GUI.
 */
public class Gui extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Gui.class);

  //TODO: implement constant in sync with maven property gui.relative_pom_path
  private static final String POM_PATH = "gui-execution";

  /**
   * @param context the {@link IdeContext}.
   */
  public Gui(IdeContext context) {

    super(context);
    addKeyword("gui");
  }


  @Override
  public String getName() {

    return "gui";
  }


  @Override
  protected void doRun() {

    LOG.debug("Starting GUI via commandlet");

    Mvn mvn = context.getCommandletManager().getCommandlet(Mvn.class);

    List<String> args = List.of(
        "-f",
        IdeVariables.IDE_ROOT.get(context).toString() + "/_ide/installation/" + POM_PATH + "/pom.xml",
        "exec:java",
        "-Dexec.mainClass=com.devonfw.tools.gui.AppLauncher"
    );

    mvn.runTool(context.newProcess(), ProcessMode.DEFAULT, args);
  }
}
