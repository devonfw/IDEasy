package com.devonfw.tools.ide.service;

/**
 * The response a {@link IdeServiceServer} sends back to a client for a {@link ServiceRequest}.
 *
 * @param success {@code true} if the request succeeded, {@code false} otherwise.
 * @param payload the resulting value on success, or {@code null} on error.
 * @param errorMessage the error description on failure, or {@code null} on success.
 */
public record ServiceResponse(boolean success, String payload, String errorMessage) {

  public static ServiceResponse ok(String payload) {
    return new ServiceResponse(true, payload, null);
  }

  public static ServiceResponse error(String error) {
    return new ServiceResponse(false, null, error);
  }
}
