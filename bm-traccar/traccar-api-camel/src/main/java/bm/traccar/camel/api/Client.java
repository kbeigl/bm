package bm.traccar.camel.api;

import bm.traccar.generated.model.dto.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This simple @SpringBootApplication demonstates how to use the CamelService in your software to
 * connect to Traccar.
 */
@SpringBootApplication
public class Client implements ApplicationRunner {

  @Autowired private CamelService api;

  @Override
  public void run(ApplicationArguments args) throws Exception {

    Device[] devices = api.devices.getById(1);
    System.out.println(devices.length);
  }

  public static void main(String[] args) {
    SpringApplication.run(Client.class, args);
  }
}
