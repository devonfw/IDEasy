package com.devonfw.tools.ide.io;

import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Optional;
import javax.net.ssl.SSLSession;

/**
 * Implementation of {@link HttpResponse} in case of an {@link Throwable error} to prevent sub-sequent {@link NullPointerException}s.
 */
public class HttpErrorResponse implements HttpResponse<Throwable> {

  private final Throwable error;

  private final HttpRequest request;

  private final URI uri;
  private static final HttpHeaders NO_HEADERS = HttpHeaders.of(Collections.emptyMap(), (x, y) -> true);

  /**
   * @param error the {@link Throwable} that was preventing the HTTP request for {@link #body()}.
   * @param request the {@link HttpRequest} for {@link #request()}.
   * @param uri the {@link URI} for {@link #uri()}.
   */
  public HttpErrorResponse(Throwable error, HttpRequest request, URI uri) {
    super();
    this.error = error;
    this.request = request;
    this.uri = uri;
  }

  @Override
  public int statusCode() {
    return -1;
  }

  @Override
  public HttpRequest request() {
    return this.request;
  }

  @Override
  public Optional<HttpResponse<Throwable>> previousResponse() {
    return Optional.empty();
  }

  @Override
  public HttpHeaders headers() {
    return NO_HEADERS;
  }

  @Override
  public Throwable body() {
    return this.error;
  }

  @Override
  public Optional<SSLSession> sslSession() {
    return Optional.empty();
  }

  @Override
  public URI uri() {
    return this.uri;
  }

  @Override
  public Version version() {
    return Version.HTTP_2;
  }
}
