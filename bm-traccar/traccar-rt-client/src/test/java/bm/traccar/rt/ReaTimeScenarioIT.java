package bm.traccar.rt;

import bm.traccar.RealTimeClient;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ReaTimeScenarioIT extends BaseReaTimeScenarioTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseReaTimeScenarioTest.class);

  @MockBean private RealTimeClient mockRealTimeClient;

  @Test
  void contextLoads() {
    logger.info("Context loads successfully");
    listAllBeansInTestContext();
  }
}
