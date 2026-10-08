#!/usr/bin/env python3
"""Build the portable C++ engine as a statically imported Workers Wasm module."""
import pathlib
import shutil
import subprocess


root = pathlib.Path(__file__).resolve().parents[1]
compiler = shutil.which('em++')
if not compiler:
    raise SystemExit('Install Emscripten and activate its SDK before building the server core.')
sources = root / 'packages/jot_core/src'
destination = root / 'multiplayer/generated'
destination.mkdir(parents=True, exist_ok=True)
subprocess.run([
    compiler, '-std=c++17', '-O3', '-fexceptions',
    *(str(sources / name) for name in ('engine.cpp', 'dictionary.cpp', 'ai.cpp', 'jot_api.cpp', 'abi_boundary.cpp')),
    '-o', str(destination / 'jot_server_core.mjs'),
    '-sMODULARIZE=1', '-sEXPORT_ES6=1', '-sENVIRONMENT=web,worker',
    '-sALLOW_MEMORY_GROWTH=1', '-sDISABLE_EXCEPTION_CATCHING=0',
    '-sDYNAMIC_EXECUTION=0',
    '-sINCOMING_MODULE_JS_API=["instantiateWasm"]',
    # The server adapter owns one stable dictionary allocation per module.
    '-sEXPORTED_FUNCTIONS=["_jot_create","_jot_execute","_jot_destroy","_malloc","_free"]',
    '-sEXPORTED_RUNTIME_METHODS=["cwrap"]',
    '-sSTACK_SIZE=2097152', '-sINITIAL_MEMORY=16777216',
], check=True, cwd=root)
shutil.copyfile(root / 'assets/dictionary/words.txt', destination / 'jot_dictionary.txt')
