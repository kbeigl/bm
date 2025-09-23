package bm.traccar.rt;

import bm.traccar.api.ApiException;
import bm.traccar.rt.scenario.ScenarioLoader;
import bm.traccar.rt.scenario.ScenarioProperties;
import java.util.Arrays;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Base class for integration tests using Spring Boot. This class provides a common setup and
 * teardown of a full scenario for realtime integration tests.
 *
 * <p>An abstract base class for Spring Boot Camel integration tests.
 *
 * <p>This class provides the necessary setup to initialize a Spring ApplicationContext that scans
 * for components, services, and Camel routes in specified packages, which is necessary when
 * no @SpringBootApplication class is present.
 *
 * <p>It disables JMX for faster test execution and provides pre-configured Camel test utilities
 * like CamelContext and ProducerTemplate.
 */
@SpringBootTest(classes = BaseReaTimeScenarioTest.TestConfig.class)
// properties = {"camel.springboot.java-routes-include-pattern=**/routes/**"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseReaTimeScenarioTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseReaTimeScenarioTest.class);

  /**
   * The Spring Boot test configuration to replace a @SpringBootApplication for tests. Explicitly
   * enable auto-configuration and define which packages to scan.
   */
  @CamelSpringBootTest
  // Spring Test context runner
  @SpringBootTest
  // replacement for auto-configuration, like Camel
  @EnableAutoConfiguration
  // entire package as @SpringBootApplication would do
  @ComponentScan(basePackages = "bm.traccar")
  @Configuration
  public static class TestConfig {
    // This class can be empty, it holds the annotations.

    // THESE TWO ScenarioProperties CAUSE AMBIGUITY PROBLEMS, TODO: resolve
    // ?? scenario-bm.traccar.rt.scenario.ScenarioProperties | ScenarioProperties
    //	  scenarioProperties                                 | ScenarioProperties
    //	  scenarioConfig                                     | ScenarioConfig$$SpringCGLIB$$0
    // workaround:
    @Bean
    @Primary
    public ScenarioProperties scenarioProperties() {
      return new ScenarioProperties();
    }
  }

  @Autowired ScenarioLoader scenario;

  @BeforeAll
  public void setup() throws ApiException {
    scenario.setupScenario();
  }

  @AfterAll
  public void teardown() throws ApiException {
    scenario.teardownScenario();
  }

  @Autowired private ApplicationContext appContext;

  /** Helper to make sure bm.traccar beans are loaded as in a @SpringBootApplication context. */
  void listAllBeansInTestContext() {

    String[] allBeanNames = appContext.getBeanDefinitionNames();
    Arrays.sort(allBeanNames); // readability
    logger.info(
        "--- START: Listing all 'bm.traccar' beans (of {}) loaded in test ApplicationContext ---",
        allBeanNames.length); // = applicationContext.getBeanDefinitionCount()

    for (String beanName : allBeanNames) {
      try {
        // if (beanName.startsWith("bm.traccar")
        if (appContext.getBean(beanName).getClass().getPackageName().startsWith("bm.traccar")) {
          logger.info(
              ">> Bean Name: {} | Class: {}",
              beanName,
              appContext.getBean(beanName).getClass().getName());
          // } else { // all SringBoot and Camel internal beans
          //   logger.info("   Bean Name: {} | Class: {}", beanName,
          //       applicationContext.getBean(beanName).getClass().getName());
        }
      } catch (Exception e) {
        logger.warn("Could not retrieve bean instance for name '{}': {}", beanName, e.getMessage());
      }
    }
    logger.info("--- END: Bean listing. ---");
  }
}
