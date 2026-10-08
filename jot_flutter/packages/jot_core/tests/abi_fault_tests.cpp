#include "jot_api.h"
#include "third_party/json.hpp"

#include <cstdlib>
#include <iostream>
#include <new>
#include <stdexcept>
#include <string>

namespace {
thread_local bool fail_allocations = false;
}

// Standard replacement allocation in the test executable only. Production
// allocation is unchanged; the exact C ABI source is linked into this process.
void* operator new(std::size_t size) {
  if (fail_allocations) throw std::bad_alloc();
  if (void* memory = std::malloc(size == 0 ? 1 : size)) return memory;
  throw std::bad_alloc();
}
void* operator new[](std::size_t size) { return ::operator new(size); }
void operator delete(void* memory) noexcept { std::free(memory); }
void operator delete[](void* memory) noexcept { std::free(memory); }
void operator delete(void* memory, std::size_t) noexcept { std::free(memory); }
void operator delete[](void* memory, std::size_t) noexcept { std::free(memory); }

int main() {
  void* handle = nullptr;
  try {
    handle = jot_create("AT\n", 1, -1);
    if (!handle) throw std::runtime_error("Could not create the fault-injection game.");
    const std::string original = jot_execute(handle, "{\"op\":\"state\"}");
    fail_allocations = true;
    const char* failed = jot_execute(handle, "{\"op\":\"state\"}");
    void* failed_create = jot_create("AT\n", 1, -1);
    fail_allocations = false;
    if (failed_create) {
      jot_destroy(failed_create);
      throw std::runtime_error("Creation must return null when allocation fails.");
    }
    const auto response = nlohmann::json::parse(failed);
    if (response["ok"] != false || !response["state"].is_null() ||
        response["message"] != "Unable to serialize game state.") {
      throw std::runtime_error("Serialization failure escaped its constant C ABI response.");
    }
    if (original != jot_execute(handle, "{\"op\":\"state\"}")) {
      throw std::runtime_error("Allocation failure mutated the game.");
    }
    jot_destroy(handle);
    handle = nullptr;
    std::cout << "C ABI allocation-failure tests passed.\n";
    return 0;
  } catch (const std::exception& error) {
    fail_allocations = false;
    jot_destroy(handle);
    std::cerr << error.what() << '\n';
    return 1;
  }
}
