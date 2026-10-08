#include "jot_api.h"
#include "third_party/json.hpp"

#include <algorithm>
#include <iostream>
#include <limits>
#include <stdexcept>
#include <string>
#include <vector>

namespace {
using Json = nlohmann::json;
#define CHECK(condition) do { if (!(condition)) throw std::runtime_error( \
  std::string("Check failed at line ") + std::to_string(__LINE__) + ": " #condition); } while (false)

std::string dictionary() {
  // Complete two-letter vocabulary makes legal move availability independent
  // of a random seed while leaving scoring and placement rules fully active.
  std::string words;
  for (char first = 'A'; first <= 'Z'; ++first) {
    for (char second = 'A'; second <= 'Z'; ++second) {
      words += first;
      words += second;
      words += '\n';
    }
  }
  words += "CAT\nCATS\nAT\nRATE\nRATES\nSTAR\nSTARS\n";
  return words;
}

struct Session {
  void* handle = nullptr;
  explicit Session(std::uint32_t seed = 123, int robot = 1) {
    const auto words = dictionary();
    handle = jot_create(words.c_str(), seed, robot);
    CHECK(handle != nullptr);
  }
  Session(const std::string& words, std::uint32_t seed, int robot) {
    handle = jot_create(words.c_str(), seed, robot);
    CHECK(handle != nullptr);
  }
  ~Session() { jot_destroy(handle); }
  Session(const Session&) = delete;
  Session& operator=(const Session&) = delete;
  Json command(const Json& value) {
    const auto encoded = value.dump();
    const char* borrowed = jot_execute(handle, encoded.c_str());
    CHECK(borrowed != nullptr);
    // Copy before the next call as required by the C API ownership contract.
    auto result = Json::parse(std::string(borrowed));
    CHECK(result.is_object());
    CHECK(result.contains("ok") && result["ok"].is_boolean());
    CHECK(result.contains("message") && result["message"].is_string());
    CHECK(result.contains("state") && result["state"].is_object());
    return result;
  }
  Json state() { return command({{"op", "state"}}).at("state"); }
  Json save() { return command({{"op", "save"}}).at("save"); }
};

void boundary_and_validation() {
  CHECK(jot_create(nullptr, 1, 1) == nullptr);
  CHECK(jot_create("", 1, 1) == nullptr);
  CHECK(jot_create("AT\n", 1, 4) == nullptr);
  CHECK(jot_create("AT\n", 1, -2) == nullptr);
  CHECK(!Json::parse(jot_execute(nullptr, "{}"))["ok"].get<bool>());
  jot_destroy(nullptr);

  Session game;
  const auto original = game.state();
  CHECK(original["size"] == 16);
  CHECK(original["rackSize"] == 8);
  CHECK(original["board"].size() == 256);
  CHECK(original["bonuses"].size() == 256);
  CHECK(original["rack"].empty());
  CHECK(original["canDraw"] == true);
  CHECK(original["canHint"] == false);
  CHECK(original["canPlay"] == false);
  CHECK(original["canShuffle"] == true);
  CHECK(original["remaining"].size() == 2);
  CHECK(original["dictionaryWords"].get<int>() >= 676);

  const std::vector<Json> invalid = {
    Json::array(), Json(nullptr), Json("state"), Json::object(),
    {{"op", 7}}, {{"op", "unknown"}},
    {{"op", "new_game"}, {"seed", -1}, {"robot", 1}},
    {{"op", "new_game"}, {"seed", 1.0}, {"robot", 1}},
    {{"op", "new_game"}, {"seed", 4294967296ULL}, {"robot", 1}},
    {{"op", "new_game"}, {"seed", 1}, {"robot", true}},
    {{"op", "new_game"}, {"seed", 1}, {"robot", 3}},
    {{"op", "draw"}, {"bag", "wild"}},
    {{"op", "draw"}, {"bag", nullptr}},
    {{"op", "place"}, {"rack", true}, {"row", 7}, {"col", 7}, {"letter", "A"}},
    {{"op", "place"}, {"rack", 0}, {"row", 16}, {"col", 7}, {"letter", "A"}},
    {{"op", "place"}, {"rack", 0}, {"row", 7.0}, {"col", 7}, {"letter", "A"}},
    {{"op", "place"}, {"rack", 0}, {"row", 7}, {"col", -1}, {"letter", "A"}},
    {{"op", "place"}, {"rack", 0}, {"row", 7}, {"col", 7}, {"letter", "AA"}},
    {{"op", "place"}, {"rack", 0}, {"row", 7}, {"col", 7}, {"letter", "@"}},
    {{"op", "place"}, {"rack", 0}, {"row", 7}, {"col", 7}, {"letter", "a"}},
    {{"op", "unplace"}, {"rack", 8}},
    {{"op", "hint"}, {"apply", "yes"}},
    {{"op", "restore"}, {"save", Json::object()}},
  };
  for (const auto& command : invalid) {
    const auto failed = game.command(command);
    CHECK(!failed["ok"].get<bool>());
    CHECK(failed["state"] == original);
  }
  CHECK(!Json::parse(jot_execute(game.handle, "{broken"))["ok"].get<bool>());
  CHECK(game.state() == original);
  CHECK(!Json::parse(jot_execute(game.handle, nullptr))["ok"].get<bool>());
  CHECK(game.state() == original);
  CHECK(game.command({{"op", "new_game"}, {"seed", 0}, {"robot", -1}})["ok"]);
  CHECK(game.state()["robotPlayer"] == -1);
}

void pending_save_roundtrip_and_rng() {
  Session original;
  CHECK(original.command({{"op", "fill"}})["ok"]);
  CHECK(original.state()["rack"].size() == 8);
  CHECK(original.state()["canHint"] == true);
  CHECK(original.state()["canDraw"] == false);
  const auto initial = original.state();
  const auto hint = original.command({{"op", "hint"}});
  CHECK(hint["ok"]);
  CHECK(hint["hint"]["placements"].size() >= 2);
  CHECK(hint["state"] == initial);  // Searching never mutates live state.
  const auto placed = original.command({{"op", "hint"}, {"apply", true}});
  CHECK(placed["ok"]);
  CHECK(placed["state"]["preview"]["valid"]);
  CHECK(placed["state"]["canPlay"] == true);
  CHECK(placed["state"]["canShuffle"] == false);
  const auto save = original.save();
  CHECK(save["version"] == 1);
  CHECK(save["fullState"]["pending"].size() >= 2);
  CHECK(std::any_of(placed["state"]["board"].begin(), placed["state"]["board"].end(),
      [](const auto& tile) { return tile.is_object() && tile["pending"] == true; }));

  Session restored(98765, -1);
  CHECK(restored.command({{"op", "restore"}, {"save", save}})["ok"]);
  CHECK(restored.state() == placed["state"]);
  CHECK(restored.save() == save);
  CHECK(original.command({{"op", "recall"}})["ok"]);
  CHECK(restored.command({{"op", "recall"}})["ok"]);
  CHECK(original.command({{"op", "shuffle"}})["ok"]);
  CHECK(restored.command({{"op", "shuffle"}})["ok"]);
  CHECK(original.save() == restored.save());  // RNG state survives persistence.

  CHECK(original.command({{"op", "hint"}, {"apply", true}})["ok"]);
  const auto preview = original.state()["preview"]["score"].get<int>();
  const auto played = original.command({{"op", "play"}});
  CHECK(played["ok"]);
  CHECK(played["state"]["currentPlayer"] == 1);
  CHECK(played["state"]["scores"][0] == preview);
  CHECK(!played["state"]["preview"]["valid"].get<bool>());
  CHECK(std::none_of(played["state"]["board"].begin(), played["state"]["board"].end(),
      [](const auto& tile) { return tile.is_object() && tile["pending"] == true; }));
  const auto committed = original.save();
  CHECK(restored.command({{"op", "restore"}, {"save", committed}})["ok"]);
  CHECK(restored.state() == played["state"]);
}

void corrupted_saves_do_not_mutate() {
  Session game;
  CHECK(game.command({{"op", "fill"}})["ok"]);
  CHECK(game.command({{"op", "hint"}, {"apply", true}})["ok"]);
  const auto state = game.state();
  const auto save = game.save();
  std::vector<Json> corrupt;
  auto candidate = save;
  candidate["version"] = 2;
  corrupt.push_back(candidate);
  candidate = save;
  candidate["fullState"]["board"].erase(candidate["fullState"]["board"].begin());
  corrupt.push_back(candidate);
  candidate = save;
  candidate["fullState"]["rngState"] = 0;
  corrupt.push_back(candidate);
  candidate = save;
  candidate["fullState"]["scores"][0] = 1.5;
  corrupt.push_back(candidate);
  candidate = save;
  candidate["fullState"]["players"][0]["rack"][0]["owner"] = 1;
  corrupt.push_back(candidate);
  candidate = save;
  candidate["fullState"]["players"][0]["consonants"] = "Z";
  corrupt.push_back(candidate);
  candidate = save;
  candidate["fullState"]["pending"][0]["row"] = 16;
  corrupt.push_back(candidate);
  candidate = save;
  candidate["fullState"]["pending"].push_back(candidate["fullState"]["pending"][0]);
  corrupt.push_back(candidate);
  candidate = save;
  candidate["fullState"]["over"] = true;
  corrupt.push_back(candidate);
  for (const auto& invalid : corrupt) {
    const auto failed = game.command({{"op", "restore"}, {"save", invalid}});
    CHECK(!failed["ok"].get<bool>());
    CHECK(failed["state"] == state);
    CHECK(game.save() == save);
  }
}

void robot_turn_and_pass_end() {
  Session game;
  CHECK(!game.command({{"op", "robot"}})["ok"].get<bool>());
  CHECK(game.command({{"op", "fill"}})["ok"]);
  CHECK(game.command({{"op", "hint"}, {"apply", true}})["ok"]);
  CHECK(game.command({{"op", "play"}})["ok"]);
  const auto before = game.state();
  const auto robot = game.command({{"op", "robot"}});
  CHECK(robot["ok"]);
  CHECK(robot["state"]["currentPlayer"] == 0);
  CHECK(robot["state"]["scores"][1].get<int>() > before["scores"][1].get<int>());
  CHECK(robot["hint"]["placements"].size() >= 1);
  CHECK(robot["state"]["revision"].get<unsigned>() > before["revision"].get<unsigned>());
  Session restored;
  CHECK(restored.command({{"op", "restore"}, {"save", game.save()}})["ok"]);
  CHECK(restored.state() == robot["state"]);  // Committed computer words are legal.
  CHECK(game.command({{"op", "pass"}})["ok"]);
  CHECK(game.command({{"op", "pass"}})["ok"]);
  CHECK(game.state()["over"]);
  CHECK(game.state()["canPlay"] == false);
  CHECK(game.state()["canHint"] == false);
  CHECK(game.state()["canDraw"] == false);
  CHECK(game.state()["canShuffle"] == false);
  CHECK(!game.command({{"op", "fill"}})["ok"].get<bool>());
  CHECK(!game.command({{"op", "hint"}})["ok"].get<bool>());
  CHECK(!game.command({{"op", "robot"}})["ok"].get<bool>());
}

void player_views_keep_private_tiles_private() {
  Session game(123, -1);
  auto view = [&](int player) {
    const auto saved = game.save();
    auto result = game.command({{"op", "view"}, {"player", player}});
    CHECK(result["ok"]);
    CHECK(!result.contains("save") && !result.contains("hint"));
    CHECK(game.save() == saved);  // Viewing never changes the game or RNG.
    return result.at("state");
  };
  auto opponent = view(1);
  CHECK(opponent["viewerPlayer"] == 1);
  CHECK(opponent["robotPlayer"] == -1);
  CHECK(opponent["isMyTurn"] == false);
  CHECK(opponent["rack"].empty());
  for (const char* action : {"canDraw", "canHint", "canPlay", "canShuffle"}) {
    CHECK(opponent[action] == false);
  }

  CHECK(game.command({{"op", "fill"}})["ok"]);
  CHECK(game.command({{"op", "hint"}, {"apply", true}})["ok"]);
  const auto own = view(0);
  CHECK(own["rack"] == game.state()["rack"]);
  CHECK(own["isMyTurn"] == true);
  CHECK(own["canPlay"] == true);
  CHECK(own["preview"]["valid"] == true);
  CHECK(own["rackCounts"][0] == 8);
  opponent = view(1);
  CHECK(opponent["currentPlayer"] == 0);
  CHECK(opponent["rack"].empty());
  CHECK(opponent["preview"]["valid"] == false);
  CHECK(opponent["preview"]["score"] == 0);
  CHECK(opponent["preview"]["words"].empty());
  CHECK(std::all_of(opponent["board"].begin(), opponent["board"].end(),
                    [](const auto& tile) { return tile.is_null(); }));
  CHECK(!opponent.contains("players") && !opponent.contains("pending"));

  CHECK(game.command({{"op", "play"}})["ok"]);
  const auto first_rack = view(0)["rack"];
  const auto committed_board = view(0)["board"];
  CHECK(first_rack.size() < 8);
  CHECK(std::any_of(committed_board.begin(), committed_board.end(),
                    [](const auto& tile) { return tile.is_object(); }));
  CHECK(view(1)["isMyTurn"] == true);
  CHECK(game.command({{"op", "fill"}})["ok"]);
  CHECK(game.command({{"op", "hint"}, {"apply", true}})["ok"]);
  const auto waiting = view(0);
  CHECK(waiting["rack"] == first_rack);  // Never return the active opponent's rack.
  CHECK(waiting["board"] == committed_board);
  CHECK(waiting["preview"]["words"].empty());
  CHECK(waiting["canPlay"] == false);
  CHECK(view(1)["rack"] == game.state()["rack"]);
  CHECK(view(1)["canPlay"] == true);
  for (const auto& invalid : std::vector<Json>{-1, 2, true, "0", 0.5}) {
    CHECK(!game.command({{"op", "view"}, {"player", invalid}})["ok"].get<bool>());
  }
}

void draw_place_and_typed_save_boundaries() {
  Session game(123, -1);
  CHECK(game.command({{"op", "draw"}, {"bag", "vowel"}})["ok"]);
  CHECK(game.command({{"op", "draw"}, {"bag", "consonant"}})["ok"]);
  CHECK(game.state()["rack"].size() == 2);
  CHECK(game.command({{"op", "fill"}})["ok"]);
  const auto tile = game.state()["rack"][0];
  const std::string letter = tile["blank"] == true ? "A" : tile["letter"].get<std::string>();
  CHECK(game.command({{"op", "place"}, {"rack", 0}, {"row", 7}, {"col", 7}, {"letter", letter}})["ok"]);
  CHECK(game.command({{"op", "unplace"}, {"rack", 0}})["ok"]);
  CHECK(!game.command({{"op", "unplace"}, {"rack", 0}})["ok"].get<bool>());
  const auto save = game.save();
  const auto unchanged = game.state();
  auto reject = [&](const Json& candidate, const std::string& expected = "") {
    const auto result = game.command({{"op", "restore"}, {"save", candidate}});
    CHECK(result["ok"] == false);
    if (!expected.empty()) CHECK(result["message"] == expected);
    CHECK(result["state"] == unchanged);
    CHECK(game.save() == save);
  };
  for (int version : {2, 3, std::numeric_limits<int>::max()}) {
    auto candidate = save;
    candidate["version"] = version;
    reject(candidate, "Unsupported save version.");
  }
  for (const auto& version : std::vector<Json>{0, -1, true, "1", nullptr, 1.5}) {
    auto candidate = save; candidate["version"] = version; reject(candidate);
  }
  for (const auto& rack : std::vector<Json>{nullptr, "AT", Json::array({nullptr, nullptr, nullptr,
      nullptr, nullptr, nullptr, nullptr, nullptr, nullptr})}) {
    auto candidate = save; candidate["fullState"]["players"][0]["rack"] = rack;
    reject(candidate, "A rack must contain at most eight tiles.");
  }
  for (const auto& letters : std::vector<Json>{std::string(129, 'A'), "a", "A!", "A B"}) {
    auto candidate = save; candidate["fullState"]["players"][0]["vowels"] = letters;
    reject(candidate, "A bag contains invalid tiles.");
  }
  for (const auto& pending : std::vector<Json>{nullptr, "AT", Json::array({nullptr, nullptr,
      nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr})}) {
    auto candidate = save; candidate["fullState"]["pending"] = pending;
    reject(candidate, "A move must contain at most eight placements.");
  }
  auto candidate = save;
  candidate["fullState"]["scores"] = Json::object(); reject(candidate);
  candidate = save;
  candidate["fullState"]["scores"][0] = -2147483649LL; reject(candidate);
  candidate = save;
  candidate["fullState"]["scores"][0] = 2147483648ULL; reject(candidate);
  candidate = save;
  candidate["fullState"]["players"][0]["rack"][0]["blank"] = "false"; reject(candidate);
  candidate = save;
  candidate["fullState"]["players"][0]["rack"][0].erase("letter"); reject(candidate);
  candidate = save;
  candidate["fullState"]["players"][0]["rack"][0]["letter"] = "!"; reject(candidate);
  candidate = save;
  candidate["fullState"]["players"][0] = Json::array(); reject(candidate);
}

void robot_no_move_retries_then_passes() {
  // This lexicon requires more letters than a rack can hold. The opening
  // search is complete, and only then may the robot choose a pass.
  Session game(std::string(16, 'Z') + '\n', 123, 0);
  CHECK(!game.command({{"op", "hint"}})["ok"].get<bool>());
  const auto result = game.command({{"op", "robot"}});
  CHECK(result["ok"]);
  CHECK(result["state"]["currentPlayer"] == 1);
  CHECK(result["state"]["scores"] == Json::array({0, 0}));
  CHECK(!result.contains("hint"));
  CHECK(game.save()["fullState"]["consecutivePasses"] == 1);
  CHECK(game.command({{"op", "pass"}})["ok"]);
  CHECK(game.state()["over"] == true);
  CHECK(!game.command({{"op", "hint"}})["ok"].get<bool>());
  CHECK(game.command({{"op", "view"}, {"player", 1}})["state"]["isMyTurn"] == false);
}

void exhausted_bags_are_projected_correctly() {
  Session seed(123, -1);
  const auto initial = seed.save();
  for (int mode = 0; mode < 3; ++mode) {
    auto save = initial;
    auto& player = save["fullState"]["players"][0];
    std::string tiles = player["consonants"].get<std::string>();
    player["consonants"] = "";
    if (mode == 2) {
      auto vowels = player["vowels"].get<std::string>();
      const char held = vowels.back();
      vowels.pop_back();
      player["rack"] = Json::array({{{"letter", std::string(1, held)},
                                     {"blank", held == '@'}, {"owner", 0}}});
      tiles += vowels;
      player["vowels"] = "";
    }
    if (mode == 1) {
      // Draining only the vowel bag still permits consonant draws.
      tiles = initial["fullState"]["players"][0]["vowels"].get<std::string>();
      player["vowels"] = "";
      player["consonants"] = initial["fullState"]["players"][0]["consonants"];
    }
    auto& board = save["fullState"]["board"];
    for (std::size_t offset = 0; offset < tiles.size(); ++offset) {
      const char tile = tiles[offset];
      board[7 * 16 + offset] = {{"letter", std::string(1, tile == '@' ? 'A' : tile)},
                               {"blank", tile == '@'}, {"owner", 0}};
    }
    std::string words;
    for (bool vertical : {false, true}) {
      for (int line = 0; line < 16; ++line) {
        std::string word;
        for (int offset = 0; offset <= 16; ++offset) {
          const int row = vertical ? offset : line;
          const int col = vertical ? line : offset;
          if (offset < 16 && board[row * 16 + col].is_object()) {
            word += board[row * 16 + col]["letter"].get<std::string>();
          } else {
            if (word.size() > 1) words += word + '\n';
            word.clear();
          }
        }
      }
    }
    Session game(words, 123, -1);
    const auto result = game.command({{"op", "restore"}, {"save", save}});
    CHECK(result["ok"]);
    CHECK(result["state"]["canDraw"] == (mode != 2));
    CHECK(result["state"]["canHint"] == (mode == 2));
    if (mode == 2) {
      CHECK(!game.command({{"op", "draw"}, {"bag", "vowel"}})["ok"].get<bool>());
      CHECK(!game.command({{"op", "draw"}, {"bag", "consonant"}})["ok"].get<bool>());
    } else {
      const char* bag = mode == 0 ? "vowel" : "consonant";
      CHECK(game.command({{"op", "draw"}, {"bag", bag}})["ok"]);
    }
  }
}

void robot_retries_a_truncated_no_move_search() {
  std::string letters = "ABCDEFGH";
  std::string words;
  do { words += letters + "Z\n"; }
  while (std::next_permutation(letters.begin(), letters.end()));
  Session game(words, 123, 0);
  auto save = game.save();
  auto& player = save["fullState"]["players"][0];
  for (char letter : std::string("ABCDEFGH")) {
    const char* bag = std::string("AEIOU").find(letter) != std::string::npos
        ? "vowels" : "consonants";
    auto inventory = player[bag].get<std::string>();
    const auto index = inventory.find(letter);
    CHECK(index != std::string::npos);
    inventory.erase(index, 1);
    player[bag] = inventory;
    player["rack"].push_back({{"letter", std::string(1, letter)}, {"blank", false}, {"owner", 0}});
  }
  CHECK(game.command({{"op", "restore"}, {"save", save}})["ok"]);
  const auto before = game.state();
  const auto canonical_save = game.save();
  CHECK(!game.command({{"op", "hint"}})["ok"].get<bool>());
  CHECK(game.state() == before);
  const auto result = game.command({{"op", "robot"}});
  CHECK(result["ok"]);
  CHECK(result["state"]["currentPlayer"] == 1);
  CHECK(result["state"]["scores"] == Json::array({0, 0}));
  CHECK(!result.contains("hint"));
  const auto persisted = game.save();
  CHECK(persisted["fullState"]["consecutivePasses"] == 1);
  CHECK(persisted["fullState"]["players"] == canonical_save["fullState"]["players"]);
}

}  // namespace

int main() {
  try {
    boundary_and_validation();
    pending_save_roundtrip_and_rng();
    corrupted_saves_do_not_mutate();
    robot_turn_and_pass_end();
    player_views_keep_private_tiles_private();
    draw_place_and_typed_save_boundaries();
    robot_no_move_retries_then_passes();
    exhausted_bags_are_projected_correctly();
    robot_retries_a_truncated_no_move_search();
    std::cout << "C JSON API tests passed.\n";
    return 0;
  } catch (const std::exception& error) {
    std::cerr << error.what() << '\n';
    return 1;
  }
}
