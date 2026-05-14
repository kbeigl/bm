package bm.gps.player;

import bm.gps.MessageOsmand;
import bm.gps.gpx.Gpx2OsmandParser;
import bm.gps.tracker.TrackerOsmAnd;
import java.io.File;
import java.util.List;
import org.apache.camel.component.quartz.QuartzComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.SimpleRegistry;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GPX track player that replays OsmAnd GPS messages via a dedicated Camel context with a
 * per-instance Quartz scheduler.
 *
 * <p><b>Multi-instance isolation:</b> Each {@code PlayerOsmAnd} creates its own {@link
 * org.apache.camel.impl.DefaultCamelContext} with a unique Quartz group/job name derived from the
 * tracker's {@code uniqueId}. This avoids the Quartz {@code TriggerKey} conflict that occurs when
 * multiple players share the same Camel application context and use identical trigger identifiers.
 *
 * <p><b>Scheduling:</b> {@link QuartzSchedulerBean} drives playback by calling {@link
 * org.quartz.Scheduler#rescheduleJob} directly on the native Quartz API. This sidesteps the Camel
 * Quartz component's limitation of being consumer-only (using it as a producer throws {@code
 * UnsupportedOperationException}).
 */
public class PlayerOsmAnd {
  private static final Logger logger = LoggerFactory.getLogger(PlayerOsmAnd.class);

  // package-private for QuartzSchedulerBean access
  TrackerOsmAnd tracker = null;
  // uniqueId is set with tracker (or when loading a GPX file)
  private String uniqueId = "";

  private final Gpx2OsmandParser parser = new Gpx2OsmandParser();
  // package-private for QuartzSchedulerBean access
  int nextIndex = 0;
  List<MessageOsmand> osmandTrack = null;
  // public for simplicity
  public boolean trackIsLoaded = false;
  // TODO
  // private Long lastSentPositionId = null;

  private DefaultCamelContext camel;
  private QuartzSchedulerBean quartzScheduler;

  /**
   * Starts playback for the loaded track.
   *
   * <p>This method fully initializes the playback runtime (if needed), sends the first track
   * message immediately, and delegates scheduling of all subsequent messages to {@link
   * QuartzSchedulerBean} based on GPX timestamps.
   *
   * @return {@code false} if tracker/track is not ready or route initialization fails; {@code true}
   *     otherwise
   */
  public synchronized boolean playOsmAndTrack() {
    if (tracker == null || osmandTrack == null || osmandTrack.isEmpty()) {
      return false;
    }
    if (nextIndex >= osmandTrack.size()) {
      return true;
    }

    if (!ensurePlaybackRoutes()) {
      return false;
    }

    // Send first message immediately and let the bean schedule follow-up messages.
    quartzScheduler.sendCurrentAndScheduleNext();
    return true;
  }

  /**
   * Lazily initializes the per-player Camel context and Quartz playback route.
   *
   * <p>The {@link QuartzComponent} (and therefore the native {@link Scheduler}) is fetched
   * <em>before</em> {@code camel.start()} so that the scheduler reference is available immediately
   * when the far-future cron route fires for the first time. This prevents the "Quartz scheduler
   * not set" warning that would otherwise appear in a narrow race window between context start and
   * the first explicit {@link #playOsmAndTrack()} call.
   *
   * @return {@code true} if routes are ready; {@code false} on startup failure
   */
  private boolean ensurePlaybackRoutes() {
    if (camel != null && camel.isStarted()) {
      return true;
    }

    try {
      String idSuffix = sanitizeQuartzId(uniqueId);
      String quartzGroup = "osmandTrackGroup-" + idSuffix;
      String quartzJob = "dynamicTrackJob-" + idSuffix;

      SimpleRegistry registry = new SimpleRegistry();
      quartzScheduler = new QuartzSchedulerBean(this, new TriggerKey(quartzJob, quartzGroup));
      camel = new DefaultCamelContext(registry);
      registry.bind("quartzSchedulerBean", quartzScheduler);

      // Create/get Quartz component early so the scheduler is available before routes can fire.
      QuartzComponent quartzComponent = (QuartzComponent) camel.getComponent("quartz");
      quartzScheduler.setQuartzScheduler(quartzComponent.getScheduler());

      camel.addRoutes(new PlayerTimerRoute(quartzGroup, quartzJob, idSuffix));
      camel.start();
      return true;
    } catch (Exception e) {
      logger.error("Failed to start playback Camel context.", e);
      return false;
    }
  }

  /**
   * Fallback: resolves the {@link Scheduler} directly from the running Camel context.
   *
   * <p>Used by {@link QuartzSchedulerBean#scheduleFireIn} as a late-binding safety net in case the
   * scheduler was not yet injected via {@link QuartzSchedulerBean#setQuartzScheduler} (e.g. in test
   * scenarios where context start and first trigger fire are very close in time).
   */
  synchronized Scheduler resolveQuartzScheduler() {
    if (camel == null) {
      return null;
    }
    try {
      QuartzComponent quartzComponent = (QuartzComponent) camel.getComponent("quartz");
      return quartzComponent.getScheduler();
    } catch (Exception e) {
      logger.debug("Could not resolve Quartz scheduler from Camel context.", e);
      return null;
    }
  }

  private static String sanitizeQuartzId(String id) {
    if (id == null || id.isBlank()) {
      return "default";
    }
    return id.replaceAll("[^A-Za-z0-9_-]", "_");
  }

  public TrackerOsmAnd getTracker() {
    return tracker;
  }

  public void setTracker(TrackerOsmAnd tracker) {
    this.tracker = tracker;
    this.uniqueId = tracker == null ? "" : tracker.getUniqueId();
    // assert that deviceId is set > registered in server and  controller
  }

  public String getUniqueId() {
    // should be set with tracker
    return uniqueId;
  }

  //  public Long getLastSentPositionId() { return lastSentPositionId; }
  //  public void setLastSentPositionId(Long previousPositionId) {
  //    this.lastSentPositionId = previousPositionId; }

  public void resetNextIndex() {
    this.nextIndex = 0;
  }

  public MessageOsmand getMessage(int index) {
    return osmandTrack.get(index);
  }

  // keep list private ?
  public List<MessageOsmand> getTrack() {
    return osmandTrack;
  }

  public int getTrackSize() {
    return osmandTrack == null ? 0 : osmandTrack.size();
  }

  public List<MessageOsmand> parse(File gpxFile, String uniqueId) {
    return parser.parse(gpxFile, uniqueId);
  }

  public void load(File gpxFile, String uniqueId) {
    this.uniqueId = uniqueId;
    List<MessageOsmand> messages = parser.parse(gpxFile, uniqueId);
    if (messages.size() <= 2) { // <= 1
      trackIsLoaded = false;
    } else {
      osmandTrack = messages;
      trackIsLoaded = true;
      resetNextIndex();
    }
  }

  public void load(File gpxFile) {
    load(gpxFile, uniqueId);
  }
}
