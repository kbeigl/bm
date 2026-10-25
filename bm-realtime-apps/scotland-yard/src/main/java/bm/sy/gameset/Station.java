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
package bm.sy.gameset;

import java.util.Objects;
import org.locationtech.jts.geom.Point;

/**
 * Immutable node of the {@link GameBoard}: a numbered station on the London map.
 *
 * @param number station number as printed on the board (1 .. {@link GameBoard#maxStation()})
 * @param name real name of the station, {@code null} for the interpolated stations
 * @param start {@code true} for one of the 18 start cards of the original game
 * @param location WGS84 position, {@code x} = longitude, {@code y} = latitude
 */
public record Station(int number, String name, boolean start, Point location) {

  public Station {
    if (number < 1) {
      throw new IllegalArgumentException("station number must be >= 1: " + number);
    }
    Objects.requireNonNull(location, "location");
  }

  /** Longitude in degrees (GeoJSON / JTS x). */
  public double lon() {
    return location.getX();
  }

  /** Latitude in degrees (GeoJSON / JTS y). */
  public double lat() {
    return location.getY();
  }

  /** Stations are identified by their number only. */
  @Override
  public boolean equals(Object o) {
    return o instanceof Station s && s.number == number;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(number);
  }

  @Override
  public String toString() {
    return name == null ? "Station " + number : "Station " + number + " (" + name + ")";
  }
}
