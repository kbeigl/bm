/*
 * (C) Copyright 2026 Kristof Beiglböck
 *               kbeigl.github.io/bm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
