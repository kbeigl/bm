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
