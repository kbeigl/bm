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
package bm.traccar.scenario;

import bm.traccar.rtapp.RealTimeAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all RealTime scenarios.
 *
 * <p>A Scenario represents a complete test workflow that:
 *
 * <ul>
 *   <li>Initializes the test environment (create users, devices, trackers)
 *   <li>Runs the actual test logic (start GPS players, verify behavior)
 *   <li>Cleans up all created resources
 * </ul>
 *
 * <p>Scenarios have access to all RealTimeApp components via the {@link RealTimeAppContext}.
 *
 * <p><b>Lifecycle:</b>
 *
 * <ol>
 *   <li>{@link #initScenario()} - Create and register trackers, users, devices
 *   <li>{@link #runScenario()} - Execute the test logic (start players, verify results)
 *   <li>{@link #shutdownScenario()} - Clean up all resources
 * </ol>
 *
 * <p><b>Example:</b>
 *
 * <pre>
 * public class MyScenario extends Scenario {
 *   {@literal @}Override
 *   public void initScenario() {
 *     // Create users and devices
 *     User testUser = context.getApi().getUsersApi().createUser(...);
 *     Device testDevice = context.getApi().getDevicesApi().createDevice(...);
 *     // Register tracker
 *     TrackerRegistration reg = context.getTrackerRegistration();
 *     reg.registerTracker(testDevice);
 *   }
 *
 *   {@literal @}Override
 *   public void runScenario() {
 *     // Start GPS players, verify real-time updates
 *     GpsPlayer player = new GpsPlayer(testDevice);
 *     player.start();
 *     // ... verify behavior
 *   }
 *
 *   {@literal @}Override
 *   public void shutdownScenario() {
 *     // Clean up
 *     context.getApi().getDevicesApi().deleteDevice(testDevice.getId());
 *     context.getApi().getUsersApi().deleteUser(testUser.getId());
 *   }
 * }
 * </pre>
 */
public abstract class Scenario {
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  protected RealTimeAppContext context;

  /**
   * Set the scenario context. This is called by the ScenarioRunner before executing the scenario.
   *
   * @param context the RealTimeAppContext providing access to RealTimeApp components
   */
  public void setContext(RealTimeAppContext context) {
    this.context = context;
  }

  /**
   * Get the name of this scenario. Override to provide a meaningful name.
   *
   * @return the scenario name (default: simple class name)
   */
  public String getName() {
    return getClass().getSimpleName();
  }

  /**
   * Initialize the scenario. Create and register:
   *
   * <ul>
   *   <li>Users (test accounts)
   *   <li>Devices (GPS trackers)
   *   <li>Tracker registrations
   *   <li>Any other required setup
   * </ul>
   *
   * @throws Exception if initialization fails
   */
  public abstract void initScenario() throws Exception;

  /**
   * Run the scenario. This is where the actual test logic executes:
   *
   * <ul>
   *   <li>Create GpsPlayers for registered trackers
   *   <li>Start GPS position updates
   *   <li>Verify real-time behavior via RealTimeController
   *   <li>Assert expected results
   * </ul>
   *
   * @throws Exception if scenario execution fails
   */
  public abstract void runScenario() throws Exception;

  /**
   * Shutdown the scenario. Clean up all created resources:
   *
   * <ul>
   *   <li>Stop all GPS players
   *   <li>Delete devices
   *   <li>Delete users
   *   <li>Clean up tracker registrations
   * </ul>
   *
   * @throws Exception if cleanup fails
   */
  public abstract void shutdownScenario() throws Exception;
}
