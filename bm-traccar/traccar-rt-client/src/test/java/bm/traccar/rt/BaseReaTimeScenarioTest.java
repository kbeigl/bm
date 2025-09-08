package bm.traccar.rt;

import bm.traccar.api.Api;
import bm.traccar.api.ApiConfig;
import bm.traccar.api.ApiException;
import bm.traccar.api.ApiService;
import bm.traccar.rt.scenario.ScenarioConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for integration tests using Spring Boot. This class provides a common setup and
 * teardown of a full scenario for realtime integration tests.
 */
@SpringBootTest
@ContextConfiguration(classes = {ApiService.class})
@TestPropertySource("classpath:application.properties")
@Import({ApiConfig.class, ScenarioConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseReaTimeScenarioTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseReaTimeScenarioTest.class);

  @Autowired protected Api api;

  // @Autowired private ScenarioProperties scenarioProperties;

  @Value("${traccar.web.serviceAccountToken}")
  protected String virtualAdmin;

  // Long adminId, player1Id, player2Id, player3Id;

  @BeforeAll
  public void setup() throws ApiException {
    // we always need an admin for (the server and) a scenario
    //    api.setBearerToken(virtualAdmin);
    //    User admin = api.users.createUserWithCredentials(adminName, adminPassword, adminMail,
    // true);
    //    adminId = admin.getId();
    //    logger.info("Created Admin {} (id={})", adminMail, adminId);

    // setupScenario(adminMail, adminPassword);
  }

  @AfterAll
  public void teardown() throws ApiException {
    // api.setBearerToken(virtualAdmin);
    // api.setBasicAuth(adminMail, adminPassword);
    // catch Execption in case user or admin have been deleted in a test
    //    api.users.deleteUser(player1Id);
    //    logger.info("Deleted User {} (id={})", player1mail, player1Id);
    //    api.users.deleteUser(adminId);
    //    logger.info("Deleted Admin {} (id={})", adminMail, adminId);
  }
}
