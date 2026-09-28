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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.ResourceAccessException;

/**
 * Verifies the contract "import ApiConfig == you get ApiException and sub-API components" without a
 * Traccar server.
 *
 * <p>Only ApiConfig is loaded. The base path points to a closed port, so every call fails with a
 * ResourceAccessException which the ApiAspect must wrap into an ApiException.
 */
@SpringBootTest(properties = {"traccar.host=http://localhost:1", "spring.main.banner-mode=off"})
@ContextConfiguration(classes = {ApiConfig.class})
@DirtiesContext
class ApiConfigAspectTest {

  @Autowired private Api api;
  @Autowired private Api.Users usersSubApi;
  @Autowired private Api.Devices devicesSubApi;
  @Autowired private bm.traccar.generated.api.UsersApi usersApi;

  @Test
  void subApiComponentsAreRegisteredAndInjected() {
    assertNotNull(api, "ApiService should be registered");
    assertNotNull(usersSubApi, "Api.Users component should be directly injectable");
    assertNotNull(devicesSubApi, "Api.Devices component should be directly injectable");
  }

  @Test
  void generatedApiBeansAreProxiedByApiConfig() {
    assertTrue(AopUtils.isAopProxy(usersApi), "UsersApi must be an AOP proxy via ApiConfig");
  }

  @Test
  void connectionFailureIsTranslatedToApiException() {
    api.setBearerToken("irrelevant");
    ApiException ex =
        assertThrows(ApiException.class, () -> api.getUsersApi().getUsers(null, null, null, null));
    assertEquals(ApiException.APIEX_NO_CONNECTION, ex.getMessage());
    assertEquals(ApiErrorCode.NO_CONNECTION, ex.getErrorCode());
    assertEquals(null, ex.getStatusCode());
    assertTrue(ex.getCause() instanceof ResourceAccessException);
  }
}
