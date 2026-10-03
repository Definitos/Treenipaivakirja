"""Varmistaa, että käsin kirjoitetut migraatiolauseet vastaavat Roomin generoimaa skeemaa."""
import glob, re, sys

impls = glob.glob("app/build/generated/ksp/debug/**/AppDatabase_Impl.*", recursive=True)
if not impls:
    print("::error::AppDatabase_Impl not found"); sys.exit(1)
impl = open(impls[0], encoding="utf-8").read()
src = open("app/src/main/java/fi/ville/treenipaivakirja/data/Database.kt", encoding="utf-8").read()
block = re.search(r"object SchemaCheck \{(.*?)\n\}", src, re.S)
if not block:
    print("::error::SchemaCheck object not found"); sys.exit(1)
mine = re.findall(r'"(CREATE [^"]+)"', block.group(1))
generated = re.findall(r'"(CREATE [^"]+)"', impl)
missing = [s for s in mine if s not in generated]
if missing:
    for s in missing:
        print(f"::error title=Migration SQL mismatch::{s}")
    for g in generated:
        print(f"::warning title=Room generated::{g}")
    sys.exit(1)
print(f"Schema OK ({len(mine)} statements)")
