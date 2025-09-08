package bm.traccar.ws;

import static org.assertj.core.api.Assertions.assertThat;

import bm.traccar.api.ApiConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@CamelSpringBootTest
@EnableAutoConfiguration
@Import(ApiConfig.class)
@ContextConfiguration(classes = {TraccarWsRoute.class})
public class TraccarWsRouteIT { // extends BaseIntegrationTest {
  // private static final Logger logger = LoggerFactory.getLogger(TraccarWsRouteIT.class);

  @Autowired private CamelContext camelContext;

  @Autowired private TraccarWsRoute traccarWsRoute;

  // MUST CREATE ADMIN BEFORE RUNNING TEST
  // @Test
  public void testLoginAndConnect() throws Exception {

    // Check that the login route is registered and started
    var loginRouteStatus = camelContext.getRouteController().getRouteStatus("traccarLoginRoute");
    assertThat(loginRouteStatus).as("traccarLoginRoute should be registered").isNotNull();
    assertThat(loginRouteStatus.isStarted()).as("traccarLoginRoute should be started").isTrue();

    // Now trigger login and WebSocket connection
    traccarWsRoute.loginAndConnect("admin@domain.com", "admin");

    // Wait for the dynamic WebSocket route to be created and started
    boolean wsStarted = false;
    for (int i = 0; i < 30; i++) { // up to 3 seconds
      var wsStatus =
          camelContext.getRouteController().getRouteStatus("traccarWebSocketDynamicRoute");
      if (wsStatus != null && wsStatus.isStarted()) {
        wsStarted = true;
        break;
      }
      Thread.sleep(100);
    }
    assertThat(wsStarted).as("traccarWebSocketDynamicRoute should be started after login").isTrue();
  }
}
