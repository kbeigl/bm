package bm.traccar.rt;

import static org.assertj.core.api.Assertions.assertThat;

import bm.traccar.RealTimeClient;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class RealTimeControllerIT extends BaseReaTimeScenarioTest {
  private static final Logger logger = LoggerFactory.getLogger(RealTimeControllerIT.class);

  @MockBean private RealTimeClient mockRealTimeClient;

  @Autowired protected RealTimeController controller;
  @Autowired private CamelContext camel;

  @Test
  void controllerTest() {
    listAllBeansInTestContext();

    try {
      // Check that the login route is registered and started before triggering
      var loginRouteStatus = camel.getRouteController().getRouteStatus("traccarLoginRoute");
      assertThat(loginRouteStatus).as("traccarLoginRoute should be registered").isNotNull();
      assertThat(loginRouteStatus.isStarted()).as("traccarLoginRoute should be started").isTrue();

      if (controller.loginAndInitialize(scenario.admin)) {
        logger.debug("RealTimeController initialized successfully.");
      } else logger.error("RealTimeController failed to initialize.");

      // Wait for the dynamic WebSocket route to be started
      boolean wsStarted = false;
      for (int i = 0; i < 30; i++) { // up to 3 seconds
        var wsStatus = camel.getRouteController().getRouteStatus("traccarWebSocketDynamicRoute");
        if (wsStatus != null && wsStatus.isStarted()) {
          wsStarted = true;
          break;
        }
        Thread.sleep(100);
      }
      assertThat(wsStarted)
          .as("traccarWebSocketDynamicRoute should be started after login")
          .isTrue();
    } catch (Exception e) {
      logger.error("Exception during RealTimeController initialization: ", e);
      throw new RuntimeException(e);
    }
    try {
      Thread.sleep(5 * 1000L);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
