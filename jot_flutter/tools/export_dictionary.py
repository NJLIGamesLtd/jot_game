#!/usr/bin/env python3
"""Reproduce the dictionary asset with the legacy Java decompressor (JDK 11+).

The original dictionary's rights are unchanged. This utility does not fetch or
replace the bundled dictionary with an external word list.
"""

import argparse
from pathlib import Path
import subprocess
import tempfile


def main() -> None:
    project = Path(__file__).resolve().parents[1]
    legacy = project.parent / "jot_android" / "app" / "src" / "main"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=legacy / "res" / "raw" / "crosswords.jot")
    parser.add_argument("--output", type=Path, default=project / "assets" / "dictionary" / "words.txt")
    args = parser.parse_args()
    sources = legacy / "java" / "com" / "jot" / "decomp"
    with tempfile.TemporaryDirectory(prefix="jot-dictionary-export-") as compiled:
        subprocess.run([
            "javac", "-d", compiled,
            str(sources / "Words.java"), str(sources / "ScanWords.java"),
            str(sources / "TreeWords.java"), str(project / "tools" / "ExportDictionary.java"),
        ], check=True)
        subprocess.run([
            "java", "-cp", compiled, "ExportDictionary", str(args.input), str(args.output),
        ], check=True)


if __name__ == "__main__":
    main()
