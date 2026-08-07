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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RealTimeAppRunner implements ApplicationRunner {
  private static final Logger logger = LoggerFactory.getLogger(RealTimeAppRunner.class);

  private final RealTimeController controller;
  //  private final Environment environment;
  private final ConfigurableApplicationContext applicationContext;

  @Value("${traccar.name}")
  private String name;

  @Value("${traccar.password}")
  private String password;

  @Value("${traccar.email}")
  private String email;

  @Value("${traccar.host}")
  private String host;

  private User realtimeAdmin;

  RealTimeAppRunner(
      RealTimeController controller,
      Environment environment,
      ConfigurableApplicationContext applicationContext) {
    this.controller = controller;
    // this.environment = environment;
    this.applicationContext = applicationContext;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    logger.debug("RealTimeAppRunner started with arguments: {}", args.getOptionNames());

    loginRealTimeAdmin();

    logger.info("RealTimeApp running ...");
  }

  /**
   * The RealTimeApp can only run if the admin user is successfully logged in! This user is
   * authorized for all actions in the scenario. For the time being the complete app is designed to
   * run with a single user.
   *
   * <p>Logs in the RealTimeController with the provided admin credentials. If login fails,
   * initiates a graceful shutdown of the application.
   *
   * @throws Exception if an error occurs during login
   */
  private void loginRealTimeAdmin() throws Exception {
    realtimeAdmin = new User();
    realtimeAdmin.setName(name);
    realtimeAdmin.setPassword(password);
    realtimeAdmin.setEmail(email);
    logger.info(
        "Login RealTimeController for '{}' to traccar server '{}` ...",
        realtimeAdmin.getEmail(),
        host);
    boolean success = controller.loginAndInitialize(realtimeAdmin);
    if (success) {
      logger.info("RealTimeController started and logged in successfully.");
    } else {
      logger.error(
          "Bad credentials - cannot run RealTimeApp without valid admin on host '{}'", host);
      gracefulShutdown();
    }
  }

  private void gracefulShutdown() {
    logger.info("Initiating graceful shutdown of RealTimeApp...");
    int exitCode = SpringApplication.exit(applicationContext, () -> 1);
    System.exit(exitCode);
  }
}
