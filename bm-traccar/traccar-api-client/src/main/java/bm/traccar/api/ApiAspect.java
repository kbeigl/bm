package bm.traccar.api;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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

  // all public methods
  // @Pointcut("execution(public * bm.traccar.generated.api.*Api.*(..))")
  // all join points (methods, constructors, etc.)
  @Around("within(bm.traccar.generated.api.*Api)")
  public Object aroundApiMethods(ProceedingJoinPoint pjp) throws Throwable {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    try {
      logger.debug("ApiAspect invoke {}", signature.toShortString());
      return pjp.proceed();
    } catch (Throwable t) {
      throw new ApiException(createApiExeptionMessage(t), t);
    }
  }

  /** Evaluate the thrown exception and create helpful Api message. */
  private String createApiExeptionMessage(Throwable thrown) {

    // assertion (use as parameter later?)
    if (!(thrown instanceof RestClientException)) {
      return "An unexpected error occurred: " + thrown.getClass().getName();
    }

    logger.debug("RestClientException");
    // RestClientException has three subtypes
    // I/O errors (e.g., connection refused, timeout)
    if (thrown instanceof ResourceAccessException) {
      logger.debug("ResourceAccessException");
      return ApiException.APIEX_NO_CONNECTION;

    } else if (thrown instanceof RestClientResponseException) {
      logger.debug("> RestClientResponseException");

      if (thrown instanceof HttpStatusCodeException) {
        HttpStatusCodeException hscEx = (HttpStatusCodeException) thrown;
        logger.debug("  > HttpStatusCodeException");
        // logger.error("Reason: " + hscEx.getResponseBodyAsString());
        // logger.error("Response headers: " + hscEx.getResponseHeaders());

        // HttpStatusCodeException has two subtypes client/server
        if (hscEx instanceof HttpClientErrorException) {
          logger.debug("    > HttpClientErrorException");
          return createHttpClientMessage(hscEx);

        } else if (hscEx instanceof HttpServerErrorException) {
          logger.debug("    > HttpServerErrorException");
          return createHttpServerMessage(hscEx.getStatusCode());
        }

        // does this ever occur?
        return hscEx.getMessage();
      } else {
        // does this ever occur?
        return ApiException.APIEX_CLIENT_REST;
      }

    } else if (thrown instanceof UnknownContentTypeException uctEx) {
      logger.error("UnknownContentTypeException");
      return uctEx.getMessage();
    }
    // should not occur, circular?
    // if (!(rcEx instanceof ApiException)) {
    // logger.error("Generic exception {}: ", rcEx.getMessage());
    return thrown.getMessage();
  }

  /**
   * Create a message for HttpClientErrorException with specific status code. Should be in
   * 4xxClientErrors.
   */
  private String createHttpClientMessage(HttpStatusCodeException hscEx) {

    HttpStatusCode sc = hscEx.getStatusCode();
    logger.debug("HttpClientErrorException: " + sc);
    // should not occur in Exception environment
    //	    if (sc.is2xxSuccessful())    return sc.toString();
    // else if (sc.is1xxInformational()) return "Informational response: " + sc;
    // else if (sc.is3xxRedirection())   return "Redirection response: " + sc;
    if (sc.is4xxClientError()) {
      if (sc.value() == 400) return ApiException.APIEX_BAD_REQUEST;
      else if (sc.value() == 401) return ApiException.APIEX_UNAUTHORIZED;
      else if (sc.value() == 403) return ApiException.APIEX_UNAUTHORIZED;
      // else if (sc.value() == 402) return "Payment Required: " + sc;
      // else if (sc.value() == 404) return "Not Found: " + sc;
      // else if (sc.value() == 405) return "Method Not Allowed: " + sc;
      // else if (sc.value() == 406) return "Not Acceptable: " + sc;
      // else if (sc.value() == 407) return "Proxy Authentication Required: " + sc;
      // else if (sc.value() == 408) return "Request Timeout: " + sc;
      // else if (sc.value() == 409) return "Conflict: " + sc;
      // else if (sc.value() == 410) return "Gone: " + sc;
      // else if (sc.value() == 411) return "Length Required: " + sc;
      // else if (sc.value() == 412) return "Precondition Failed: " + sc;
      // else if (sc.value() == 413) return "Payload Too Large: " + sc;
      // else if (sc.value() == 414) return "URI Too Long: " + sc;
      // else if (sc.value() == 415) return "Unsupported Media Type: " + sc;
      // else if (sc.value() == 416) return "Range Not Satisfiable: " + sc;
      // else if (sc.value() == 417) return "Expectation Failed: " + sc;
      // else if (sc.value() == 422) return "Unprocessable Entity: " + sc; // validation error
    } // else
    return hscEx.getMessage(); // use original message
  }

  private String createHttpServerMessage(HttpStatusCode sc) {

    if (sc.is5xxServerError()) {
      return "Server error: " + sc;
    }
    return "unknown status code; " + sc;
  }

  //  add pointcuts for ApiClient and Service methods ?
  //  @Pointcut("execution(public * bm.traccar.invoke.ApiClient.*(..))")
  //  public void ApiClientMethods() {}
  //
  //  @Pointcut("@within(bm.traccar.generated.api.ApiService)")
  //  @Pointcut("@within(org.springframework.stereotype.Service)")
  //  @Pointcut("@target(org.springframework.stereotype.Service)")
  //  public void ServiceMethods() {}
}
