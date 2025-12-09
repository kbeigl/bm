package bm.tracker.gpstracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// @ComponentScan(basePackages = {"bm.tracker.gpstracker"})
public class GpsTrackerApplication {
  public static void main(String[] args) {
    SpringApplication.run(GpsTrackerApplication.class, args);
  }
}
