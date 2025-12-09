package bm.tracker.gpstracker;

import static org.assertj.core.api.Assertions.assertThat;

import bm.traccar.BaseRealTimeClientTest;
import bm.traccar.api.Api;
import bm.tracker.gpstracker.model.GpsMessage;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for the OsmAndTracker client component in BaseRealTimeClient context, i.e.
 * scenario.
 *
 * <p>Dependencies to api and real-time-client modules are only required for integration tests. The
 * Tracker should only depend on generated model classes.
 *
 * <p>Although this is an integration test, the RealTimeClient is not started here. The focus is on
 * testing the Tracker component in a scenario with devices (and users?).
 */
@SpringBootTest(
    classes = {
      bm.traccar.RealTimeClient.class,
      bm.tracker.gpstracker.GpsOsmandTrackerTestConfig.class
    })
@ComponentScan(basePackages = {"bm.tracker.gpstracker"})
@ActiveProfiles("test")
class TrackerScenarioIT extends BaseRealTimeClientTest {
  private static final Logger logger = LoggerFactory.getLogger(TrackerScenarioIT.class);

  @Autowired TrackerRegistrationService registrationService;
  @Autowired private Api api;

  @Test
  void sendMessagesWithDevices() throws Exception {

    logger.info("Verifying that the Spring context loads correctly.");
    assertThat(controller).isNotNull();
    assertThat(scenario).isNotNull();
    assertThat(client).isNotNull();

    logger.info("Setup scenario devices");
    // devices from scenarioLoader from DB. will be RT client later
    // create map(uniqueId, tracker) and relate to Device (in RT) ?
    // Device adminDevice, managerDevice, hideDevice, seekDevice;

    // define positions according to existing tracks
    // skip real device in scenario !?
    OsmAndTracker realTracker =
        registrationService.registerTracker(scenario.realDevice.getUniqueId());
    OsmAndTracker runnerTracker =
        registrationService.registerTracker(scenario.runnerDevice.getUniqueId());

    GpsMessage m1 = new GpsMessage();
    // actually the id is tranfered in the message
    m1.setId(scenario.realDevice.getUniqueId());
    m1.setLat(52.0);
    m1.setLon(13.0);
    m1.setTimestamp(System.currentTimeMillis());
    realTracker.send(m1);

    GpsMessage m2 = new GpsMessage();
    m2.setId(scenario.runnerDevice.getUniqueId());
    m2.setLat(52.01);
    m2.setLon(13.01);
    m2.setTimestamp(System.currentTimeMillis());

    // how to lookup tracker by name + device id ?
    runnerTracker.send(m2);

    OsmAndTracker chaser1Tracker =
        registrationService.registerTracker(scenario.chaser1Device.getUniqueId());
    GpsMessage m3 = new GpsMessage();
    m3.setId(scenario.chaser1Device.getUniqueId());
    m3.setLat(52.02);
    m3.setLon(13.02);
    m3.setTimestamp(System.currentTimeMillis());
    chaser1Tracker.send(m3);

    OsmAndTracker chaser2Tracker =
        registrationService.registerTracker(scenario.chaser2Device.getUniqueId());
    GpsMessage m4 = new GpsMessage();
    m4.setId(scenario.chaser2Device.getUniqueId());
    m4.setLat(52.03);
    m4.setLon(13.03);
    m4.setTimestamp(System.currentTimeMillis());
    chaser2Tracker.send(m4);

    // listAllBeansInTestContext();

    // now use API to verify that positions are received and stored for devices
    api.getDevicesApi()
        .getDevices(null)
        .forEach(
            device -> {
              try {
                logger.info("\nDevice.id{}: {} ", device.getId(), device);
                // assertThat(positions.size()).isGreaterThan(0);
              } catch (Exception e) {
                logger.error(
                    "Error retrieving positions for Device.id{} ({}): {}",
                    device.getId(),
                    device.getName(),
                    e.getMessage());
              }
            });
  }

  // to be removed later
  @Autowired private ApplicationContext appContext;

  /** Helper to make sure bm.traccar beans are loaded as in a @SpringBootApplication context. */
  void listAllBeansInTestContext() {

    String[] allBeanNames = appContext.getBeanDefinitionNames();
    Arrays.sort(allBeanNames); // readability
    logger.info(
        "--- START: Listing all 'bm.traccar' || 'bm.tracker' beans (of {}) in test ApplicationContext ---",
        allBeanNames.length); // = applicationContext.getBeanDefinitionCount()

    for (String beanName : allBeanNames) {
      try {
        // if (beanName.startsWith("bm.traccar")
        if ((appContext.getBean(beanName).getClass().getPackageName().startsWith("bm.traccar"))
            || (appContext
                .getBean(beanName)
                .getClass()
                .getPackageName()
                .startsWith("bm.tracker"))) {
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

    // Explicitly log all OsmAndTracker beans
    StringBuilder trackerBeans = new StringBuilder("\nOsmAndTracker beans in context: ");
    appContext
        .getBeansOfType(OsmAndTracker.class)
        .forEach(
            (name, bean) ->
                trackerBeans.append("[" + name + ": " + bean.getClass().getName() + "]\n "));
    logger.info(trackerBeans.toString());
  }
}
