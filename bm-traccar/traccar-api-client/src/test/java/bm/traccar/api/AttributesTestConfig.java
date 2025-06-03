package bm.traccar.api;

import bm.traccar.generated.api.AttributesApi;
import bm.traccar.invoke.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AttributesTestConfig {

  @Bean
  AttributesApi attributesApi(ApiClient apiClient) {
    return new AttributesApi(apiClient);
  }
}
