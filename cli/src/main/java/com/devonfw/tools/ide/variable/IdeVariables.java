package com.devonfw.tools.ide.variable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.git.GitUrlSyntax;

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

  /** {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getWorkspacePath() WORKSPACE_PATH}. */
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

  /** {@link VariableDefinition} arguments for maven to locate the settings file. */
  VariableDefinitionString MAVEN_ARGS = new VariableDefinitionString("MAVEN_ARGS", null, IdeContext::getMavenArgs, false, true);

  /** {@link VariableDefinition} arguments for maven to set the m2 repo location. */
  VariableDefinitionPath M2_REPO = new VariableDefinitionPath("M2_REPO", null, IdeVariables::getMavenRepositoryPath, false, true);

  /** {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getWorkspaceName() WORKSPACE}. */
  VariableDefinitionString DOCKER_EDITION = new VariableDefinitionString("DOCKER_EDITION", null, c -> "rancher");

  /** {@link VariableDefinition} for default build options of mvn */
  VariableDefinitionString MVN_BUILD_OPTS = new VariableDefinitionString("MVN_BUILD_OPTS", null, c -> "clean install");

  /** {@link VariableDefinition} for default build options of npm */
  // TODO: add default build options, see: https://github.com/devonfw/IDEasy/issues/441
  VariableDefinitionString NPM_BUILD_OPTS = new VariableDefinitionString("NPM_BUILD_OPTS", null, c -> "");

  /** {@link VariableDefinition} for default build options of gradle */
  VariableDefinitionString GRADLE_BUILD_OPTS = new VariableDefinitionString("GRADLE_BUILD_OPTS", null, c -> "clean dist");

  /** {@link VariableDefinition} for default build options of yarn */
  // TODO: add default build options, see: https://github.com/devonfw/IDEasy/issues/441
  VariableDefinitionString YARN_BUILD_OPTS = new VariableDefinitionString("YARN_BUILD_OPTS", null, c -> "");

  /** {@link VariableDefinition} for options of jasypt */
  VariableDefinitionString JASYPT_OPTS = new VariableDefinitionString("JASYPT_OPTS", null,
      c -> "algorithm=PBEWITHHMACSHA512ANDAES_256 ivGeneratorClassName=org.jasypt.iv.RandomIvGenerator");

  /** {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getProjectName() PROJECT_NAME}. */
  VariableDefinitionString PROJECT_NAME = new VariableDefinitionString("PROJECT_NAME", null, c -> c.getProjectName());

  /** Preferred Git protocol (HTTPS/SSH) as defined by {@link GitUrlSyntax}. */
  VariableDefinitionEnum<GitUrlSyntax> PREFERRED_GIT_PROTOCOL = new VariableDefinitionEnum<>("PREFERRED_GIT_PROTOCOL", null, GitUrlSyntax.class,
      c -> GitUrlSyntax.DEFAULT);

  /**
   * {@link VariableDefinition} for support of legacy variable syntax when
   * {@link com.devonfw.tools.ide.environment.EnvironmentVariables#resolve(String, Object, boolean) resolving variables} in configuration templates.
   */
  VariableDefinitionBoolean IDE_VARIABLE_SYNTAX_LEGACY_SUPPORT_ENABLED = new VariableDefinitionBoolean("IDE_VARIABLE_SYNTAX_LEGACY_SUPPORT_ENABLED", null,
      c -> Boolean.TRUE);

  /**
   * {@link VariableDefinition} for support of legacy xml templates without XML merge namespace
   */
  VariableDefinitionBoolean IDE_XML_MERGE_LEGACY_SUPPORT_ENABLED = new VariableDefinitionBoolean("IDE_XML_MERGE_LEGACY_SUPPORT_ENABLED", null,
      c -> Boolean.FALSE);

  /** {@link VariableDefinition} for {@link com.devonfw.tools.ide.context.IdeContext#getProjectName() DEVON_IDE_CUSTOM_TOOLS}. */
  VariableDefinitionString DEVON_IDE_CUSTOM_TOOLS = new VariableDefinitionString("DEVON_IDE_CUSTOM_TOOLS");

  /** A {@link Collection} with all pre-defined {@link VariableDefinition}s. */
  Collection<VariableDefinition<?>> VARIABLES = List.of(PATH, HOME, WORKSPACE_PATH, IDE_HOME, IDE_ROOT, WORKSPACE, IDE_TOOLS, CREATE_START_SCRIPTS,
      IDE_MIN_VERSION, MVN_VERSION, M2_REPO, DOCKER_EDITION, MVN_BUILD_OPTS, NPM_BUILD_OPTS, GRADLE_BUILD_OPTS, YARN_BUILD_OPTS, JASYPT_OPTS, MAVEN_ARGS,
      PROJECT_NAME, IDE_VARIABLE_SYNTAX_LEGACY_SUPPORT_ENABLED, PREFERRED_GIT_PROTOCOL);

  /**
   * @param name the name of the requested {@link VariableDefinition}.
   * @return the {@link VariableDefinition} for the given {@code name} or {@code null} if not defined.
   * @see VariableDefinition#getName()
   * @see VariableDefinition#getLegacyName()
   */
  static VariableDefinition<?> get(String name) {

    return IdeVariablesList.get(name);
  }

  /**
   * @param name the name of the variable.
   * @return {@code true} if a {@link VariableDefinition#getLegacyName() legacy variable}, {@code false} otherwise.
   */
  static boolean isLegacyVariable(String name) {
    VariableDefinition<?> variableDefinition = IdeVariablesList.get(name);
    if (variableDefinition != null) {
      return name.equals(variableDefinition.getLegacyName());
    }
    return false;
  }

  private static Path getMavenRepositoryPath(IdeContext context) {
    Path mvnConf = context.getMavenConfigurationFolder();
    if (mvnConf == null) {
      return null;
    }
    return mvnConf.resolve("repository");
  }
}
