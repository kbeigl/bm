package bm.traccar.rt.scenario;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableConfigurationProperties(ScenarioProperties.class)
@PropertySource("classpath:scenario.properties")
public class ScenarioConfig {
  /* empty */
}
