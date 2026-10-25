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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Scotland Yard game board as an immutable, navigable graph.
 *
 * <p>The board is loaded from the GeoJSON {@code FeatureCollection} {@value #RESOURCE} on the
 * classpath (see {@code asciidoc/mapping/sy-board.adoc}) with the GeoTools {@link GeoJSONReader}:
 *
 * <ul>
 *   <li>{@code Point} features with {@code kind=station} become {@link Station}s. Their {@code
 *       taxi}, {@code bus}, {@code underground} and {@code boat} properties hold the neighbour
 *       station numbers per {@link Modality} and define the <b>adjacency</b> of the graph.
 *   <li>{@code LineString} features with {@code kind=link} become {@link Link}s and carry the
 *       printed route geometry between two stations for one modality.
 * </ul>
 *
 * <p>The graph is <b>undirected</b> and <b>trimodal</b> (plus the ferry): from any station a move
 * is possible along a link matching the modality to the adjacent station for that modality. All
 * collections returned by this class are unmodifiable and the board cannot be changed after
 * construction, hence a single instance can be shared by the game controller and all players.
 *
 * <p>Coordinates are WGS84 (EPSG:4326) in GeoJSON order, i.e. JTS {@code x} = longitude and {@code
 * y} = latitude.
 */
public final class GameBoard {
  private static final Logger logger = LoggerFactory.getLogger(GameBoard.class);

  /** Default board resource on the classpath. */
  public static final String RESOURCE = "/london.geojson";

  // GeoJSON property keys, see sy-board.adoc
  static final String P_KIND = "kind";
  static final String KIND_STATION = "station";
  static final String KIND_LINK = "link";
  static final String P_NO = "no";
  static final String P_NAME = "name";
  static final String P_START = "start";
  static final String P_TRANSPORT = "transport";
  static final String P_FROM = "from";
  static final String P_TO = "to";
  static final String P_VIA = "via";

  private final Map<Integer, Station> stations; // sorted by number
  private final List<Station> startStations;
  private final Map<Integer, Map<Modality, Set<Station>>> neighbours;
  private final Map<Integer, Set<Station>> anyNeighbours;
  private final Map<String, Link> links;
  private final Map<Integer, Set<Link>> linksByStation;
  private final ReferencedEnvelope bounds;
  private final int maxStation;
  private final int maxStartStation;

  /** Load the default board {@value #RESOURCE} from the classpath. */
  public GameBoard() {
    this(openResource(RESOURCE));
  }

  /**
   * Load a board from a GeoJSON stream in the format described in {@code sy-board.adoc}.
   *
   * @param geojson the GeoJSON {@code FeatureCollection}, closed by this constructor
   * @throws UncheckedIOException if the stream cannot be read or parsed
   * @throws IllegalStateException if the board data is inconsistent
   */
  public GameBoard(InputStream geojson) {
    Objects.requireNonNull(geojson, "geojson");

    Map<Integer, Station> stationMap = new TreeMap<>();
    Map<Integer, EnumMap<Modality, Set<Integer>>> adjacency = new HashMap<>();
    List<Link> linkList = new ArrayList<>();
    ReferencedEnvelope envelope = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);

    try (InputStream in = geojson;
        GeoJSONReader reader = new GeoJSONReader(in);
        SimpleFeatureIterator features = reader.getIterator()) {
      while (features.hasNext()) {
        SimpleFeature feature = features.next();
        String kind = asString(feature.getAttribute(P_KIND));
        if (KIND_STATION.equals(kind)) {
          readStation(feature, stationMap, adjacency);
        } else if (KIND_LINK.equals(kind)) {
          readLink(feature, linkList);
        } else {
          logger.warn("ignoring feature {} with kind '{}'", feature.getID(), kind);
        }
        Geometry geometry = (Geometry) feature.getDefaultGeometry();
        if (geometry != null) {
          envelope.expandToInclude(geometry.getEnvelopeInternal());
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read game board", e);
    }
    if (stationMap.isEmpty()) {
      throw new IllegalStateException("game board without stations");
    }

    // links connect existing stations and complete the adjacency (undirected)
    Map<String, Link> linkMap = new HashMap<>();
    for (Link link : linkList) {
      requireStation(stationMap, link.from(), link);
      requireStation(stationMap, link.to(), link);
      Link previous = linkMap.putIfAbsent(key(link.modality(), link.from(), link.to()), link);
      if (previous != null) {
        throw new IllegalStateException("duplicate link " + link);
      }
      addNeighbour(adjacency, link.modality(), link.from(), link.to());
    }

    // make the adjacency symmetric and resolve numbers to stations
    Map<Integer, Map<Modality, Set<Station>>> nb = new HashMap<>();
    Map<Integer, Set<Station>> any = new HashMap<>();
    Map<Integer, Set<Link>> byStation = new HashMap<>();
    List<int[]> reverse = new ArrayList<>(); // {modality ordinal, from, to}
    for (Map.Entry<Integer, EnumMap<Modality, Set<Integer>>> e : adjacency.entrySet()) {
      for (Map.Entry<Modality, Set<Integer>> m : e.getValue().entrySet()) {
        for (Integer other : m.getValue()) {
          requireStation(stationMap, other, "neighbour of station " + e.getKey());
          reverse.add(new int[] {m.getKey().ordinal(), other, e.getKey()});
        }
      }
    }
    for (int[] r : reverse) {
      addNeighbour(adjacency, Modality.values()[r[0]], r[1], r[2]);
    }
    for (Station station : stationMap.values()) {
      EnumMap<Modality, Set<Station>> perModality = new EnumMap<>(Modality.class);
      Set<Station> all = new TreeSet<>(Comparator.comparingInt(Station::number));
      EnumMap<Modality, Set<Integer>> numbers = adjacency.get(station.number());
      for (Modality modality : Modality.values()) {
        Set<Station> set = new TreeSet<>(Comparator.comparingInt(Station::number));
        if (numbers != null && numbers.containsKey(modality)) {
          for (Integer n : numbers.get(modality)) {
            set.add(stationMap.get(n));
          }
        }
        all.addAll(set);
        perModality.put(modality, Collections.unmodifiableSet(set));
      }
      nb.put(station.number(), Collections.unmodifiableMap(perModality));
      any.put(station.number(), Collections.unmodifiableSet(all));
      byStation.put(station.number(), new LinkedHashSet<>());
    }
    for (Link link : linkMap.values()) {
      byStation.get(link.from()).add(link);
      byStation.get(link.to()).add(link);
    }
    byStation.replaceAll((k, v) -> Collections.unmodifiableSet(v));

    this.stations = Collections.unmodifiableMap(stationMap);
    this.startStations = stationMap.values().stream().filter(Station::start).toList();
    this.neighbours = Collections.unmodifiableMap(nb);
    this.anyNeighbours = Collections.unmodifiableMap(any);
    this.links = Collections.unmodifiableMap(linkMap);
    this.linksByStation = Collections.unmodifiableMap(byStation);
    this.bounds = envelope;
    this.maxStation = ((TreeMap<Integer, Station>) stationMap).lastKey();
    this.maxStartStation =
        startStations.stream().mapToInt(Station::number).max().orElse(maxStation);

    logger.info(
        "loaded game board with {} stations ({} start stations), {} links, bounds {}",
        stations.size(),
        startStations.size(),
        links.size(),
        bounds);
  }

  // --------------------------------------------------------------- stations

  /**
   * The station with the given number.
   *
   * @throws IllegalArgumentException if the board has no such station
   */
  public Station getStation(int number) {
    Station station = stations.get(number);
    if (station == null) {
      throw new IllegalArgumentException("no station " + number + " on the board");
    }
    return station;
  }

  /** Does the board have a station with this number? */
  public boolean hasStation(int number) {
    return stations.containsKey(number);
  }

  /** All stations ordered by number. */
  public Collection<Station> getStations() {
    return stations.values();
  }

  /** Number of stations on the board. */
  public int stationCount() {
    return stations.size();
  }

  /** The highest station number on the board (199 for London). */
  public int maxStation() {
    return maxStation;
  }

  /** The highest station number that is a start station (198 for London). */
  public int maxStartStation() {
    return maxStartStation;
  }

  /** The start stations (18 start cards in the original game) ordered by number. */
  public List<Station> getStartStations() {
    return startStations;
  }

  /** WGS84 bounding box of all stations and links, {@code x} = longitude, {@code y} = latitude. */
  public ReferencedEnvelope getBoundbox() {
    return new ReferencedEnvelope(bounds);
  }

  // ------------------------------------------------------------- navigation

  /** All stations reachable from {@code station} with any modality, ordered by number. */
  public Set<Station> getNeighbors(Station station) {
    return getNeighbors(station.number());
  }

  /** All stations reachable from station {@code number} with any modality, ordered by number. */
  public Set<Station> getNeighbors(int number) {
    Set<Station> set = anyNeighbours.get(number);
    if (set == null) {
      throw new IllegalArgumentException("no station " + number + " on the board");
    }
    return set;
  }

  /** The stations reachable from {@code station} with the given modality, ordered by number. */
  public Set<Station> getNeighbors(Station station, Modality modality) {
    return getNeighbors(station.number(), modality);
  }

  /** The stations reachable from station {@code number} with the given modality. */
  public Set<Station> getNeighbors(int number, Modality modality) {
    Objects.requireNonNull(modality, "modality");
    Map<Modality, Set<Station>> perModality = neighbours.get(number);
    if (perModality == null) {
      throw new IllegalArgumentException("no station " + number + " on the board");
    }
    return perModality.get(modality);
  }

  /** The modalities available at a station, i.e. with at least one neighbour. */
  public Set<Modality> getModalities(Station station) {
    EnumSet<Modality> set = EnumSet.noneOf(Modality.class);
    for (Modality modality : Modality.values()) {
      if (!getNeighbors(station, modality).isEmpty()) {
        set.add(modality);
      }
    }
    return Collections.unmodifiableSet(set);
  }

  /** The modalities that connect two stations directly (empty if not adjacent). */
  public Set<Modality> getModalities(Station from, Station to) {
    EnumSet<Modality> set = EnumSet.noneOf(Modality.class);
    for (Modality modality : Modality.values()) {
      if (getNeighbors(from, modality).contains(to)) {
        set.add(modality);
      }
    }
    return Collections.unmodifiableSet(set);
  }

  /** Is a move from {@code from} to {@code to} with the given modality legal on the board? */
  public boolean canMove(Station from, Station to, Modality modality) {
    return getNeighbors(from, modality).contains(to);
  }

  // ------------------------------------------------------------------ links

  /** All links of the board. */
  public Collection<Link> getLinks() {
    return links.values();
  }

  /** Number of links on the board. */
  public int linkCount() {
    return links.size();
  }

  /** All links ending at the given station. */
  public Set<Link> getLinks(Station station) {
    Set<Link> set = linksByStation.get(station.number());
    if (set == null) {
      throw new IllegalArgumentException("no station " + station.number() + " on the board");
    }
    return set;
  }

  /** The link (with its route geometry) between two adjacent stations for a modality. */
  public Optional<Link> getLink(Station from, Station to, Modality modality) {
    return Optional.ofNullable(links.get(key(modality, from.number(), to.number())));
  }

  @Override
  public String toString() {
    return "GameBoard[" + stations.size() + " stations, " + links.size() + " links]";
  }

  // ---------------------------------------------------------------- parsing

  private static void readStation(
      SimpleFeature feature,
      Map<Integer, Station> stations,
      Map<Integer, EnumMap<Modality, Set<Integer>>> adjacency) {
    int number = asInt(feature.getAttribute(P_NO), feature);
    if (!(feature.getDefaultGeometry() instanceof Point point)) {
      throw new IllegalStateException("station " + number + " is not a Point");
    }
    Station station =
        new Station(
            number,
            asString(feature.getAttribute(P_NAME)),
            asBoolean(feature.getAttribute(P_START)),
            point);
    if (stations.putIfAbsent(number, station) != null) {
      throw new IllegalStateException("duplicate station " + number);
    }
    for (Modality modality : Modality.values()) {
      for (int neighbour : asIntList(feature.getAttribute(modality.key()), feature)) {
        addNeighbour(adjacency, modality, number, neighbour);
      }
    }
  }

  private static void readLink(SimpleFeature feature, List<Link> links) {
    String transport = asString(feature.getAttribute(P_TRANSPORT));
    Modality modality =
        Modality.fromKey(transport)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "unknown transport '" + transport + "' in link " + feature.getID()));
    if (!(feature.getDefaultGeometry() instanceof LineString route)) {
      throw new IllegalStateException("link " + feature.getID() + " is not a LineString");
    }
    links.add(
        new Link(
            modality,
            asInt(feature.getAttribute(P_FROM), feature),
            asInt(feature.getAttribute(P_TO), feature),
            asIntList(feature.getAttribute(P_VIA), feature),
            route));
  }

  private static void addNeighbour(
      Map<Integer, EnumMap<Modality, Set<Integer>>> adjacency, Modality modality, int a, int b) {
    if (a == b) {
      throw new IllegalStateException("station " + a + " must not link to itself");
    }
    adjacency
        .computeIfAbsent(a, k -> new EnumMap<>(Modality.class))
        .computeIfAbsent(modality, k -> new TreeSet<>())
        .add(b);
  }

  private static void requireStation(Map<Integer, Station> stations, int number, Object context) {
    if (!stations.containsKey(number)) {
      throw new IllegalStateException("unknown station " + number + " referenced by " + context);
    }
  }

  private static String key(Modality modality, int a, int b) {
    return modality.key() + "-" + Math.min(a, b) + "-" + Math.max(a, b);
  }

  private static InputStream openResource(String resource) {
    InputStream in = GameBoard.class.getResourceAsStream(resource);
    if (in == null) {
      throw new IllegalStateException("resource " + resource + " not found on the classpath");
    }
    return in;
  }

  /*
   * The properties of london.geojson are written with native JSON types by the generator,
   * but JOSM re-exports every property as a string ("no": "13", "taxi": "[4,14,23,24]").
   * The converters below accept both.
   */

  private static String asString(Object value) {
    if (value == null) {
      return null;
    }
    String s = value.toString().trim();
    return s.isEmpty() || "null".equals(s) ? null : s;
  }

  private static boolean asBoolean(Object value) {
    if (value instanceof Boolean b) {
      return b;
    }
    return Boolean.parseBoolean(asString(value));
  }

  private static int asInt(Object value, SimpleFeature feature) {
    if (value instanceof Number n) {
      return n.intValue();
    }
    String s = asString(value);
    if (s == null) {
      throw new IllegalStateException("missing number in feature " + feature.getID());
    }
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      throw new IllegalStateException("invalid number '" + s + "' in feature " + feature.getID());
    }
  }

  private static List<Integer> asIntList(Object value, SimpleFeature feature) {
    if (value == null) {
      return List.of();
    }
    if (value instanceof Collection<?> c) {
      return c.stream().map(v -> asInt(v, feature)).toList();
    }
    if (value instanceof Object[] a) {
      return asIntList(List.of(a), feature);
    }
    String s = asString(value);
    if (s == null) {
      return List.of();
    }
    s = s.replace("[", "").replace("]", "").replace("\"", "").trim();
    if (s.isEmpty()) {
      return List.of();
    }
    List<Integer> list = new ArrayList<>();
    for (String part : s.split(",")) {
      list.add(asInt(part.trim(), feature));
    }
    return list;
  }
}
