package bm.traccar.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TraccarClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(TraccarClientApplication.class, args);
    System.out.println(
        "Traccar WebSocket Client Anwendung gestartet. Überprüfen Sie die Logs auf Verbindungsstatus und empfangene Nachrichten.");
  }
}
