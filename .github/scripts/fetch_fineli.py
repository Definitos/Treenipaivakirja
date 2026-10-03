"""Lataa Finelin avoimen datan (THL, CC BY 4.0) ja tuottaa sovelluksen ruokalistan.

Tuottaa:
  app/src/main/assets/fineli.csv       id;nimi_fi;nimi_en;kcal;proteiini;hiilihydraatti;rasva  (per 100 g)
  app/src/main/assets/fineli_info.txt  julkaisun nimi ja hakupäivä (näytetään asetuksissa)
"""
import csv, datetime, io, re, sys, urllib.request, zipfile

PAGES = [
    "https://fineli.fi/fineli/fi/avoin-data",
    "https://fineli.fi/fineli/en/avoin-data",
]
UA = {"User-Agent": "Mozilla/5.0 (X11; Linux x86_64) Treenipaivakirja-build"}
OUT = "app/src/main/assets/fineli.csv"
INFO = "app/src/main/assets/fineli_info.txt"


def get(url):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read(), r.headers.get("Content-Disposition", "")


def candidates():
    links = []
    for page in PAGES:
        try:
            html, _ = get(page)
        except Exception as e:
            print(f"page {page}: {e}")
            continue
        html = html.decode("utf-8", "replace")
        for href in re.findall(r'href="([^"]+)"', html):
            if "content/file" in href or href.lower().endswith(".zip"):
                if href.startswith("/"):
                    href = "https://fineli.fi" + href
                if href not in links:
                    links.append(href)
    print("candidate links:", links)
    # Varalla: tunnetut tiedostonumerot
    for n in range(1, 120):
        u = f"https://fineli.fi/fineli/content/file/{n}"
        if u not in links:
            links.append(u)
    return links


def find_package():
    """Palauttaa (zip, nimi) paketille, jossa on food.csv + component_value.csv (+ mieluiten englanninkieliset nimet)."""
    best = None
    for url in candidates():
        try:
            data, disp = get(url)
        except Exception as e:
            continue
        if data[:2] != b"PK":
            continue
        z = zipfile.ZipFile(io.BytesIO(data))
        names = [n.lower() for n in z.namelist()]
        base = {n.rsplit("/", 1)[-1] for n in names}
        print(f"{url} {disp} -> {sorted(base)[:30]}")
        if "food.csv" in base and "component_value.csv" in base:
            score = 2 if "foodname_en.csv" in base else 1
            label = re.search(r'filename="?([^";]+)', disp)
            label = label.group(1) if label else url
            if best is None or score > best[0]:
                best = (score, z, label)
            if score == 2:
                break
    if not best:
        sys.exit("::error::Fineli-pakettia ei löytynyt")
    return best[1], best[2]


def read_csv(z, name):
    path = next(n for n in z.namelist() if n.lower().rsplit("/", 1)[-1] == name)
    raw = z.read(path)
    for enc in ("utf-8-sig", "cp1252", "latin-1"):
        try:
            text = raw.decode(enc)
            break
        except UnicodeDecodeError:
            pass
    rows = list(csv.reader(io.StringIO(text), delimiter=";"))
    header = [h.strip().upper() for h in rows[0]]
    print(f"{name}: {header} ({len(rows) - 1} rows), sample: {rows[1] if len(rows) > 1 else None}")
    return [dict(zip(header, r)) for r in rows[1:] if r]


def num(s):
    s = (s or "").strip().replace(",", ".")
    try:
        return float(s)
    except ValueError:
        return 0.0


def main():
    z, label = find_package()
    foods = read_csv(z, "food.csv")
    names_fi = {r["FOODID"]: r["FOODNAME"].strip() for r in foods}
    names_en = {}
    if any(n.lower().endswith("foodname_en.csv") for n in z.namelist()):
        for r in read_csv(z, "foodname_en.csv"):
            names_en[r["FOODID"]] = r["FOODNAME"].strip()

    want = {"ENERC": "kj", "PROT": "p", "CHOAVL": "c", "FAT": "f"}
    vals = {}
    for r in read_csv(z, "component_value.csv"):
        code = (r.get("EUFDNAME") or "").strip().upper()
        if code in want:
            vals.setdefault(r["FOODID"], {})[want[code]] = num(r.get("BESTLOC"))

    out = []
    for fid, name in names_fi.items():
        v = vals.get(fid)
        if not v or "kj" not in v:
            continue
        kcal = v["kj"] / 4.184
        out.append([fid, name.replace(";", ","), names_en.get(fid, "").replace(";", ","),
                    f"{kcal:.1f}", f"{v.get('p', 0):.1f}", f"{v.get('c', 0):.1f}", f"{v.get('f', 0):.1f}"])
    if len(out) < 1000:
        sys.exit(f"::error::Liian vähän ruokia ({len(out)})")

    with open(OUT, "w", encoding="utf-8", newline="") as fh:
        w = csv.writer(fh, delimiter=";", lineterminator="\n")
        w.writerows(out)
    with open(INFO, "w", encoding="utf-8") as fh:
        fh.write(f"{label}\n{datetime.date.today().isoformat()}\n")
    print(f"Wrote {len(out)} foods from {label}")
    for row in out[:5]:
        print(row)


if __name__ == "__main__":
    main()
