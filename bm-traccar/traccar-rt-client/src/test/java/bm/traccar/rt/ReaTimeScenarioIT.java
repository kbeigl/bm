package bm.traccar.rt;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReaTimeScenarioIT extends BaseReaTimeScenarioTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseReaTimeScenarioTest.class);

  @Test
  void contextLoads() {
    logger.info("Context loads successfully");
    listAllBeansInTestContext();
  }
}
