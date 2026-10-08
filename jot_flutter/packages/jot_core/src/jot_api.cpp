#include "jot_api.h"
#include "abi_boundary.hpp"
#include "ai.hpp"
#include "engine.hpp"
#include "third_party/json.hpp"

#include <algorithm>
#include <limits>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <utility>

namespace {
using Json = nlohmann::json;
struct Handle {
  std::shared_ptr<const jot::Dictionary> dictionary;
  std::unique_ptr<jot::Game> game;
  std::string response;
};

std::shared_ptr<const jot::Dictionary> dictionary_for(const char* words) {
  // Every online room uses the same immutable lexicon. Retain one dictionary
  // per runtime instead of rebuilding a large trie for every short-lived game
  // handle. The mutex also protects native callers using different isolates.
  static std::mutex mutex;
  static std::string cached_words;
  static std::shared_ptr<const jot::Dictionary> cached_dictionary;
  const std::lock_guard<std::mutex> guard(mutex);
  if (!cached_dictionary || cached_words != words) {
    auto dictionary = std::make_shared<jot::Dictionary>(words);
    cached_words = words;
    cached_dictionary = std::move(dictionary);
  }
  return cached_dictionary;
}

const Json& field(const Json& object, const char* key) {
  if (!object.is_object() || !object.contains(key)) {
    throw std::invalid_argument(std::string("Missing field: ") + key);
  }
  return object.at(key);
}

std::int64_t integer(const Json& value, const char* name,
                     std::int64_t minimum, std::int64_t maximum) {
  if (!value.is_number_integer()) {
    throw std::invalid_argument(std::string(name) + " must be an integer.");
  }
  if (value.is_number_unsigned()) {
    const auto number = value.get<std::uint64_t>();
    if (number > static_cast<std::uint64_t>(maximum) ||
        (minimum > 0 && number < static_cast<std::uint64_t>(minimum))) {
      throw std::invalid_argument(std::string(name) + " is out of range.");
    }
    return static_cast<std::int64_t>(number);
  }
  const auto number = value.get<std::int64_t>();
  if (number < minimum || number > maximum) {
    throw std::invalid_argument(std::string(name) + " is out of range.");
  }
  return number;
}

int int_field(const Json& object, const char* key, int minimum, int maximum) {
  return static_cast<int>(integer(field(object, key), key, minimum, maximum));
}

std::uint32_t uint_field(const Json& object, const char* key) {
  return static_cast<std::uint32_t>(integer(field(object, key), key, 0,
                                          std::numeric_limits<std::uint32_t>::max()));
}

bool bool_field(const Json& object, const char* key) {
  const auto& value = field(object, key);
  if (!value.is_boolean()) {
    throw std::invalid_argument(std::string(key) + " must be a boolean.");
  }
  return value.get<bool>();
}

std::string string_field(const Json& object, const char* key) {
  const auto& value = field(object, key);
  if (!value.is_string()) {
    throw std::invalid_argument(std::string(key) + " must be a string.");
  }
  return value.get<std::string>();
}

char letter_field(const Json& object, const char* key, bool allow_blank = false) {
  const auto value = string_field(object, key);
  if (value.size() != 1 ||
      !((value[0] >= 'A' && value[0] <= 'Z') || (allow_blank && value[0] == '@'))) {
    throw std::invalid_argument(std::string(key) + " must be one uppercase letter.");
  }
  return value[0];
}

Json tile_json(const jot::Tile& tile) {
  return {{"letter", std::string(1, tile.letter)},
          {"blank", tile.blank}, {"owner", tile.owner},
          {"score", tile.blank ? 0 : jot::letter_score(tile.letter)}};
}

jot::Tile read_tile(const Json& value) {
  return {letter_field(value, "letter", true),
          bool_field(value, "blank"), int_field(value, "owner", 0, 1)};
}

Json placement_json(const jot::Placement& placement) {
  return {{"rack", placement.rack_index}, {"row", placement.row},
          {"col", placement.col}, {"letter", std::string(1, placement.letter)}};
}

jot::Placement read_placement(const Json& value) {
  return {int_field(value, "rack", 0, jot::kRackSize - 1),
          int_field(value, "row", 0, jot::kBoardSize - 1),
          int_field(value, "col", 0, jot::kBoardSize - 1),
          letter_field(value, "letter")};
}

Json evaluation_json(const jot::Evaluation& evaluation) {
  Json words = Json::array();
  for (const auto& word : evaluation.words) {
    words.push_back({{"word", word.word}, {"score", word.score}});
  }
  return {{"valid", evaluation.valid}, {"score", evaluation.score},
          {"message", evaluation.message}, {"words", std::move(words)}};
}

Json state_json(const jot::Game& game, int viewer = -1) {
  const auto& state = game.state();
  const bool is_turn = viewer < 0 || viewer == state.current_player;
  const int rack_player = viewer < 0 ? state.current_player : viewer;
  Json board = Json::array();
  Json bonuses = Json::array();
  for (int index = 0; index < jot::kBoardSize * jot::kBoardSize; ++index) {
    if (state.board[index]) {
      auto tile = tile_json(*state.board[index]);
      tile["pending"] = false;
      board.push_back(std::move(tile));
    } else {
      board.push_back(nullptr);
    }
    bonuses.push_back(jot::bonus_at(index / jot::kBoardSize,
                                   index % jot::kBoardSize).label);
  }
  std::array<bool, jot::kRackSize> placed{};
  if (is_turn) {
    for (const auto& placement : state.pending) {
      const auto& tile = state.players[state.current_player].rack.at(placement.rack_index);
      auto entry = tile_json(tile);
      entry["letter"] = std::string(1, placement.letter);
      entry["pending"] = true;
      entry["rack"] = placement.rack_index;
      board[placement.row * jot::kBoardSize + placement.col] = std::move(entry);
      placed.at(placement.rack_index) = true;
    }
  }
  Json rack = Json::array();
  const auto& player = state.players[rack_player];
  for (std::size_t index = 0; index < player.rack.size(); ++index) {
    auto tile = tile_json(player.rack[index]);
    tile["placed"] = placed.at(index);
    rack.push_back(std::move(tile));
  }
  Json remaining = Json::array();
  for (const auto& entry : state.players) {
    remaining.push_back({{"consonants", entry.consonants.size()},
                         {"vowels", entry.vowels.size()}});
  }
  const auto preview = is_turn ? game.preview()
      : jot::Evaluation{false, 0, "Waiting for the other player's turn.", {}};
  const bool can_draw = is_turn && !state.over && state.pending.empty() &&
                        player.rack.size() < jot::kRackSize &&
                        (!player.consonants.empty() || !player.vowels.empty());
  Json result{{"size", jot::kBoardSize}, {"rackSize", jot::kRackSize},
          {"currentPlayer", state.current_player}, {"robotPlayer", state.robot_player},
          {"over", state.over}, {"revision", state.revision},
          {"scores", state.scores}, {"board", std::move(board)},
          {"bonuses", std::move(bonuses)}, {"rack", std::move(rack)},
          {"remaining", std::move(remaining)}, {"preview", evaluation_json(preview)},
          {"canPlay", is_turn && !state.over && game.rack_ready() && preview.valid},
          {"canHint", is_turn && !state.over && game.rack_ready()},
          {"canDraw", can_draw}, {"canShuffle", is_turn && !state.over && state.pending.empty()},
          {"dictionaryWords", game.dictionary().words().size()}};
  if (viewer >= 0) {
    result["viewerPlayer"] = viewer;
    result["isMyTurn"] = is_turn && !state.over;
    result["rackCounts"] = {state.players[0].rack.size(), state.players[1].rack.size()};
  }
  return result;
}

Json save_json(const jot::State& state) {
  Json board = Json::array();
  for (const auto& tile : state.board) {
    board.push_back(tile ? tile_json(*tile) : Json(nullptr));
  }
  Json players = Json::array();
  for (const auto& player : state.players) {
    Json rack = Json::array();
    for (const auto& tile : player.rack) rack.push_back(tile_json(tile));
    players.push_back({{"rack", std::move(rack)},
                       {"consonants", std::string(player.consonants.begin(), player.consonants.end())},
                       {"vowels", std::string(player.vowels.begin(), player.vowels.end())}});
  }
  Json pending = Json::array();
  for (const auto& placement : state.pending) pending.push_back(placement_json(placement));
  return {{"version", 1}, {"fullState", {
    {"board", std::move(board)}, {"players", std::move(players)},
    {"scores", state.scores}, {"pending", std::move(pending)},
    {"currentPlayer", state.current_player}, {"robotPlayer", state.robot_player},
    {"consecutivePasses", state.consecutive_passes}, {"over", state.over},
    {"rngState", state.rng_state}, {"revision", state.revision}}}};
}

const Json& array_field(const Json& object, const char* name, std::size_t count) {
  const auto& value = field(object, name);
  if (!value.is_array() || value.size() != count) {
    throw std::invalid_argument(std::string(name) + " has the wrong array size.");
  }
  return value;
}

jot::State read_save(const Json& save) {
  if (int_field(save, "version", 1, std::numeric_limits<int>::max()) != 1) {
    throw std::invalid_argument("Unsupported save version.");
  }
  const auto& value = field(save, "fullState");
  jot::State state;
  state.current_player = int_field(value, "currentPlayer", 0, 1);
  state.robot_player = int_field(value, "robotPlayer", -1, 1);
  state.consecutive_passes = int_field(value, "consecutivePasses", 0, 2);
  state.over = bool_field(value, "over");
  state.rng_state = uint_field(value, "rngState");
  state.revision = uint_field(value, "revision");
  const auto& scores = array_field(value, "scores", 2);
  for (std::size_t index = 0; index < 2; ++index) {
    state.scores[index] = static_cast<int>(integer(scores[index], "score",
                             std::numeric_limits<int>::min(), std::numeric_limits<int>::max()));
  }
  const auto& board = array_field(value, "board", jot::kBoardSize * jot::kBoardSize);
  for (std::size_t index = 0; index < board.size(); ++index) {
    if (!board[index].is_null()) state.board[index] = read_tile(board[index]);
  }
  const auto& players = array_field(value, "players", 2);
  for (std::size_t index = 0; index < 2; ++index) {
    const auto& rack = field(players[index], "rack");
    if (!rack.is_array() || rack.size() > jot::kRackSize) {
      throw std::invalid_argument("A rack must contain at most eight tiles.");
    }
    for (const auto& tile : rack) state.players[index].rack.push_back(read_tile(tile));
    for (const auto* bag : {"consonants", "vowels"}) {
      const auto letters = string_field(players[index], bag);
      if (letters.size() > 128 ||
          !std::all_of(letters.begin(), letters.end(), [](char letter) {
            return (letter >= 'A' && letter <= 'Z') || letter == '@';
          })) {
        throw std::invalid_argument("A bag contains invalid tiles.");
      }
      auto& destination = std::string(bag) == "vowels" ? state.players[index].vowels
                                                      : state.players[index].consonants;
      destination.assign(letters.begin(), letters.end());
    }
  }
  const auto& pending = field(value, "pending");
  if (!pending.is_array() || pending.size() > jot::kRackSize) {
    throw std::invalid_argument("A move must contain at most eight placements.");
  }
  for (const auto& placement : pending) state.pending.push_back(read_placement(placement));
  return state;
}

Json hint_json(const jot::Move& move) {
  Json placements = Json::array();
  for (const auto& placement : move.placements) placements.push_back(placement_json(placement));
  return {{"score", move.evaluation.score},
          {"word", move.evaluation.words.empty() ? "" : move.evaluation.words.front().word},
          {"placements", std::move(placements)}, {"searchExhausted", move.search_exhausted}};
}

Json execute(Handle& handle, const Json& command) {
  const auto op = string_field(command, "op");
  jot::ActionResult action{true, ""};
  Json additional = Json::object();
  int viewer = -1;
  auto& game = *handle.game;
  if (op == "state") {
    action.message = "Game state.";
  } else if (op == "view") {
    viewer = int_field(command, "player", 0, 1);
    action.message = "Game state.";
  } else if (op == "new_game") {
    const auto seed = uint_field(command, "seed");
    const auto robot = int_field(command, "robot", -1, 1);
    auto next = std::make_unique<jot::Game>(handle.dictionary, seed, robot);
    handle.game = std::move(next);
    action.message = "New game started.";
  } else if (op == "draw") {
    const auto bag = string_field(command, "bag");
    if (bag != "vowel" && bag != "consonant") {
      throw std::invalid_argument("bag must be vowel or consonant.");
    }
    action = game.draw(bag == "vowel");
  } else if (op == "fill") {
    action = game.fill_rack();
  } else if (op == "place") {
    const auto placement = read_placement(command);
    action = game.place(placement);
  } else if (op == "unplace") {
    const auto rack = int_field(command, "rack", 0, jot::kRackSize - 1);
    action = game.unplace(rack);
  } else if (op == "recall") {
    action = game.recall();
  } else if (op == "shuffle") {
    action = game.shuffle();
  } else if (op == "play") {
    action = game.play();
  } else if (op == "pass") {
    action = game.pass();
  } else if (op == "hint") {
    const bool apply = command.contains("apply") ? bool_field(command, "apply") : false;
    if (game.state().over) {
      action = {false, "The game has ended."};
    } else {
      const auto move = jot::find_best_move(game);
      if (move && game.evaluate(move->placements).valid) {
        action = apply ? game.stage(*move)
                       : jot::ActionResult{true, "A legal move is available."};
        if (action.ok) additional["hint"] = hint_json(*move);
      } else {
        // A bounded search cannot establish that a player has no legal move.
        action = {false, "No hint found within the search budget. You can continue playing."};
      }
    }
  } else if (op == "robot") {
    if (game.state().over || game.state().current_player != game.state().robot_player) {
      action = {false, "It is not the computer's turn."};
    } else {
      // Perform the complete computer turn on a private copy, committing once.
      auto next = std::make_unique<jot::Game>(game);
      next->recall();
      next->fill_rack();
      auto move = jot::find_best_move(*next);
      if (!move || move->placements.empty()) {
        // Only a complete search can authorize passing an otherwise playable rack.
        move = jot::find_best_move(*next, std::numeric_limits<std::size_t>::max());
      }
      if (move && !move->placements.empty()) {
        const auto evaluation = next->evaluate(move->placements);
        if (!evaluation.valid) throw std::runtime_error("The computer produced an invalid move.");
        action = next->stage(*move);
        if (action.ok) action = next->play();
        if (action.ok) additional["hint"] = hint_json(*move);
      } else {
        action = next->pass();
      }
      if (action.ok) handle.game = std::move(next);
    }
  } else if (op == "save") {
    additional["save"] = save_json(game.state());
    action.message = "Game saved.";
  } else if (op == "restore") {
    const auto restored = read_save(field(command, "save"));
    action = game.restore(restored);
  } else {
    action = {false, "Unknown command: " + op};
  }
  Json response{{"ok", action.ok}, {"message", action.message},
                {"state", state_json(*handle.game, viewer)}};
  for (auto iterator = additional.begin(); iterator != additional.end(); ++iterator) {
    response[iterator.key()] = std::move(iterator.value());
  }
  return response;
}

const char* failure(Handle& handle, const char* message) noexcept {
  try {
    handle.response = Json{{"ok", false}, {"message", message},
                           {"state", state_json(*handle.game)}}.dump();
    return handle.response.c_str();
  } catch (...) {
    return "{\"ok\":false,\"message\":\"Unable to serialize game state.\",\"state\":null}";
  }
}

const char* command_response(void* context, const char* command_json) {
  auto& handle = *static_cast<Handle*>(context);
  if (!command_json) throw std::invalid_argument("Command JSON is null.");
  const auto command = Json::parse(command_json);
  if (!command.is_object()) throw std::invalid_argument("Command must be an object.");
  handle.response = execute(handle, command).dump();
  return handle.response.c_str();
}

const char* error_response(void* context, const char* message) noexcept {
  return failure(*static_cast<Handle*>(context), message);
}
}  // namespace

extern "C" {
JOT_EXPORT void* jot_create(const char* dictionary, uint32_t seed, int32_t robot_player) {
  try {
    if (!dictionary || robot_player < -1 || robot_player > 1) return nullptr;
    auto handle = std::make_unique<Handle>();
    handle->dictionary = dictionary_for(dictionary);
    if (handle->dictionary->words().empty()) return nullptr;
    handle->game = std::make_unique<jot::Game>(handle->dictionary, seed, robot_player);
    return handle.release();
  } catch (...) {
    return nullptr;
  }
}

JOT_EXPORT const char* jot_execute(void* opaque, const char* command_json) {
  if (!opaque) return "{\"ok\":false,\"message\":\"Invalid game handle.\",\"state\":null}";
  return jot::invoke_command(opaque, command_json, command_response, error_response);
}

JOT_EXPORT void jot_destroy(void* handle) {
  // Handle contains only shared_ptr, unique_ptr and string; all of their
  // destructors and the standard deallocation function are noexcept.
  delete static_cast<Handle*>(handle);
}
}
