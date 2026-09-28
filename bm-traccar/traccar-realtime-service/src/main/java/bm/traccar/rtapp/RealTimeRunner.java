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
package bm.traccar.rtapp;

import bm.traccar.generated.model.dto.User;
import bm.traccar.rt.RealTimeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class RealTimeRunner implements ApplicationRunner {
  private static final Logger logger = LoggerFactory.getLogger(RealTimeRunner.class);

  private final RealTimeController controller;
  //  private final Environment environment;
  private final ConfigurableApplicationContext applicationContext;
  private final RealTimeContext context;

  @Value("${traccar.name}")
  private String name;

  @Value("${traccar.password}")
  private String password;

  @Value("${traccar.email}")
  private String email;

  @Value("${traccar.host}")
  private String host;

  private User realtimeAccount;

  RealTimeRunner(
      RealTimeController controller,
      // Environment environment,
      ConfigurableApplicationContext applicationContext,
      RealTimeContext context) {
    this.controller = controller;
    // this.environment = environment;
    this.applicationContext = applicationContext;
    this.context = context;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    logger.debug("RealTimeRunner started with arguments: {}", args.getOptionNames());

    loginRealTimeAccount();

    logger.info("RealTimeService running ...");
  }

  /**
   * The RealTimeService can only run if the admin user is successfully logged in! This user is
   * authorized for all actions in the scenario. For the time being the complete app is designed to
   * run with a single user.
   *
   * <p>Logs in the RealTimeController with the provided admin credentials. If login fails,
   * initiates a graceful shutdown of the application.
   *
   * @throws Exception if an error occurs during login
   */
  private void loginRealTimeAccount() throws Exception {
    realtimeAccount = new User();
    realtimeAccount.setName(name);
    realtimeAccount.setPassword(password);
    realtimeAccount.setEmail(email);
    logger.info(
        "Login RealTimeController for '{}' to traccar server '{}` ...",
        realtimeAccount.getEmail(),
        host);
    boolean success = controller.loginAndInitialize(realtimeAccount);
    if (success) {
      logger.info("RealTimeController started and logged in successfully.");
      User loggedIn = controller.getCurrentUser().orElse(realtimeAccount);
      context.setRealtimeAdmin(loggedIn);
    } else {
      logger.error(
          "Bad credentials - cannot run RealTimeService without valid admin on host '{}'", host);
      gracefulShutdown();
    }
  }

  private void gracefulShutdown() {
    logger.info("Initiating graceful shutdown of RealTimeService...");
    int exitCode = SpringApplication.exit(applicationContext, () -> 1);
    System.exit(exitCode);
  }
}
