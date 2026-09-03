/*
 * (C) Copyright 2026 Kristof Beiglböck
 *               kbeigl.github.io/bm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bm.traccar.rt;

import bm.traccar.api.ApiConfig;
import bm.traccar.api.ApiException;
import bm.traccar.api.ApiService;
import bm.traccar.api.scenario.ScenarioConfig;
import bm.traccar.api.scenario.ScenarioLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for integration tests using Spring Boot. This class provides a common setup and
 * teardown of a full scenario for realtime integration tests.
 *
 * <p>An abstract base class for Spring Boot Camel integration tests.
 *
 * <p>This class provides the necessary setup to initialize a Spring ApplicationContext that scans
 * for components, services, and Camel routes in specified packages, which is necessary when no
 * {@code @SpringBootApplication} class is present.
 *
 * <p>Uses explicit configuration instead of @SpringBootApplication to test library components in
 * isolation. This is a SLICE TEST that only loads RealTimeManager and API components, without the
 * full RealTimeClient infrastructure (no Camel, no WebSocket, no Controller).
 */
@SpringBootTest
@ContextConfiguration(
    classes = {
      RealTimeManager.class, // Only the state manager, not the full RealTimeConfig
      ApiService.class,
      ApiConfig.class,
      ScenarioLoader.class,
      ScenarioConfig.class
    })
@TestPropertySource(locations = "classpath:test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseRealTimeTest {
  // private static final Logger logger = LoggerFactory.getLogger(BaseRealTimeTest.class);

  @Autowired ScenarioLoader scenario;
  @Autowired protected RealTimeManager stateManager;

  @BeforeAll
  public void setup() throws ApiException {
    scenario.setupScenario();
  }

  @AfterAll
  public void teardown() throws ApiException {
    scenario.teardownScenario();
  }

  // @Autowired private ApplicationContext appContext;

}
