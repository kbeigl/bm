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

// unchecked Ex > usage error
// ApiException is a RuntimeException:
// The caller is not forced by the compiler to catch ApiException.
// However, good practice dictates they should if they want to handle it.
public class ApiException extends RuntimeException {

  // checked Ex > situational error
  // > we can reasonably expect the caller of our method to be able to recover.
  // public class ApiException extends Exception {

  private static final long serialVersionUID = -191972923538140025L;

  /** Constants for errors that should be handled by client. */
  public static final String
      // ResourceAccessException > trigger server reconnect intervals
      APIEX_NO_CONNECTION = "Cannot connect to server!",
      // RestClientResponseException
      APIEX_CLIENT_REST = "Exception with REST client!",
      // could be a PK violation, if object already exists
      APIEX_BAD_REQUEST = "Request returned error!",
      // could be a PK violation, if object already exists
      APIEX_UNAUTHORIZED = "Unauthorized: check credentials!";

  public ApiException(String message) {
    super(message);
  }

  public ApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
