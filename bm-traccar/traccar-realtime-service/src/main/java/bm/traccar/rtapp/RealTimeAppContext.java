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
package bm.traccar.rtapp;

import bm.gps.tracker.TrackerRegistration;
import bm.traccar.api.Api;
import bm.traccar.generated.model.dto.User;
import bm.traccar.rt.RealTimeController;
import org.springframework.stereotype.Component;

/**
 * RealTimeAppContext provides access to the major components of the RealTimeApp:
 *
 * <ul>
 *   <li><b>RealTimeController</b> - to receive websocket messages from Traccar server
 *   <li><b>Api</b> - to create, remove, and administer Traccar entities (users, devices, etc.)
 *   <li><b>TrackerRegistration</b> - to create and manage GPS tracker registrations
 *   <li><b>RealTimeAppService</b> - high-level service combining the above components for common
 *       operations
 * </ul>
 *
 * <p>This context is injected into scenarios to provide access to all necessary RealTimeApp
 * components without tight coupling.
 */
@Component
public class RealTimeAppContext {

  private final RealTimeController realTimeController;
  private final Api api;
  private final TrackerRegistration trackerRegistration;
  private final RealTimeAppService appService;

  private User realtimeAdmin;

  public RealTimeAppContext(
      RealTimeController realTimeController,
      Api api,
      TrackerRegistration trackerRegistration,
      RealTimeAppService appService) {
    this.realTimeController = realTimeController;
    this.api = api;
    this.trackerRegistration = trackerRegistration;
    this.appService = appService;
  }

  /**
   * Get the RealTimeController to receive websocket messages and manage real-time state.
   *
   * @return the RealTimeController instance
   */
  public RealTimeController getRealTimeController() {
    return realTimeController;
  }

  /**
   * Get the Api to perform CRUD operations on Traccar entities (users, devices, etc.).
   *
   * @return the Api instance
   */
  public Api getApi() {
    return api;
  }

  /**
   * Get the TrackerRegistration to create and manage GPS tracker registrations.
   *
   * @return the TrackerRegistration instance
   */
  public TrackerRegistration getTrackerRegistration() {
    return trackerRegistration;
  }

  /**
   * Get the RealTimeAppService for high-level operations combining controller, API, and tracker
   * registration.
   *
   * @return the RealTimeAppService instance
   */
  public RealTimeAppService getAppService() {
    return appService;
  }

  /**
   * Get the admin user that is logged in to the RealTimeApp.
   *
   * @return the admin User
   */
  public User getRealtimeAdmin() {
    return realtimeAdmin;
  }

  /**
   * Set the admin user that is logged in to the RealTimeApp. This is typically called by the
   * RealTimeAppRunner after successful login.
   *
   * @param realtimeAdmin the admin User
   */
  public void setRealtimeAdmin(User realtimeAdmin) {
    this.realtimeAdmin = realtimeAdmin;
  }
}
