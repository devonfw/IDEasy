package com.devonfw.tools.ide.tool.node;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.PackageManagerBasedLocalToolCommandlet;
import com.devonfw.tools.ide.tool.PackageManagerRequest;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link LocalToolCommandlet} for tools based on <a href="https://www.npmjs.com/">npm</a>.
 *
 * @param <P> type of the {@link ToolCommandlet} acting as {@link #getPackageManagerClass() package manager}.
 */
public abstract class NodeBasedCommandlet<P extends ToolCommandlet> extends PackageManagerBasedLocalToolCommandlet<P> {

  /** File name of the {@link #findBuildDescriptor(Path) build descriptor} . */
  public static final String PACKAGE_JSON = "package.json";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public NodeBasedCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  @Override
  protected Node getParentTool() {

    return this.context.getCommandletManager().getCommandlet(Node.class);
  }

  @Override
  public boolean isInstalled() {

    return hasNodeBinary(this.tool);
  }

  @Override
  public String getInstalledEdition() {

    if (getInstalledVersion() != null) {
      return this.tool;
    }
    return null;
  }

  /**
   * Checks if a provided binary can be found within node.
   *
   * @param binary name of the binary.
   * @return {@code true} if a binary was found in the node installation, {@code false} if not.
   */
  protected boolean hasNodeBinary(String binary) {

    return Files.exists(getToolBinPath().resolve(binary));
  }

  @Override
  protected String completeRequestOption(PackageManagerRequest request) {

    return switch (request.getType()) {
      case PackageManagerRequest.TYPE_INSTALL -> "-gf";
      case PackageManagerRequest.TYPE_UNINSTALL -> "-g";
      default -> null;
    };
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }

  @Override
  public Path findBuildDescriptor(Path directory) {

    Path buildDescriptor = directory.resolve(PACKAGE_JSON);
    if (Files.exists(buildDescriptor)) {
      return buildDescriptor;
    }
    return super.findBuildDescriptor(directory);
  }
}
