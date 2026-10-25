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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Plain JUnit 5 tests for the {@link GameBoard} loaded from {@code london.geojson}. */
class GameBoardTest {

  // facts of the London board, see sy-board.adoc
  static final int STATIONS = 199;
  static final int LINKS = 461;
  static final List<Integer> START_STATIONS =
      List.of(13, 26, 29, 34, 50, 53, 91, 94, 103, 112, 117, 132, 138, 141, 155, 174, 197, 198);

  static GameBoard board;

  @BeforeAll
  static void loadBoard() {
    board = new GameBoard();
  }

  @Test
  void loadsLondonFromClasspath() {
    assertNotNull(board);
    assertEquals(STATIONS, board.stationCount());
    assertEquals(STATIONS, board.getStations().size());
    assertEquals(LINKS, board.linkCount());
    assertEquals(LINKS, board.getLinks().size());
    assertEquals("GameBoard[199 stations, 461 links]", board.toString());
  }

  @Nested
  class Stations {

    @Test
    void maxStation() {
      assertEquals(199, board.maxStation());
      assertTrue(board.hasStation(1));
      assertTrue(board.hasStation(199));
      assertFalse(board.hasStation(0));
      assertFalse(board.hasStation(200));
    }

    @Test
    void stationsAreNumberedContinuously() {
      int expected = 1;
      for (Station s : board.getStations()) {
        assertEquals(expected++, s.number(), "stations ordered by number without gaps");
      }
    }

    @Test
    void getStation() {
      Station s13 = board.getStation(13);
      assertEquals(13, s13.number());
      assertEquals("King's Cross St Pancras", s13.name());
      assertTrue(s13.start());
      assertEquals(-0.12122397564, s13.lon(), 1e-9);
      assertEquals(51.53088264838, s13.lat(), 1e-9);
      assertSame(s13, board.getStation(13), "stations are shared instances");

      Station s64 = board.getStation(64);
      assertFalse(s64.start());
      assertEquals(null, s64.name(), "interpolated station has no name");
    }

    @Test
    void unknownStation() {
      assertThrows(IllegalArgumentException.class, () -> board.getStation(0));
      assertThrows(IllegalArgumentException.class, () -> board.getStation(200));
      assertThrows(IllegalArgumentException.class, () -> board.getNeighbors(200));
      assertThrows(IllegalArgumentException.class, () -> board.getNeighbors(200, Modality.TAXI));
    }

    @Test
    void startStations() {
      List<Integer> numbers =
          board.getStartStations().stream().map(Station::number).collect(Collectors.toList());
      assertEquals(START_STATIONS, numbers);
      assertEquals(198, board.maxStartStation());
      assertTrue(board.getStartStations().stream().allMatch(Station::start));
      long startFlags = board.getStations().stream().filter(Station::start).count();
      assertEquals(START_STATIONS.size(), startFlags);
    }

    @Test
    void anchorStationsHaveNames() {
      long named = board.getStations().stream().filter(s -> s.name() != null).count();
      assertEquals(14, named, "14 underground anchor stations carry their real name");
      assertEquals("Baker Street", board.getStation(46).name());
      assertEquals("Vauxhall", board.getStation(185).name());
    }
  }

  @Nested
  class BoundingBox {

    @Test
    void coversLondonCentre() {
      ReferencedEnvelope bbox = board.getBoundbox();
      assertNotNull(bbox.getCoordinateReferenceSystem());
      // x = lon, y = lat (GeoJSON order)
      assertEquals(-0.1875, bbox.getMinX(), 0.001);
      assertEquals(-0.0717, bbox.getMaxX(), 0.001);
      assertEquals(51.4804, bbox.getMinY(), 0.001);
      assertEquals(51.5366, bbox.getMaxY(), 0.001);
      for (Station s : board.getStations()) {
        assertTrue(bbox.contains(s.lon(), s.lat()), s + " inside bounding box");
      }
    }

    @Test
    void isACopy() {
      ReferencedEnvelope bbox = board.getBoundbox();
      bbox.expandToInclude(0, 0);
      assertFalse(board.getBoundbox().contains(0, 0), "board bounds must not be modifiable");
    }
  }

  @Nested
  class Neighbours {

    @Test
    void station13PerModality() {
      Station s13 = board.getStation(13);
      assertEquals(Set.of(4, 14, 23, 24), numbers(board.getNeighbors(s13, Modality.TAXI)));
      assertEquals(Set.of(14, 23, 52), numbers(board.getNeighbors(s13, Modality.BUS)));
      assertEquals(Set.of(46, 67, 89), numbers(board.getNeighbors(s13, Modality.SUB)));
      assertEquals(Set.of(), numbers(board.getNeighbors(s13, Modality.FERRY)));
      assertEquals(Set.of(Modality.TAXI, Modality.BUS, Modality.SUB), board.getModalities(s13));
    }

    @Test
    void station13AnyModality() {
      Set<Station> any = board.getNeighbors(board.getStation(13));
      assertEquals(Set.of(4, 14, 23, 24, 52, 46, 67, 89), numbers(any));
      // ordered by number
      assertEquals(
          List.of(4, 14, 23, 24, 46, 52, 67, 89), any.stream().map(Station::number).toList());
      assertEquals(any, board.getNeighbors(13));
    }

    @Test
    void station1() {
      Station s1 = board.getStation(1);
      assertEquals(Set.of(8, 9), numbers(board.getNeighbors(s1, Modality.TAXI)));
      assertEquals(Set.of(46), numbers(board.getNeighbors(s1, Modality.BUS)));
      assertEquals(Set.of(46), numbers(board.getNeighbors(s1, Modality.SUB)));
      assertEquals(Set.of(8, 9, 46), numbers(board.getNeighbors(s1)));
    }

    @Test
    void ferry() {
      // the three boat links on the Thames: 108-115, 115-157, 157-194
      assertEquals(Set.of(115), numbers(board.getNeighbors(108, Modality.FERRY)));
      assertEquals(Set.of(108, 157), numbers(board.getNeighbors(115, Modality.FERRY)));
      assertEquals(Set.of(115, 194), numbers(board.getNeighbors(157, Modality.FERRY)));
      assertEquals(Set.of(157), numbers(board.getNeighbors(194, Modality.FERRY)));
      long ferryStations =
          board.getStations().stream()
              .filter(s -> !board.getNeighbors(s, Modality.FERRY).isEmpty())
              .count();
      assertEquals(4, ferryStations);
    }

    @ParameterizedTest
    @EnumSource(Modality.class)
    void adjacencyIsSymmetric(Modality modality) {
      for (Station a : board.getStations()) {
        for (Station b : board.getNeighbors(a, modality)) {
          assertTrue(
              board.getNeighbors(b, modality).contains(a),
              () -> modality + " " + a + " -> " + b + " has no way back");
          assertTrue(board.canMove(a, b, modality));
          assertTrue(board.canMove(b, a, modality));
        }
      }
    }

    @Test
    void everyStationIsConnectedByTaxi() {
      for (Station s : board.getStations()) {
        assertFalse(board.getNeighbors(s, Modality.TAXI).isEmpty(), s + " has no taxi link");
        assertFalse(board.getNeighbors(s).contains(s), s + " links to itself");
      }
    }

    @Test
    void modalitiesBetweenStations() {
      Station s13 = board.getStation(13);
      assertEquals(
          Set.of(Modality.TAXI, Modality.BUS), board.getModalities(s13, board.getStation(14)));
      assertEquals(Set.of(Modality.SUB), board.getModalities(s13, board.getStation(46)));
      assertEquals(Set.of(), board.getModalities(s13, board.getStation(1)));
      assertTrue(board.canMove(s13, board.getStation(46), Modality.SUB));
      assertFalse(board.canMove(s13, board.getStation(46), Modality.TAXI));
      assertFalse(board.canMove(s13, board.getStation(1), Modality.TAXI));
    }

    @Test
    void neighbourCountMatchesLinkCount() {
      // every undirected link appears twice in the adjacency
      int entries = 0;
      for (Station s : board.getStations()) {
        for (Modality m : Modality.values()) {
          entries += board.getNeighbors(s, m).size();
        }
      }
      assertEquals(2 * LINKS, entries);
    }
  }

  @Nested
  class Links {

    @Test
    void countPerModality() {
      assertEquals(344, count(Modality.TAXI));
      assertEquals(96, count(Modality.BUS));
      assertEquals(18, count(Modality.SUB));
      assertEquals(3, count(Modality.FERRY));
    }

    @Test
    void linkGeometryStartsAndEndsAtStations() {
      for (Link link : board.getLinks()) {
        Station from = board.getStation(link.from());
        Station to = board.getStation(link.to());
        var first = link.route().getStartPoint();
        var last = link.route().getEndPoint();
        boolean forward =
            first.isWithinDistance(from.location(), 1e-5)
                && last.isWithinDistance(to.location(), 1e-5);
        boolean backward =
            first.isWithinDistance(to.location(), 1e-5)
                && last.isWithinDistance(from.location(), 1e-5);
        assertTrue(forward || backward, link + " route does not connect its stations");
        assertTrue(link.route().getNumPoints() >= 2 + link.via().size());
      }
    }

    @Test
    void underground13to46ViaStations() {
      Station s13 = board.getStation(13);
      Station s46 = board.getStation(46);
      Link link = board.getLink(s13, s46, Modality.SUB).orElseThrow();
      assertEquals(Modality.SUB, link.modality());
      assertTrue(link.connects(13, 46));
      assertEquals(46, link.otherEnd(13));
      assertEquals(List.of(47, 34, 22, 23), link.via());
      assertEquals(6, link.route().getNumPoints());
      assertEquals("underground-13-46", link.toString());
      assertSame(link, board.getLink(s46, s13, Modality.SUB).orElseThrow(), "undirected");
      assertTrue(board.getLink(s13, s46, Modality.TAXI).isEmpty());
      assertTrue(board.getLinks(s13).contains(link));
      assertEquals(10, board.getLinks(s13).size(), "4 taxi + 3 bus + 3 underground");
    }

    @Test
    void everyNeighbourHasALink() {
      for (Station a : board.getStations()) {
        for (Modality m : Modality.values()) {
          for (Station b : board.getNeighbors(a, m)) {
            assertTrue(board.getLink(a, b, m).isPresent(), m + " " + a + " - " + b);
          }
        }
      }
    }

    private long count(Modality modality) {
      return board.getLinks().stream().filter(l -> l.modality() == modality).count();
    }
  }

  @Nested
  class Immutability {

    @Test
    void collectionsAreUnmodifiable() {
      Station s13 = board.getStation(13);
      assertThrows(UnsupportedOperationException.class, () -> board.getNeighbors(s13).clear());
      assertThrows(
          UnsupportedOperationException.class,
          () -> board.getNeighbors(s13, Modality.TAXI).add(board.getStation(1)));
      assertThrows(UnsupportedOperationException.class, () -> board.getStations().clear());
      assertThrows(UnsupportedOperationException.class, () -> board.getStartStations().clear());
      assertThrows(UnsupportedOperationException.class, () -> board.getLinks().clear());
      assertThrows(UnsupportedOperationException.class, () -> board.getLinks(s13).clear());
    }

    @Test
    void stationIdentity() {
      assertEquals(board.getStation(5), board.getStation(5));
      assertEquals(board.getStation(5).hashCode(), board.getStation(5).hashCode());
      assertFalse(board.getStation(5).equals(board.getStation(6)));
    }
  }

  @Nested
  @DisplayName("custom boards and error handling")
  class CustomBoards {

    @Test
    void minimalBoardWithNativeJsonTypes() {
      // generator format: numbers, booleans and arrays instead of strings
      String json =
          """
          {"type":"FeatureCollection","features":[
            {"type":"Feature","geometry":{"type":"Point","coordinates":[0,0]},
             "properties":{"kind":"station","no":1,"start":true,"taxi":[2],"bus":[],"underground":[],"boat":[]}},
            {"type":"Feature","geometry":{"type":"Point","coordinates":[1,1]},
             "properties":{"kind":"station","no":2,"start":false,"taxi":[1],"bus":[],"underground":[],"boat":[]}},
            {"type":"Feature","geometry":{"type":"LineString","coordinates":[[0,0],[1,1]]},
             "properties":{"kind":"link","transport":"taxi","from":1,"to":2,"via":[]}}
          ]}
          """;
      GameBoard mini = new GameBoard(stream(json));
      assertEquals(2, mini.stationCount());
      assertEquals(2, mini.maxStation());
      assertEquals(1, mini.maxStartStation());
      assertEquals(1, mini.linkCount());
      assertEquals(Set.of(2), numbers(mini.getNeighbors(1, Modality.TAXI)));
      assertEquals(Set.of(1), numbers(mini.getNeighbors(2, Modality.TAXI)));
      assertTrue(mini.getNeighbors(1, Modality.BUS).isEmpty());
    }

    @Test
    void missingNeighbourListIsCompletedFromLinks() {
      String json =
          """
          {"type":"FeatureCollection","features":[
            {"type":"Feature","geometry":{"type":"Point","coordinates":[0,0]},
             "properties":{"kind":"station","no":"1","start":"false","taxi":"[2]"}},
            {"type":"Feature","geometry":{"type":"Point","coordinates":[1,1]},
             "properties":{"kind":"station","no":"2","start":"false","taxi":"[]"}},
            {"type":"Feature","geometry":{"type":"LineString","coordinates":[[0,0],[1,1]]},
             "properties":{"kind":"link","transport":"bus","from":"1","to":"2","via":"[]"}}
          ]}
          """;
      GameBoard mini = new GameBoard(stream(json));
      assertAll(
          () -> assertEquals(Set.of(2), numbers(mini.getNeighbors(1, Modality.TAXI))),
          () -> assertEquals(Set.of(1), numbers(mini.getNeighbors(2, Modality.TAXI)), "symmetric"),
          () -> assertEquals(Set.of(2), numbers(mini.getNeighbors(1, Modality.BUS)), "from link"),
          () -> assertEquals(Set.of(1), numbers(mini.getNeighbors(2, Modality.BUS))));
    }

    @Test
    void unknownNeighbourFails() {
      String json =
          """
          {"type":"FeatureCollection","features":[
            {"type":"Feature","geometry":{"type":"Point","coordinates":[0,0]},
             "properties":{"kind":"station","no":1,"taxi":[99]}}
          ]}
          """;
      assertThrows(IllegalStateException.class, () -> new GameBoard(stream(json)));
    }

    @Test
    void unknownTransportFails() {
      String json =
          """
          {"type":"FeatureCollection","features":[
            {"type":"Feature","geometry":{"type":"Point","coordinates":[0,0]},
             "properties":{"kind":"station","no":1}},
            {"type":"Feature","geometry":{"type":"LineString","coordinates":[[0,0],[1,1]]},
             "properties":{"kind":"link","transport":"zeppelin","from":1,"to":1}}
          ]}
          """;
      assertThrows(IllegalStateException.class, () -> new GameBoard(stream(json)));
    }

    @Test
    void emptyBoardFails() {
      assertThrows(
          IllegalStateException.class,
          () -> new GameBoard(stream("{\"type\":\"FeatureCollection\",\"features\":[]}")));
    }

    private InputStream stream(String json) {
      return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
  }

  @Test
  void modalityKeys() {
    assertEquals(Modality.SUB, Modality.fromKey("underground").orElseThrow());
    assertEquals(Modality.FERRY, Modality.fromKey("boat").orElseThrow());
    assertEquals(Modality.TAXI, Modality.fromKey("TAXI").orElseThrow());
    assertEquals(Modality.SUB, Modality.fromKey("sub").orElseThrow());
    assertTrue(Modality.fromKey("zeppelin").isEmpty());
    assertTrue(Modality.fromKey(null).isEmpty());
  }

  private static Set<Integer> numbers(Set<Station> stations) {
    return stations.stream().map(Station::number).collect(Collectors.toSet());
  }
}
