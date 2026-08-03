package com.devonfw.tools.ide.network;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cache.CachedValue;
import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Implementation of {@link NetworkStatus}.
 */
public class NetworkStatusImpl implements NetworkStatus {

  private static final Logger LOG = LoggerFactory.getLogger(NetworkStatusImpl.class);

  private final AbstractIdeContext context;

  private NetworkProxy networkProxy;

  private final String onlineCheckUrl;

  protected final CachedValue<Throwable> onlineCheck;

  private static final String ERROR_TEXT_PKIX = "pkix path building failed";

  /**
   * @param ideContext the {@link AbstractIdeContext}.
   */
  public NetworkStatusImpl(AbstractIdeContext ideContext) {
    this(ideContext, null, CachedValue.DEFAULT_RETENTION);
  }

  /**
   * @param context the {@link AbstractIdeContext}.
   * @param onlineCheckUrl the URL to test for the online-check.
   * @param retention the retention of the {@link CachedValue}.
   */
  protected NetworkStatusImpl(AbstractIdeContext context, String onlineCheckUrl, long retention) {
    this.context = context;
    if (onlineCheckUrl == null) {
      onlineCheckUrl = "https://www.github.com";
    }
    this.onlineCheckUrl = onlineCheckUrl;
    this.onlineCheck = new CachedValue<>(this::doOnlineCheck, retention);
  }

  @Override
  public boolean isOfflineMode() {

    return this.context.isOfflineMode();
  }

  @Override
  public boolean isOnline() {

    return getError() == null;
  }

  @Override
  public Throwable getError() {

    return this.onlineCheck.get();
  }

  private Throwable doOnlineCheck() {
    configureNetworkProxy();
    try {
      int timeout = 1000;
      //open a connection to URL and try to retrieve data
      //getContent fails if there is no connection
      URLConnection connection = new URL(this.onlineCheckUrl).openConnection();
      connection.setConnectTimeout(timeout);
      connection.getContent();
      return null;
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Error when trying to connect to {}", this.onlineCheckUrl, e);
      }
      return e;
    }
  }

  private void configureNetworkProxy() {

    if (this.networkProxy == null) {
      this.networkProxy = new NetworkProxy(this.context);
      this.networkProxy.configure();
    }
  }

  @Override
  public void logStatusMessage() {

    if (isOfflineMode()) {
      LOG.warn("You are offline because you have enabled offline mode via CLI option.");
      return;
    }
    Throwable error = getError();
    if (error == null) {
      IdeLogLevel.SUCCESS.log(LOG, "You are online.");
      return;
    }
    String message = "You are offline because of the following error:";
    if (LOG.isDebugEnabled()) {
      LOG.error(message, error);
    } else {
      LOG.error(message);
      LOG.error(error.toString());
    }
    if (isTlsTrustIssue(error)) {
      logTruststoreFixHint(this.onlineCheckUrl);
    } else {
      IdeLogLevel.INTERACTION.log(LOG, "Please check potential proxy settings, ensure you are properly connected to the internet and retry this operation.");
    }
  }

  @Override
  public <T> T invokeNetworkTask(Callable<T> callable, String uri) {

    if (isOfflineMode()) {
      throw CliOfflineException.ofDownloadViaUrl(uri);
    }
    configureNetworkProxy();
    try {
      return callable.call();
    } catch (Exception e) {
      if (e instanceof IOException ioException) {
        this.onlineCheck.set(ioException);
      }
      // the underlying SSLHandshakeException is often wrapped (e.g. as an IllegalStateException by the downloader), hence we scan the entire cause chain and
      // must not rely on the exception being an IOException.
      if (isTlsTrustIssue(e)) {
        logTruststoreFixHint(uri);
        throw new IllegalStateException("TLS certificate trust error whilst communicating to " + uri, e);
      }
      if (e instanceof IOException) {
        throw new IllegalStateException("Network error whilst communicating to " + uri, e);
      }
      throw new IllegalStateException("Unexpected error whilst communicating to " + uri, e);
    }
  }

  private void logTruststoreFixHint(String uri) {

    LOG.warn("The TLS connection to {} failed due to a certificate trust error (PKIX / certificate-path / SSL handshake). "
        + "This commonly happens behind a corporate VPN or proxy that intercepts TLS traffic.", uri);
    LOG.warn("Please first verify that the URL above is a legitimate endpoint that you trust and that you are reaching it securely. "
        + "If it is trustworthy, you can register its certificate in a custom truststore by running:\nide fix-vpn-tls-problem {}", uri);
    IdeLogLevel.INTERACTION.log(LOG, "For more details see: https://github.com/devonfw/IDEasy/blob/main/documentation/proxy-support.adoc#tls-certificate-issues");
  }

  boolean isTlsTrustIssue(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      String message = current.getMessage();
      if (containsTlsTrustIndicator(message)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  boolean containsTlsTrustIndicator(String text) {
    if ((text == null) || text.isBlank()) {
      return false;
    }
    String normalized = text.toLowerCase(Locale.ROOT);
    return normalized.contains(ERROR_TEXT_PKIX);
  }

}
