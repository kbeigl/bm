package bm.traccar.ws;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

// @SpringBootTest
@CamelSpringBootTest
@EnableAutoConfiguration
@TestPropertySource(
    properties = {
      "traccar.user.password=testpassword",
      "traccar.user.email=test@example.com",
      "traccar.host=http://localhost:8082",
      "traccar.websocket.url=ws://localhost:8082/api/socket"
    })
@ContextConfiguration(classes = {TraccarWsClientRoute.class, PositionProcessor.class})
class TraccarWsClientRouteUnitTest {

  @Autowired private CamelContext camel;
  @Autowired private ProducerTemplate producer;
  @MockBean private PositionProcessor positionProcessor;

  @Test
  void testProcessWebSocketMessage() {
    // the first message via wscat comman
    String jsonMessage = "{\"positions\":[]}";
    producer.sendBody("direct:traccarWebSocketMessages", jsonMessage);

    // Then PositionProcessor should be called with a Map containing 'positions'
    Mockito.verify(positionProcessor, Mockito.timeout(1000))
        .processPositions(Mockito.argThat(map -> map != null && map.containsKey("positions")));
  }

  @Test
  void testNullWebSocketMessage() {
    // When sending a null message to the route
    producer.sendBody("direct:traccarWebSocketMessages", null);
    // Then PositionProcessor should NOT be called
    Mockito.verify(positionProcessor, Mockito.after(500).never()).processPositions(Mockito.any());
  }
}
