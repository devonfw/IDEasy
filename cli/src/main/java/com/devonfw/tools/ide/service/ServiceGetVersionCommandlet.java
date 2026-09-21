package com.devonfw.tools.ide.service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.url.model.UrlMetadata;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Resolves a tool version via the {@link IdeServiceServer} if one is running, falling back to local resolution otherwise.
 */
public class ServiceGetVersionCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceGetVersionCommandlet.class);
  private final ToolProperty tool;
  private final StringProperty edition;
  private final StringProperty version;

  public ServiceGetVersionCommandlet(IdeContext context) {
    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
    this.edition = add(new StringProperty("--edition", false, "-e"));
    this.version = add(new StringProperty("--version", false, "-v"));
  }

  @Override
  public String getName() {
    return "service-get-version";
  }

  public ToolProperty getTool() {
    return this.tool;
  }

  public StringProperty getEdition() {
    return this.edition;
  }

  public StringProperty getVersion() {
    return this.version;
  }

  public boolean isProcessableOutput() {
    return true;
  }

  @Override
  protected void doRun() {
    ServiceRequest request = new ServiceRequest(ServiceOperation.GET_VERSION, buildParams());
    Path portFile = this.context.getIdePath().resolve(IdeServiceClient.PORT_FILE_NAME);
    Optional<ServiceResponse> response = new IdeServiceClient(portFile).tryDelegate(request);

    if (response.isPresent() && response.get().success()) {
      IdeLogLevel.PROCESSABLE.log(LOG, "{}", response.get().payload());
      return;
    }

    if (response.isPresent()) {
      throw new CliException(response.get().errorMessage(), 1);
    }

    LOG.info("IDEasy service not running - resolving version locally.");
    String resolved = resolveLocally();
    IdeLogLevel.PROCESSABLE.log(LOG, "{}", resolved);
  }

  private String resolveLocally() {
    UrlRepository repository = UrlRepository.load(this.context.getUrlsPath());
    UrlMetadata urls = new UrlMetadata(this.context, repository);
    String edition = this.edition.getValue();
    if (edition == null || edition.isBlank()) {
      edition = this.tool.getValue().getName();
    }

    VersionIdentifier range = this.version.getValue() == null ? null : VersionIdentifier.of(this.version.getValue());
    return urls.resolveVersion(this.tool.getValue().getName(), edition, range, null).toString();
  }

  private Map<String, String> buildParams() {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("tool", this.tool.getValue().getName());
    String edition = this.edition.getValue();
    if (edition != null && !edition.isBlank()) {
      params.put("edition", edition);
    }
    String version = this.version.getValue();
    if (version != null && !version.isBlank()) {
      params.put("version", version);
    }
    return params;
  }
}
