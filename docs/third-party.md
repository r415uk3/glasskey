# Third-party assets added by Glasskey

On top of everything HeliBoard ships (see README.md and LICENSE):

| Asset | Source | License |
|---|---|---|
| `app/src/main/assets/dicts/main_kk.dict` | `main_kk__experimental.dict` from https://codeberg.org/Helium314/aosp-dictionaries | as stated in that repository |
| `app/src/main/res/font/inter_regular.ttf` | Inter 4.1, https://github.com/rsms/inter | SIL OFL 1.1 (`assets/licenses/inter-OFL.txt`) |
| `app/src/main/res/font/golos_text_regular.ttf` | Golos Text, https://github.com/google/fonts/tree/main/ofl/golostext | SIL OFL 1.1 (`assets/licenses/golos_text-OFL.txt`) |
| `app/src/main/res/font/onest_regular.ttf` | Onest, https://github.com/google/fonts/tree/main/ofl/onest | SIL OFL 1.1 (`assets/licenses/onest-OFL.txt`) |
| `app/src/main/res/font/noto_sans_regular.ttf` | Noto Sans, https://github.com/google/fonts/tree/main/ofl/notosans | SIL OFL 1.1 (`assets/licenses/noto_sans-OFL.txt`) |

Fonts are static Regular instances subset to Latin + Cyrillic + punctuation + currency signs by `tools/fonts/make_fonts.py`
(needs `fonttools`), which also checks that every Kazakh letter and ₸ is present.
