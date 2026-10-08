#!/usr/bin/env python3
"""Build the same portable C++ engine for the browser, without Dart rules."""
import pathlib
import shutil
import subprocess

root = pathlib.Path(__file__).resolve().parents[1]
compiler = shutil.which('em++')
if not compiler:
    raise SystemExit('Install Emscripten and activate its SDK before building web.')
sources = root / 'packages/jot_core/src'
subprocess.run([
    compiler, '-std=c++17', '-O3', '-fexceptions',
    *(str(sources / name) for name in ('engine.cpp', 'dictionary.cpp', 'ai.cpp', 'jot_api.cpp', 'abi_boundary.cpp')),
    '-o', str(root / 'web/jot_core.js'),
    '-sMODULARIZE=1', '-sEXPORT_NAME=createJotModule', '-sALLOW_MEMORY_GROWTH=1',
    '-sENVIRONMENT=web,worker,node', '-sDISABLE_EXCEPTION_CATCHING=0',
    '-sEXPORTED_FUNCTIONS=["_jot_create","_jot_execute","_jot_destroy"]',
    '-sEXPORTED_RUNTIME_METHODS=["cwrap"]',
    '-sSTACK_SIZE=2097152', '-sINITIAL_MEMORY=33554432',
], check=True, cwd=root)
