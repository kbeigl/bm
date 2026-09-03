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
package bm.traccar;

import bm.traccar.api.Api;
import bm.traccar.api.Api.Users;
import bm.traccar.generated.api.UsersApi;
import bm.traccar.generated.model.dto.User;
import bm.traccar.invoke.ApiClient;
import bm.traccar.rt.RealTimeController;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = RealTimeApp.class,
    properties = "logging.config=classpath:logback-test.xml")
@ContextConfiguration(initializers = {BaseRealTimeAppTest.Initializer.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseRealTimeAppTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseRealTimeAppTest.class);

  // Static fields for pre-Spring setup
  static User adminCreatedBeforeSpring;
  private static Properties testProps;

  public User admin;

  @Autowired protected Api api;
  @Autowired protected RealTimeController controller;

  @Value("${traccar.web.serviceAccountToken}")
  protected String virtualAdmin;

  /**
   * Spring ApplicationContextInitializer runs BEFORE Spring context starts. This is where we create
   * the admin user on the server.
   */
  static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Logger initLogger = LoggerFactory.getLogger(Initializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      initLogger.debug("*** INITIALIZE BEFORE Spring context starts ***");

      try {
        // Load properties manually (Spring hasn't started yet)
        testProps = new Properties();
        try (InputStream input =
            BaseRealTimeAppTest.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
          testProps.load(input);
        }

        // Manually resolve property placeholders (Spring hasn't started yet)
        String host = testProps.getProperty("traccar.host", "http://localhost:80");

        String adminName = testProps.getProperty("traccar.name"),
            adminPassword = testProps.getProperty("traccar.password"),
            adminEmail = testProps.getProperty("traccar.email"),
            virtualAdminToken = testProps.getProperty("traccar.web.serviceAccountToken");

        initLogger.debug("--- Setup Admin on server BEFORE Spring context ---");
        initLogger.info("Using host: {}", host);

        // Create admin user
        User newAdmin = new User();
        newAdmin.setName(adminName);
        newAdmin.setPassword(adminPassword);
        newAdmin.setEmail(adminEmail);
        newAdmin.setAdministrator(true);

        newAdmin.setDeviceLimit(-1);
        newAdmin.setUserLimit(-1);

        // Manually create API client (no Spring autowiring available yet)
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(host + "/api");
        apiClient.setBearerToken(virtualAdminToken);

        // SkIPPING implemented API and calling the generated API directly
        UsersApi usersApi = new UsersApi(apiClient);
        adminCreatedBeforeSpring = usersApi.usersPost(newAdmin);

        initLogger.info(
            "Created User: {}.id{}",
            adminCreatedBeforeSpring.getName(),
            adminCreatedBeforeSpring.getId());
      } catch (Exception e) {
        initLogger.error("Failed to create admin before Spring context", e);
        throw new RuntimeException("Pre-Spring admin creation failed", e);
      }
    }
  }

  /**
   * Instance @BeforeAll runs AFTER Spring context starts. Uses the admin already created in the
   * Initializer.
   */
  @BeforeAll
  public void setup() throws Exception {
    logger.debug("*** instance setup AFTER Spring context starts ***");

    // Use the admin that was created before Spring started
    if (adminCreatedBeforeSpring == null) {
      throw new IllegalStateException(
          "Admin was not created in Initializer. Check logs for errors.");
    }

    admin = adminCreatedBeforeSpring;
    logger.info("Using admin created before Spring: {}.{}", admin.getName(), admin.getId());

    // We want to ensure that tests start with authenticated admin
    api.setBasicAuth(admin.getEmail(), admin.getName());
  }

  @AfterAll
  public void teardown() {
    logger.info("Tearing down RealTimeAppTest context.");
    controller.shutdown(); // clean up WebSocket route
    // delete the admin user from the server
    if (admin != null) {

      // do not apply virtualAdmin token
      api.setBearerToken(virtualAdmin);

      Users userApi = api.getUsersApi();
      userApi.deleteUser(admin.getId());
      logger.info("Deleted User.id{}: {}", admin.getId(), admin.getName());
    }
  }
}
