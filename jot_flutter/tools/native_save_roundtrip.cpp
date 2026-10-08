// Small C-ABI interoperability helper used by test_web_core.cjs.
#include "jot_api.h"
#include "third_party/json.hpp"
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>

std::string read(const char* path) {
  std::ifstream input(path);
  if (!input) throw std::runtime_error("Could not read test input.");
  return {std::istreambuf_iterator<char>(input), std::istreambuf_iterator<char>()};
}

int main(int argc, char** argv) {
  void* game = nullptr;
  try {
    if (argc != 3) throw std::runtime_error("Usage: native_save_roundtrip dictionary.txt save.json");
    const auto dictionary = read(argv[1]);
    game = jot_create(dictionary.c_str(), 987, -1);
    if (!game) throw std::runtime_error("Native core did not initialize.");
    const auto command = nlohmann::json{{"op", "restore"},
                                      {"save", nlohmann::json::parse(read(argv[2]))}}.dump();
    const auto restored = nlohmann::json::parse(jot_execute(game, command.c_str()));
    if (!restored.at("ok").get<bool>()) throw std::runtime_error(restored.at("message").get<std::string>());
    std::cout << jot_execute(game, "{\"op\":\"save\"}") << '\n';
    jot_destroy(game);
    return 0;
  } catch (const std::exception& error) {
    if (game) jot_destroy(game);
    std::cerr << error.what() << '\n';
    return 1;
  }
}
