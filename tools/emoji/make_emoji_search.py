"""Builds assets/emoji_search/<lang>.txt from Unicode CLDR emoji annotations (Unicode License v3).

usage: python3 make_emoji_search.py <cache dir> <repo>/app/src/main
Languages are those of the keyboard layouts in res/xml/method.xml that CLDR has annotations for.
CLDR JSON files are downloaded into <cache dir> when missing:
  ann-<lang>.json: cldr-json/cldr-annotations-full/annotations/<lang>/annotations.json
  der-<lang>.json: cldr-json/cldr-annotations-derived-full/annotationsDerived/<lang>/annotations.json
Output lines: emoji TAB name TAB keyword|keyword..., in emoji screen order, only emoji from assets/emoji.
Keep ALIASES in sync with IosEmojiSearchIndex.ALIASES.
"""
import json, os, re, sys, urllib.request

cache, main = sys.argv[1], sys.argv[2]
assets = f"{main}/assets"
BASE = "https://raw.githubusercontent.com/unicode-org/cldr-json/main/cldr-json"
FILES = ["SMILEYS_AND_EMOTION", "PEOPLE_AND_BODY", "ANIMALS_AND_NATURE", "FOOD_AND_DRINK", "ACTIVITIES",
         "TRAVEL_AND_PLACES", "OBJECTS", "SYMBOLS", "FLAGS"]
# old Android language codes and Norwegian Bokmål -> CLDR
ALIASES = {"in": "id", "iw": "he", "tl": "fil", "nb": "no"}
# regional CLDR files only hold differences, so only script variants get their own file
SCRIPT_VARIANT = re.compile(r"^[a-z]+-[A-Z][a-z]{3}$")

def fetch(name, url):
    path = f"{cache}/{name}"
    if not os.path.exists(path):
        try:
            urllib.request.urlretrieve(url, path)
        except Exception:
            return None
    return json.load(open(path, encoding="utf-8"))

def key(e):
    return e.replace("️", "")

tags = set(t.replace("_", "-") for t in re.findall(r'(?:imeSubtypeLocale|languageTag)="([^"]*)"', open(f"{main}/res/xml/method.xml").read()))
langs = set()
for t in tags:
    if t.endswith("ZZ") or t == "zz":
        continue
    base = t.split("-")[0]
    langs.add(ALIASES.get(base, base))
    if SCRIPT_VARIANT.match(t):
        langs.add(t)
langs.add("en")

emojis = []
for name in FILES:
    for line in open(f"{assets}/emoji/{name}.txt", encoding="utf-8"):
        if line.strip():
            emojis.append(line.split()[0])

os.makedirs(cache, exist_ok=True)
os.makedirs(f"{assets}/emoji_search", exist_ok=True)
done, skipped, total = [], [], 0
for lang in sorted(langs):
    ann = fetch(f"ann-{lang}.json", f"{BASE}/cldr-annotations-full/annotations/{lang}/annotations.json")
    if ann is None:
        skipped.append(lang)
        continue
    data = {}
    for e, v in ann.get("annotations", {}).get("annotations", {}).items():
        data.setdefault(key(e), v)
    der = fetch(f"der-{lang}.json", f"{BASE}/cldr-annotations-derived-full/annotationsDerived/{lang}/annotations.json")
    if der is not None:
        for e, v in der.get("annotationsDerived", {}).get("annotations", {}).items():
            data.setdefault(key(e), v)
    lines = []
    for e in emojis:
        v = data.get(key(e))
        if v is None or ("tts" not in v and "default" not in v):
            continue
        name = v.get("tts", [""])[0].replace("\t", " ")
        keywords = "|".join(k.replace("\t", " ").replace("|", " ") for k in v.get("default", []))
        lines.append(f"{e}\t{name}\t{keywords}")
    if len(lines) < 100:  # nearly empty CLDR file, not worth shipping
        skipped.append(lang)
        continue
    out = f"{assets}/emoji_search/{lang}.txt"
    with open(out, "w", encoding="utf-8") as fo:
        fo.write("\n".join(lines) + "\n")
    total += os.path.getsize(out)
    done.append(lang)
print(f"{len(done)} languages, {total // 1024} KB: {' '.join(done)}")
print(f"no CLDR data: {' '.join(skipped)}")
