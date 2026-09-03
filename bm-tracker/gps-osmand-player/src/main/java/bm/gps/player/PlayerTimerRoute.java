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
package bm.gps.player;

import org.apache.camel.builder.RouteBuilder;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

/**
 * Camel routes for GPX track playback using Quartz as consumer only.
 *
 * <ul>
 *   <li><b>Logic route</b> – Quartz consumer with a far-future cron (year 2099) so it registers
 *       cleanly but never fires on its own. {@link Scheduler#rescheduleJob(TriggerKey, Trigger)}
 *       updates the trigger time via the native Quartz API, avoiding any Camel Quartz producer
 *       usage.
 * </ul>
 */
public class PlayerTimerRoute extends RouteBuilder {

  private final String playbackQuartzUri;
  private final String routeId;

  /** Far-future cron so the route registers without auto-firing. */
  private static final String CRON_NEVER = "0+0+0+1+1+?+2099";

  public PlayerTimerRoute(String quartzGroup, String quartzJob, String routeIdSuffix) {
    this.playbackQuartzUri = "quartz://" + quartzGroup + "/" + quartzJob;
    this.routeId = "osmand-track-playback-" + routeIdSuffix;
  }

  @Override
  public void configure() {

    // Logic: Quartz consumer – initially set to year 2099, rescheduled dynamically by the bean
    from(playbackQuartzUri + "?cron=" + CRON_NEVER)
        .routeId(routeId)
        // .log("Track-Event at: ${date:now:HH:mm:ss}")
        .bean("quartzSchedulerBean", "sendCurrentAndScheduleNext");
  }
}
