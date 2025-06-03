package bm.traccar.camel.api;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  @Bean
  CamelContextConfiguration contextConfiguration() {
    return new CamelContextConfiguration() {
      @Override
      public void beforeApplicationStart(CamelContext camelContext) {
        camelContext.getGlobalOptions().put("CamelJacksonEnableTypeConverter", "true");
        camelContext.getGlobalOptions().put("CamelJacksonTypeConverterToPojo", "true"); // required?
      }

      @Override
      public void afterApplicationStart(CamelContext camelContext) {}
    };
  }
}
