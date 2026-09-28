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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

class ApiExceptionTest {

  @Test
  void testConstructorWithMessageInference() {
    ApiException exNoConn = new ApiException(ApiException.APIEX_NO_CONNECTION);
    assertEquals(ApiErrorCode.NO_CONNECTION, exNoConn.getErrorCode());
    assertNull(exNoConn.getStatusCode());

    ApiException exUnauth = new ApiException(ApiException.APIEX_UNAUTHORIZED);
    assertEquals(ApiErrorCode.UNAUTHORIZED, exUnauth.getErrorCode());
    assertNull(exUnauth.getStatusCode());

    ApiException exBadReq = new ApiException(ApiException.APIEX_BAD_REQUEST);
    assertEquals(ApiErrorCode.BAD_REQUEST, exBadReq.getErrorCode());
    assertNull(exBadReq.getStatusCode());

    ApiException exClientRest = new ApiException(ApiException.APIEX_CLIENT_REST);
    assertEquals(ApiErrorCode.CLIENT_REST, exClientRest.getErrorCode());
    assertNull(exClientRest.getStatusCode());

    ApiException exUnknown = new ApiException("Random error message");
    assertEquals(ApiErrorCode.UNKNOWN, exUnknown.getErrorCode());
    assertNull(exUnknown.getStatusCode());
  }

  @Test
  void testConstructorWithErrorCodeAndMessage() {
    ApiException ex = new ApiException(ApiErrorCode.NOT_FOUND, "Not found");
    assertEquals(ApiErrorCode.NOT_FOUND, ex.getErrorCode());
    assertEquals(404, ex.getStatusCode());
    assertEquals("Not found", ex.getMessage());

    ApiException exServer = new ApiException(ApiErrorCode.SERVER_ERROR, "Server failure");
    assertEquals(ApiErrorCode.SERVER_ERROR, exServer.getErrorCode());
    assertEquals(500, exServer.getStatusCode());
  }

  @Test
  void testConstructorWithExplicitStatusCodeAndCause() {
    RuntimeException cause = new RuntimeException("root cause");
    ApiException ex = new ApiException(ApiErrorCode.BAD_REQUEST, 400, "Bad input", cause);
    assertEquals(ApiErrorCode.BAD_REQUEST, ex.getErrorCode());
    assertEquals(400, ex.getStatusCode());
    assertEquals("Bad input", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  @Test
  void testApiAspectExceptionMapping() {
    ApiAspect aspect = new ApiAspect();

    // 1. ResourceAccessException -> NO_CONNECTION
    ResourceAccessException raEx = new ResourceAccessException("Connection refused");
    ApiException apiEx1 = aspect.createApiException(raEx);
    assertEquals(ApiErrorCode.NO_CONNECTION, apiEx1.getErrorCode());
    assertNull(apiEx1.getStatusCode());
    assertEquals(ApiException.APIEX_NO_CONNECTION, apiEx1.getMessage());

    // 2. HttpClientErrorException.Unauthorized (401) -> UNAUTHORIZED
    HttpClientErrorException unauthEx =
        HttpClientErrorException.create(
            HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, null, null);
    ApiException apiEx2 = aspect.createApiException(unauthEx);
    assertEquals(ApiErrorCode.UNAUTHORIZED, apiEx2.getErrorCode());
    assertEquals(401, apiEx2.getStatusCode());
    assertEquals(ApiException.APIEX_UNAUTHORIZED, apiEx2.getMessage());

    // 3. HttpClientErrorException.BadRequest (400) -> BAD_REQUEST
    HttpClientErrorException badReqEx =
        HttpClientErrorException.create(
            HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, null, null);
    ApiException apiEx3 = aspect.createApiException(badReqEx);
    assertEquals(ApiErrorCode.BAD_REQUEST, apiEx3.getErrorCode());
    assertEquals(400, apiEx3.getStatusCode());
    assertEquals(ApiException.APIEX_BAD_REQUEST, apiEx3.getMessage());

    // 4. HttpClientErrorException.NotFound (404) -> NOT_FOUND
    HttpClientErrorException notFoundEx =
        HttpClientErrorException.create(
            HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null);
    ApiException apiEx4 = aspect.createApiException(notFoundEx);
    assertEquals(ApiErrorCode.NOT_FOUND, apiEx4.getErrorCode());
    assertEquals(404, apiEx4.getStatusCode());

    // 5. HttpServerErrorException.InternalServerError (500) -> SERVER_ERROR
    HttpServerErrorException serverEx =
        HttpServerErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Error", HttpHeaders.EMPTY, null, null);
    ApiException apiEx5 = aspect.createApiException(serverEx);
    assertEquals(ApiErrorCode.SERVER_ERROR, apiEx5.getErrorCode());
    assertEquals(500, apiEx5.getStatusCode());

    // 6. Generic RestClientException -> CLIENT_REST
    RestClientException clientEx = new RestClientException("Generic REST error");
    ApiException apiEx6 = aspect.createApiException(clientEx);
    assertEquals(ApiErrorCode.CLIENT_REST, apiEx6.getErrorCode());
    assertNull(apiEx6.getStatusCode());

    // 7. Non-RestClientException -> UNKNOWN
    IllegalArgumentException illegalArgEx = new IllegalArgumentException("Invalid argument");
    ApiException apiEx7 = aspect.createApiException(illegalArgEx);
    assertEquals(ApiErrorCode.UNKNOWN, apiEx7.getErrorCode());
    assertNull(apiEx7.getStatusCode());
  }
}
