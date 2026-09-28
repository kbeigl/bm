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

/**
 * Typed error codes for {@link ApiException} to enable explicit, type-safe programmatic decisions
 * (e.g. triggering reconnection intervals vs handling invalid credentials).
 */
public enum ApiErrorCode {
  /** Connection failure (e.g. server unreachable, timeout, connection refused). */
  NO_CONNECTION,

  /** Authentication / authorization failure (HTTP 401 Unauthorized or 403 Forbidden). */
  UNAUTHORIZED,

  /** Bad request (HTTP 400 Bad Request, validation errors, out of range values, etc.). */
  BAD_REQUEST,

  /** Resource not found (HTTP 404 Not Found or entity not found on client search). */
  NOT_FOUND,

  /** Other client REST communication errors. */
  CLIENT_REST,

  /** Server error (HTTP 5xx Server Error). */
  SERVER_ERROR,

  /** Generic or unknown error condition. */
  UNKNOWN
}
