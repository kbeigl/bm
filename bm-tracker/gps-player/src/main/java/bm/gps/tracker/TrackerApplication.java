package bm.gps.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// @ComponentScan(basePackages = {"bm.tracker.gpstracker"})
public class TrackerApplication {
  public static void main(String[] args) {
    SpringApplication.run(TrackerApplication.class, args);
  }
}
