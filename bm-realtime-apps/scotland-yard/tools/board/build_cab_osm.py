#!/usr/bin/env python3
"""
Build ``cab.osm`` - the real London street routes behind the taxi links of the
Scotland Yard board.

Input (src/main/resources)
    london.geojson   geo-referenced board: stations (Point) and links (LineString)
    roads.osm        OSM road network (highway=* ways with their nodes)

Algorithm
    1. Board taxi stations = stations with a non-empty ``taxi`` list,
       board taxi links    = LineString features with transport=taxi.
    2. The road network inside the board bbox (+margin) is loaded into an
       undirected graph (edge weight = metres). Ways a cab cannot use
       (pedestrian, footway, steps, ...) are skipped.
    3. Every board station is snapped to the nearest road node.
    4. For every taxi link A-B the shortest road path between the two
       snapped nodes is computed (Dijkstra).
    5. Every path is written as one way (negative id, the real OSM node ids)
       and one relation per board link, tagged like bus.osm.

Output (src/main/resources/cab.osm)
    * nodes     - the OSM road nodes of all paths; the snapped station node is
                  tagged ``sy:station=<no>``
    * nodes     - one synthetic node per board taxi station (id -<no>,
                  ``sy:station``, ``sy:name``, ``sy:taxi_neighbours``)
    * ways      - one per board link (negative id): ``sy:link=A-B``,
                  ``sy:kind=taxi_path``
    * relations - one per board link (negative id):
                  type=route, route=taxi, sy:link=A-B, sy:from / sy:to,
                  sy:segment_m, sy:osm_ways=<road way ids>,
                  sy:streets=<street names along the path>

Usage
    python3 tools/board/build_cab_osm.py            # from scotland-yard/
    python3 tools/board/build_cab_osm.py --verbose
"""

import argparse
import heapq
import json
import math
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from datetime import date
from pathlib import Path
from xml.sax.saxutils import quoteattr

HERE = Path(__file__).resolve().parent
MODULE = HERE.parent.parent
RES = MODULE / "src" / "main" / "resources"
GEOJSON = RES / "london.geojson"
ROADS_OSM = RES / "roads.osm"
OUT_PATH = RES / "cab.osm"

BBOX_MARGIN_DEG = 0.01           # ~1 km around the board bbox
NOT_FOR_CABS = {"pedestrian", "footway", "path", "steps", "cycleway",
                "bridleway", "track", "corridor", "platform", "construction",
                "proposed", "raceway", "bus_guideway", "busway"}
SNAP_WARN_M = 150.0              # warn if a station is far from any road node


# ------------------------------------------------------------------ geometry
def dist_m(lat1, lon1, lat2, lon2):
    k = math.pi / 180.0
    x = (lon2 - lon1) * k * math.cos((lat1 + lat2) * 0.5 * k)
    y = (lat2 - lat1) * k
    return 6371000.0 * math.hypot(x, y)


def esc(v):
    return quoteattr(str(v))


# ------------------------------------------------------------------- board
def prop(p, key, default=None):
    """GeoJSON property - JOSM re-exports numbers and lists as strings."""
    v = p.get(key, default)
    if isinstance(v, str):
        s = v.strip()
        if s and (s[0] in "[{-" or s[0].isdigit()):
            try:
                return json.loads(s)
            except ValueError:
                pass
        if s.lower() in ("true", "false"):
            return s.lower() == "true"
        if s.lower() in ("null", "none", ""):
            return None
    return v


def load_board():
    g = json.loads(GEOJSON.read_text(encoding="utf-8"))
    stations, links = {}, []
    for f in g["features"]:
        p = f["properties"]
        kind = prop(p, "kind")
        if kind == "station" and prop(p, "taxi"):
            lon, lat = f["geometry"]["coordinates"]
            no = int(prop(p, "no"))
            stations[no] = {"no": no, "name": prop(p, "name"),
                            "lat": lat, "lon": lon,
                            "taxi": [int(x) for x in prop(p, "taxi")]}
        elif kind == "link" and prop(p, "transport") == "taxi":
            links.append(tuple(sorted((int(prop(p, "from")), int(prop(p, "to"))))))
    lats = [s["lat"] for s in stations.values()]
    lons = [s["lon"] for s in stations.values()]
    bbox = g.get("bbox") or [min(lons), min(lats), max(lons), max(lats)]
    return stations, sorted(set(links)), bbox


# ------------------------------------------------------------------- roads
class Roads:
    def __init__(self, bbox):
        minlon, minlat, maxlon, maxlat = bbox
        self.minlat, self.maxlat = minlat - BBOX_MARGIN_DEG, maxlat + BBOX_MARGIN_DEG
        self.minlon, self.maxlon = minlon - BBOX_MARGIN_DEG, maxlon + BBOX_MARGIN_DEG
        self.nodes = {}          # id -> (lat, lon)
        self.ways = {}           # id -> (refs, tags)
        self.adj = defaultdict(list)   # node -> [(node, metres, way_id)]

    def inside(self, lat, lon):
        return self.minlat <= lat <= self.maxlat and self.minlon <= lon <= self.maxlon

    def parse(self, path):
        for _ev, el in ET.iterparse(str(path), events=("end",)):
            if el.tag == "node":
                lat, lon = float(el.get("lat")), float(el.get("lon"))
                if self.inside(lat, lon):
                    self.nodes[int(el.get("id"))] = (lat, lon)
            elif el.tag == "way":
                tags = {t.get("k"): t.get("v") for t in el.findall("tag")}
                hw = tags.get("highway")
                if hw and hw not in NOT_FOR_CABS:
                    refs = [int(n.get("ref")) for n in el.findall("nd")]
                    if any(r in self.nodes for r in refs):
                        self.ways[int(el.get("id"))] = (refs, tags)
            else:
                continue
            el.clear()
        for wid, (refs, _tags) in self.ways.items():
            for a, b in zip(refs, refs[1:]):
                na, nb = self.nodes.get(a), self.nodes.get(b)
                if na and nb:
                    d = dist_m(na[0], na[1], nb[0], nb[1])
                    self.adj[a].append((b, d, wid))
                    self.adj[b].append((a, d, wid))

    def nearest_node(self, lat, lon):
        best, best_d = None, float("inf")
        for nid in self.adj:          # only nodes that are part of the graph
            la, lo = self.nodes[nid]
            d = dist_m(lat, lon, la, lo)
            if d < best_d:
                best, best_d = nid, d
        return best, best_d

    def shortest_path(self, src, dst):
        """Dijkstra -> (metres, [node ids], [way ids]) or None."""
        dist = {src: 0.0}
        prev = {}
        heap = [(0.0, src)]
        while heap:
            d, u = heapq.heappop(heap)
            if u == dst:
                break
            if d > dist.get(u, float("inf")):
                continue
            for v, w, wid in self.adj[u]:
                nd = d + w
                if nd < dist.get(v, float("inf")):
                    dist[v] = nd
                    prev[v] = (u, wid)
                    heapq.heappush(heap, (nd, v))
        if dst not in dist:
            return None
        nodes, ways = [dst], []
        u = dst
        while u != src:
            u, wid = prev[u]
            nodes.append(u)
            if not ways or ways[-1] != wid:
                ways.append(wid)
        nodes.reverse()
        ways.reverse()
        return dist[dst], nodes, ways


# ----------------------------------------------------------------- output
def write_osm(roads, stations, links, paths, snapped, verbose):
    out = ["<?xml version='1.0' encoding='UTF-8'?>",
           "<osm version='0.6' generator='tools/board/build_cab_osm.py' upload='false'>"]
    lats = [s["lat"] for s in stations.values()]
    lons = [s["lon"] for s in stations.values()]
    out.append("  <bounds minlat=%s minlon=%s maxlat=%s maxlon=%s />"
               % (esc(min(lats)), esc(min(lons)), esc(max(lats)), esc(max(lons))))

    stamp = "%sT00:00:00Z" % date.today().isoformat()
    attrs = "visible='true' version='1' timestamp='%s'" % stamp
    node2station = {nid: no for no, (nid, _d) in snapped.items()}

    # synthetic board station nodes
    for st in sorted(stations.values(), key=lambda s: s["no"]):
        tags = {"sy:station": st["no"], "sy:kind": "board_station",
                "sy:taxi_neighbours": ";".join(map(str, st["taxi"])),
                "sy:osm_node": snapped[st["no"]][0],
                "sy:snap_m": int(snapped[st["no"]][1])}
        if st["name"]:
            tags["sy:name"] = st["name"]
        out.append("  <node id='%d' visible='true' lat=%s lon=%s>"
                   % (-st["no"], esc(st["lat"]), esc(st["lon"])))
        for k, v in tags.items():
            out.append("    <tag k=%s v=%s />" % (esc(k), esc(v)))
        out.append("  </node>")

    # OSM road nodes
    used_nodes = set()
    for p in paths.values():
        if p:
            used_nodes.update(p[1])
    for nid in sorted(used_nodes):
        lat, lon = roads.nodes[nid]
        st = node2station.get(nid)
        if st is not None:
            out.append("  <node id='%d' %s lat=%s lon=%s>" % (nid, attrs, esc(lat), esc(lon)))
            out.append("    <tag k='sy:station' v='%d' />" % st)
            out.append("  </node>")
        else:
            out.append("  <node id='%d' %s lat=%s lon=%s />" % (nid, attrs, esc(lat), esc(lon)))

    # one way + one relation per link
    ways_out, rels_out = [], []
    matched = 0
    for idx, (a, b) in enumerate(links):
        way_id, rel_id = -(2000 + idx), -(1000 + idx)
        p = paths.get((a, b))
        link = "%d-%d" % (a, b)
        if not p:
            if verbose:
                print("UNMATCHED %3d - %3d" % (a, b), file=sys.stderr)
            rels_out.append("  <relation id='%d' visible='true'>" % rel_id)
            rels_out.append("    <member type='node' ref='%d' role='stop' />" % -a)
            rels_out.append("    <member type='node' ref='%d' role='stop' />" % -b)
            for k, v in {"type": "route", "route": "taxi", "sy:link": link,
                         "sy:from": a, "sy:to": b, "sy:unmatched": "yes",
                         "name": "Scotland Yard taxi %d - %d (no road path found)" % (a, b)}.items():
                rels_out.append("    <tag k=%s v=%s />" % (esc(k), esc(v)))
            rels_out.append("  </relation>")
            continue
        matched += 1
        length, nodes, way_ids = p
        streets = []
        for wid in way_ids:
            nm = roads.ways[wid][1].get("name")
            if nm and (not streets or streets[-1] != nm):
                streets.append(nm)
        ways_out.append("  <way id='%d' visible='true'>" % way_id)
        for nid in nodes:
            ways_out.append("    <nd ref='%d' />" % nid)
        for k, v in {"highway": "road", "sy:kind": "taxi_path", "sy:link": link,
                     "sy:from": a, "sy:to": b}.items():
            ways_out.append("    <tag k=%s v=%s />" % (esc(k), esc(v)))
        ways_out.append("  </way>")

        rels_out.append("  <relation id='%d' visible='true'>" % rel_id)
        rels_out.append("    <member type='node' ref='%d' role='stop' />" % snapped[a][0])
        rels_out.append("    <member type='node' ref='%d' role='stop' />" % snapped[b][0])
        rels_out.append("    <member type='way' ref='%d' role='' />" % way_id)
        tags = {
            "type": "route", "route": "taxi",
            "name": "Scotland Yard taxi %d - %d" % (a, b),
            "sy:link": link, "sy:from": a, "sy:to": b,
            "sy:segment_m": int(length),
            "sy:straight_m": int(dist_m(stations[a]["lat"], stations[a]["lon"],
                                        stations[b]["lat"], stations[b]["lon"])),
            "sy:osm_ways": ";".join(map(str, way_ids)),
            "sy:streets": "; ".join(streets),
        }
        for k, v in tags.items():
            rels_out.append("    <tag k=%s v=%s />" % (esc(k), esc(v)))
        rels_out.append("  </relation>")
        if verbose:
            print("%3d - %3d  %5dm (straight %4dm)  %s"
                  % (a, b, length, tags["sy:straight_m"], tags["sy:streets"]),
                  file=sys.stderr)

    out += ways_out + rels_out + ["</osm>"]
    OUT_PATH.write_text("\n".join(out) + "\n", encoding="utf-8")
    return matched, len(used_nodes)


# ------------------------------------------------------------------- main
def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--verbose", "-v", action="store_true")
    args = ap.parse_args()

    stations, links, bbox = load_board()
    print("board: %d taxi stations, %d taxi links" % (len(stations), len(links)))

    roads = Roads(bbox)
    roads.parse(ROADS_OSM)
    print("roads: %d nodes, %d ways, %d graph nodes inside bbox"
          % (len(roads.nodes), len(roads.ways), len(roads.adj)))

    snapped = {}
    for st in stations.values():
        nid, d = roads.nearest_node(st["lat"], st["lon"])
        snapped[st["no"]] = (nid, d)
        if d > SNAP_WARN_M:
            print("warning: station %d (%s) is %.0f m from the nearest road node"
                  % (st["no"], st["name"], d))

    paths = {}
    for a, b in links:
        paths[(a, b)] = roads.shortest_path(snapped[a][0], snapped[b][0])

    matched, n_nodes = write_osm(roads, stations, links, paths, snapped, args.verbose)
    print("links routed: %d, unrouted: %d" % (matched, len(links) - matched))
    print("written %s (%d nodes, %d ways, %d relations)"
          % (OUT_PATH.relative_to(MODULE), n_nodes, matched, len(links)))


if __name__ == "__main__":
    main()
