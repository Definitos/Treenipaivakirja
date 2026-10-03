"""Muuntaa Finelin avoimen datan paketin (THL, CC BY 4.0) sovelluksen ruokalistaksi.

Käyttö: python3 .github/scripts/import_fineli.py Fineli_RelXX_....zip [muita.zip]
Paketti ladataan selaimella: https://fineli.fi/fineli/fi/avoin-data

Tuottaa:
  app/src/main/assets/fineli.csv       id;nimi_fi;nimi_en;kcal;proteiini;hiilihydraatti;rasva  (per 100 g)
  app/src/main/assets/fineli_info.txt  julkaisun nimi ja hakupäivä (näytetään asetuksissa)
"""
import csv, datetime, io, sys, zipfile

OUT = "app/src/main/assets/fineli.csv"
INFO = "app/src/main/assets/fineli_info.txt"


def find_package(paths):
    """Valitsee annetuista zip-paketeista sen, jossa on food.csv + component_value.csv (mieluiten myös englanninkieliset nimet)."""
    best = None
    for path in paths:
        z = zipfile.ZipFile(path)
        base = {n.lower().rsplit("/", 1)[-1] for n in z.namelist()}
        print(f"{path}: {sorted(base)}")
        if "food.csv" in base and "component_value.csv" in base:
            score = 2 if "foodname_en.csv" in base else 1
            if best is None or score > best[0]:
                best = (score, z, path.rsplit("/", 1)[-1])
    if not best:
        sys.exit("Fineli-pakettia (food.csv + component_value.csv) ei löytynyt")
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
    z, label = find_package(sys.argv[1:])
    foods = read_csv(z, "food.csv")
    names_fi = {r["FOODID"]: r["FOODNAME"].strip() for r in foods}
    names_en = {}
    for other in [z] + [zipfile.ZipFile(p) for p in sys.argv[1:]]:
        if any(n.lower().endswith("foodname_en.csv") for n in other.namelist()):
            for r in read_csv(other, "foodname_en.csv"):
                names_en[r["FOODID"]] = r["FOODNAME"].strip()
            break

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
