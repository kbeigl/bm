package bm.gps.player;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"bm.gps.player", "bm.gps.gpx"})
public class PlayerApplication {
  public static void main(String[] args) {
    SpringApplication.run(PlayerApplication.class, args);
  }
}
