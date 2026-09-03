/*
 * (C) Copyright 2026 Kristof Beiglböck
 *               kbeigl.github.io/bm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * teardown for all integration tests. It includes app properties for the virtualAdmin user, an
 * admin and a regular user.
 *
 * <p>The virtualAdmin should only be used on a fresh Traccar instance to create actors. Once
 * created, the virtualAdmin should not be used anymore in the tests. An admin and regular user are
 * created in the Traccar database in @BeforeAll method. Consequently, the admin and regular user
 * should be logged in the tests to perform API calls.
 */
@SpringBootTest(properties = "logging.config=classpath:logback-test.xml")
// move/add ApiAspect.class from AspectIT and GeneratedSessionApiIT here
// Tell Spring Boot to create a context containing ONLY these beans.
@ContextConfiguration(classes = {ApiService.class})
@Import(ApiConfig.class) // @Configuration class
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

  // Service is more convenient to use
  @Autowired protected ApiService api;
  //     @Autowired protected Api api;

  Long userId, adminId;

  @BeforeAll
  public void setup() throws ApiException {
    logger.info("--- create Admin and User ---");
    api.setBearerToken(virtualAdmin);
    User user = api.users.createUserWithCredentials(userName, userPassword, userMail, false);
    userId = user.getId();
    logger.info("Created User {} (id={})", userMail, userId);
    User admin = api.users.createUserWithCredentials(adminName, adminPassword, adminMail, true);
    adminId = admin.getId();
    logger.info("Created Admin {} (id={})", adminMail, adminId);

    // login as admin for subsequent tests ?
  }

  @AfterAll
  public void teardown() throws ApiException {
    logger.info("--- delete Admin and User ---");
    api.setBasicAuth(adminMail, adminPassword);
    // catch Execption in case user or admin have been deleted in a test
    // fallback api.setBearerToken(virtualAdmin);
    api.users.deleteUser(userId);
    logger.info("Deleted User {} (id={})", userMail, userId);
    api.users.deleteUser(adminId);
    logger.info("Deleted Admin {} (id={})", adminMail, adminId);
  }
}
