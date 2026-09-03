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

import bm.traccar.generated.api.DevicesApi;
import bm.traccar.generated.api.PermissionsApi;
import bm.traccar.generated.api.ServerApi;
import bm.traccar.generated.api.SessionApi;
import bm.traccar.generated.api.UsersApi;
import bm.traccar.invoke.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class ApiConfig {

  @Value("${traccar.host:http://localhost}") // :80
  private String basePath;

  @Bean
  ApiClient apiClient() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(basePath + "/api");
    return apiClient;
  }

  @Bean
  UsersApi usersApi(ApiClient apiClient) {
    return new UsersApi(apiClient);
  }

  @Bean
  SessionApi sessionApi(ApiClient apiClient) {
    return new SessionApi(apiClient);
  }

  @Bean
  DevicesApi devicesApi(ApiClient apiClient) {
    return new DevicesApi(apiClient);
  }

  @Bean
  ServerApi serverApi(ApiClient apiClient) {
    return new ServerApi(apiClient);
  }

  @Bean
  PermissionsApi permissionsApi(ApiClient apiClient) {
    return new PermissionsApi(apiClient);
  }
}
