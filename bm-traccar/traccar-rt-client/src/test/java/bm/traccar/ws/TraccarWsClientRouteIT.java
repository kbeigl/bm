package bm.traccar.ws;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@CamelSpringBootTest
@EnableAutoConfiguration
// @ActiveProfiles("test")
@ContextConfiguration(classes = {TraccarWsClientRoute.class})
public class TraccarWsClientRouteIT {

  @Autowired private CamelContext camelContext;

  // @Autowired private ProducerTemplate producerTemplate;

  @Autowired
  @Qualifier("traccarWsClientRoute")
  private TraccarWsClientRoute traccarWsClientRoute;

  //  @Test
  public void testLoginAndConnect() throws Exception {
    String testEmail = "admin@domain.com";
    String testPassword = "admin";

    // Check that the login route is registered and started
    var loginRouteStatus = camelContext.getRouteController().getRouteStatus("traccarLoginRoute");
    assertThat(loginRouteStatus).as("traccarLoginRoute should be registered").isNotNull();
    assertThat(loginRouteStatus.isStarted()).as("traccarLoginRoute should be started").isTrue();

    // Now trigger login and WebSocket connection
    traccarWsClientRoute.loginAndConnect(testEmail, testPassword);

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
