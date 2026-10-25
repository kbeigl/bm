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

import java.util.List;
import java.util.Objects;
import org.locationtech.jts.geom.LineString;

/**
 * Immutable, undirected edge of the {@link GameBoard}: a connection between two stations with one
 * {@link Modality} and the printed route as {@link LineString}.
 *
 * <p>A link is identified by its modality and the unordered pair of station numbers. The {@code
 * via} stations are intermediate vertices of the line that cannot be used to get off.
 *
 * @param modality means of transportation
 * @param from station number of the first end
 * @param to station number of the second end
 * @param via station numbers passed on the way (may be empty)
 * @param route printed route, first and last vertex are the stations, WGS84 [lon, lat]
 */
public record Link(Modality modality, int from, int to, List<Integer> via, LineString route) {

  public Link {
    Objects.requireNonNull(modality, "modality");
    Objects.requireNonNull(route, "route");
    via = List.copyOf(via == null ? List.of() : via);
  }

  /** Does this link connect the two stations (in any direction)? */
  public boolean connects(int a, int b) {
    return (from == a && to == b) || (from == b && to == a);
  }

  /** Does this link end at the given station? */
  public boolean touches(int station) {
    return from == station || to == station;
  }

  /** The station at the opposite end of {@code station}. */
  public int otherEnd(int station) {
    if (station == from) {
      return to;
    }
    if (station == to) {
      return from;
    }
    throw new IllegalArgumentException("station " + station + " is not an end of " + this);
  }

  /** Length of the printed route in degrees (only useful for comparison, not a distance). */
  public double length() {
    return route.getLength();
  }

  /** Links are undirected: {@code from}/{@code to} swapped is the same link. */
  @Override
  public boolean equals(Object o) {
    return o instanceof Link l && l.modality == modality && l.connects(from, to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modality, Math.min(from, to), Math.max(from, to));
  }

  @Override
  public String toString() {
    return modality.key() + "-" + Math.min(from, to) + "-" + Math.max(from, to);
  }
}
