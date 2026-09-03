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
package bm.gps.tracker;

import bm.traccar.BaseRealTimeClientTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for GPS tracker integration tests.
 *
 * <p>The Tracker is designed to be independant!
 *
 * <p>Integration tests for the OsmAndTracker client component in BaseRealTimeClient context, i.e.
 * scenario. Dependencies to api and real-time-client modules are only required for integration
 * tests.
 */
public abstract class BaseGpsTrackerIT extends BaseRealTimeClientTest {

  private static final Logger logger = LoggerFactory.getLogger(BaseGpsTrackerIT.class);

  @Autowired protected TrackerRegistration registrationService;

  protected TrackerOsmAnd runnerTracker, chaser1Tracker, chaser2Tracker, mobileTracker;

  @BeforeAll
  public void registerScenarioTrackers() {
    logger.info("--- register Scenario Trackers ---");
    try {
      if (registrationService == null) {
        logger.debug("No TrackerRegistration available in this context.");
        return;
      }
      if (scenario.realDevice != null)
        mobileTracker = registrationService.registerTracker(scenario.realDevice.getUniqueId());
      if (scenario.runnerDevice != null)
        runnerTracker = registrationService.registerTracker(scenario.runnerDevice.getUniqueId());
      if (scenario.chaser1Device != null)
        chaser1Tracker = registrationService.registerTracker(scenario.chaser1Device.getUniqueId());
      if (scenario.chaser2Device != null)
        chaser2Tracker = registrationService.registerTracker(scenario.chaser2Device.getUniqueId());

    } catch (Exception e) {
      logger.warn("Failed to register scenario trackers: {}", e.getMessage());
    }
    logger.info("--- Scenario Trackers registered ---");
    logger.info("\t********** start of tests **********");
  }

  /*  Destroying the tracker bean (destroySingleton) does not automatically remove a route
   *  with the same id unless you explicitly remove it from Camel (getContext().removeRoute(routeId)).
   *  TODO: if you create per-tracker routes you must remove them explicitly on bean destruction.
   *  -> already implemented in TrackerOsmAnd destroy() method, but not triggered with destroySingleton?
   */
  @AfterAll
  public void unregisterScenarioTrackers() {

    if (registrationService == null) {
      logger.debug("No TrackerRegistration available to unregister trackers.");
      return;
    }
    try {
      // loop over all devices and unregister trackers if they exist
      if (scenario.realDevice != null)
        registrationService.unregisterTracker(scenario.realDevice.getUniqueId());
      if (scenario.runnerDevice != null)
        registrationService.unregisterTracker(scenario.runnerDevice.getUniqueId());
      if (scenario.chaser1Device != null)
        registrationService.unregisterTracker(scenario.chaser1Device.getUniqueId());
      if (scenario.chaser2Device != null)
        registrationService.unregisterTracker(scenario.chaser2Device.getUniqueId());
    } catch (Exception e) {
      logger.warn(
          "Failed to unregister scenario trackers via registrationService: {}", e.getMessage());
    }
    logger.info("\t***** end of tests *****");
  }
}
