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

@SpringBootApplication
public class RealTimeClient implements CommandLineRunner {
  private static final Logger logger = LoggerFactory.getLogger(RealTimeClient.class);

  @Autowired private RealTimeController controller;

  @Autowired private ConfigurableApplicationContext context;

  @Value("${traccar.admin.name}")
  private String adminName;

  @Value("${traccar.admin.password}")
  private String adminPassword;

  @Value("${traccar.admin.email}")
  private String adminEmail;

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(RealTimeClient.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    app.run(args);
  }

  @Override
  public void run(String... args) throws Exception {
    User admin = new User();
    admin.setName(adminName);
    admin.setPassword(adminPassword);
    admin.setEmail(adminEmail);
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

  private void shutdown() {
    logger.info("Graceful shutdown initiated.");
    if (context != null) {
      context.close();
    }
  }
}
