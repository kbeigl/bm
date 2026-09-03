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
package bm.gps;

/** A record (immutable data class) to hold a single GPS snapshot. */
public record MessageOsmand(
    // device uniqueId
    String id,
    double lat,
    double lon,
    // gps fixtime
    long timestamp,
    // should be helpful:
    // long deviceTimestamp,
    // nullable object fields
    Double speed,
    Double bearing,
    Double altitude,
    Double battery,
    // remove hdop ? lacks meaning in roaf
    Double hdop) {

  /*  Factory methods to create a GpsMessage timestamp (epoch seconds)
   *  with the current system time
   */
  public static MessageOsmand now(
      String id,
      double lat,
      double lon,
      double speed,
      double bearing,
      double altitude,
      Double battery,
      Double hdop) {
    return new MessageOsmand(
        id,
        lat,
        lon,
        System.currentTimeMillis() / 1000L,
        Double.valueOf(speed),
        Double.valueOf(bearing),
        Double.valueOf(altitude),
        battery,
        hdop);
  }

  /** Functional string representation of the GPS message to append to the HTTP GET request */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("id=")
        .append(id)
        .append("&lat=")
        .append(lat)
        .append("&lon=")
        .append(lon)
        .append("&timestamp=")
        .append(timestamp);
    if (speed != null) {
      sb.append("&speed=").append(speed);
    }
    if (bearing != null) {
      sb.append("&bearing=").append(bearing);
    }
    if (altitude != null) {
      sb.append("&altitude=").append(altitude);
    }
    if (battery != null) {
      sb.append("&batt=").append(battery);
    }
    if (hdop != null) {
      sb.append("&hdop=").append(hdop);
    }
    return sb.toString();
  }
}
