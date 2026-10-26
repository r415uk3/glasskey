"""Builds assets/emoji_search/<lang>.txt from Unicode CLDR emoji annotations (Unicode License v3).

usage: python3 make_emoji_search.py <dir with ann-<lang>.json and der-<lang>.json> <repo>/app/src/main/assets
  ann-<lang>.json: cldr-json/cldr-annotations-full/annotations/<lang>/annotations.json
  der-<lang>.json: cldr-json/cldr-annotations-derived-full/annotationsDerived/<lang>/annotations.json
Output lines: emoji TAB name TAB keyword|keyword..., in emoji screen order, only emoji from assets/emoji.
"""
import json, os, sys

src, assets = sys.argv[1], sys.argv[2]
FILES = ["SMILEYS_AND_EMOTION", "PEOPLE_AND_BODY", "ANIMALS_AND_NATURE", "FOOD_AND_DRINK", "ACTIVITIES",
         "TRAVEL_AND_PLACES", "OBJECTS", "SYMBOLS", "FLAGS"]

def key(e):
    return e.replace("️", "")

emojis = []
for name in FILES:
    for line in open(f"{assets}/emoji/{name}.txt", encoding="utf-8"):
        if line.strip():
            emojis.append(line.split()[0])

for lang in ("en", "ru", "kk"):
    data = {}
    for f, path in ((f"ann-{lang}.json", ("annotations", "annotations")), (f"der-{lang}.json", ("annotationsDerived", "annotations"))):
        d = json.load(open(f"{src}/{f}", encoding="utf-8"))
        for p in path:
            d = d[p]
        for e, v in d.items():
            data.setdefault(key(e), v)
    lines, missing = [], 0
    for e in emojis:
        v = data.get(key(e))
        if v is None:
            missing += 1
            continue
        name = v.get("tts", [""])[0].replace("\t", " ")
        keywords = "|".join(k.replace("\t", " ").replace("|", " ") for k in v.get("default", []))
        lines.append(f"{e}\t{name}\t{keywords}")
    out = f"{assets}/emoji_search/{lang}.txt"
    with open(out, "w", encoding="utf-8") as fo:
        fo.write("\n".join(lines) + "\n")
    print(f"{lang}: {len(lines)} emoji, {missing} without annotation, {os.path.getsize(out)//1024} KB")
