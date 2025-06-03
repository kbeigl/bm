package bm.traccar.api;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;

// this only works for testing! -> AOP for actual @Service
public class ClientExceptionHandler implements TestExecutionExceptionHandler {

  @Override
  public void handleTestExecutionException(ExtensionContext context, Throwable throwable)
      throws Throwable {
    System.err.println(
        "Test " + context.getDisplayName() + " failed with exception: " + throwable.getMessage());
    System.err.println(throwable);

    // Status code: 401
    // Reason: HTTP 401 Unauthorized - WebApplicationException (SecurityRequestFilter:116 < ... <
    // OverrideFilter:50
    // < ...)

    // handle RestClientResponseException > HttpStatusCodeException .. for all methods
    if (throwable instanceof HttpStatusCodeException) {
      // catch (HttpStatusCodeException e)
      HttpStatusCodeException e = (HttpStatusCodeException) throwable;
      System.err.println("Status code: " + e.getStatusCode().value());
      System.err.println("Reason: " + e.getResponseBodyAsString());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
      return;
    }
    throw throwable;
  }
}
