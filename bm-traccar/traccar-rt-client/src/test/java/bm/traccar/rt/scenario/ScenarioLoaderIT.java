package bm.traccar.rt.scenario;

import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.api.Api;
import bm.traccar.api.ApiConfig;
import bm.traccar.api.ApiService;
import bm.traccar.generated.model.dto.Server;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(
    classes = {ApiService.class, ApiConfig.class, ScenarioConfig.class, ScenarioLoader.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScenarioLoaderIT {
  private static final Logger logger = LoggerFactory.getLogger(ScenarioLoaderIT.class);

  @Autowired protected Api api;
  @Autowired ScenarioLoader scenarioSetup;

  @Value("${traccar.web.serviceAccountToken}")
  protected String virtualAdmin;

  @Test
  @Order(1)
  void shouldSetupScenario() {
    scenarioSetup.setupScenario();
    // we are admin now
    // verify server settings
    Server server = api.getServerApi().getServerInfo();
    // logger.info("Server Info: {}", server);
    assertTrue(server.getRegistration(), "Server registration should be enabled");

    // verify users
    String adminId = scenarioSetup.admin.getId().toString();
    api.getUsersApi().getUsers(adminId).stream()
        .forEach(u -> logger.info("User{}: {}", u.getId(), u.getEmail()));
  }

  @Test
  @Order(2)
  void shouldTeardownScenario() {
    scenarioSetup.teardownScenario();
  }
}
