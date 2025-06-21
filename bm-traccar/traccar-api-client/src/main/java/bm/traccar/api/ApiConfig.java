package bm.traccar.api;

import bm.traccar.generated.api.DevicesApi;
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

  @Value("${traccar.host:https://demo3.traccar.org}")
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
}
