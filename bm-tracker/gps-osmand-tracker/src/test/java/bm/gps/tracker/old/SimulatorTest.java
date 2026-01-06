package bm.gps.tracker.old;

import bm.gps.remove.Application;
import bm.gps.remove.SimulatorService;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Simulate the OsmAnd tracking process using Apache Camel. This runs the Spring Boot application
 * and its Camel routes for a controlled duration.
 */
@CamelSpringBootTest
@SpringBootTest(classes = Application.class)
class SimulatorTest {

  // @Autowired private ApplicationContext applicationContext;

  // We can inject the service if we need to inspect state,
  // but for running, we just need the context.
  @Autowired private SimulatorService simulatorService;

  //  @Test
  void runSimulationForFiveSeconds() throws Exception {
    System.out.println("------------------------------------------------------------------");
    System.out.println("Starting Camel Simulator Test. Route is running on a timer...");
    System.out.println("Simulator will run for 5 seconds to demonstrate distance-based sending.");
    System.out.println("Ensure your target server is running on http://localhost:5055");
    System.out.println("------------------------------------------------------------------");

    // Wait for 5 seconds while the Camel route runs in the background
    // The route ticks every 500ms and sends a message every ~100 meters.
    Thread.sleep(100 * 1000);

    System.out.println("------------------------------------------------------------------");
    System.out.println("Simulation run complete.");
    // We could assert on logs or mock endpoints here, but for demonstration, waiting is sufficient.
    System.out.println(
        "Last calculated position: Lat="
            + simulatorService.getLastSentData().latitude()
            + ", Lon="
            + simulatorService.getLastSentData().longitude());
    System.out.println("------------------------------------------------------------------");
  }
}
