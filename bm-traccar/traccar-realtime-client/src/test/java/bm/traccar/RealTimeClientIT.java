package bm.traccar;

import org.junit.jupiter.api.Test;

/**
 * Integration test for RealTimeClient using the full scenario setup from BaseRealTimeClientTest.
 */
public class RealTimeClientIT extends BaseRealTimeClientTest {

  @Test
  void contextLoads() {
    // Basic test to ensure the Spring context and scenario are loaded correctly.
    // Add further integration tests as needed.
    assert controller != null;
    assert client != null;
    assert scenario != null;
  }
}
