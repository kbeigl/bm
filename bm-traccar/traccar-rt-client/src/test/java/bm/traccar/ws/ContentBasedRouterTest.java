package bm.traccar.ws;

import static org.junit.jupiter.api.Assertions.*;

import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Position;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultProducerTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Unit tests for EIP content based router (CBR) of expected WebSocket messages from Traccar.
 *
 * <p>Assumption: Traccar only sends one type of message, i.e. 'positions' at a time. <br>
 * TODO: Check and verify in Traccar source.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ContentBasedRouterTest {
  private CamelContext camel;
  private ProducerTemplate producer;
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  /* This CBR does not apply additional logic on the content.
   * Separate route for future extensions (e.g. filtering, transformation).
   * In addtion some messages can be ignored due to outdated infos etc.
   * Events could be raised from selected differences between messages.
   * More insight of 'events' (in relation to entity.fields) required...
   */
  @BeforeAll
  public void setUp() throws Exception {
    camel = new DefaultCamelContext();
    // mapper = new ObjectMapper();
    // mapper.registerModule(new JavaTimeModule());
    camel.addRoutes(
        new RouteBuilder() {
          @Override
          public void configure() {

            onException(JsonParseException.class)
                .handled(true)
                .log("Invalid JSON received: ${exception.message}")
                .to("mock:unknown");

            from("direct:start-cbr")
                .routeId("traccarContentBasedRouting")
                .log("Process WebSocket raw  message: ${body}")
                .unmarshal()
                .json()
                .choice()
                // ===== empty message
                .when(simple("${body.isEmpty()}"))
                .log("Empty message received: ${body}")
                // return null as NAK instead of Ack
                .setBody(simple("${null}"))
                .to("mock:null")
                // ===== positions
                .when(simple("${body[positions]} != null"))
                // . (simple("${body.containsKey('positions')}"))
                .log("positions message received: ${body}")
                .setBody(simple("${body[positions]}"))
                .process(
                    ex -> {
                      Position[] pos = mapper.convertValue(ex.getIn().getBody(), Position[].class);
                      // return empty array as NAK instead of Ack
                      ex.getIn().setBody(pos);
                    })
                .to("mock:positions")
                // ===== events
                .when(simple("${bodyAs(String)} contains 'events'"))
                .log("events message received: ${body}")
                .to("mock:events")
                // ===== unknown (or to be implemented)
                .otherwise()
                .log("unknown message received: ${body}")
                .to("mock:unknown")
                // ===== end() only required, if more processing steps (should) follow
                .end();
          }
        });
    camel.start();
    producer = new DefaultProducerTemplate(camel);
    producer.start();
  }

  String minEvent = "{\"events\":[{\"id\":1}]}";

  @Test
  public void testEventsMessage() throws Exception {
    MockEndpoint mockEvents = camel.getEndpoint("mock:events", MockEndpoint.class);
    mockEvents.expectedMessageCount(1);
    producer.sendBody("direct:start-cbr", minEvent);
    mockEvents.assertIsSatisfied();
  }

  // position test message Strings in json format
  String emptyPositions = "{\"positions\":[]}",
      // one message per array
      fullPosition1 = // dates to verify JavaTimeModule registration
      "{\"positions\":[{\"id\":1,\"attributes\":{\"motion\":true,\"odometer\":0,\"activity\":\"still\",\"batteryLevel\":86,\"distance\":0.0,\"totalDistance\":0.0},\"deviceId\":1,\"protocol\":\"osmand\",\"serverTime\":\"2025-10-08T20:11:05.996+00:00\",\"deviceTime\":\"2025-10-08T20:07:41.037+00:00\",\"fixTime\":\"2025-10-08T20:07:41.037+00:00\",\"valid\":true,\"latitude\":49.4269825,\"longitude\":8.6819525,\"altitude\":-1.0,\"speed\":0.0,\"course\":0.0,\"address\":null,\"accuracy\":0.0,\"network\":null,\"geofenceIds\":null}]}",
      fullPosition2 = // next pos only differs in serverTime 2025-10-08T20:11:05.996
      "{\"positions\":[{\"id\":2,\"attributes\":{\"motion\":true,\"odometer\":0,\"activity\":\"still\",\"batteryLevel\":86,\"distance\":0.0,\"totalDistance\":0.0},\"deviceId\":1,\"protocol\":\"osmand\",\"serverTime\":\"2025-10-08T20:11:06.859+00:00\",\"deviceTime\":\"2025-10-08T20:07:41.037+00:00\",\"fixTime\":\"2025-10-08T20:07:41.037+00:00\",\"valid\":true,\"latitude\":49.4269825,\"longitude\":8.6819525,\"altitude\":-1.0,\"speed\":0.0,\"course\":0.0,\"address\":null,\"accuracy\":0.0,\"network\":null,\"geofenceIds\":null}]}",
      // positions providing no information > ignore?
      // -> assumption: Traccar always sends a full position message, single fields cannot be sent
      minPosition = "{\"positions\":[{\"id\":1}]}",
      timePosition = // next pos only with new serverTime 2025-10-08T20:11:06.859
      "{\"positions\":[{\"id\":2,\"deviceId\":1,\"serverTime\":\"2025-10-08T20:11:06.859+00:00\"}]}";

  @Test
  public void testPositionsMessage() throws Exception {
    MockEndpoint mockPositions = camel.getEndpoint("mock:positions", MockEndpoint.class);

    mockPositions.expectedMessageCount(3);
    producer.sendBody("direct:start-cbr", fullPosition1);
    producer.sendBody("direct:start-cbr", fullPosition2);
    producer.sendBody("direct:start-cbr", emptyPositions);
    mockPositions.assertIsSatisfied();

    Position[] positions1 = mockPositions.getExchanges().get(0).getIn().getBody(Position[].class),
        positions2 = mockPositions.getExchanges().get(1).getIn().getBody(Position[].class),
        positions3 = mockPositions.getExchanges().get(2).getIn().getBody(Position[].class);

    // mockPositions has three position array, last one is empty
    assertEquals(0, positions3.length, "empty array for empty positions message expected");
    // now we can access valid positions
    // server times are swaped ! same fixtime
    assertNotEquals(positions1[0].getServerTime(), positions2[0].getServerTime());

    // these messages are valid, but don't contain infos > extra logic required
    mockPositions.expectedMessageCount(5);
    producer.sendBody("direct:start-cbr", minPosition);
    producer.sendBody("direct:start-cbr", timePosition);
    mockPositions.assertIsSatisfied();

    Position[] positions4 = mockPositions.getExchanges().get(3).getIn().getBody(Position[].class),
        positions5 = mockPositions.getExchanges().get(4).getIn().getBody(Position[].class);

    assertNull(positions4[0].getDeviceId(), "Expected null deviceId for minPosition");
    assertEquals(1, positions5[0].getDeviceId(), "Expected deviceId=1 for timePosition");
  }

  String invalidJson = "asdf345 6e47z", unknownJson = "{\"foo\":[{\"id\":1}]}";

  @Test
  public void testInvalidOrUnknownMessage() throws Exception {
    MockEndpoint mockUnknown = camel.getEndpoint("mock:unknown", MockEndpoint.class);
    mockUnknown.expectedMessageCount(1);
    producer.sendBody("direct:start-cbr", invalidJson);
    mockUnknown.assertIsSatisfied();
    assertEquals(
        invalidJson,
        mockUnknown.getExchanges().get(0).getIn().getBody(String.class),
        "Expected original invalid JSON message");
    System.err.println("Invalid message: " + mockUnknown.getExchanges().get(0).getIn().getBody());

    mockUnknown.expectedMessageCount(2);
    producer.sendBody("direct:start-cbr", unknownJson);
    mockUnknown.assertIsSatisfied();
    System.err.println("Unknown message: " + mockUnknown.getExchanges().get(1).getIn().getBody());
  }

  String emptyJson = "{}";

  /** You cannot send a null body, so we send an empty JSON object instead. */
  @Test
  public void testEmptyMessage() throws Exception {
    MockEndpoint mockNull = camel.getEndpoint("mock:null", MockEndpoint.class);
    mockNull.expectedMessageCount(1);
    producer.sendBody("direct:start-cbr", emptyJson);
    mockNull.assertIsSatisfied();
    assertNull(
        mockNull.getExchanges().get(0).getIn().getBody(),
        "Expected null body for empty JSON message");
  }

  String devicesJson =
      "{\"devices\":[{\"id\":1,\"attributes\":{},\"groupId\":0,\"name\":\"Kristof Mobile\",\"uniqueId\":\"574322\",\"status\":\"online\",\"lastUpdate\":\"2025-10-08T20:11:05.999+00:00\",\"positionId\":0,\"phone\":null,\"model\":\"Samsung A52\",\"contact\":null,\"category\":null,\"disabled\":false}]}";

  // device branch not in place yet
  // @Test
  public void testMessageRequest() {
    Object result = producer.requestBody("direct:start-cbr", devicesJson);
    // unmarshal
    Device[] devices = mapper.convertValue(result, Device[].class);

    assertTrue(result instanceof Device[], "Expected Device[] as result");
    //    Device[] devices = (Device[]) result;
    assertEquals(1, devices.length);
    assertEquals("Kristof Mobile", devices[0].getName());
    assertEquals("574322", devices[0].getUniqueId());
  }

  @AfterAll
  public void tearDown() throws Exception {
    producer.stop();
    camel.stop();
  }
}
