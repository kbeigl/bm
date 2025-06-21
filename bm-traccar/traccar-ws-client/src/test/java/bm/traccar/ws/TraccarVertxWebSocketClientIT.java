package bm.traccar.ws;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
// ,classes = TraccarClientApplication.class)
@CamelSpringBootTest
public class TraccarVertxWebSocketClientIT {

  @Autowired private CamelContext camelContext;

  //  @Test
  void testTraccarWebSocketConnectionAndReceivesMessages() throws Exception {
    // get MockEndpoint for incoming WebSocket-meassages.

    // this mock doesn't work? message "Connect" is recieved by original .log in route

    // mock 'log:receivedTraccarData' endpoint of the route
    MockEndpoint messagesReceiverMock =
        camelContext.getEndpoint("mock:receivedTraccarData", MockEndpoint.class);

    // Erwartet, dass mindestens eine Nachricht innerhalb eines Timeouts empfangen wird.
    // Dies erfordert, dass ein Gerät tatsächlich Daten an den Traccar-Server sendet.
    messagesReceiverMock.expectedMinimumMessageCount(1);
    //    messagesReceiverMock.setResultWaitTime(10 * 60 * 1000); // bis zu x Minuten

    System.out.println("--- Starte Traccar WebSocket Integrationstest ---");

    // Assert that the mock endpoint received the expected messages within the timeout.
    messagesReceiverMock.assertIsSatisfied();

    System.out.println("Mindestens eine WebSocket-Nachricht erfolgreich empfangen!");

    // Optional: Überprüfen Sie den Inhalt der empfangenen Nachricht
    Exchange receivedExchange = messagesReceiverMock.getExchanges().get(0);
    Object receivedBody = receivedExchange.getIn().getBody();

    assertNotNull(receivedBody, "Empfangener Body sollte nicht null sein.");
    assertTrue(
        receivedBody instanceof Map,
        "Empfangener Body sollte eine Map sein (nach JSON Unmarshalling).");

    @SuppressWarnings("unchecked")
    Map<String, Object> bodyMap = (Map<String, Object>) receivedBody;

    // Traccar-Nachrichten enthalten typischerweise 'devices', 'positions' oder 'events'.
    assertTrue(
        bodyMap.containsKey("devices")
            || bodyMap.containsKey("positions")
            || bodyMap.containsKey("events"),
        "Die Nachricht sollte 'devices', 'positions' oder 'events' enthalten.");

    System.out.println(
        "Integrationstest erfolgreich: Traccar WebSocket-Client verbunden und Nachrichten empfangen.");
  }
}
