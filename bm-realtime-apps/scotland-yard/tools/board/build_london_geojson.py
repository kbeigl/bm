#!/usr/bin/env python3
"""
Build the modern, geo-referenced Scotland Yard board ``london.geojson`` from the
legacy ``london.xml`` / ``SY_BOARD.DTD`` board definition.

Legacy format (2000-2004)
    <STATION x="118" y="34"><NO>1</NO><TAXI><TO>8</TO></TAXI>...</STATION>
    Pixel coordinates on a 1000 x 773 canvas, links stored once per pair,
    tickets per number of detectives in <DEFAULTS>.

Modern format (GeoJSON, RFC 7946)
    * one Point feature per station  (WGS84 lon/lat, station number, name,
      start flag, legacy pixel position, symmetric link lists)
    * one LineString feature per link (taxi / bus / underground / boat),
      routed through the legacy VIA stations
    * foreign member ``board`` with the game defaults (tickets), the canvas
      size and the geo-referencing report

Geo-referencing
    The board is a stylised map, not a projection. 14 stations are tube
    stations whose real position is known (OpenStreetMap nodes, see
    ANCHORS). A thin-plate spline pixel -> metric plane is fitted which is
    exact at the anchors and smooth in between; all other stations are
    interpolated. The accuracy of the interpolation is estimated with a
    leave-one-out test over the anchors and written to the output.

Usage
    python3 tools/board/build_london_geojson.py          # from scotland-yard/
    python3 tools/board/build_london_geojson.py --verify stations_geo_estimate.csv

Requires numpy and scipy.
"""

import argparse
import csv
import json
import math
import re
import sys
from datetime import date
from pathlib import Path

import numpy as np
from scipy.interpolate import RBFInterpolator

HERE = Path(__file__).resolve().parent
MODULE = HERE.parent.parent
BOARDS = MODULE / "src" / "main" / "resources" / "boards"
XML_PATH = BOARDS / "london.xml"
OUT_PATH = BOARDS / "london.geojson"

# ----------------------------------------------------------------- anchors
# station no -> (OSM node id, name, lat, lon)
# Underground stations printed on the board whose real position is known.
# Coordinates taken from OpenStreetMap (scotland-java/mapping/tube_stations.osm,
# station numbers from scotland-java/docs/STATIONS.md).
ANCHORS = {
    1: (6401499742, "St. John's Wood", 51.5353523, -0.1742097),
    13: (119274464, "King's Cross St Pancras", 51.530609, -0.1239491),
    46: (1264468143, "Baker Street", 51.5225853, -0.1567431),
    67: (3693265703, "Holborn", 51.5171149, -0.1200657),
    74: (1264482797, "Paddington", 51.5154795, -0.175365),
    79: (3076573913, "Bond Street", 51.5139808, -0.1494231),
    89: (1637578440, "Bank", 51.5131048, -0.0893749),
    93: (6486933493, "Queensway", 51.5104729, -0.1872329),
    111: (6407577779, "Green Park", 51.5066244, -0.1429437),
    128: (3660623413, "Elephant & Castle", 51.4948884, -0.1005731),
    140: (25534427, "Westminster", 51.5013562, -0.1249302),
    153: (441947584, "Victoria", 51.4963688, -0.1431522),
    163: (5183897519, "South Kensington", 51.4940799, -0.1729072),
    185: (3817850257, "Vauxhall", 51.4860091, -0.1248604),
}

TRANSPORT = {"TAXI": "taxi", "BUS": "bus", "UGRD": "underground", "BOAT": "boat"}

TICKETS = {
    "D_TAXI": ("detective", "taxi"),
    "D_BUS": ("detective", "bus"),
    "D_UGRD": ("detective", "underground"),
    "X_TAXI": ("misterX", "taxi"),
    "X_BUS": ("misterX", "bus"),
    "X_UGRD": ("misterX", "underground"),
    "X_BLACK": ("misterX", "black"),
    "X_TWO_TIMES": ("misterX", "doubleMove"),
    "HELI": ("misterX", "helicopter"),
    "BARRIER": ("detective", "barrier"),
}
DETECTIVE_GROUPS = ["TWO_DETECTIVES", "THREE_DETECTIVES", "FOUR_DETECTIVES", "FIVE_DETECTIVES"]


# ---------------------------------------------------------------- parsing
def parse_board(path: Path):
    data = path.read_text(encoding="latin-1")
    m = re.search(r'<SY_BOARD xsize="(\d+)" ysize="(\d+)"', data)
    canvas = {"width": int(m.group(1)), "height": int(m.group(2))}
    name = re.search(r"<NAME>(.*?)</NAME>", data).group(1)

    defaults = {}
    inherited = {}
    for group in DETECTIVE_GROUPS:
        block = re.search(rf"<{group}>(.*?)</{group}>", data, re.S)
        values = dict(inherited)
        if block:
            for tag, val in re.findall(r"<([A-Z_]+)>(\d+)</\1>", block.group(1)):
                values[tag] = int(val)
        inherited = values
        n = DETECTIVE_GROUPS.index(group) + 2
        tickets = {"detective": {}, "misterX": {}}
        for tag, (who, kind) in TICKETS.items():
            if tag in values:
                tickets[who][kind] = values[tag]
        defaults[str(n)] = tickets
    flags = {
        "bigPool": "<BIGPOOL/>" in data,
        "misterXGetsUsedTickets": "<XGETSUSEDTICKETS/>" in data,
    }

    stations = {}
    links = []
    for attrs, body in re.findall(r"<STATION([^>]*)>(.*?)</STATION>", data, re.S):
        x = int(re.search(r'x="(\d+)"', attrs).group(1))
        y = int(re.search(r'y="(\d+)"', attrs).group(1))
        no = int(re.search(r"<NO>(\d+)</NO>", body).group(1))
        sname = re.search(r"<SNAME>(.*?)</SNAME>", body)
        stations[no] = {
            "no": no,
            "name": sname.group(1) if sname else None,
            "start": "<START/>" in body,
            "pixel": [x, y],
        }
        for tag, inner in re.findall(r"<(TAXI|BUS|UGRD|BOAT)>(.*?)</\1>", body, re.S):
            via = [int(v) for v in re.findall(r"<VIA>(\d+)</VIA>", inner)]
            to = int(re.search(r"<TO>(\d+)</TO>", inner).group(1))
            links.append({"transport": TRANSPORT[tag], "from": no, "to": to, "via": via})
    return name, canvas, defaults, flags, stations, links


# --------------------------------------------------------- geo referencing
class LocalPlane:
    """Equirectangular metres around a reference latitude/longitude."""

    def __init__(self, lat0, lon0):
        self.lat0, self.lon0 = lat0, lon0
        self.m_per_deg_lat = 111_132.954 - 559.822 * math.cos(2 * math.radians(lat0))
        self.m_per_deg_lon = 111_412.84 * math.cos(math.radians(lat0))

    def to_xy(self, lat, lon):
        return ((lon - self.lon0) * self.m_per_deg_lon, (lat - self.lat0) * self.m_per_deg_lat)

    def to_latlon(self, x, y):
        return (self.lat0 + y / self.m_per_deg_lat, self.lon0 + x / self.m_per_deg_lon)


def haversine_m(lat1, lon1, lat2, lon2):
    r = 6_371_008.8
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def tps(src, dst):
    return RBFInterpolator(np.asarray(src, float), np.asarray(dst, float), kernel="thin_plate_spline")


def georeference(stations):
    nos = sorted(ANCHORS)
    lat0 = float(np.mean([ANCHORS[n][2] for n in nos]))
    lon0 = float(np.mean([ANCHORS[n][3] for n in nos]))
    plane = LocalPlane(lat0, lon0)

    px = np.array([stations[n]["pixel"] for n in nos], float)
    xy = np.array([plane.to_xy(ANCHORS[n][2], ANCHORS[n][3]) for n in nos])

    # affine-only fit: how far is the board from a true map?
    A = np.column_stack([px, np.ones(len(px))])
    coef, *_ = np.linalg.lstsq(A, xy, rcond=None)
    affine_res = np.hypot(*(A @ coef - xy).T)
    sx = math.hypot(coef[0, 0], coef[0, 1])
    sy = math.hypot(coef[1, 0], coef[1, 1])

    # leave-one-out TPS: expected accuracy of the interpolated stations
    loo = []
    for i in range(len(nos)):
        keep = [j for j in range(len(nos)) if j != i]
        pred = tps(px[keep], xy[keep])(px[i : i + 1])[0]
        loo.append(float(np.hypot(*(pred - xy[i]))))

    model = tps(px, xy)
    all_nos = sorted(stations)
    all_px = np.array([stations[n]["pixel"] for n in all_nos], float)
    all_xy = model(all_px)
    for n, (x, y) in zip(all_nos, all_xy):
        lat, lon = plane.to_latlon(float(x), float(y))
        stations[n]["lat"], stations[n]["lon"] = round(lat, 6), round(lon, 6)
    for n in nos:  # anchors are exact by construction, use the OSM value verbatim
        stations[n]["lat"], stations[n]["lon"] = ANCHORS[n][2], ANCHORS[n][3]

    report = {
        "method": "thin-plate-spline pixel->local metric plane, exact at anchors",
        "anchorCount": len(nos),
        "affineScaleMetresPerPixel": {"x": round(sx, 2), "y": round(sy, 2)},
        "affineResidualMetres": {
            "mean": round(float(affine_res.mean()), 1),
            "max": round(float(affine_res.max()), 1),
        },
        "leaveOneOutErrorMetres": {
            "mean": round(float(np.mean(loo)), 1),
            "median": round(float(np.median(loo)), 1),
            "max": round(float(np.max(loo)), 1),
        },
        "anchors": [
            {
                "no": n,
                "name": ANCHORS[n][1],
                "osmNode": ANCHORS[n][0],
                "lat": ANCHORS[n][2],
                "lon": ANCHORS[n][3],
                "affineResidualMetres": round(float(affine_res[i]), 1),
                "leaveOneOutErrorMetres": round(loo[i], 1),
            }
            for i, n in enumerate(nos)
        ],
    }
    return report


# ------------------------------------------------------------------ output
def build_geojson(name, canvas, defaults, flags, stations, links, report):
    neighbours = {n: {t: set() for t in TRANSPORT.values()} for n in stations}
    for link in links:
        neighbours[link["from"]][link["transport"]].add(link["to"])
        neighbours[link["to"]][link["transport"]].add(link["from"])

    features = []
    for n in sorted(stations):
        s = stations[n]
        name_ = ANCHORS[n][1] if n in ANCHORS else s["name"]
        props = {
            "kind": "station",
            "no": n,
            "name": name_,
            "start": s["start"],
            "pixel": s["pixel"],
            "geoSource": "osm" if n in ANCHORS else "tps",
        }
        if n in ANCHORS:
            props["osmNode"] = ANCHORS[n][0]
        for t in TRANSPORT.values():
            props[t] = sorted(neighbours[n][t])
        features.append(
            {
                "type": "Feature",
                "id": f"station-{n}",
                "geometry": {"type": "Point", "coordinates": [s["lon"], s["lat"]]},
                "properties": props,
            }
        )

    for link in sorted(links, key=lambda l: (l["transport"], min(l["from"], l["to"]), max(l["from"], l["to"]))):
        path = [link["from"], *link["via"], link["to"]]
        a, b = sorted((link["from"], link["to"]))
        features.append(
            {
                "type": "Feature",
                "id": f"{link['transport']}-{a}-{b}",
                "geometry": {
                    "type": "LineString",
                    "coordinates": [[stations[p]["lon"], stations[p]["lat"]] for p in path],
                },
                "properties": {
                    "kind": "link",
                    "transport": link["transport"],
                    "from": link["from"],
                    "to": link["to"],
                    "via": link["via"],
                },
            }
        )

    lons = [stations[n]["lon"] for n in stations]
    lats = [stations[n]["lat"] for n in stations]
    return {
        "type": "FeatureCollection",
        "name": name,
        "bbox": [min(lons), min(lats), max(lons), max(lats)],
        "board": {
            "name": name,
            "formatVersion": "2.0",
            "generated": date.today().isoformat(),
            "generator": "tools/board/build_london_geojson.py",
            "crs": "EPSG:4326 (WGS84), GeoJSON coordinate order [lon, lat]",
            "legacySource": "boards/london.xml (SY_BOARD.DTD)",
            "canvas": canvas,
            "stationCount": len(stations),
            "linkCount": len(links),
            "startStations": sorted(n for n in stations if stations[n]["start"]),
            "rules": flags,
            "ticketsByDetectives": defaults,
            "georeference": report,
        },
        "features": features,
    }


# ----------------------------------------------------------- verification
def verify_legacy_csv(csv_path: Path, stations, focus=(1, 14)):
    rows = {int(r["no"]): r for r in csv.DictReader(csv_path.open(encoding="utf-8")) if r.get("no")}
    print(f"\nVerification of legacy estimate {csv_path.name} against the new board:")
    dists = {}
    for n, r in rows.items():
        if n in stations:
            dists[n] = haversine_m(float(r["lat"]), float(r["lon"]), stations[n]["lat"], stations[n]["lon"])
    for n in focus:
        if n in dists:
            s, r = stations[n], rows[n]
            print(
                f"  station {n:3d}: legacy ({r['lat']}, {r['lon']}) [{r['source']}]  ->  "
                f"new ({s['lat']}, {s['lon']}) [{'osm' if n in ANCHORS else 'tps'}]  "
                f"distance {dists[n]:7.0f} m"
            )
    arr = np.array(list(dists.values()))
    print(f"  all {len(arr)} stations: mean {arr.mean():.0f} m, median {np.median(arr):.0f} m, max {arr.max():.0f} m")
    legacy_anchor = [n for n, r in rows.items() if r["source"] == "anchor"]
    print(f"  legacy 'anchor' stations {legacy_anchor}")
    print(f"  legacy anchors confirmed by OSM: {[n for n in legacy_anchor if n in ANCHORS]}")
    print(
        f"  legacy anchors NOT confirmed (named differently): "
        f"{[(n, rows[n]['name']) for n in legacy_anchor if n not in ANCHORS]}"
    )


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--xml", type=Path, default=XML_PATH)
    ap.add_argument("--out", type=Path, default=OUT_PATH)
    ap.add_argument("--verify", type=Path, help="legacy stations_geo_estimate.csv to compare against")
    args = ap.parse_args(argv)

    name, canvas, defaults, flags, stations, links = parse_board(args.xml)
    print(f"parsed {args.xml.name}: {len(stations)} stations, {len(links)} links, canvas {canvas}")

    report = georeference(stations)
    print(
        f"georeference: {report['anchorCount']} OSM anchors, "
        f"affine scale {report['affineScaleMetresPerPixel']} m/px, "
        f"affine residual mean {report['affineResidualMetres']['mean']} m / max {report['affineResidualMetres']['max']} m"
    )
    print(
        f"leave-one-out TPS error: mean {report['leaveOneOutErrorMetres']['mean']} m, "
        f"median {report['leaveOneOutErrorMetres']['median']} m, max {report['leaveOneOutErrorMetres']['max']} m"
    )
    for a in report["anchors"]:
        print(f"  #{a['no']:3d} {a['name']:24s} affine {a['affineResidualMetres']:6.1f} m   loo {a['leaveOneOutErrorMetres']:6.1f} m")

    geojson = build_geojson(name, canvas, defaults, flags, stations, links, report)
    args.out.write_text(json.dumps(geojson, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"\nwrote {args.out} ({len(geojson['features'])} features)")

    if args.verify:
        verify_legacy_csv(args.verify, stations)
    return 0


if __name__ == "__main__":
    sys.exit(main())
