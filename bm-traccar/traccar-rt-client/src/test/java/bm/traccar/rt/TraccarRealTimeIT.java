package bm.traccar.rt;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TraccarRealTimeIT extends BaseReaTimeScenarioTest {
  private static final Logger logger = LoggerFactory.getLogger(TraccarRealTimeIT.class);

  @Test
  void test() {
    logger.info("execute TraccarRealTimeIT");
  }

  /* setup scenario (move to BaseIT)
   * -create admin > over all users (players) and (their) devices
   * -create user/s > over his own device/s
   * -create device/s for admin/user
   * -login as admin > load all devices
   * -> one MrX and two/three Detectives
   */
  //  private void setupScenario() {
  //    api.setBearerToken(virtualAdmin);
  //    // dont use these User objects > get it from the database via API and *Id
  //    User user = api.users.createUserWithCredentials(userName, userPassword, userMail, false);
  //    userId = user.getId();
  //    logger.info("Created User {} (id={})", userMail, userId);
  //    User admin = api.users.createUserWithCredentials(adminName, adminPassword, adminMail, true);
  //    adminId = admin.getId();
  //    logger.info("Created Admin {} (id={})", adminMail, adminId);
  //  }
}
