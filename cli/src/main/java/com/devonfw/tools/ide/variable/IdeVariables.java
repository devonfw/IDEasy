package com.devonfw.tools.ide.variable;

import java.util.Collection;
import java.util.List;

/**
 * Interface (mis)used to define all the available variables.
 */
public interface IdeVariables {

  /** {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getIdeHome() IDE_HOME}. */
  VariableDefinitionPath IDE_HOME = new VariableDefinitionPath("IDE_HOME", "DEVON_IDE_HOME", c -> c.getIdeHome(), true);

  /** {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getIdeRoot() IDE_ROOT}. */
  VariableDefinitionPath IDE_ROOT = new VariableDefinitionPath("IDE_ROOT", null, c -> c.getIdeRoot());

  /** {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getUserHome() HOME}. */
  VariableDefinitionPath HOME = new VariableDefinitionPath("HOME", null, c -> c.getUserHome(), true);

  /** {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getWorkspaceName() WORKSPACE}. */
  VariableDefinitionString WORKSPACE = new VariableDefinitionString("WORKSPACE", null, c -> c.getWorkspaceName(), true);

  /** {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getPath() PATH}. */
  VariableDefinitionSystemPath PATH = new VariableDefinitionSystemPath("PATH", null, c -> c.getPath(), true, true);

  /**
   * {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getWorkspacePath() WORKSPACE_PATH}.
   */
  VariableDefinitionPath WORKSPACE_PATH = new VariableDefinitionPath("WORKSPACE_PATH", null, c -> c.getWorkspacePath(), true);

  /** {@link VariableDefinition} for list of tools to install by default. */
  VariableDefinitionStringList IDE_TOOLS = new VariableDefinitionStringList("IDE_TOOLS", "DEVON_IDE_TOOLS", c -> List.of("mvn", "npm"));

  /** {@link VariableDefinition} for list of IDE tools to create start scripts for. */
  VariableDefinitionStringList CREATE_START_SCRIPTS = new VariableDefinitionStringList("CREATE_START_SCRIPTS", "DEVON_CREATE_START_SCRIPTS");

  /** {@link VariableDefinition} for minimum IDE product version. */
  // TODO define initial IDEasy version as default value
  VariableDefinitionVersion IDE_MIN_VERSION = new VariableDefinitionVersion("IDE_MIN_VERSION", "DEVON_IDE_MIN_VERSION");

  /** {@link VariableDefinition} for version of maven (mvn). */
  VariableDefinitionVersion MVN_VERSION = new VariableDefinitionVersion("MVN_VERSION", "MAVEN_VERSION");

  /** {@link VariableDefinition} arguments for maven to set the m2 repo location. */
  VariableDefinitionString MAVEN_ARGS = new VariableDefinitionString("MAVEN_ARGS", null, c -> c.getMavenArgs(), false, true);

  /** {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getWorkspaceName() WORKSPACE}. */
  VariableDefinitionString DOCKER_EDITION = new VariableDefinitionString("DOCKER_EDITION", null, c -> "rancher");

  /** {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getWorkspaceName() WORKSPACE}. */

  /** {@link VariableDefinition} for options of jasypt */
  VariableDefinitionString JASYPT_OPTS = new VariableDefinitionString("JASYPT_OPTS", null,
      c -> "algorithm=PBEWITHHMACSHA512ANDAES_256 ivGeneratorClassName=org.jasypt.iv.RandomIvGenerator");

  /** {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getProjectName() PROJECT_NAME}. */
  VariableDefinitionString PROJECT_NAME = new VariableDefinitionString("PROJECT_NAME", null, c -> c.getProjectName());

  /** A {@link Collection} with all pre-defined {@link VariableDefinition}s. */
  Collection<VariableDefinition<?>> VARIABLES = List.of(PATH, HOME, WORKSPACE_PATH, IDE_HOME, IDE_ROOT, WORKSPACE, IDE_TOOLS, CREATE_START_SCRIPTS,
      IDE_MIN_VERSION, MVN_VERSION, DOCKER_EDITION, JASYPT_OPTS, MAVEN_ARGS, PROJECT_NAME);

  /**
   * @param name the name of the requested {@link VariableDefinition}.
   * @return the {@link VariableDefinition} for the given {@code name} or {@code null} if not defined.
   * @see VariableDefinition#getName()
   * @see VariableDefinition#getLegacyName()
   */
  static VariableDefinition<?> get(String name) {

    return IdeVariablesList.get(name);
  }

}
