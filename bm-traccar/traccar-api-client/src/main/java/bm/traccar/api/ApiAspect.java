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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.UnknownContentTypeException;

/**
 * Encapsulate cross-cutting concerns around the ApiClient and generated.api.*Api. Wrap all
 * Exceptions thrown in ApiException to provide single point of problems with API inside complex
 * environments.
 */
@Aspect
@Component
public class ApiAspect {

  private static final Logger logger = LoggerFactory.getLogger(ApiAspect.class);

  // TODO: narrow to all public methods?
  // @Pointcut("execution(public * bm.traccar.generated.api.*Api.*(..))")
  // all join points (methods, constructors, etc.)
  @Around("within(bm.traccar.generated.api.*Api)")
  public Object aroundApiMethods(ProceedingJoinPoint pjp) throws Throwable {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    try {
      logger.debug("ApiAspect invoke {}", signature.toShortString());
      return pjp.proceed();
    } catch (Throwable t) {
      throw createApiException(t);
    }
  }

  /** Evaluate the thrown exception and create typed ApiException. */
  public ApiException createApiException(Throwable thrown) {
    if (thrown instanceof ResourceAccessException) {
      logger.debug("ResourceAccessException");
      logger.warn(thrown.getMessage());
      return new ApiException(
          ApiErrorCode.NO_CONNECTION, null, ApiException.APIEX_NO_CONNECTION, thrown);

    } else if (thrown instanceof HttpStatusCodeException hscEx) {
      HttpStatusCode sc = hscEx.getStatusCode();
      int statusCode = sc.value();
      if (sc.is4xxClientError()) {
        if (statusCode == 400) {
          return new ApiException(
              ApiErrorCode.BAD_REQUEST, statusCode, ApiException.APIEX_BAD_REQUEST, thrown);
        } else if (statusCode == 401 || statusCode == 403) {
          return new ApiException(
              ApiErrorCode.UNAUTHORIZED, statusCode, ApiException.APIEX_UNAUTHORIZED, thrown);
        } else if (statusCode == 404) {
          return new ApiException(ApiErrorCode.NOT_FOUND, statusCode, hscEx.getMessage(), thrown);
        } else {
          return new ApiException(ApiErrorCode.CLIENT_REST, statusCode, hscEx.getMessage(), thrown);
        }
      } else if (sc.is5xxServerError()) {
        return new ApiException(
            ApiErrorCode.SERVER_ERROR, statusCode, "Server error: " + sc, thrown);
      } else {
        return new ApiException(ApiErrorCode.CLIENT_REST, statusCode, hscEx.getMessage(), thrown);
      }

    } else if (thrown instanceof UnknownContentTypeException uctEx) {
      logger.error("UnknownContentTypeException");
      int statusCode = uctEx.getStatusCode().value();
      return new ApiException(ApiErrorCode.CLIENT_REST, statusCode, uctEx.getMessage(), thrown);

    } else if (thrown instanceof RestClientResponseException rcrEx) {
      logger.debug("> RestClientResponseException");
      int statusCode = rcrEx.getStatusCode().value();
      return new ApiException(
          ApiErrorCode.CLIENT_REST, statusCode, ApiException.APIEX_CLIENT_REST, thrown);

    } else if (thrown instanceof RestClientException) {
      logger.debug("RestClientException");
      return new ApiException(ApiErrorCode.CLIENT_REST, null, thrown.getMessage(), thrown);

    } else {
      return new ApiException(
          ApiErrorCode.UNKNOWN,
          null,
          "An unexpected error occurred: " + thrown.getClass().getName(),
          thrown);
    }
  }

  //  add pointcuts for Api!, ApiClient and Service methods ?
  //  @Pointcut("execution(public * bm.traccar.invoke.ApiClient.*(..))")
  //  public void ApiClientMethods() {}
  //
  //  @Pointcut("@within(bm.traccar.generated.api.ApiService)")
  //  @Pointcut("@within(org.springframework.stereotype.Service)")
  //  @Pointcut("@target(org.springframework.stereotype.Service)")
  //  public void ServiceMethods() {}
}
