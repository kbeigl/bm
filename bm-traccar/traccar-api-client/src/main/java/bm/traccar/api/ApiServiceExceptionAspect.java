package bm.traccar.api;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aspect for handling exceptions thrown by ApiService methods. Logs exceptions and rethrows as
 * ApiException for centralized error handling.
 */
// @Aspect
// @Component
public class ApiServiceExceptionAspect {
  private static final Logger logger = LoggerFactory.getLogger(ApiServiceExceptionAspect.class);

  /** Pointcut matches all public methods in ApiService. */
  @Around("execution(public * bm.traccar.api.ApiService.*(..))")
  public Object handleApiServiceExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
    try {
      return joinPoint.proceed();
    } catch (Exception ex) {
      logger.error(
          "Exception in {}.{}: {}",
          joinPoint.getSignature().getDeclaringTypeName(),
          joinPoint.getSignature().getName(),
          ex.getMessage(),
          ex);

      // Optionally, wrap in a custom exception if needed
      // throw new RuntimeException("Error in ApiService: " + ex.getMessage(), ex);
      throw new ApiException("Error in ApiService: " + ex.getMessage(), ex);
    }
  }
}
