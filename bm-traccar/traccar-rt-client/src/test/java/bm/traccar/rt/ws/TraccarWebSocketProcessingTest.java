package bm.traccar.rt.ws;

import bm.traccar.api.Api;
import bm.traccar.api.ApiConfig;
import bm.traccar.api.ApiService;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Position;
import bm.traccar.ws.TraccarSessionManager;
import bm.traccar.ws.TraccarWebSocketRoute;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@CamelSpringBootTest
@EnableAutoConfiguration
@ContextConfiguration(
    classes = {
      ApiService.class,
      ApiConfig.class,
      TraccarWebSocketRoute.class,
      TraccarSessionManager.class
    })
// DOES NOT extend BaseReaTimeScenarioTest
// no server required, only CBR route
public class TraccarWebSocketProcessingTest {

  @Autowired protected Api api;
  // @Autowired private CamelContext camel;
  @MockitoSpyBean private TraccarWebSocketRoute traccarLiveRoute;
  @Autowired private ProducerTemplate producer;

  // test: device relation to position

  String positionsJson =
      "{\"positions\":[{\"id\":1,\"attributes\":{\"motion\":true,\"odometer\":0,\"activity\":\"still\",\"batteryLevel\":86,\"distance\":0.0,\"totalDistance\":0.0},\"deviceId\":1,\"protocol\":\"osmand\",\"serverTime\":\"2025-10-08T20:11:05.996+00:00\",\"deviceTime\":\"2025-10-08T20:07:41.037+00:00\",\"fixTime\":\"2025-10-08T20:07:41.037+00:00\",\"outdated\":false,\"valid\":true,\"latitude\":49.4269825,\"longitude\":8.6819525,\"altitude\":-1.0,\"speed\":0.0,\"course\":0.0,\"address\":null,\"accuracy\":0.0,\"network\":null,\"geofenceIds\":null}]}";

  String deviceEmpty = "{\"devices\":[]}",
      devicesJson = //  lastUpdate 20:11:05.999
      "{\"devices\":[{\"id\":1,\"attributes\":{},\"groupId\":0,\"name\":\"Kristof Mobile\",\"uniqueId\":\"574322\",\"status\":\"online\",\"lastUpdate\":\"2025-10-08T20:11:05.999+00:00\",\"positionId\":0,\"phone\":null,\"model\":\"Samsung A52\",\"contact\":null,\"category\":null,\"disabled\":false}]}",
      deviceUpdate = // lastUpdate 20:11:06.861
      "{\"devices\":[{\"id\":1,\"attributes\":{},\"groupId\":0,\"name\":\"Kristof Mobile\",\"uniqueId\":\"574322\",\"status\":\"online\",\"lastUpdate\":\"2025-10-08T20:11:06.861+00:00\",\"positionId\":0,\"phone\":null,\"model\":\"Samsung A52\",\"contact\":null,\"category\":null,\"disabled\":false}]}";

  @Test
  public void testDevicesMessage() throws Exception {
    producer.sendBody("direct:start-websocket-connection", devicesJson);
  }

  String emptyJson = "{}";

  /** You cannot send a null body, BUT an empty JSON object. */
  @Test
  public void testEmptyMessage() throws Exception {
    producer.sendBody("direct:start-websocket-connection", emptyJson);
    // verify that the error handler was invoked
  }

  String invalidJson = "asdf345 6e47z";

  @Test
  public void testInvalidMessage() throws Exception {
    producer.sendBody("direct:start-websocket-connection", invalidJson);
    // verify that the error handler was invoked
  }

  // @Test
  public void testWebSocketMessageProcessingRoute_devices_and_positions() {
    Object devicesResult = producer.requestBody("direct:start-websocket-connection", devicesJson);
    Object positionsResult =
        producer.requestBody("direct:start-websocket-connection", positionsJson);

    // Assert devices
    assert devicesResult instanceof Device[] : "Expected Device[] as result";
    Device[] devices = (Device[]) devicesResult;
    assert devices.length == 1 : "Expected 1 device";
    assert devices[0].getName().equals("Kristof Mobile");
    assert devices[0].getUniqueId().equals("574322");

    // Assert positions
    assert positionsResult instanceof Position[] : "Expected Position[] as result";
    Position[] positions = (Position[]) positionsResult;
    assert positions.length == 1 : "Expected 1 position";
    assert positions[0].getDeviceId() == 1;
    //    assert positions[0].getLatitude() == 49.4269825;
    //    assert positions[0].getLongitude() == 8.6819525;
  }
}
