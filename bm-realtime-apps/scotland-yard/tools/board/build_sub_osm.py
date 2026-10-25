#!/usr/bin/env python3
"""
Build ``sub.osm`` - the real London Underground lines behind the underground
links of the Scotland Yard board.

Same algorithm and output attributes as ``build_bus_osm.py`` (which does the
work), configured for the subway network:

Input (src/main/resources)
    london.geojson            board: stations (Point) and links (LineString)
    subway_routes.osm(.pbf)   OSM route relations route=subway (London Underground)
                              and route=light_rail (DLR) incl. stop positions,
                              ways and nodes

Output (src/main/resources/sub.osm)
    * relations - one per board underground link (negative id):
                  type=route, route=subway, ref=<line>, sy:link=A-B,
                  sy:from / sy:to, sy:osm_relation, sy:osm_route,
                  sy:routes=<all lines serving the link>, sy:stops_between,
                  sy:segment_m; members: the two OSM stop positions (``stop``)
                  and the track ways between them
    * nodes     - OSM stop positions used (``sy:station=<no>``), track nodes
    * nodes     - one synthetic node per board underground station (id -<no>,
                  ``sy:station``, ``sy:name``, ``sy:underground_neighbours``,
                  ``sy:routes``)

Usage
    python3 tools/board/build_sub_osm.py -v         # from scotland-yard/
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_bus_osm as base  # noqa: E402

base.MODE.update({
    "transport": "underground",
    "label": "underground",
    "route": "subway",
    "route_types": ("subway", "light_rail"),
    "network": "London Underground",
    "generator": "tools/board/build_sub_osm.py",
    "stops": None,                              # stop positions are in the routes file
    "routes": base.RES / "subway_routes.osm",
    "out": base.RES / "sub.osm",
    # tube stations are large: platforms / stop positions can be 300 m apart
    "radius": 400.0,
    "fallback": [(600.0, 1), (800.0, 1), (600.0, 2)],
    "night_prefix": "",                         # no night lines
    # all 14 board tube stations are named: only assign same-named stop
    # positions (Lancaster Gate lies within 400 m of Paddington, etc.)
    "match_names": True,
})

if __name__ == "__main__":
    base.main()
