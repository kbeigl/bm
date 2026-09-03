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
package bm.gps.gpx;

import bm.gps.MessageOsmand;
import bm.gps.player.PlayerOsmAnd;
import bm.gps.tracker.TrackerOsmAnd;
import bm.gps.tracker.TrackerRegistration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

/**
 * Polls a send directory for GPX files during the complete application runtime.
 *
 * <p>On startup the send directory is created if it does not exist. By default it is located next
 * to the executable jar file ({@code <jar-dir>/send}); a different location can be preconfigured
 * via {@code gps.player.send-directory}.
 *
 * <p>Every GPX file placed in the send directory is parsed into an OsmAnd message list and a new
 * {@link PlayerOsmAnd} is created and loaded with it. Processed files are moved to {@code parsed/},
 * invalid files to {@code error/}. This procedure repeats for every file placed in the send folder.
 */
@Component
@ConditionalOnProperty(
    name = "gps.player.loader-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class Gpx2OsmandLoader extends RouteBuilder {
  private static final Logger logger = LoggerFactory.getLogger(Gpx2OsmandLoader.class);

  private final Gpx2OsmandParser parser;
  private final TrackerRegistration trackerRegistration;
  private final String sendDirectory;
  private final String include;
  private final long pollDelayMs;
  private final long initialDelayMs;
  private final String deviceId;

  /** Loaded players by uniqueId, one per successfully parsed GPX file. */
  private final Map<String, PlayerOsmAnd> players = new ConcurrentHashMap<>();

  public Gpx2OsmandLoader(
      Gpx2OsmandParser parser,
      TrackerRegistration trackerRegistration,
      @Value("${gps.player.send-directory:}") String sendDirectory,
      @Value("${gps.player.include:.*\\.gpx}") String include,
      @Value("${gps.player.poll-delay-ms:1000}") long pollDelayMs,
      @Value("${gps.player.initial-delay-ms:0}") long initialDelayMs,
      @Value("${gps.player.device-id:}") String deviceId) {
    this.parser = parser;
    this.trackerRegistration = trackerRegistration;
    this.sendDirectory = resolveSendDirectory(sendDirectory);
    this.include = include;
    this.pollDelayMs = pollDelayMs;
    this.initialDelayMs = initialDelayMs;
    this.deviceId = deviceId;
  }

  /**
   * Resolves the send directory: preconfigured path if set, otherwise a {@code send} directory next
   * to the jar file (or the working directory when not running from a jar).
   */
  static String resolveSendDirectory(String configured) {
    if (configured != null && !configured.isBlank()) {
      return configured.trim();
    }
    // ApplicationHome resolves the directory containing the jar,
    // or the classes dir / working dir during development
    File home = new ApplicationHome(Gpx2OsmandLoader.class).getDir();
    return new File(home, "send").getAbsolutePath();
  }

  @Override
  public void configure() throws IOException {
    Path sendPath = Path.of(sendDirectory);
    Files.createDirectories(sendPath);
    logger.info("GPX loader polling send directory: {}", sendPath.toAbsolutePath());

    onException(IllegalArgumentException.class)
        .maximumRedeliveries(0)
        .handled(false)
        .logExhausted(false)
        .logExhaustedMessageHistory(false)
        .logRetryAttempted(false)
        .logRetryStackTrace(false)
        .logStackTrace(false)
        .retryAttemptedLogLevel(LoggingLevel.DEBUG);

    fromF(
            "file:%s?include=%s&delay=%d&initialDelay=%d&move=parsed/${file:name}&moveFailed=error/${file:name}",
            sendDirectory, include, pollDelayMs, initialDelayMs)
        .routeId("gpx-osmand-loader")
        .process(
            exchange -> {
              File gpxFile = exchange.getIn().getBody(File.class);
              if (gpxFile == null) {
                throw new IllegalArgumentException(
                    "Unable to resolve GPX file from Camel file exchange");
              }
              loadAndStartPlayer(gpxFile);
            });
  }

  /**
   * Parses the GPX file, creates or reuses a {@link PlayerOsmAnd} loaded with the message list and
   * starts playback. If a player for the same uniqueId already exists, it is stopped gracefully and
   * reused for the new track.
   */
  private void loadAndStartPlayer(File gpxFile) {
    String configuredDeviceId = (deviceId == null || deviceId.isBlank()) ? null : deviceId.trim();
    List<MessageOsmand> messages = parser.parse(gpxFile, configuredDeviceId);
    String uniqueId = messages.getFirst().id();

    PlayerOsmAnd player = players.get(uniqueId);
    if (player != null) {
      logger.info(
          "Existing player found for '{}' – stopping gracefully before reloading", uniqueId);
      player.stopOsmAndTrack();
    } else {
      player = new PlayerOsmAnd();
      // re/register tracker as Spring bean: autowires the ProducerTemplate (transmitter)
      // and re/registers the TrackerSender routes via @PostConstruct
      TrackerOsmAnd tracker = trackerRegistration.registerTracker(uniqueId);
      if (tracker == null) {
        throw new IllegalStateException("Could not register tracker for uniqueId " + uniqueId);
      }
      player.setTracker(tracker);
      players.put(uniqueId, player);
    }

    player.setOnPlaybackFinished(p -> onPlayerFinished(uniqueId));
    player.load(uniqueId, messages);

    logger.info(
        "Loaded player '{}' with {} messages from '{}' (players total: {})",
        uniqueId,
        messages.size(),
        gpxFile.getName(),
        players.size());

    if (player.playOsmAndTrack()) {
      logger.info("Started playback for player '{}'", uniqueId);
    } else {
      logger.warn("Could not start playback for player '{}'", uniqueId);
    }
  }

  void onPlayerFinished(String uniqueId) {
    PlayerOsmAnd player = players.remove(uniqueId);
    if (player != null) {
      player.stopOsmAndTrack();
    }
    trackerRegistration.unregisterTracker(uniqueId);
    int remaining = players.size();
    logger.info(
        "player {} has finished playing and is removed. {} remaining.", uniqueId, remaining);
    if (remaining == 0) {
      logger.info("all players have finished playing. Waiting for next gpx file");
    }
  }

  public PlayerOsmAnd getPlayer(String uniqueId) {
    return players.get(uniqueId);
  }

  public Map<String, PlayerOsmAnd> getPlayers() {
    return players;
  }

  public String getSendDirectory() {
    return sendDirectory;
  }
}
