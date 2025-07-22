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
class TraccarWsClientRouteFullTest {

  @Autowired private CamelContext camel;
  @Autowired private ProducerTemplate producer;
  @MockBean private PositionProcessor positionProcessor;

  @Test
  void testConnectTraccarWebSocketRoute() {
    String sessionId = "testJSessionId";
    producer.sendBodyAndHeader("direct:connectTraccarWebSocket", null, "JSESSIONID", sessionId);
  }

  @Test
  void testTraccarWebSocketMessageProcessingRoute_withValidMessage() {
    String jsonMessage = "{\"positions\":[]}";
    producer.sendBody("direct:traccarWebSocketMessages", jsonMessage);
    Mockito.verify(positionProcessor, Mockito.timeout(1000))
        .processPositions(Mockito.argThat(map -> map != null && map.containsKey("positions")));
  }

  @Test
  void testTraccarWebSocketMessageProcessingRoute_withNullMessage() {
    // Send a null message to the second route
    producer.sendBody("direct:traccarWebSocketMessages", null);
    // Verify PositionProcessor is NOT called
    Mockito.verify(positionProcessor, Mockito.after(500).never()).processPositions(Mockito.any());
  }
}
