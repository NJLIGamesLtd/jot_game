cmake_minimum_required(VERSION 3.16)
project(jot_core VERSION 0.1.0 LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

option(JOT_ENABLE_COVERAGE "Instrument first-party C++ code for LLVM source coverage" OFF)
if(JOT_ENABLE_COVERAGE)
  if(NOT CMAKE_CXX_COMPILER_ID MATCHES "Clang")
    message(FATAL_ERROR "JOT_ENABLE_COVERAGE requires Clang and llvm-cov/llvm-profdata")
  endif()
  add_compile_options(-fprofile-instr-generate -fcoverage-mapping)
  add_link_options(-fprofile-instr-generate)
endif()

set(JOT_SOURCES
  src/abi_boundary.cpp
  src/dictionary.cpp
  src/engine.cpp
  src/ai.cpp
  src/jot_api.cpp
)
add_library(jot_core SHARED ${JOT_SOURCES})
target_include_directories(jot_core PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/src)
set_target_properties(jot_core PROPERTIES CXX_VISIBILITY_PRESET hidden
  VISIBILITY_INLINES_HIDDEN YES POSITION_INDEPENDENT_CODE ON)
if(MSVC)
  target_compile_options(jot_core PRIVATE /W4)
else()
  target_compile_options(jot_core PRIVATE -Wall -Wextra -Wpedantic)
endif()

include(CTest)
if(BUILD_TESTING)
  # Engine symbols remain private in the shipped library; tests link their own
  # static copy so the platform-facing exported API stays deliberately small.
  add_library(jot_engine_test STATIC src/dictionary.cpp src/engine.cpp src/ai.cpp src/abi_boundary.cpp)
  target_include_directories(jot_engine_test PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/src)
  add_executable(jot_core_tests tests/core_tests.cpp)
  target_link_libraries(jot_core_tests PRIVATE jot_engine_test)
  add_test(NAME core_rules COMMAND jot_core_tests)

  add_executable(jot_api_tests tests/api_tests.cpp)
  target_link_libraries(jot_api_tests PRIVATE jot_core)
  add_test(NAME c_json_api COMMAND jot_api_tests)

  # Fault injection uses a standard replacement allocator in one executable.
  # Link the unchanged public API source here so sanitizer/DLL allocators cannot
  # bypass it; ordinary integration tests above still load the shipped library.
  add_executable(jot_abi_fault_tests tests/abi_fault_tests.cpp src/jot_api.cpp)
  target_link_libraries(jot_abi_fault_tests PRIVATE jot_engine_test)
  add_test(NAME c_abi_allocation_failures COMMAND jot_abi_fault_tests)

  add_executable(jot_ai_tests tests/ai_tests.cpp)
  target_link_libraries(jot_ai_tests PRIVATE jot_engine_test)
  add_test(NAME dictionary_ai COMMAND jot_ai_tests
    ${CMAKE_CURRENT_SOURCE_DIR}/../../assets/dictionary/words.txt)
endif()
