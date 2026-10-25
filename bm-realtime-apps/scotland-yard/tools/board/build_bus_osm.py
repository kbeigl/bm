#!/usr/bin/env python3
"""
Build ``bus.osm`` - the real London bus network behind the Scotland Yard board.

Input (src/main/resources)
    london.geojson   geo-referenced board: stations (Point) and links (LineString)
    bus_stops.osm    OSM bus stops          (highway=bus_stop / public_transport=platform)
    bus_routes.osm   OSM bus route relations (type=route, route=bus) incl. ways + nodes
    (``.osm.pbf`` is accepted as well; it is converted on the fly with osmium-tool)

Algorithm
    1. Board bus stations = stations with a non-empty ``bus`` list,
       board bus links    = LineString features with transport=bus.
    2. Every OSM bus stop within STOP_RADIUS_M of a board bus station is
       assigned to its *nearest* board station.
    3. For every OSM route relation the ordered platform/stop members are
       translated into the sequence of board stations they pass.
    4. A board link A-B is served by a route if A and B are consecutive in
       that sequence (either direction). The best route is the one with the
       fewest OSM stops between A and B (ties: shortest way length). Day
       routes are preferred over night routes (ref N...). If no route
       connects A and B directly, a route passing one other board station
       in between is accepted (``sy:skipped_stations``), and finally the
       matching is retried with a wider stop radius (``sy:stop_radius_m``).
    5. For every matched link the ways of the route between the two stops
       (relation member order) are copied into bus.osm together with the two
       stops, as a new relation tagged with the board numbers and the bus
       ``ref``. Unmatched links are reported and written as plain
       ``sy:unmatched`` relations with the board end points only.

Output (src/main/resources/bus.osm)
    * nodes   - the OSM stops used (tag ``sy:station=<no>``) and the way nodes
    * nodes   - one synthetic node per board bus station (negative id,
                ``sy:station``, ``sy:name``) for orientation
    * ways    - the OSM ways of the matched segments
    * relations - one per board bus link (negative id):
                type=route, route=bus, ref=<bus number>, sy:link=A-B,
                sy:from / sy:to, sy:osm_relation=<source relation id>,
                sy:routes=<all bus numbers that serve the link>

Usage
    python3 tools/board/build_bus_osm.py            # from scotland-yard/
    python3 tools/board/build_bus_osm.py --radius 400 --verbose
"""

import argparse
import json
import math
import re
import shutil
import subprocess
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

# transport mode configuration - build_sub_osm.py overrides this dict
MODE = {
    "transport": "bus",                 # board link transport / station list key
    "label": "bus",                     # used in names and sy:<label>_neighbours
    "route": "bus",                     # OSM route=* tag written
    "route_types": ("bus",),            # OSM route=* values read
    "network": "London Buses",
    "generator": "tools/board/build_bus_osm.py",
    "stops": RES / "bus_stops.osm",     # may be None
    "routes": RES / "bus_routes.osm",
    "out": RES / "bus.osm",
    "radius": 350.0,                    # board accuracy is ~100-400 m
    # (stop radius m, max skipped board stations) tried for unmatched links
    "fallback": [(500.0, 1), (650.0, 1), (500.0, 2)],
    "night_prefix": "N",                # refs starting with it are ranked last
    "match_names": False,               # named stops only for same-named stations
}

STOP_RADIUS_M = MODE["radius"]
MAX_SKIP = 1           # a route may pass one other board station between A and B
                       # (fallback only - direct connections are always preferred)

STOP_ROLES = {"platform", "platform_entry_only", "platform_exit_only",
              "stop", "stop_entry_only", "stop_exit_only"}


# ------------------------------------------------------------------ geometry
def dist_m(lat1, lon1, lat2, lon2):
    """Equirectangular distance in metres - good enough for < 50 km."""
    k = math.pi / 180.0
    x = (lon2 - lon1) * k * math.cos((lat1 + lat2) * 0.5 * k)
    y = (lat2 - lat1) * k
    return 6371000.0 * math.hypot(x, y)


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
    tr = MODE["transport"]
    stations, links = {}, []
    for f in g["features"]:
        p = f["properties"]
        kind = prop(p, "kind")
        if kind == "station" and prop(p, tr):
            lon, lat = f["geometry"]["coordinates"]
            no = int(prop(p, "no"))
            stations[no] = {"no": no, "name": prop(p, "name"),
                            "lat": lat, "lon": lon,
                            "neighbours": [int(x) for x in prop(p, tr)]}
        elif kind == "link" and prop(p, "transport") == tr:
            a, b = sorted((int(prop(p, "from")), int(prop(p, "to"))))
            links.append((a, b))
    links = sorted(set(links))
    return stations, links


# --------------------------------------------------------------------- osm
def resolve_osm(path):
    """Accept ``x.osm`` or ``x.osm.pbf`` - whichever exists."""
    path = Path(path)
    if path.exists():
        return path
    for alt in (path.with_suffix(path.suffix + ".pbf"), path.with_suffix("")):
        if alt.exists():
            return alt
    sys.exit("input not found: %s (nor .osm.pbf)" % path)


def open_osm(path):
    """Binary stream of OSM XML; PBF files are converted on the fly with osmium."""
    if str(path).endswith(".pbf"):
        if not shutil.which("osmium"):
            sys.exit("%s is a PBF file - install osmium-tool or convert it to .osm" % path)
        proc = subprocess.Popen(["osmium", "cat", "-f", "osm", str(path)],
                                stdout=subprocess.PIPE)
        return proc.stdout
    return open(path, "rb")


class Osm:
    def __init__(self, route_types=("bus",)):
        self.route_types = set(route_types)
        self.nodes = {}      # id -> (lat, lon, tags)
        self.ways = {}       # id -> (refs, tags)
        self.routes = {}     # id -> (members[(type, ref, role)], tags)

    def parse(self, path, want_ways=True, want_relations=True):
        path = resolve_osm(path)
        with open_osm(path) as src:
            self._parse_stream(src, want_ways, want_relations)

    def _parse_stream(self, src, want_ways, want_relations):
        for _ev, el in ET.iterparse(src, events=("end",)):
            tag = el.tag
            if tag == "node":
                tags = {t.get("k"): t.get("v") for t in el.findall("tag")}
                self.nodes[int(el.get("id"))] = (float(el.get("lat")),
                                                 float(el.get("lon")), tags)
            elif tag == "way" and want_ways:
                refs = [int(n.get("ref")) for n in el.findall("nd")]
                tags = {t.get("k"): t.get("v") for t in el.findall("tag")}
                self.ways[int(el.get("id"))] = (refs, tags)
            elif tag == "relation" and want_relations:
                tags = {t.get("k"): t.get("v") for t in el.findall("tag")}
                if tags.get("type") == "route" and tags.get("route") in self.route_types:
                    members = [(m.get("type"), int(m.get("ref")), m.get("role") or "")
                               for m in el.findall("member")]
                    self.routes[int(el.get("id"))] = (members, tags)
            else:
                continue
            el.clear()

    def is_stop(self, nid):
        n = self.nodes.get(nid)
        if not n:
            return False
        t = n[2]
        return (t.get("highway") == "bus_stop"
                or t.get("railway") in ("stop", "station", "halt")
                or t.get("public_transport") in ("platform", "stop_position", "station"))


# --------------------------------------------------------------- matching
def norm_name(s):
    return re.sub(r"[^a-z0-9]", "", (s or "").lower().replace("saint", "st"))


def names_match(stop_name, station_name):
    a = norm_name(stop_name)
    if not a:
        return False
    for cand in [station_name] + MODE.get("aliases", {}).get(station_name, []):
        b = norm_name(cand)
        if b and (a.startswith(b) or b.startswith(a)):
            return True
    return False


def assign_stops(osm, stations, radius, match_names=False):
    """OSM stop node id -> board station no (nearest within radius).
    With match_names a named stop is only assigned to a board station of the
    same name (tube stop positions of neighbouring stations can lie within the
    radius of a board station, e.g. Lancaster Gate / Paddington)."""
    stop2station = {}
    for nid, (lat, lon, tags) in osm.nodes.items():
        if not osm.is_stop(nid):
            continue
        stop_name = tags.get("name")
        best, best_d = None, radius
        for st in stations.values():
            if match_names and stop_name and st["name"] \
                    and not names_match(stop_name, st["name"]):
                continue
            d = dist_m(lat, lon, st["lat"], st["lon"])
            if d < best_d:
                best, best_d = st["no"], d
        if best is not None:
            stop2station[nid] = best
    return stop2station


def route_sequence(members, stop2station, osm):
    """Ordered list of (station_no, stop_node_id, member_index) along the route.
    Consecutive duplicates (platform + stop_position of the same halt) merged."""
    seq = []
    for i, (mtype, ref, role) in enumerate(members):
        if mtype != "node" or role not in STOP_ROLES:
            continue
        st = stop2station.get(ref)
        if st is None:
            # a stop outside every station radius still counts as an
            # intermediate halt
            seq.append((None, ref, i))
        else:
            if seq and seq[-1][0] == st:
                continue
            seq.append((st, ref, i))
    return seq


def way_order(members):
    """[(member_index, way_id)] of the route's way members (non-platform roles)."""
    return [(i, ref) for i, (mtype, ref, role) in enumerate(members)
            if mtype == "way" and role in ("", "forward", "backward")]


def nearest_way_index(osm, ways, stop_lat, stop_lon):
    """Index into *ways* whose geometry is closest to the stop."""
    best, best_d = None, float("inf")
    for idx, (_mi, wid) in enumerate(ways):
        w = osm.ways.get(wid)
        if not w:
            continue
        for nid in w[0]:
            n = osm.nodes.get(nid)
            if not n:
                continue
            d = dist_m(stop_lat, stop_lon, n[0], n[1])
            if d < best_d:
                best, best_d = idx, d
    return best, best_d


def way_length(osm, wid):
    refs = osm.ways[wid][0]
    total = 0.0
    for a, b in zip(refs, refs[1:]):
        na, nb = osm.nodes.get(a), osm.nodes.get(b)
        if na and nb:
            total += dist_m(na[0], na[1], nb[0], nb[1])
    return total


def find_candidates(osm, stations, links, stop2station):
    """link (a,b) -> list of candidate dicts sorted best first."""
    wanted = set(links)
    cands = defaultdict(list)
    for rid, (members, tags) in osm.routes.items():
        seq = route_sequence(members, stop2station, osm)
        halts = [s for s in seq if s[0] is not None]
        if len(halts) < 2:
            continue
        ways = way_order(members)
        ref = tags.get("ref", "?")
        night = bool(MODE["night_prefix"]) and ref.upper().startswith(MODE["night_prefix"])
        # pair board stations that are at most MAX_SKIP board stations apart
        # along the route (skip=0: consecutive, preferred)
        for k in range(len(halts) - 1):
            for j in range(1, MAX_SKIP + 2):
                if k + j >= len(halts):
                    break
                s1, s2 = halts[k], halts[k + j]
                key = tuple(sorted((s1[0], s2[0])))
                if key not in wanted or s1[0] == s2[0]:
                    continue
                # intermediate OSM stops between the two halts
                i1 = seq.index(s1)
                i2 = seq.index(s2)
                between = i2 - i1 - 1
                # segment of ways between the two stops
                n1 = osm.nodes[s1[1]]
                n2 = osm.nodes[s2[1]]
                w1, d1 = nearest_way_index(osm, ways, n1[0], n1[1])
                w2, d2 = nearest_way_index(osm, ways, n2[0], n2[1])
                seg = []
                if w1 is not None and w2 is not None:
                    lo, hi = sorted((w1, w2))
                    seg = [wid for _mi, wid in ways[lo:hi + 1]]
                length = sum(way_length(osm, w) for w in seg if w in osm.ways)
                cands[key].append({
                    "rel": rid, "ref": ref, "name": tags.get("name", ""),
                    "from_station": s1[0], "to_station": s2[0],
                    "stop_from": s1[1], "stop_to": s2[1],
                    "skip": j - 1, "night": night,
                    "between": between, "ways": seg, "length": length,
                    "snap": max(d1, d2) if seg else float("inf"),
                })
    for key, lst in cands.items():
        lst.sort(key=lambda c: (c["skip"], c["night"], c["between"],
                                not c["ways"], c["length"]))
    return cands


# ----------------------------------------------------------------- output
def esc(v):
    return quoteattr(str(v))


def write_osm(osm, stations, links, cands, radius, verbose):
    label, route = MODE["label"], MODE["route"]
    out = []
    out.append("<?xml version='1.0' encoding='UTF-8'?>")
    out.append("<osm version='0.6' generator='%s' upload='false'>" % MODE["generator"])
    lats = [s["lat"] for s in stations.values()]
    lons = [s["lon"] for s in stations.values()]
    out.append("  <bounds minlat=%s minlon=%s maxlat=%s maxlon=%s />"
               % (esc(min(lats)), esc(min(lons)), esc(max(lats)), esc(max(lons))))

    used_nodes, used_ways = {}, []   # id -> station no or None
    relations = []
    matched = unmatched = 0
    refs_per_station = defaultdict(set)

    for idx, (a, b) in enumerate(links):
        rel_id = -(1000 + idx)
        lst = cands.get((a, b), [])
        all_refs = sorted({c["ref"] for c in lst if c["skip"] == 0},
                          key=lambda r: (len(r), r))
        if not lst:
            unmatched += 1
            if verbose:
                print("UNMATCHED %3d - %3d" % (a, b), file=sys.stderr)
            relations.append((rel_id, [("node", -a, "stop"), ("node", -b, "stop")], {
                "type": "route", "route": route, "sy:link": "%d-%d" % (a, b),
                "sy:from": a, "sy:to": b, "sy:unmatched": "yes",
                "name": "Scotland Yard %s %d - %d (no OSM route found)" % (label, a, b),
            }))
            continue
        matched += 1
        best = lst[0]
        refs_per_station[a].add(best["ref"])
        refs_per_station[b].add(best["ref"])
        for nid, st in ((best["stop_from"], best["from_station"]),
                        (best["stop_to"], best["to_station"])):
            used_nodes[nid] = st
        for wid in best["ways"]:
            if wid in osm.ways:
                used_ways.append(wid)
                for nid in osm.ways[wid][0]:
                    used_nodes.setdefault(nid, None)
        members = [("node", best["stop_from"], "stop"),
                   ("node", best["stop_to"], "stop")]
        members += [("way", wid, "") for wid in best["ways"] if wid in osm.ways]
        tags = {
            "type": "route", "route": route, "ref": best["ref"],
            "network": MODE["network"],
            "name": "Scotland Yard %s %d - %d (%s)" % (label, a, b, best["ref"]),
            "sy:link": "%d-%d" % (a, b), "sy:from": a, "sy:to": b,
            "sy:osm_relation": best["rel"], "sy:osm_route": best["name"],
            "sy:stops_between": best["between"],
            "sy:routes": ";".join(all_refs) or best["ref"],
            "sy:segment_m": int(best["length"]),
        }
        if best["skip"]:
            tags["sy:skipped_stations"] = best["skip"]
        if best.get("radius"):
            tags["sy:stop_radius_m"] = int(best["radius"])
        relations.append((rel_id, members, tags))
        if verbose:
            print("%3d - %3d  route %-5s skip=%d between=%d ways=%d len=%5dm  all=%s"
                  % (a, b, best["ref"], best["skip"], best["between"], len(best["ways"]),
                     best["length"], ",".join(all_refs)), file=sys.stderr)

    # synthetic board station nodes
    for st in sorted(stations.values(), key=lambda s: s["no"]):
        tags = {"sy:station": st["no"], "sy:kind": "board_station",
                "sy:%s_neighbours" % label: ";".join(map(str, st["neighbours"])),
                "sy:routes": ";".join(sorted(refs_per_station.get(st["no"], ()),
                                             key=lambda r: (len(r), r)))}
        if st["name"]:
            tags["sy:name"] = st["name"]
        out.append("  <node id='%d' visible='true' lat=%s lon=%s>"
                   % (-st["no"], esc(st["lat"]), esc(st["lon"])))
        for k, v in tags.items():
            out.append("    <tag k=%s v=%s />" % (esc(k), esc(v)))
        out.append("  </node>")

    # OSM nodes (positive ids need version + timestamp for JOSM)
    stamp = "%sT00:00:00Z" % date.today().isoformat()
    attrs = "visible='true' version='1' timestamp='%s'" % stamp
    for nid in sorted(used_nodes):
        lat, lon, tags = osm.nodes[nid]
        tags = dict(tags)
        st = used_nodes[nid]
        if st is not None:
            tags["sy:station"] = st
        if tags:
            out.append("  <node id='%d' %s lat=%s lon=%s>"
                       % (nid, attrs, esc(lat), esc(lon)))
            for k, v in tags.items():
                out.append("    <tag k=%s v=%s />" % (esc(k), esc(v)))
            out.append("  </node>")
        else:
            out.append("  <node id='%d' %s lat=%s lon=%s />"
                       % (nid, attrs, esc(lat), esc(lon)))

    # OSM ways
    for wid in sorted(set(used_ways)):
        refs, tags = osm.ways[wid]
        out.append("  <way id='%d' %s>" % (wid, attrs))
        for nid in refs:
            out.append("    <nd ref='%d' />" % nid)
        for k, v in tags.items():
            out.append("    <tag k=%s v=%s />" % (esc(k), esc(v)))
        out.append("  </way>")

    # relations
    for rid, members, tags in relations:
        out.append("  <relation id='%d' visible='true'>" % rid)
        for mtype, ref, role in members:
            out.append("    <member type='%s' ref='%d' role=%s />" % (mtype, ref, esc(role)))
        for k, v in tags.items():
            out.append("    <tag k=%s v=%s />" % (esc(k), esc(v)))
        out.append("  </relation>")
    out.append("</osm>")
    MODE["out"].write_text("\n".join(out) + "\n", encoding="utf-8")
    return matched, unmatched, len(used_nodes), len(set(used_ways))


# ------------------------------------------------------------------- main
def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--radius", type=float, default=MODE["radius"],
                    help="max distance board station <-> OSM stop in metres")
    ap.add_argument("--verbose", "-v", action="store_true")
    args = ap.parse_args(argv)
    label = MODE["label"]

    stations, links = load_board()
    print("board: %d %s stations, %d %s links" % (len(stations), label, len(links), label))

    osm = Osm(MODE["route_types"])
    if MODE["stops"]:
        osm.parse(MODE["stops"], want_ways=False, want_relations=False)
    osm.parse(MODE["routes"])
    print("osm: %d nodes, %d ways, %d %s routes"
          % (len(osm.nodes), len(osm.ways), len(osm.routes), label))

    stop2station = assign_stops(osm, stations, args.radius, MODE.get("match_names"))
    per_station = defaultdict(int)
    for st in stop2station.values():
        per_station[st] += 1
    missing = [no for no in stations if no not in per_station]
    print("stops within %.0f m: %d, stations without stop: %s"
          % (args.radius, len(stop2station), missing or "none"))

    cands = find_candidates(osm, stations, links, stop2station)
    # fallback stages for the links still unmatched: wider stop radius
    # (without name matching), then allow a route to pass two other board stations
    global MAX_SKIP
    for radius, max_skip in MODE["fallback"]:
        missing_links = [l for l in links if not cands.get(l)]
        if not missing_links:
            break
        MAX_SKIP = max_skip
        wide = assign_stops(osm, stations, radius, False)
        found = find_candidates(osm, stations, missing_links, wide)
        for key, lst in found.items():
            for c in lst:
                c["radius"] = radius
            cands[key] = lst
        print("fallback radius %.0f m / skip %d: %d of %d links recovered"
              % (radius, max_skip, len(found), len(missing_links)))
    matched, unmatched, n_nodes, n_ways = write_osm(
        osm, stations, links, cands, args.radius, args.verbose)
    print("links matched: %d, unmatched: %d" % (matched, unmatched))
    print("written %s (%d nodes, %d ways, %d relations)"
          % (MODE["out"].relative_to(MODULE), n_nodes, n_ways, len(links)))


if __name__ == "__main__":
    main()
