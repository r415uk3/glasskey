import sys, os
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
from fontTools import subset

src, out = sys.argv[1], sys.argv[2]
KK = "әғқңөұүһіӘҒҚҢӨҰҮҺІёъЁЪ"
# Basic Latin, Latin-1, Latin Ext-A, Cyrillic + Cyrillic Supplement, general punctuation, currency (₸ U+20B8), arrows
UNICODES = list(range(0x20, 0x7F)) + list(range(0xA0, 0x180)) + list(range(0x400, 0x530)) \
    + list(range(0x2000, 0x2070)) + list(range(0x20A0, 0x20C1)) + [0x2116, 0x2122]

jobs = [
    ("inter", f"{src}/inter/extras/ttf/Inter-Regular.ttf", None, f"{src}/inter/extras/ttf/Inter-Medium.ttf", None),
    ("golos_text", f"{src}/GolosText.ttf", {"wght": 400}, f"{src}/GolosText.ttf", {"wght": 500}),
    ("onest", f"{src}/Onest.ttf", {"wght": 400}, f"{src}/Onest.ttf", {"wght": 500}),
    ("noto_sans", f"{src}/NotoSans.ttf", {"wght": 400, "wdth": 100}, f"{src}/NotoSans.ttf", {"wght": 500, "wdth": 100}),
]
for name, rpath, rloc, mpath, mloc in jobs:
    for weight, path, loc in (("regular", rpath, rloc),):
        f = TTFont(path)
        if loc is not None:
            f = instancer.instantiateVariableFont(f, loc)
        opts = subset.Options(); opts.layout_features = ["*"]; opts.name_IDs = ["*"]; opts.notdef_outline = True
        s = subset.Subsetter(opts); s.populate(unicodes=UNICODES); s.subset(f)
        cmap = f.getBestCmap()
        missing = [c for c in KK if ord(c) not in cmap]
        dest = f"{out}/{name}_{weight}.ttf"
        f.save(dest)
        print(f"{name}_{weight}: {os.path.getsize(dest)//1024} KB, kazakh missing: {missing or 'none'}, has ₸: {0x20B8 in cmap}")
