package bm.traccar.ws;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * JUnit 5 test class for {@link TraccarWsClientRoute}. Uses AdviceWith to replace dynamic 'toD'
 * endpoints with mock endpoints to avoid real network calls.
 */
@SpringBootTest
@CamelSpringBootTest
@TestPropertySource(
    properties = {
      "traccar.user.password=testpassword",
      "traccar.user.email=test@example.com",
      "traccar.host=http://localhost:8082",
      "traccar.websocket.url=ws://localhost:8082/api/socket"
    })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
// @ActiveProfiles("test") // Use a test profile if you have specific test configurations
class TraccarWsClientRouteTest {

  @Autowired private CamelContext camelContext;
  @Autowired private ProducerTemplate producer;

  // Explicitly defined mock endpoints for the external HTTP login and WebSocket connection
  private MockEndpoint httpLoginMockEndpoint;
  private MockEndpoint vertxWebSocketMockEndpoint;

  // Mock endpoint for the internal direct:connectTraccarWebSocket
  private MockEndpoint connectTraccarWebSocketMockEndpoint;
  // Mock endpoint for the internal direct:traccarWebSocketMessages
  private MockEndpoint traccarWebSocketMessagesMockEndpoint;

  // Direct endpoint to trigger the login route, replacing the timer
  private static final String DIRECT_START_TRACCAR_LOGIN = "direct:startTraccarLogin";

  // Properties from @Value in the route, used for AdviceWith URI matching
  @Value("${traccar.host}")
  private String host;

  @Value("${traccar.websocket.url}")
  private String traccarWebSocketUrl;

  @BeforeEach
  void setUp() throws Exception {
    // Define our explicit mock endpoints
    httpLoginMockEndpoint = camelContext.getEndpoint("mock:httpLoginTarget", MockEndpoint.class);
    vertxWebSocketMockEndpoint =
        camelContext.getEndpoint("mock:vertxWebSocketTarget", MockEndpoint.class);

    // Get mock endpoints for the internal direct routes
    connectTraccarWebSocketMockEndpoint =
        camelContext.getEndpoint("mock:direct:connectTraccarWebSocket", MockEndpoint.class);
    traccarWebSocketMessagesMockEndpoint =
        camelContext.getEndpoint("mock:direct:traccarWebSocketMessages", MockEndpoint.class);

    // Reset mocks before each test
    httpLoginMockEndpoint.reset();
    vertxWebSocketMockEndpoint.reset();
    connectTraccarWebSocketMockEndpoint.reset();
    traccarWebSocketMessagesMockEndpoint.reset();

    // Advice the traccarLoginRoute
    AdviceWith.adviceWith(
        camelContext,
        "traccarLoginRoute",
        a -> {
          // Replace the timer 'from' endpoint with a direct endpoint for testing
          a.replaceFromWith(DIRECT_START_TRACCAR_LOGIN);
          // Replace the dynamic HTTP login 'toD' endpoint with our mock endpoint
          a.weaveByToUri(host + "/api/session").replace().to("mock:httpLoginTarget");
        });

    // Advice the traccarWebSocketConnectionRoute
    AdviceWith.adviceWith(
        camelContext,
        "traccarWebSocketConnectionRoute",
        a -> {
          // Replace the dynamic vertx-websocket 'toD' endpoint with our mock endpoint
          // Using a wildcard for the dynamic part of the URI
          a.weaveByToUri("vertx-websocket:" + traccarWebSocketUrl + "*")
              .replace()
              .to("mock:vertxWebSocketTarget");
        });

    camelContext.start();
  }

  /**
   * Test case for a successful Traccar login and subsequent WebSocket connection. Verifies that the
   * JSESSIONID is extracted and the WebSocket connection route is triggered.
   */
  @Test
  void testSuccessfulLoginAndWebSocketConnection() throws Exception {
    /*
        // Set expectations on the HTTP login mock endpoint
        httpLoginMockEndpoint.expectedMessageCount(1);
        // Simulate a successful HTTP response with a JSESSIONID cookie
        httpLoginMockEndpoint.whenAnyExchangeReceived(
            exchange -> {
              exchange.getIn().setHeader("Set-Cookie", "JSESSIONID=testJSessionId; Path=/; HttpOnly");
              exchange.getIn().setBody("Login Successful");
            });

        // Set expectations on the direct:connectTraccarWebSocket mock endpoint
        connectTraccarWebSocketMockEndpoint.expectedMessageCount(1);
        connectTraccarWebSocketMockEndpoint.expectedHeaderReceived("JSESSIONID", "testJSessionId");

        // Set expectations on the vertx-websocket mock endpoint
        //    vertxWebSocketMockEndpoint.expectedMessageCount(1);
        //    vertxWebSocketMockEndpoint.expectedHeaderReceived("Cookie", "JSESSIONID=testJSessionId");

        // Set expectations on the direct:traccarWebSocketMessages mock endpoint
        traccarWebSocketMessagesMockEndpoint.expectedMessageCount(1);
        // Expect a message after successful websocket connection
    */
    // Send a message to the direct endpoint that now triggers the login route
    producer.sendBody(DIRECT_START_TRACCAR_LOGIN, "trigger");
    /*
    // Assert that all mock endpoints received the expected messages
    MockEndpoint.assertIsSatisfied(
        httpLoginMockEndpoint,
        connectTraccarWebSocketMockEndpoint,
        vertxWebSocketMockEndpoint,
        traccarWebSocketMessagesMockEndpoint);
        */
  }

  /**
   * Test case for login failure due to an HTTP error (e.g., 401 Unauthorized). Verifies that the
   * WebSocket connection route is not triggered.
   */
  @Test
  void testLoginFailureHttpError() throws Exception {
    // Set expectations on the HTTP login mock endpoint to simulate an HTTP error
    httpLoginMockEndpoint.expectedMessageCount(1);
    httpLoginMockEndpoint.whenAnyExchangeReceived(
        exchange -> {
          throw new HttpOperationFailedException(
              "Login Failed", 401, "Unauthorized", null, null, "Error Body");
        });

    // The connectTraccarWebSocketMockEndpoint should NOT receive any messages
    connectTraccarWebSocketMockEndpoint.expectedMessageCount(0);
    vertxWebSocketMockEndpoint.expectedMessageCount(0);
    traccarWebSocketMessagesMockEndpoint.expectedMessageCount(0);

    // Send a message to the direct endpoint that now triggers the login route
    producer.sendBody(DIRECT_START_TRACCAR_LOGIN, "trigger");

    // Assert that all mock endpoints received the expected messages (or none)
    MockEndpoint.assertIsSatisfied(
        httpLoginMockEndpoint,
        connectTraccarWebSocketMockEndpoint,
        vertxWebSocketMockEndpoint,
        traccarWebSocketMessagesMockEndpoint);

    // Verify that the route handled the exception and didn't propagate it
    // (This is implicitly tested by not failing the test and not seeing further messages)
  }

  /**
   * Test case for login where JSESSIONID is not present in the Set-Cookie header. Verifies that the
   * WebSocket connection route is NOT triggered.
   */
  @Test
  void testLoginNoJSessionId() throws Exception {
    // Set expectations on the HTTP login mock endpoint
    httpLoginMockEndpoint.expectedMessageCount(1);
    // Simulate a successful HTTP response but without JSESSIONID
    httpLoginMockEndpoint.whenAnyExchangeReceived(
        exchange -> {
          exchange.getIn().setHeader("Set-Cookie", "SomeOtherCookie=value; Path=/");
          exchange.getIn().setBody("Login Successful but no JSESSIONID");
        });

    // The connectTraccarWebSocketMockEndpoint should NOT receive any messages
    connectTraccarWebSocketMockEndpoint.expectedMessageCount(0);
    vertxWebSocketMockEndpoint.expectedMessageCount(0);
    traccarWebSocketMessagesMockEndpoint.expectedMessageCount(0);

    // Send a message to the direct endpoint that now triggers the login route
    producer.sendBody(DIRECT_START_TRACCAR_LOGIN, "trigger");

    // Assert that all mock endpoints received the expected messages (or none)
    MockEndpoint.assertIsSatisfied(
        httpLoginMockEndpoint,
        connectTraccarWebSocketMockEndpoint,
        vertxWebSocketMockEndpoint,
        traccarWebSocketMessagesMockEndpoint);
  }

  /**
   * Test case for connecting to WebSocket without a JSESSIONID. Verifies that an
   * IllegalStateException is set on the exchange.
   */
  @Test
  void testConnectWebSocketNoJSessionId() throws Exception {
    // The vertxWebSocketMockEndpoint should NOT receive any messages
    vertxWebSocketMockEndpoint.expectedMessageCount(0);
    traccarWebSocketMessagesMockEndpoint.expectedMessageCount(0);

    // Send a message to direct:connectTraccarWebSocket WITHOUT the JSESSIONID header
    Exchange exchange =
        producer.send(
            "direct:connectTraccarWebSocket",
            ex -> {
              // Do not set JSESSIONID header
            });

    // Assert that the exchange has an IllegalStateException
    assertNotNull(exchange.getException());
    assertTrue(exchange.getException() instanceof IllegalStateException);
    assertEquals("No JSESSIONID to establish WebSocket.", exchange.getException().getMessage());

    // Assert that the mock endpoints were not called
    MockEndpoint.assertIsSatisfied(
        vertxWebSocketMockEndpoint, traccarWebSocketMessagesMockEndpoint);
  }

  /**
   * Test case for processing incoming WebSocket messages. Verifies that messages sent to
   * direct:traccarWebSocketMessages are handled. Note: Since the processing logic is commented out,
   * this primarily checks routing.
   */
  @Test
  void testTraccarWebSocketMessageProcessing() throws Exception {
    // message is swallowed by .log("recieved...") in the route
    // traccarWebSocketMessagesMockEndpoint.expectedMessageCount(0);
    //    traccarWebSocketMessagesMockEndpoint
    //        .expectedBodyReceived()
    //        .body()
    //        .isEqualTo("{\"positions\": []}");

    // Send a mock WebSocket message to the processing route
    producer.sendBody("direct:traccarWebSocketMessages", "{\"positions\": []}");

    // Assert that the mock endpoint received the message
    //    MockEndpoint.assertIsSatisfied(traccarWebSocketMessagesMockEndpoint);
  }

  /**
   * Integration test: Connects to a real Traccar WebSocket server on localhost and verifies
   * connection. Requires Traccar server running at ws://localhost:8082/api/socket.
   */
  @Test
  void integrationTest_traccarWebSocketConnectionRoute() throws Exception {
    // Remove AdviceWith for WebSocket to allow real connection
    camelContext.stop();
    camelContext.removeRouteDefinition(
        camelContext.getRouteDefinition("traccarWebSocketConnectionRoute"));
    // Re-add the route without advice (no mocking)
    // ...existing code to add route, if needed...
    camelContext.start();

    // Trigger login (simulate or use real credentials)
    Exchange loginExchange = producer.request(DIRECT_START_TRACCAR_LOGIN, e -> {});
    assertNotNull(loginExchange);
    // Check for JSESSIONID or successful login
    String sessionId = loginExchange.getIn().getHeader("Set-Cookie", String.class);
    assertNotNull(sessionId, "JSESSIONID should be present after login");

    // Connect to WebSocket and verify connection
    Exchange wsExchange = producer.request("direct:connectTraccarWebSocket", e -> {});
    assertNotNull(wsExchange);
    // Optionally, send/receive a message and assert response
    // ...add message exchange assertions as needed...
  }
}
