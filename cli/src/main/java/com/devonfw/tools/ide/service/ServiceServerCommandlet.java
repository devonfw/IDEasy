package com.devonfw.tools.ide.service;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Starts the {@link IdeServiceServer} in the foreground and keeps it running until the process is terminated.
 */
public class ServiceServerCommandlet extends Commandlet {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceServerCommandlet.class);

  public ServiceServerCommandlet(IdeContext context) {
    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {
    return "service-server";
  }

  @Override
  protected void doRun() {
    try {
      Path portFile = this.context.getIdePath().resolve(IdeServiceServer.PORT_FILE_NAME);
      IdeServiceServer server = new IdeServiceServer(this.context);
      int port = server.start(portFile);
      IdeLogLevel.SUCCESS.log(LOG, "IDEasy service is listening on port {} (port file {})", port, portFile);
      LOG.info("Press Ctrl+C to stop the service");
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (IOException e) {
      LOG.error("Failed to start IDEasy service: {}", e.getMessage());
      throw new CliException("Failed to start IDEasy service");
    }
  }
}
