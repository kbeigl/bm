package bm.traccar;

import bm.traccar.generated.model.dto.User;
import bm.traccar.rt.RealTimeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class RealTimeClient implements CommandLineRunner {
  private static final Logger logger = LoggerFactory.getLogger(RealTimeClient.class);

  @Autowired private RealTimeController controller;
  @Autowired private ConfigurableApplicationContext context;
  @Autowired private Environment environment;

  @Value("${traccar.name}")
  private String name;

  @Value("${traccar.password}")
  private String password;

  @Value("${traccar.email}")
  private String email;

  /**
   * Start RealTimeClient and wait for messages
   *
   * @param args
   */
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(RealTimeClient.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    app.run(args);
  }

  @Override
  public void run(String... args) throws Exception {

    // hook to skip run() in test context
    if (isTestContext()) return;

    User admin = new User();
    admin.setName(name);
    admin.setPassword(password);
    admin.setEmail(email);
    logger.info("Starting RealTimeController login for admin: {}", admin.getEmail());
    boolean success = controller.loginAndInitialize(admin);
    if (success) {
      logger.info("RealTimeController started and logged in successfully.");
    } else {
      logger.error("RealTimeController failed to log in.");
      shutdown();
      return;
    }

    // Wait for manual shutdown (e.g., Ctrl+C)
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    logger.info("Application running. Press Ctrl+C to exit.");
    synchronized (this) {
      this.wait();
    }
  }

  /** used in test context to avoid blocking */
  private boolean isTestContext() {
    for (String profile : environment.getActiveProfiles()) {
      if ("test".equals(profile)) {
        return true;
      }
    }
    return false;
  }

  private void shutdown() {
    logger.info("Graceful shutdown initiated.");
    if (context != null) {
      context.close();
    }
  }
}
