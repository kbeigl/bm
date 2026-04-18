package bm.gps.tracker;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Main configuration for the gps-osmand-tracker module.
 *
 * <p>This config lives in main sources so other modules can import it without depending on the
 * test-jar.
 */
@Configuration
@ComponentScan(basePackages = "bm.gps.tracker")
public class GpsOsmandTrackerConfig {}
