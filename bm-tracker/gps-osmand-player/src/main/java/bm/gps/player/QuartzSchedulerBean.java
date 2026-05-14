package bm.gps.player;

import bm.gps.MessageOsmand;
import java.util.Date;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camel bean that sends the current track point and reschedules the Quartz trigger for the next one
 * using the native Quartz {@link Scheduler#rescheduleJob} API.
 *
 * <p>This avoids the {@code UnsupportedOperationException} thrown when Camel's Quartz component is
 * used as a producer, and also avoids the "Trigger key already in use" error from {@code toD}.
 */
public class QuartzSchedulerBean {
  private static final Logger logger = LoggerFactory.getLogger(QuartzSchedulerBean.class);
  private static final long RETRY_DELAY_MS = 250L;

  private final PlayerOsmAnd player;
  private final TriggerKey triggerKey;
  // @Autowired works but is too late for the first trigger fire
  private Scheduler quartzScheduler;

  /**
   * Each player instance has its own QuartzSchedulerBean with a unique TriggerKey derived from the
   * tracker's uniqueId, ensuring multi-instance isolation without Quartz trigger conflicts.
   */
  public QuartzSchedulerBean(PlayerOsmAnd player, TriggerKey triggerKey) {
    this.player = player;
    this.triggerKey = triggerKey;
  }

  /** Injected by {@link PlayerOsmAnd} after the Camel context has started. */
  public void setQuartzScheduler(Scheduler scheduler) {
    this.quartzScheduler = scheduler;
  }

  /** Reschedules the Quartz trigger to fire {@code delayMs} milliseconds from now. */
  public void scheduleFireIn(long delayMs) {
    if (quartzScheduler == null) {
      quartzScheduler = player.resolveQuartzScheduler();
    }
    if (quartzScheduler == null) {
      logger.warn("Quartz scheduler not set – cannot schedule next trigger.");
      return;
    }
    Date fireTime = new Date(System.currentTimeMillis() + delayMs);
    Trigger newTrigger =
        TriggerBuilder.newTrigger().withIdentity(triggerKey).startAt(fireTime).build();
    try {
      quartzScheduler.rescheduleJob(triggerKey, newTrigger);
      logger.info("next trigger planed in {}s at {}.", delayMs / 1000, fireTime);
    } catch (SchedulerException e) {
      logger.error("Exception replaning Quartz-Trigger.", e);
    }
  }

  /** Called by the Quartz consumer route on each trigger fire. */
  public void sendCurrentAndScheduleNext() {
    synchronized (player) {
      if (player.tracker == null
          || player.osmandTrack == null
          || player.osmandTrack.isEmpty()
          || player.nextIndex >= player.osmandTrack.size()) {
        return;
      }

      MessageOsmand current = player.osmandTrack.get(player.nextIndex);
      try {
        player.tracker.sendMessage(current);
      } catch (RuntimeException e) {
        // Tracker routes can still be spinning up right after playback start; retry shortly.
        logger.warn("Track send failed, retrying in {} ms.", RETRY_DELAY_MS, e);
        scheduleFireIn(RETRY_DELAY_MS);
        return;
      }
      player.nextIndex++;

      if (player.nextIndex >= player.osmandTrack.size()) return;

      long nextTimestamp = player.osmandTrack.get(player.nextIndex).timestamp();
      long currentTimestamp = current.timestamp();
      long delayMs = Math.max(0L, (nextTimestamp - currentTimestamp) * 1000L);

      scheduleFireIn(delayMs);
    }
  }
}
