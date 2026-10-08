#include "engine.hpp"

#include <algorithm>
#include <array>
#include <limits>
#include <queue>
#include <set>
#include <stdexcept>
#include <tuple>

namespace jot {
namespace {

constexpr char kConsonants[] = "BBCDDFGGHJKLLLMNNNPPQRRRRSSSTTTTVWXYZ";
constexpr char kVowels[] = "AAAAAEEEEEEEEIIIIIOOOOOUUU@";
constexpr int kScoreLimit = 1000000;

// Positive values multiply a letter; negative values multiply a word.
constexpr int kBonuses[kBoardSize][kBoardSize] = {
    {0, 0, 0, 0, -3, 0, 0, 0, 0, 0, 0, -3, 0, 0, 0, 0},
    {0, 3, 0, 3, 0, -2, 0, 0, 0, 0, -2, 0, 3, 0, 3, 0},
    {0, 0, -3, 0, 0, 0, 2, 0, 0, 2, 0, 0, 0, -3, 0, 0},
    {0, 3, 0, 3, 0, 0, 0, -2, -2, 0, 0, 0, 3, 0, 3, 0},
    {-3, 0, 0, 0, 3, 0, 2, 0, 0, 2, 0, 3, 0, 0, 0, -3},
    {0, -2, 0, 0, 0, -2, 0, 0, 0, 0, -2, 0, 0, 0, -2, 0},
    {0, 0, 2, 0, 2, 0, 2, 0, 0, 2, 0, 2, 0, 2, 0, 0},
    {0, 0, 0, -2, 0, 0, 0, 2, 2, 0, 0, 0, -2, 0, 0, 0},
    {0, 0, 0, -2, 0, 0, 0, 2, 2, 0, 0, 0, -2, 0, 0, 0},
    {0, 0, 2, 0, 2, 0, 2, 0, 0, 2, 0, 2, 0, 2, 0, 0},
    {0, -2, 0, 0, 0, -2, 0, 0, 0, 0, -2, 0, 0, 0, -2, 0},
    {-3, 0, 0, 0, 3, 0, 2, 0, 0, 2, 0, 3, 0, 0, 0, -3},
    {0, 3, 0, 3, 0, 0, 0, -2, -2, 0, 0, 0, 3, 0, 3, 0},
    {0, 0, -3, 0, 0, 0, 2, 0, 0, 2, 0, 0, 0, -3, 0, 0},
    {0, 3, 0, 3, 0, -2, 0, 0, 0, 0, -2, 0, 3, 0, 3, 0},
    {0, 0, 0, 0, -3, 0, 0, 0, 0, 0, 0, -3, 0, 0, 0, 0},
};

bool inside(int row, int col) {
  return row >= 0 && row < kBoardSize && col >= 0 && col < kBoardSize;
}

int index(int row, int col) { return row * kBoardSize + col; }

bool alpha(char letter) { return letter >= 'A' && letter <= 'Z'; }

bool vowel(char letter) {
  return letter == 'A' || letter == 'E' || letter == 'I' ||
         letter == 'O' || letter == 'U' || letter == '@';
}

bool centre(int row, int col) {
  return (row == 7 || row == 8) && (col == 7 || col == 8);
}

bool exhausted(const PlayerState& player) {
  return player.rack.empty() && player.consonants.empty() && player.vowels.empty();
}

// Staging can be incomplete, but must always reference distinct rack tiles and
// distinct vacant cells. This check is shared by staging and state restoration.
std::string placement_error(const State& state,
                            const std::vector<Placement>& placements) {
  const auto& rack = state.players[state.current_player].rack;
  std::array<bool, kRackSize> used_tiles{};
  std::array<bool, kBoardSize * kBoardSize> used_cells{};
  for (const auto& placement : placements) {
    if (placement.rack_index < 0 ||
        placement.rack_index >= static_cast<int>(rack.size())) {
      return "That rack tile is unavailable.";
    }
    if (!inside(placement.row, placement.col)) {
      return "Choose a square on the board.";
    }
    const int cell = index(placement.row, placement.col);
    if (state.board[cell]) return "That square already contains a tile.";
    if (used_tiles[placement.rack_index]) return "A rack tile can be used only once.";
    if (used_cells[cell]) return "Place only one tile in each square.";
    const auto& tile = rack[placement.rack_index];
    if (!alpha(placement.letter)) return "Choose a letter from A to Z for the blank.";
    if (!tile.blank && placement.letter != tile.letter) {
      return "The placed letter must match the rack tile.";
    }
    used_tiles[placement.rack_index] = true;
    used_cells[cell] = true;
  }
  return {};
}

int remaining_points(const PlayerState& player) {
  int points = 0;
  for (const auto& tile : player.rack) {
    points += tile.blank ? 0 : letter_score(tile.letter);
  }
  for (char letter : player.consonants) points += letter_score(letter);
  for (char letter : player.vowels) points += letter_score(letter);
  return points;
}

bool board_connected(const State& state) {
  std::array<bool, kBoardSize * kBoardSize> visited{};
  std::queue<int> queue;
  int occupied = 0;
  for (int row = 0; row < kBoardSize; ++row) {
    for (int col = 0; col < kBoardSize; ++col) {
      const int cell = index(row, col);
      if (!state.board[cell]) continue;
      ++occupied;
      if (centre(row, col) && queue.empty()) {
        visited[cell] = true;
        queue.push(cell);
      }
    }
  }
  if (occupied == 0) return true;
  if (occupied == 1) return false;
  int reached = 0;
  constexpr int dr[] = {-1, 0, 1, 0};
  constexpr int dc[] = {0, -1, 0, 1};
  while (!queue.empty()) {
    const int cell = queue.front();
    queue.pop();
    ++reached;
    for (int direction = 0; direction < 4; ++direction) {
      const int row = cell / kBoardSize + dr[direction];
      const int col = cell % kBoardSize + dc[direction];
      if (!inside(row, col)) continue;
      const int adjacent = index(row, col);
      if (state.board[adjacent] && !visited[adjacent]) {
        visited[adjacent] = true;
        queue.push(adjacent);
      }
    }
  }
  return reached == occupied;
}

bool board_words_valid(const State& state, const Dictionary& dictionary) {
  for (bool vertical : {false, true}) {
    for (int line = 0; line < kBoardSize; ++line) {
      std::string word;
      for (int offset = 0; offset <= kBoardSize; ++offset) {
        const int row = vertical ? offset : line;
        const int col = vertical ? line : offset;
        if (offset < kBoardSize && state.board[index(row, col)]) {
          word += state.board[index(row, col)]->letter;
        } else {
          if (word.size() > 1 && !dictionary.contains(word)) return false;
          word.clear();
        }
      }
    }
  }
  return true;
}

}  // namespace

int letter_score(char letter) {
  constexpr int values[] = {1, 3, 4, 2, 1, 4, 2, 3, 1, 8, 6, 1, 3,
                            1, 1, 3, 10, 1, 1, 1, 2, 5, 4, 7, 4, 9};
  return alpha(letter) ? values[letter - 'A'] : 0;
}

Bonus bonus_at(int row, int col) {
  Bonus bonus;
  if (!inside(row, col)) return bonus;
  const int value = kBonuses[row][col];
  if (value > 0) {
    bonus.letter_multiplier = value;
    bonus.label = "L" + std::to_string(value);
  } else if (value < 0) {
    bonus.word_multiplier = -value;
    bonus.label = "W" + std::to_string(-value);
  }
  return bonus;
}

Game::Game(std::shared_ptr<const Dictionary> dictionary, std::uint32_t seed,
           int robot_player)
    : dictionary_(std::move(dictionary)) {
  if (!dictionary_) throw std::invalid_argument("A dictionary is required.");
  if (robot_player < -1 || robot_player > 1) {
    throw std::invalid_argument("Robot player must be -1, 0, or 1.");
  }
  state_.rng_state = seed == 0 ? 1 : seed;
  state_.robot_player = robot_player;
  for (auto& player : state_.players) {
    player.consonants.assign(kConsonants, kConsonants + sizeof(kConsonants) - 1);
    player.vowels.assign(kVowels, kVowels + sizeof(kVowels) - 1);
  }
}

bool Game::rack_ready() const {
  const auto& player = state_.players[state_.current_player];
  return player.rack.size() == kRackSize ||
         (player.consonants.empty() && player.vowels.empty());
}

Evaluation Game::evaluate(const std::vector<Placement>& placements) const {
  Evaluation result;
  if (state_.over) {
    result.message = "The game is over.";
    return result;
  }
  if (placements.empty()) {
    result.message = "Place tiles first.";
    return result;
  }
  result.message = placement_error(state_, placements);
  if (!result.message.empty()) return result;

  const int first_row = placements.front().row;
  const int first_col = placements.front().col;
  bool same_row = true;
  bool same_col = true;
  int min_row = first_row, max_row = first_row;
  int min_col = first_col, max_col = first_col;
  auto board = state_.board;
  std::array<bool, kBoardSize * kBoardSize> fresh{};
  const auto& rack = state_.players[state_.current_player].rack;
  bool touches_centre = false;
  for (const auto& placement : placements) {
    same_row = same_row && placement.row == first_row;
    same_col = same_col && placement.col == first_col;
    min_row = std::min(min_row, placement.row);
    max_row = std::max(max_row, placement.row);
    min_col = std::min(min_col, placement.col);
    max_col = std::max(max_col, placement.col);
    const auto& tile = rack[placement.rack_index];
    const int cell = index(placement.row, placement.col);
    board[cell] = Tile{placement.letter, tile.blank, state_.current_player};
    fresh[cell] = true;
    touches_centre = touches_centre || centre(placement.row, placement.col);
  }
  if (!same_row && !same_col) {
    result.message = "Place your tiles in one row or column.";
    return result;
  }
  for (int row = min_row; row <= max_row; ++row) {
    for (int col = min_col; col <= max_col; ++col) {
      if (!board[index(row, col)]) {
        result.message = "Leave no gaps between your tiles.";
        return result;
      }
    }
  }

  const bool empty = std::none_of(state_.board.begin(), state_.board.end(),
                                [](const auto& tile) { return tile.has_value(); });
  if (empty) {
    if (placements.size() < 2) {
      result.message = "The first word needs at least two letters.";
      return result;
    }
    if (!touches_centre) {
      result.message = "The first word must touch a centre square.";
      return result;
    }
  } else {
    bool connected = false;
    constexpr int dr[] = {-1, 0, 1, 0};
    constexpr int dc[] = {0, -1, 0, 1};
    for (const auto& placement : placements) {
      for (int direction = 0; direction < 4; ++direction) {
        const int row = placement.row + dr[direction];
        const int col = placement.col + dc[direction];
        if (inside(row, col) && state_.board[index(row, col)]) connected = true;
      }
    }
    if (!connected) {
      result.message = "Connect your word to the tiles on the board.";
      return result;
    }
  }

  std::set<std::tuple<int, int, bool>> seen_words;
  std::int64_t total = 0;
  for (const auto& placement : placements) {
    for (bool vertical : {false, true}) {
      const int dr = vertical ? 1 : 0;
      const int dc = vertical ? 0 : 1;
      int row = placement.row;
      int col = placement.col;
      while (inside(row - dr, col - dc) && board[index(row - dr, col - dc)]) {
        row -= dr;
        col -= dc;
      }
      if (!seen_words.emplace(row, col, vertical).second) continue;
      WordScore word;
      word.row = row;
      word.col = col;
      word.vertical = vertical;
      int points = 0;
      int multiplier = 1;
      while (inside(row, col) && board[index(row, col)]) {
        const int cell = index(row, col);
        const auto& tile = *board[cell];
        word.word += tile.letter;
        int tile_points = tile.blank ? 0 : letter_score(tile.letter);
        if (fresh[cell]) {
          const Bonus bonus = bonus_at(row, col);
          tile_points *= bonus.letter_multiplier;
          multiplier *= bonus.word_multiplier;
        }
        points += tile_points;
        row += dr;
        col += dc;
      }
      if (word.word.size() < 2) continue;
      word.score = points * multiplier;
      result.words.push_back(word);
      if (!dictionary_->contains(word.word)) {
        result.message = word.word + " is not in the dictionary.";
        return result;
      }
      total += word.score;
    }
  }
  // Opening continuity plus the two-tile minimum guarantees a word. On later
  // turns, the required adjacency to a committed tile guarantees a word. A
  // valid placement therefore always has at least one scored two-letter word.
  if (placements.size() == kRackSize) total += 100;
  std::int64_t resulting_score = state_.scores[state_.current_player] + total;
  const auto& player = state_.players[state_.current_player];
  if (placements.size() == player.rack.size() &&
      player.consonants.empty() && player.vowels.empty()) {
    resulting_score += remaining_points(state_.players[state_.current_player ^ 1]);
  }
  if (total > kScoreLimit || resulting_score > kScoreLimit) {
    result.message = "The score exceeds the supported game limit.";
    return result;
  }
  result.valid = true;
  result.score = static_cast<int>(total);
  result.message = "Valid placement.";
  return result;
}

std::uint32_t Game::random() {
  std::uint32_t value = state_.rng_state;
  value ^= value << 13;
  value ^= value >> 17;
  value ^= value << 5;
  state_.rng_state = value;
  return value;
}

ActionResult Game::draw(bool draw_vowel) {
  if (state_.over) return {false, "The game is over."};
  if (!state_.pending.empty()) return {false, "Recall your placed tiles before drawing."};
  auto& player = state_.players[state_.current_player];
  if (player.rack.size() >= kRackSize) return {false, "Your rack is full."};
  auto& bag = draw_vowel ? player.vowels : player.consonants;
  if (bag.empty()) return {false, draw_vowel ? "Your vowel bag is empty." : "Your consonant bag is empty."};
  const std::size_t picked = random() % bag.size();
  const char letter = bag[picked];
  player.rack.push_back(Tile{letter, letter == '@', state_.current_player});
  bag.erase(bag.begin() + static_cast<std::ptrdiff_t>(picked));
  ++state_.revision;
  return {true, "Tile drawn."};
}

ActionResult Game::fill_rack() {
  if (state_.over) return {false, "The game is over."};
  if (!state_.pending.empty()) return {false, "Recall your placed tiles before drawing."};
  auto& player = state_.players[state_.current_player];
  while (player.rack.size() < kRackSize &&
         (!player.vowels.empty() || !player.consonants.empty())) {
    const auto vowels = std::count_if(player.rack.begin(), player.rack.end(),
                                     [](const auto& tile) { return tile.blank || vowel(tile.letter); });
    const bool draw_vowel = !player.vowels.empty() &&
                           (player.consonants.empty() || vowels * 2 <= static_cast<int>(player.rack.size()));
    draw(draw_vowel);
  }
  return {true, "Rack filled with the available tiles."};
}

ActionResult Game::place(Placement placement) {
  if (state_.over) return {false, "The game is over."};
  auto pending = state_.pending;
  const auto previous = std::find_if(pending.begin(), pending.end(),
                                     [&](const auto& item) { return item.rack_index == placement.rack_index; });
  if (previous == pending.end()) pending.push_back(placement);
  else *previous = placement;
  const auto error = placement_error(state_, pending);
  if (!error.empty()) return {false, error};
  state_.pending = std::move(pending);
  ++state_.revision;
  return {true, "Tile placed."};
}

ActionResult Game::unplace(int rack_index) {
  if (state_.over) return {false, "The game is over."};
  const auto found = std::find_if(state_.pending.begin(), state_.pending.end(),
                                [&](const auto& item) { return item.rack_index == rack_index; });
  if (found == state_.pending.end()) return {false, "That tile is not placed."};
  state_.pending.erase(found);
  ++state_.revision;
  return {true, "Tile returned to your rack."};
}

ActionResult Game::recall() {
  if (state_.over) return {false, "The game is over."};
  if (!state_.pending.empty()) {
    state_.pending.clear();
    ++state_.revision;
  }
  return {true, "Tiles returned to your rack."};
}

ActionResult Game::shuffle() {
  if (state_.over) return {false, "The game is over."};
  if (!state_.pending.empty()) return {false, "Recall your placed tiles before shuffling."};
  auto& rack = state_.players[state_.current_player].rack;
  for (std::size_t count = rack.size(); count > 1; --count) {
    std::swap(rack[count - 1], rack[random() % count]);
  }
  if (rack.size() > 1) ++state_.revision;
  return {true, "Rack shuffled."};
}

ActionResult Game::play() {
  if (state_.over) return {false, "The game is over."};
  if (!rack_ready()) {
    return {false, "Draw eight tiles before playing. Recall placed tiles to draw more."};
  }
  const Evaluation evaluation = evaluate(state_.pending);
  if (!evaluation.valid) return {false, evaluation.message};
  State next = state_;
  const int player = next.current_player;
  auto& rack = next.players[player].rack;
  std::vector<int> consumed;
  for (const auto& placement : next.pending) {
    const auto& tile = rack[placement.rack_index];
    next.board[index(placement.row, placement.col)] = Tile{placement.letter, tile.blank, player};
    consumed.push_back(placement.rack_index);
  }
  std::sort(consumed.rbegin(), consumed.rend());
  for (int rack_index : consumed) rack.erase(rack.begin() + rack_index);
  next.pending.clear();
  next.scores[player] += evaluation.score;
  next.consecutive_passes = 0;
  ++next.revision;
  state_ = std::move(next);
  if (exhausted(state_.players[player])) finish(player);
  else state_.current_player ^= 1;
  return {true, "Played for " + std::to_string(evaluation.score) + " points."};
}

void Game::finish(int out_player) {
  int leftover = 0;
  for (int player = 0; player < 2; ++player) {
    const int points = remaining_points(state_.players[player]);
    state_.scores[player] = std::max(0, state_.scores[player] - points);
    leftover += points;
  }
  if (out_player >= 0) state_.scores[out_player] += leftover;
  state_.pending.clear();
  state_.over = true;
}

ActionResult Game::pass() {
  if (state_.over) return {false, "The game is over."};
  state_.pending.clear();
  ++state_.consecutive_passes;
  ++state_.revision;
  if (state_.consecutive_passes >= 2) finish();
  else state_.current_player ^= 1;
  return {true, state_.over ? "Both players passed. The game is over." : "Turn passed."};
}

ActionResult Game::stage(const Move& move) {
  const Evaluation evaluation = evaluate(move.placements);
  if (!evaluation.valid) return {false, evaluation.message};
  state_.pending = move.placements;
  ++state_.revision;
  return {true, "Move placed. Press play to confirm."};
}

ActionResult Game::restore(const State& state) {
  if (state.current_player < 0 || state.current_player > 1 ||
      state.robot_player < -1 || state.robot_player > 1) {
    return {false, "Invalid player in saved game."};
  }
  if (state.rng_state == 0) return {false, "Invalid random state in saved game."};
  if (state.consecutive_passes < 0 || state.consecutive_passes > 2 ||
      (state.consecutive_passes == 2 && !state.over)) {
    return {false, "Invalid pass count in saved game."};
  }
  if (state.pending.size() > kRackSize || (state.over && !state.pending.empty())) {
    return {false, "Invalid pending move in saved game."};
  }
  std::array<std::array<int, 27>, 2> counts{};
  std::array<int, 27> expected{};
  const auto count_letter = [](std::array<int, 27>& inventory, char letter) {
    ++inventory[letter == '@' ? 26 : letter - 'A'];
  };
  for (char letter : std::string(kConsonants) + kVowels) count_letter(expected, letter);
  for (int player = 0; player < 2; ++player) {
    if (state.scores[player] < 0 || state.scores[player] > kScoreLimit) {
      return {false, "Invalid score in saved game."};
    }
    const auto& saved = state.players[player];
    if (saved.rack.size() > kRackSize ||
        saved.consonants.size() >= sizeof(kConsonants) ||
        saved.vowels.size() >= sizeof(kVowels)) {
      return {false, "Invalid rack or bag size in saved game."};
    }
    for (const auto& tile : saved.rack) {
      if (tile.owner != player ||
          (tile.blank ? tile.letter != '@' : !alpha(tile.letter))) {
        return {false, "Invalid rack tile in saved game."};
      }
      count_letter(counts[player], tile.blank ? '@' : tile.letter);
    }
    for (char letter : saved.consonants) {
      if (!alpha(letter) || vowel(letter)) return {false, "Invalid consonant bag in saved game."};
      count_letter(counts[player], letter);
    }
    for (char letter : saved.vowels) {
      if (!vowel(letter)) return {false, "Invalid vowel bag in saved game."};
      count_letter(counts[player], letter);
    }
  }
  for (const auto& tile : state.board) {
    if (!tile) continue;
    if (tile->owner < 0 || tile->owner > 1 || !alpha(tile->letter)) {
      return {false, "Invalid board tile in saved game."};
    }
    count_letter(counts[tile->owner], tile->blank ? '@' : tile->letter);
  }
  if (counts[0] != expected || counts[1] != expected) {
    return {false, "The saved tile inventory does not match a Jot game."};
  }
  const auto error = placement_error(state, state.pending);
  if (!error.empty()) return {false, "Invalid pending move: " + error};
  if (!board_connected(state) || !board_words_valid(state, *dictionary_)) {
    return {false, "The saved board is disconnected or contains invalid words."};
  }
  const bool someone_out = exhausted(state.players[0]) || exhausted(state.players[1]);
  if ((!state.over && someone_out) ||
      (state.over && !someone_out && state.consecutive_passes < 2) ||
      (someone_out && (state.consecutive_passes != 0 ||
                      !exhausted(state.players[state.current_player]))) ||
      (exhausted(state.players[0]) && exhausted(state.players[1]))) {
    return {false, "Invalid game-over state in saved game."};
  }
  state_ = state;
  return {true, "Saved game restored."};
}

}  // namespace jot
