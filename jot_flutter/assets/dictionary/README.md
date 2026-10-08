`words.txt` contains the 143,722 entries exported from
`jot_android/app/src/main/res/raw/crosswords.jot`. It is included to preserve the
existing game's vocabulary, rather than substitute a different word list.

Recreate it from the original asset with a JDK (11 or later) and Python 3:

```sh
python3 tools/export_dictionary.py
```

The export helper uses the original Java `TreeWords` decompressor and checks each
entry against that implementation before writing sorted uppercase lines. The
original source and asset rights remain unchanged; no additional license is
granted by this conversion.
