package bm.gps.tracker;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {"bm.gps.tracker"})
@Import(TrackerRegistration.class)
public class GpsOsmandTrackerTestConfig {}
