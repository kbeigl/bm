package bm.traccar.api.scenario;

import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.api.Api;
import bm.traccar.api.ApiConfig;
import bm.traccar.api.ApiService;
import bm.traccar.generated.model.dto.Server;
import bm.traccar.generated.model.dto.User;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(
    classes = {ApiService.class, ApiConfig.class, ScenarioLoader.class, ScenarioConfig.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScenarioLoaderIT {
  private static final Logger logger = LoggerFactory.getLogger(ScenarioLoaderIT.class);

  @Autowired protected Api api;
  @Autowired ScenarioLoader scenarioSetup;

  @Test
  @Order(1)
  void shouldSetupScenario() {
    scenarioSetup.setupScenario();
    // move into scenarioSetup and remove api from here?
    // we are admin now
    // verify server settings
    Server server = api.getServerApi().getServerInfo();
    // logger.info("Server Info: {}", server);
    assertTrue(server.getRegistration(), "Server registration should be enabled");

    // verify users
    String adminId = scenarioSetup.admin.getId().toString();
    User serverAdmin = api.getUsersApi().getUserById(adminId);
    logger.info(
        "Found Admin User.id{}: {}/{}",
        serverAdmin.getId(),
        serverAdmin.getName(),
        serverAdmin.getEmail());
  }

  // this does not run without setup, due to authentication
  @Test
  @Order(2)
  void shouldTeardownScenario() {
    scenarioSetup.teardownScenario();
  }
}
