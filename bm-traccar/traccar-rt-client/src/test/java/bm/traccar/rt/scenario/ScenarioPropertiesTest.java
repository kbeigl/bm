package bm.traccar.rt.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.rt.scenario.ScenarioProperties.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest // (classes = ScenarioProperties.class)
@ContextConfiguration(classes = {ScenarioConfig.class})
public class ScenarioPropertiesTest {

  @Autowired private ScenarioProperties props;

  @Test
  void propertiesDevicesShouldBeLoadedAndMapped() {
    assertNotNull(props.getDevice().get(0), "Device0 object should not be null");
  }

  @Test
  void propertiesUsersShouldBeLoadedAndMapped() {
    assertNotNull(props.getUser().get(0), "User0 object should not be null");
    assertNotNull(props.getUser().get(1), "User1 object should not be null");
    assertNotNull(props.getUser().get(2), "User2 object should not be null");
    assertNotNull(props.getUser().get(3), "User3 object should not be null");

    // Verify User properties
    User user0 = props.getUser().get(0);
    assertEquals("admin", user0.name);
    assertEquals("admin", user0.password);
    assertEquals("admin@scenario.com", user0.email);
    assertTrue(user0.administrator);

    User user1 = props.getUser().get(1);
    assertEquals("manager", user1.name);
    assertEquals("manager", user1.password);
    assertEquals("manager@scenario.com", user1.email);

    User user2 = props.getUser().get(2);
    assertEquals("hide", user2.name);
    assertEquals("hide", user2.password);
    assertEquals("hide@scenario.com", user2.email);

    User user3 = props.getUser().get(3);
    assertEquals("seek", user3.name);
    assertEquals("seek", user3.password);
    assertEquals("seek@scenario.com", user3.email);
  }
}
