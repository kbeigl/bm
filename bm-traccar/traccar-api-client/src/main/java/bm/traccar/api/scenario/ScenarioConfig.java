package bm.traccar.api.scenario;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
// this causes bean ambiguity with ScenarioProperties
@EnableConfigurationProperties(ScenarioProperties.class)
@PropertySource("classpath:scenario.properties")
public class ScenarioConfig {
  /* empty */
}
