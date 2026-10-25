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

import java.util.Optional;

/**
 * Means of transportation on the Scotland Yard board.
 *
 * <p>From any given station, movement is possible along a link matching the modality to the closest
 * adjacent station for that modality. Each modality corresponds to a neighbour list on the station
 * features and to the {@code transport} property of the link features in {@code london.geojson}.
 */
public enum Modality {
  /** Taxi - the street network, property {@code taxi}. */
  TAXI("taxi"),
  /** Bus - property {@code bus}. */
  BUS("bus"),
  /** Subway / London Underground - property {@code underground}. */
  SUB("underground"),
  /** Ferry on the Thames (black ticket only) - property {@code boat}. */
  FERRY("boat");

  private final String key;

  Modality(String key) {
    this.key = key;
  }

  /** The property key / {@code transport} value used in {@code london.geojson}. */
  public String key() {
    return key;
  }

  /** Resolve a GeoJSON property key or {@code transport} value, e.g. {@code "underground"}. */
  public static Optional<Modality> fromKey(String key) {
    if (key == null) {
      return Optional.empty();
    }
    for (Modality m : values()) {
      if (m.key.equalsIgnoreCase(key) || m.name().equalsIgnoreCase(key)) {
        return Optional.of(m);
      }
    }
    return Optional.empty();
  }
}
