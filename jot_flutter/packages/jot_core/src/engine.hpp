#pragma once
#include "dictionary.hpp"
#include <array>
#include <cstdint>
#include <memory>
#include <optional>
#include <string>
#include <vector>

namespace jot {
constexpr int kBoardSize = 16;
constexpr int kRackSize = 8;
struct Tile {
  char letter = '@';
  bool blank = false;
  int owner = 0;
};
struct Placement {
  int rack_index = 0;
  int row = 0;
  int col = 0;
  char letter = 'A';
};
struct WordScore {
  std::string word;
  int score = 0;
  int row = 0;
  int col = 0;
  bool vertical = false;
};
struct Evaluation {
  bool valid = false;
  int score = 0;
  std::string message;
  std::vector<WordScore> words;
};
struct Move {
  std::vector<Placement> placements;
  Evaluation evaluation;
  bool search_exhausted = false;
};
struct ActionResult {
  bool ok = false;
  std::string message;
};
struct PlayerState {
  std::vector<Tile> rack;
  std::vector<char> consonants;
  std::vector<char> vowels;
};
struct State {
  std::array<std::optional<Tile>, kBoardSize * kBoardSize> board{};
  std::array<PlayerState, 2> players;
  std::array<int, 2> scores{};
  std::vector<Placement> pending;
  int current_player = 0;
  int robot_player = 1;
  int consecutive_passes = 0;
  bool over = false;
  std::uint32_t rng_state = 1;
  std::uint32_t revision = 0;
};
struct Bonus {
  int letter_multiplier = 1;
  int word_multiplier = 1;
  std::string label;
};
int letter_score(char letter);
Bonus bonus_at(int row, int col);
class Game {
 public:
  explicit Game(std::shared_ptr<const Dictionary> dictionary,
                std::uint32_t seed = 1, int robot_player = 1);
  const State& state() const { return state_; }
  const Dictionary& dictionary() const { return *dictionary_; }
  bool rack_ready() const;
  Evaluation evaluate(const std::vector<Placement>& placements) const;
  Evaluation preview() const { return evaluate(state_.pending); }
  ActionResult draw(bool vowel);
  ActionResult fill_rack();
  ActionResult place(Placement placement);
  ActionResult unplace(int rack_index);
  ActionResult recall();
  ActionResult shuffle();
  ActionResult play();
  ActionResult pass();
  ActionResult stage(const Move& move);
  ActionResult restore(const State& state);
 private:
  std::shared_ptr<const Dictionary> dictionary_;
  State state_;
  std::uint32_t random();
  void finish(int out_player = -1);
};
}
