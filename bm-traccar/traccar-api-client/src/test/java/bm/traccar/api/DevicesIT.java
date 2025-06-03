package bm.traccar.api;

import bm.traccar.generated.model.dto.Device;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Test <code>api/devices</code> methods, @see <a
 * href="https://www.traccar.org/api-reference/#tag/Devices">DevicesApi</a> with ApiService. <br>
 * Create, update, delete Devices for Users in traccar datamodel.
 */
@SpringBootTest
@EnableAutoConfiguration
@ContextConfiguration(classes = {ApiService.class})
@TestPropertySource("classpath:application.properties")
@Import(ApiConfig.class)
@ExtendWith(ClientExceptionHandler.class)
public class DevicesIT {

  // convenience for authentication
  @Autowired
  @Value("${traccar.web.serviceAccountToken}")
  private String token;

  @Autowired private ApiService api;

  /*
   * storyboard create device > who does it belong create user > create device for user access device as user and as
   * admin/superuser
   */

  @Test
  public void getDevices() {
    api.setBearerToken(token);
    List<Device> devices = api.devices.getDevices(null);
  }
}
