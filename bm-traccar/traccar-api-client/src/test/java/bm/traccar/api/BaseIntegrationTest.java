package bm.traccar.api;

import bm.traccar.generated.model.dto.User;
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
 * teardown for all integration tests.
 *
 * <p>It includes properties for the virtualAdmin user, an admin and a regular user. The admin and
 * regular user are created in the Traccar database in @BeforeAll method, so they can be used in all
 * ITests. The @AfterAll method is used to clean the Traccar database after all tests in the class
 * have been executed for the next ITest.
 */
@SpringBootTest // implies @EnableAutoConfiguration
// move/add ApiAspect.class from AspectIT and GeneratedSessionApiIT here
// Tell Spring Boot to create a context containing ONLY these beans.
@ContextConfiguration(classes = {ApiService.class})
@Import(ApiConfig.class) // typically @Configuration classes
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseIntegrationTest.class);

  // consider moving to ApiConfig (for testing only)
  @Value("${traccar.web.serviceAccountToken}")
  protected String virtualAdmin;

  @Value("${traccar.admin.name}")
  protected String adminName;

  @Value("${traccar.admin.password}")
  protected String adminPassword;

  @Value("${traccar.admin.email}")
  protected String adminMail;

  @Value("${traccar.user.name}")
  protected String userName;

  @Value("${traccar.user.password}")
  protected String userPassword;

  @Value("${traccar.user.email}")
  protected String userMail;

  @Autowired protected Api api;

  Long userId, adminId;

  @BeforeAll
  public void setup() throws ApiException {
    api.setBearerToken(virtualAdmin);
    // dont use these User objects > get it from the database via API and *Id
    // User user = api.users.createUserWithCredentials(userName, userPassword, userMail, false);
    User user =
        api.getUsersApi().createUserWithCredentials(userName, userPassword, userMail, false);
    userId = user.getId();
    logger.info("Created User {} (id={})", userMail, userId);
    User admin =
        api.getUsersApi().createUserWithCredentials(adminName, adminPassword, adminMail, true);
    adminId = admin.getId();
    logger.info("Created Admin {} (id={})", adminMail, adminId);
  }

  @AfterAll
  public void teardown() throws ApiException {
    // api.setBearerToken(virtualAdmin);
    api.setBasicAuth(adminMail, adminPassword);
    // catch Execption in case user or admin have been deleted in a test
    api.getUsersApi().deleteUser(userId);
    logger.info("Deleted User {} (id={})", userMail, userId);
    api.getUsersApi().deleteUser(adminId);
    logger.info("Deleted Admin {} (id={})", adminMail, adminId);
  }
}
