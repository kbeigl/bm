package bm.traccar.rt;

import bm.traccar.api.ApiConfig;
import bm.traccar.api.scenario.ScenarioConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Main configuration for the traccar-realtime-client module.
 *
 * <p>This config lives in main sources so other modules can import it without depending on the
 * {@code @SpringBootApplication} class.
 *
 * <p>When using this library, simply import this configuration:
 *
 * <pre>{@code
 * @SpringBootApplication
 * @Import(RealTimeConfig.class)
 * public class MyApp { }
 * }</pre>
 */
@Configuration
@ComponentScan(basePackages = {"bm.traccar", "bm.traccar.rt", "bm.traccar.ws"})
@Import({ApiConfig.class, ScenarioConfig.class}) // Import dependency configs
public class RealTimeConfig {}
