package bm.traccar;

import bm.gps.tracker.TrackerOsmandConfig;
import bm.traccar.rt.RealTimeConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({RealTimeConfig.class, TrackerOsmandConfig.class})
public class RealTimeApp {

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(RealTimeApp.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    app.run(args);
  }
}
