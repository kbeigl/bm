package bm.traccar.api;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  @Pointcut("execution(public * bm.traccar.generated.api.*Api.*(..))")
  // @Pointcut( "within(bm.traccar.generated.api.*Api)")
  public void ApiMethods() { // throws
  }

  /**
   * ALWAYS throw ApiException from this advice when RestClientException from RestTemplate triggers!
   * All paths lead to 'throw new ApiException(...)', otherwise the original Exception would
   * propagate to the caller.
   */
  // we'll see if it makes sense to branch to different -Exceptions to create
  // individual messages for APIuser.
  @AfterThrowing(pointcut = "ApiMethods()", throwing = "rcEx")
  // public void afterThrowingApiMethod(JoinPoint joinPoint, Throwable ex) throws
  // ApiException {
  public void afterThrowingApiMethod(JoinPoint joinPoint, RestClientException rcEx)
      throws ApiException {

    String shortMethod = joinPoint.getSignature().toShortString();
    String excepMessage = rcEx.getMessage(); // equal for all subtypes
    logger.debug("RestClientException in ApiMethod {}: {}", shortMethod, excepMessage); // , rcEx);

    // RestClientException has three subtypes
    if (rcEx instanceof ResourceAccessException raEx) { // implicit cast
      logger.error("ResourceAccessException: {}", excepMessage);
      throw new ApiException(ApiException.APIEX_NO_CONNECTION, raEx);

    } else if (rcEx instanceof RestClientResponseException rcrEx) {
      logger.error("RestClientResponseException: {}", excepMessage);

      if (rcrEx instanceof HttpStatusCodeException hscEx) {
        logger.error("HttpStatusCodeException: {}", excepMessage);
        // same code for client/server subtypes?
        int httpStatusCode = hscEx.getStatusCode().value();
        // logger.error("Reason: " + hscEx.getResponseBodyAsString());
        // logger.error("Response headers: " + hscEx.getResponseHeaders());

        // HttpStatusCodeException has two subtypes client/server
        if (hscEx instanceof HttpClientErrorException hceEx) {
          logger.error("HttpClientErrorException: {}", excepMessage);
          // > different types Unauthorized etc.
          throw new ApiException("HttpClientError status code: " + httpStatusCode, hceEx);

        } else if (hscEx instanceof HttpServerErrorException hseEx) {
          // > different types InternalServerError etc.
          logger.error("HttpServerErrorException: {}", excepMessage);
          throw new ApiException("HttpServerError status code: " + httpStatusCode, hseEx);
        }

        // does this ever occur?
        throw new ApiException("HttpStatusCodeException", hscEx);
      } else {
        // does this ever occur?
        throw new ApiException(ApiException.APIEX_CLIENT_RESPONSE, rcEx);
      }

    } else if (rcEx instanceof UnknownContentTypeException uctEx) {
      logger.error("UnknownContentTypeException: {}", uctEx.getMessage());
      throw new ApiException("UnknownContentTypeException", uctEx);
    }
    // should not occur
    // if (!(rcEx instanceof ApiException)) {
    // logger.error("Generic exception {}: ", rcEx.getMessage());
    throw new ApiException("An unexpected error occurred!", rcEx);
  }

  /*
   * @Around("ApiMethods()") public Object logApiMethod(ProceedingJoinPoint pjp) throws Throwable { MethodSignature
   * signature = (MethodSignature) pjp.getSignature(); logger.info("invoke {}", signature.toShortString()); try {
   * return pjp.proceed(); } catch (Throwable t) { logger.error("Exception in method: {}", signature.toShortString(),
   * t.getMessage(), t); throw t; } }
   * @Pointcut("execution(public * bm.traccar.invoke.ApiClient.*(..))") public void ApiClientMethods() { }
   * @Pointcut("@within(bm.traccar.generated.api.ApiService)")
   * @Pointcut("@within(org.springframework.stereotype.Service)")
   * @Pointcut("@target(org.springframework.stereotype.Service)") public void ServiceMethods() { }
   * @Around("ApiClientMethods()") public Object logApiClientMethod(ProceedingJoinPoint pjp) throws Throwable {
   * MethodSignature signature = (MethodSignature) pjp.getSignature(); logger.info("invoke {}",
   * signature.toShortString()); logger.info("invoke {} with args: {}", signature.toShortString(),
   * Arrays.toString(pjp.getArgs())); try { return pjp.proceed(); } catch (Throwable t) {
   * logger.error("Exception in method: {}", signature.toShortString(), t.getMessage()); throw t; } }
   * @AfterThrowing(pointcut = "ApiClientMethods()", throwing = "ex") public void
   * afterThrowingApiClientMethod(JoinPoint joinPoint, Throwable ex) { logger.
   * error("Exception in ApiClientMethod: {} with cause: {} \n Exception: {}",
   * joinPoint.getSignature().toShortString(), ex.getMessage(), ex); }
   */
}
