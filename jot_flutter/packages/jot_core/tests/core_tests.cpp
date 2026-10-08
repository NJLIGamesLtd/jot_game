#include "engine.hpp"
#include "abi_boundary.hpp"

#include <algorithm>
#include <functional>
#include <iostream>
#include <memory>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

namespace {

#define CHECK(condition)                                                        \
  do {                                                                          \
    if (!(condition)) {                                                         \
      throw std::runtime_error(std::string("Check failed at line ") +            \
                               std::to_string(__LINE__) + ": " #condition);      \
    }                                                                           \
  } while (false)

using jot::Game;
using jot::Placement;
using jot::State;
using jot::Tile;

std::shared_ptr<const jot::Dictionary> small_dictionary() {
  return std::make_shared<const jot::Dictionary>(
      "AT\nCAT\nCATS\nSCAT\nAS\nAA\nST\nSA\nIT\nBANISTER\nREADINGS\nBRAINSTORM\n");
}

std::string fingerprint(const State& state) {
  std::ostringstream out;
  out << state.current_player << ',' << state.robot_player << ','
      << state.consecutive_passes << ',' << state.over << ',' << state.rng_state
      << ',' << state.revision << ';';
  for (const auto& tile : state.board) {
    if (tile) out << tile->letter << ',' << tile->blank << ',' << tile->owner << ';';
    else out << "-;";
  }
  for (int player = 0; player < 2; ++player) {
    out << state.scores[player] << ':';
    for (const auto& tile : state.players[player].rack) {
      out << tile.letter << ',' << tile.blank << ',' << tile.owner << ';';
    }
    out << '/';
    for (char letter : state.players[player].consonants) out << letter;
    out << '/';
    for (char letter : state.players[player].vowels) out << letter;
    out << '|';
  }
  for (const auto& placement : state.pending) {
    out << placement.rack_index << ',' << placement.row << ',' << placement.col
        << ',' << placement.letter << ';';
  }
  return out.str();
}

Tile take(State& state, int player, char letter, bool blank = false) {
  const char inventory_letter = blank ? '@' : letter;
  for (auto* bag : {&state.players[player].consonants, &state.players[player].vowels}) {
    const auto found = std::find(bag->begin(), bag->end(), inventory_letter);
    if (found != bag->end()) {
      bag->erase(found);
      return Tile{letter, blank, player};
    }
  }
  throw std::runtime_error("Fixture requested an unavailable letter.");
}

void rack(State& state, int player, const std::string& letters, bool full = true) {
  for (char letter : letters) {
    state.players[player].rack.push_back(take(state, player, letter, letter == '@'));
  }
  while (full && state.players[player].rack.size() < jot::kRackSize) {
    auto& bag = state.players[player].vowels;
    CHECK(!bag.empty());
    state.players[player].rack.push_back(take(state, player, bag.front()));
  }
}

void board_tile(State& state, int player, int row, int col, char letter,
                bool blank = false) {
  state.board[row * jot::kBoardSize + col] = take(state, player, letter, blank);
}

void rejected_unchanged(Game& game, const std::function<jot::ActionResult()>& action) {
  const auto before = fingerprint(game.state());
  CHECK(!action().ok);
  CHECK(fingerprint(game.state()) == before);
}

int points(const jot::PlayerState& player) {
  int score = 0;
  for (const auto& tile : player.rack) score += tile.blank ? 0 : jot::letter_score(tile.letter);
  for (char letter : player.consonants) score += jot::letter_score(letter);
  for (char letter : player.vowels) score += jot::letter_score(letter);
  return score;
}

void test_draw_and_seed() {
  Game game(small_dictionary(), 42);
  CHECK(game.state().players[0].rack.empty());
  CHECK(!game.rack_ready());
  CHECK(game.state().players[0].consonants.size() == 37);
  CHECK(game.state().players[0].vowels.size() == 27);
  CHECK(game.state().players[1].consonants.size() == 37);
  Game same = game;
  CHECK(game.draw(true).ok && same.draw(true).ok);
  CHECK(game.draw(false).ok && same.draw(false).ok);
  CHECK(fingerprint(game.state()) == fingerprint(same.state()));
  CHECK(game.state().players[1].rack.empty());
  CHECK(game.fill_rack().ok);
  CHECK(game.state().players[0].rack.size() == 8);
  CHECK(game.rack_ready());
  int vowels = 0;
  for (const auto& tile : game.state().players[0].rack) {
    if (tile.blank || std::string("AEIOU").find(tile.letter) != std::string::npos) ++vowels;
    CHECK(tile.owner == 0);
  }
  CHECK(vowels == 4);
  rejected_unchanged(game, [&] { return game.draw(false); });
  const auto save = game.state();
  Game loaded(small_dictionary(), 999);
  CHECK(loaded.restore(save).ok);
  CHECK(game.shuffle().ok && loaded.shuffle().ok);
  CHECK(fingerprint(game.state()) == fingerprint(loaded.state()));
  Game zero(small_dictionary(), 0);
  CHECK(zero.state().rng_state != 0);
  bool threw = false;
  try { Game invalid(small_dictionary(), 1, 2); }
  catch (const std::invalid_argument&) { threw = true; }
  CHECK(threw);
}

void test_first_word_and_atomic_play() {
  Game game(small_dictionary(), 1, -1);
  State fixture = game.state();
  rack(fixture, 0, "CATS");
  CHECK(game.restore(fixture).ok);
  CHECK(!game.evaluate({Placement{1, 7, 7, 'A'}}).valid);
  CHECK(!game.evaluate({Placement{1, 0, 0, 'A'}, Placement{2, 0, 1, 'T'}}).valid);
  CHECK(!game.evaluate({Placement{1, 7, 7, 'A'}, Placement{2, 8, 8, 'T'}}).valid);
  CHECK(!game.evaluate({Placement{1, 7, 7, 'A'}, Placement{2, 7, 9, 'T'}}).valid);
  const auto before = fingerprint(game.state());
  const auto evaluated = game.evaluate({Placement{1, 7, 7, 'A'}, Placement{2, 7, 8, 'T'}});
  CHECK(evaluated.valid && evaluated.score == 4);
  CHECK(evaluated.words.size() == 1 && evaluated.words[0].word == "AT");
  CHECK(fingerprint(game.state()) == before);
  CHECK(game.place({1, 7, 7, 'A'}).ok);
  rejected_unchanged(game, [&] { return game.draw(true); });
  rejected_unchanged(game, [&] { return game.shuffle(); });
  rejected_unchanged(game, [&] { return game.place({2, 7, 7, 'T'}); });
  CHECK(game.place({2, 7, 8, 'T'}).ok);
  CHECK(game.preview().valid);
  CHECK(game.play().ok);
  CHECK(game.state().scores[0] == 4);
  CHECK(game.state().current_player == 1);
  CHECK(game.state().pending.empty());
  CHECK(game.state().players[0].rack.size() == 6);
  CHECK(game.state().players[0].rack[0].letter == 'C');
  CHECK(game.state().players[0].rack[1].letter == 'S');
  CHECK(game.state().board[7 * 16 + 7]->letter == 'A');
  CHECK(game.state().board[7 * 16 + 8]->letter == 'T');
  CHECK(game.restore(game.state()).ok);

  Game short_rack(small_dictionary());
  fixture = short_rack.state();
  rack(fixture, 0, "AT", false);
  CHECK(short_rack.restore(fixture).ok);
  CHECK(!short_rack.rack_ready());
  CHECK(short_rack.place({0, 7, 7, 'A'}).ok);
  CHECK(short_rack.place({1, 7, 8, 'T'}).ok);
  CHECK(short_rack.preview().valid);
  rejected_unchanged(short_rack, [&] { return short_rack.play(); });
  CHECK(short_rack.recall().ok);
  CHECK(short_rack.fill_rack().ok);
  CHECK(short_rack.rack_ready());
}

void test_bridge_and_copy_isolation() {
  for (bool vertical : {false, true}) {
    Game game(small_dictionary());
    State fixture = game.state();
    board_tile(fixture, 1, 7, 7, 'A');
    board_tile(fixture, 1, vertical ? 8 : 7, vertical ? 7 : 8, 'T');
    rack(fixture, 0, "CS");
    CHECK(game.restore(fixture).ok);
    const std::vector<Placement> placements = vertical
        ? std::vector<Placement>{{0, 6, 7, 'C'}, {1, 9, 7, 'S'}}
        : std::vector<Placement>{{0, 7, 6, 'C'}, {1, 7, 9, 'S'}};
    const auto before = fingerprint(game.state());
    CHECK(game.evaluate(placements).valid);
    CHECK(game.evaluate(placements).score == 7);
    CHECK(fingerprint(game.state()) == before);
    Game copied = game;
    jot::Move move;
    move.placements = placements;
    CHECK(copied.stage(move).ok && copied.play().ok);
    CHECK(fingerprint(game.state()) == before);
    CHECK(copied.state().scores[0] == 7);
    CHECK(copied.restore(copied.state()).ok);
  }
}

void test_crosswords_and_blanks() {
  Game game(small_dictionary());
  State fixture = game.state();
  board_tile(fixture, 1, 7, 6, 'A');
  board_tile(fixture, 1, 7, 7, 'T');
  rack(fixture, 0, "AS");
  CHECK(game.restore(fixture).ok);
  const auto evaluation = game.evaluate({{0, 6, 6, 'A'}, {1, 6, 7, 'S'}});
  CHECK(evaluation.valid && evaluation.score == 8);
  CHECK(evaluation.words.size() == 3);
  CHECK(!game.evaluate({{0, 0, 0, 'A'}, {1, 0, 1, 'S'}}).valid);
  CHECK(!game.evaluate({{0, 6, 6, 'A'}, {1, 7, 8, 'S'}}).valid);
  CHECK(!game.evaluate({{0, 6, 6, 'A'}, {0, 6, 7, 'A'}}).valid);

  Game blank(small_dictionary());
  fixture = blank.state();
  rack(fixture, 0, "@T");
  CHECK(blank.restore(fixture).ok);
  CHECK(blank.place({0, 7, 7, 'A'}).ok);
  CHECK(blank.place({1, 7, 8, 'T'}).ok);
  CHECK(blank.preview().valid && blank.preview().score == 2);
  CHECK(blank.state().players[0].rack[0].letter == '@');
  CHECK(blank.state().players[0].rack[0].blank);
  CHECK(blank.recall().ok);
  CHECK(blank.state().players[0].rack[0].letter == '@');
  CHECK(blank.place({0, 7, 7, 'A'}).ok);
  CHECK(blank.place({1, 7, 8, 'T'}).ok);
  CHECK(blank.play().ok);
  CHECK(blank.state().board[7 * 16 + 7]->blank);
  CHECK(blank.state().board[7 * 16 + 7]->letter == 'A');
  CHECK(blank.restore(blank.state()).ok);
}

void test_invalid_word_and_bingo() {
  Game game(small_dictionary());
  State fixture = game.state();
  rack(fixture, 0, "CT");
  CHECK(game.restore(fixture).ok);
  CHECK(game.place({0, 7, 7, 'C'}).ok);
  CHECK(game.place({1, 7, 8, 'T'}).ok);
  CHECK(!game.preview().valid);
  rejected_unchanged(game, [&] { return game.play(); });
  CHECK(game.unplace(0).ok);
  rejected_unchanged(game, [&] { return game.unplace(0); });
  CHECK(game.recall().ok);

  Game bingo(small_dictionary());
  fixture = bingo.state();
  rack(fixture, 0, "BANISTER");
  CHECK(bingo.restore(fixture).ok);
  std::vector<Placement> placements;
  for (int offset = 0; offset < 8; ++offset) placements.push_back({offset, 7, 4 + offset, "BANISTER"[offset]});
  const auto evaluation = bingo.evaluate(placements);
  CHECK(evaluation.valid && evaluation.score == 112);
  jot::Move move;
  move.placements = placements;
  CHECK(bingo.stage(move).ok && bingo.play().ok);
  CHECK(bingo.state().players[0].rack.empty());
  CHECK(!bingo.state().over);
  CHECK(bingo.state().scores[0] == 112);
  CHECK(bingo.restore(bingo.state()).ok);
}

void test_word_multipliers() {
  Game game(small_dictionary());
  State fixture = game.state();
  board_tile(fixture, 1, 7, 8, 'S');
  board_tile(fixture, 1, 7, 9, 'T');
  rack(fixture, 0, "BRAINORM");
  CHECK(game.restore(fixture).ok);
  std::vector<Placement> placements;
  int rack_index = 0;
  for (int offset = 0; offset < 10; ++offset) {
    if (offset == 5 || offset == 6) continue;
    placements.push_back({rack_index++, 7, 3 + offset, "BRAINSTORM"[offset]});
  }
  // Two new W2 cells multiply together; the existing S on L2 is not
  // multiplied again, while the newly placed N on L2 is multiplied.
  CHECK(game.evaluate(placements).valid);
  CHECK(game.evaluate(placements).score == 160);

  Game triple(small_dictionary());
  fixture = triple.state();
  for (int col = 0; col < 8; ++col) board_tile(fixture, 1, 7, col, "READINGS"[col]);
  rack(fixture, 0, "BANISTE");
  CHECK(triple.restore(fixture).ok);
  placements.clear();
  for (int row = 0; row < 7; ++row) placements.push_back({row, row, 0, "BANISTER"[row]});
  CHECK(triple.evaluate(placements).valid);
  CHECK(triple.evaluate(placements).score == 30);
}

void test_pass_and_endgame() {
  Game game(small_dictionary(), 1, -1);
  State fixture = game.state();
  fixture.scores = {400, 300};
  rack(fixture, 0, "AT");
  CHECK(game.restore(fixture).ok);
  const int leftovers0 = points(fixture.players[0]);
  const int leftovers1 = points(fixture.players[1]);
  CHECK(game.place({0, 7, 7, 'A'}).ok);
  CHECK(game.pass().ok);
  CHECK(game.state().current_player == 1);
  CHECK(game.state().pending.empty());
  CHECK(game.state().consecutive_passes == 1 && !game.state().over);
  CHECK(game.pass().ok);
  CHECK(game.state().over && game.state().consecutive_passes == 2);
  CHECK(game.state().scores[0] == std::max(0, 400 - leftovers0));
  CHECK(game.state().scores[1] == std::max(0, 300 - leftovers1));
  CHECK(game.restore(game.state()).ok);
  rejected_unchanged(game, [&] { return game.pass(); });
  rejected_unchanged(game, [&] { return game.draw(true); });

  // Construct an inventory-conserving near-end board. The synthetic dictionary
  // contains its words before and after the final move, isolating end scoring.
  Game seed(small_dictionary());
  fixture = seed.state();
  fixture.players[0].rack.push_back(take(fixture, 0, 'T'));
  std::vector<char> remaining = fixture.players[0].consonants;
  remaining.insert(remaining.end(), fixture.players[0].vowels.begin(), fixture.players[0].vowels.end());
  fixture.players[0].consonants.clear();
  fixture.players[0].vowels.clear();
  CHECK(remaining.size() == 63);
  for (int offset = 0; offset < 63; ++offset) {
    const char letter = remaining[offset];
    fixture.board[(6 + offset / 16) * 16 + offset % 16] = Tile{letter == '@' ? 'A' : letter, letter == '@', 0};
  }
  auto after = fixture;
  after.board[9 * 16 + 15] = Tile{'T', false, 0};
  std::string lines;
  for (const auto* position : {&fixture, &after}) {
    for (bool vertical : {false, true}) {
      for (int line = 0; line < 16; ++line) {
        std::string word;
        for (int offset = 0; offset <= 16; ++offset) {
          const int row = vertical ? offset : line;
          const int col = vertical ? line : offset;
          if (offset < 16 && position->board[row * 16 + col]) word += position->board[row * 16 + col]->letter;
          else { if (word.size() > 1) lines += word + '\n'; word.clear(); }
        }
      }
    }
  }
  Game finish(std::make_shared<const jot::Dictionary>(lines));
  fixture.scores = {500, 1000};
  CHECK(finish.restore(fixture).ok);
  CHECK(finish.state().players[0].rack.size() == 1 && finish.rack_ready());
  const int opponent_leftovers = points(fixture.players[1]);
  CHECK(finish.place({0, 9, 15, 'T'}).ok);
  const int last_score = finish.preview().score;
  CHECK(finish.preview().valid);
  CHECK(finish.play().ok);
  CHECK(finish.state().over);
  CHECK(finish.state().scores[0] == 500 + last_score + opponent_leftovers);
  CHECK(finish.state().scores[1] == 1000 - opponent_leftovers);
  CHECK(finish.state().players[0].rack.empty());
  CHECK(finish.restore(finish.state()).ok);
}

void test_restore_rejects_corruption() {
  Game game(small_dictionary());
  const State good = game.state();
  const auto rejected = [&](const std::function<void(State&)>& corrupt) {
    State invalid = good;
    corrupt(invalid);
    rejected_unchanged(game, [&] { return game.restore(invalid); });
  };
  rejected([](State& state) { state.current_player = 2; });
  rejected([](State& state) { state.robot_player = 2; });
  rejected([](State& state) { state.rng_state = 0; });
  rejected([](State& state) { state.scores[0] = -1; });
  rejected([](State& state) { state.scores[0] = 1000001; });
  rejected([](State& state) { state.consecutive_passes = 3; });
  rejected([](State& state) { state.consecutive_passes = 2; });
  rejected([](State& state) { state.over = true; });
  rejected([](State& state) { state.players[0].consonants.pop_back(); });
  rejected([](State& state) { state.players[0].vowels.push_back('B'); });
  rejected([](State& state) { rack(state, 0, "AT"); state.players[0].rack[0].owner = 1; });
  rejected([](State& state) { rack(state, 0, "@"); state.players[0].rack[0].letter = 'A'; });
  rejected([](State& state) { board_tile(state, 0, 0, 0, 'A'); board_tile(state, 0, 0, 1, 'T'); });
  rejected([](State& state) { board_tile(state, 0, 7, 7, 'A'); board_tile(state, 0, 8, 8, 'T'); });
  rejected([](State& state) { board_tile(state, 0, 7, 7, 'Q'); board_tile(state, 1, 7, 8, 'Q'); });
  rejected([](State& state) { rack(state, 0, "AT"); state.pending = {{0, 7, 7, 'A'}, {0, 7, 8, 'A'}}; });
  rejected([](State& state) { rack(state, 0, "AT"); state.pending = {{0, 7, 7, 'A'}, {1, 7, 7, 'T'}}; });
  State partial = good;
  rack(partial, 0, "AT");
  partial.pending = {{0, 1, 1, 'A'}};  // Incomplete staging is legal saved state.
  CHECK(game.restore(partial).ok);
  CHECK(!game.preview().valid);
  CHECK(game.restore(game.state()).ok);
}

void test_bonus_and_scores() {
  CHECK(jot::letter_score('C') == 4);
  CHECK(jot::letter_score('H') == 3);
  CHECK(jot::letter_score('K') == 6);
  CHECK(jot::letter_score('Z') == 9);
  CHECK(jot::letter_score('@') == 0);
  CHECK(jot::bonus_at(7, 7).letter_multiplier == 2);
  CHECK(jot::bonus_at(8, 8).label == "L2");
  CHECK(jot::bonus_at(4, 4).label == "L3");
  CHECK(jot::bonus_at(5, 5).label == "W2");
  CHECK(jot::bonus_at(0, 4).word_multiplier == 3);
  CHECK(jot::bonus_at(0, 4).label == "W3");
  CHECK(jot::bonus_at(-1, 0).label.empty());
  for (int row = 0; row < 16; ++row) {
    for (int col = 0; col < 16; ++col) {
      CHECK(jot::bonus_at(row, col).label == jot::bonus_at(15 - row, 15 - col).label);
    }
  }
}

std::string board_dictionary(const State& state) {
  std::string words;
  for (bool vertical : {false, true}) {
    for (int line = 0; line < jot::kBoardSize; ++line) {
      std::string word;
      for (int offset = 0; offset <= jot::kBoardSize; ++offset) {
        const int row = vertical ? offset : line;
        const int col = vertical ? line : offset;
        if (offset < jot::kBoardSize && state.board[row * jot::kBoardSize + col]) {
          word += state.board[row * jot::kBoardSize + col]->letter;
        } else {
          if (word.size() > 1) words += word + '\n';
          word.clear();
        }
      }
    }
  }
  return words;
}

void test_validation_boundaries_and_tile_relocation() {
  bool missing_dictionary = false;
  try { Game invalid(nullptr); }
  catch (const std::invalid_argument& error) {
    missing_dictionary = std::string(error.what()) == "A dictionary is required.";
  }
  CHECK(missing_dictionary);
  Game game(small_dictionary());
  auto fixture = game.state();
  rack(fixture, 0, "AT");
  CHECK(game.restore(fixture).ok);
  for (const auto& bad : std::vector<Placement>{{-1, 7, 7, 'A'}, {8, 7, 7, 'A'},
      {0, -1, 7, 'A'}, {0, 7, 16, 'A'}, {0, 7, 7, '@'}, {0, 7, 7, '['}, {0, 7, 7, 'Z'}}) {
    rejected_unchanged(game, [&] { return game.place(bad); });
    CHECK(!game.evaluate({bad}).valid);
  }
  CHECK(game.place({0, 7, 7, 'A'}).ok);
  const auto first_revision = game.state().revision;
  CHECK(game.place({0, 8, 7, 'A'}).ok);
  CHECK(game.state().pending.size() == 1);
  CHECK(game.state().pending[0].row == 8);
  CHECK(game.state().revision == first_revision + 1);
  rejected_unchanged(game, [&] { return game.fill_rack(); });
  CHECK(game.recall().ok);
  const auto revision = game.state().revision;
  CHECK(game.recall().ok && game.state().revision == revision);
  jot::Move invalid;
  invalid.placements = {{0, 0, 0, 'A'}, {1, 0, 1, 'T'}};
  rejected_unchanged(game, [&] { return game.stage(invalid); });
  Game occupied(small_dictionary());
  auto blocked = occupied.state();
  board_tile(blocked, 1, 7, 7, 'A');
  board_tile(blocked, 1, 7, 8, 'T');
  rack(blocked, 0, "A");
  CHECK(occupied.restore(blocked).ok);
  rejected_unchanged(occupied, [&] { return occupied.place({0, 7, 7, 'A'}); });
  Game no_tiles(small_dictionary());
  const auto empty = fingerprint(no_tiles.state());
  CHECK(no_tiles.shuffle().ok && fingerprint(no_tiles.state()) == empty);
  auto single = no_tiles.state();
  rack(single, 0, "A", false);
  CHECK(no_tiles.restore(single).ok);
  const auto one_tile = fingerprint(no_tiles.state());
  CHECK(no_tiles.shuffle().ok && fingerprint(no_tiles.state()) == one_tile);

  fixture.scores[0] = 1000000;
  CHECK(game.restore(fixture).ok);
  const auto exceeded = game.evaluate({{0, 7, 7, 'A'}, {1, 7, 8, 'T'}});
  CHECK(!exceeded.valid && exceeded.message == "The score exceeds the supported game limit.");
  CHECK(game.place({0, 7, 7, 'A'}).ok && game.place({1, 7, 8, 'T'}).ok);
  rejected_unchanged(game, [&] { return game.play(); });

  CHECK(game.pass().ok && game.pass().ok);
  for (const auto& action : std::vector<std::function<jot::ActionResult()>>{
      [&] { return game.fill_rack(); }, [&] { return game.place({0, 7, 7, 'A'}); },
      [&] { return game.unplace(0); }, [&] { return game.recall(); },
      [&] { return game.shuffle(); }, [&] { return game.play(); },
      [&] { return game.stage(invalid); }}) rejected_unchanged(game, action);
  CHECK(game.evaluate({{0, 7, 7, 'A'}}).message == "The game is over.");
}

void test_empty_bag_draw_and_fill_fallback() {
  for (bool empty_vowels : {false, true}) {
    Game seed(small_dictionary());
    auto fixture = seed.state();
    auto& bag = empty_vowels ? fixture.players[0].vowels : fixture.players[0].consonants;
    const std::vector<char> all_tiles = bag;
    bag.clear();
    for (std::size_t offset = 0; offset < all_tiles.size(); ++offset) {
      const char tile = all_tiles[offset];
      fixture.board[(7 + offset / 16) * 16 + offset % 16] =
          Tile{tile == '@' ? 'A' : tile, tile == '@', 0};
    }
    Game game(std::make_shared<const jot::Dictionary>(board_dictionary(fixture)));
    CHECK(game.restore(fixture).ok);
    CHECK(!game.rack_ready());
    rejected_unchanged(game, [&] { return game.draw(empty_vowels); });
    CHECK(game.fill_rack().ok);
    CHECK(game.state().players[0].rack.size() == 8);
    for (const auto& tile : game.state().players[0].rack) {
      const bool is_vowel = tile.blank || std::string("AEIOU").find(tile.letter) != std::string::npos;
      CHECK(is_vowel != empty_vowels);
    }
    // Using a whole rack while the other bag still has tiles is a normal turn,
    // even when the first bag is exhausted. It must not award end-game points.
    auto ready = game.state();
    auto after = ready;
    jot::Move move;
    for (int index = 0; index < jot::kRackSize; ++index) {
      const auto& tile = ready.players[0].rack[index];
      const char letter = tile.blank ? 'A' : tile.letter;
      move.placements.push_back({index, 6, index, letter});
      after.board[6 * 16 + index] = Tile{letter, tile.blank, 0};
    }
    Game playing(std::make_shared<const jot::Dictionary>(
        board_dictionary(ready) + board_dictionary(after)));
    CHECK(playing.restore(ready).ok);
    const auto evaluation = playing.evaluate(move.placements);
    CHECK(evaluation.valid);
    CHECK(playing.stage(move).ok && playing.play().ok);
    CHECK(playing.state().scores[0] == evaluation.score);
    CHECK(!playing.state().over && playing.state().current_player == 1);
  }
}

void test_restore_all_structural_rejections() {
  Game game(small_dictionary());
  const auto good = game.state();
  const auto reject = [&](const std::function<void(State&)>& corrupt) {
    auto candidate = good;
    corrupt(candidate);
    rejected_unchanged(game, [&] { return game.restore(candidate); });
  };
  reject([](State& state) { state.current_player = -1; });
  reject([](State& state) { state.robot_player = -2; });
  bool invalid_robot = false;
  try { Game invalid(small_dictionary(), 1, -2); }
  catch (const std::invalid_argument&) { invalid_robot = true; }
  CHECK(invalid_robot);
  reject([](State& state) { state.consecutive_passes = -1; });
  reject([](State& state) { state.pending.resize(9); });
  reject([](State& state) { state.over = true; state.pending.push_back({}); });
  reject([](State& state) { state.players[0].rack.resize(9); });
  reject([](State& state) { state.players[0].consonants.push_back('B'); });
  reject([](State& state) { state.players[0].vowels.push_back('A'); });
  reject([](State& state) { state.players[0].consonants[0] = 'A'; });
  reject([](State& state) { state.players[0].consonants[0] = '!'; });
  reject([](State& state) { state.players[0].vowels[0] = 'B'; });
  reject([](State& state) { state.players[1].consonants.pop_back(); });
  reject([](State& state) { rack(state, 0, "A"); state.players[0].rack[0].letter = '!'; });
  reject([](State& state) { board_tile(state, 0, 7, 7, 'A'); });
  reject([](State& state) { board_tile(state, 0, 7, 7, 'A'); state.board[7 * 16 + 7]->owner = -1; });
  reject([](State& state) { board_tile(state, 0, 7, 7, 'A'); state.board[7 * 16 + 7]->owner = 2; });
  reject([](State& state) { board_tile(state, 0, 7, 7, 'A'); state.board[7 * 16 + 7]->letter = '@'; });
}

void exhaust_inventory_onto_board(State& state, int owner, std::size_t first_cell) {
  auto& player = state.players[owner];
  std::vector<char> tiles = player.consonants;
  tiles.insert(tiles.end(), player.vowels.begin(), player.vowels.end());
  player.consonants.clear();
  player.vowels.clear();
  for (std::size_t offset = 0; offset < tiles.size(); ++offset) {
    const char tile = tiles[offset];
    state.board[first_cell + offset] = Tile{tile == '@' ? 'A' : tile, tile == '@', owner};
  }
}

void test_exhausted_game_restore_boundaries() {
  for (int owner : {0, 1}) {
    Game seed(small_dictionary());
    auto fixture = seed.state();
    exhaust_inventory_onto_board(fixture, owner, 7 * jot::kBoardSize);
    fixture.current_player = owner;
    fixture.over = true;
    Game game(std::make_shared<const jot::Dictionary>(board_dictionary(fixture)));
    CHECK(game.restore(fixture).ok);
    CHECK(game.state().over && game.state().current_player == owner);
    auto invalid = fixture;
    invalid.over = false;
    rejected_unchanged(game, [&] { return game.restore(invalid); });
    invalid = fixture;
    invalid.consecutive_passes = 1;
    rejected_unchanged(game, [&] { return game.restore(invalid); });
    invalid = fixture;
    invalid.current_player = owner ^ 1;
    rejected_unchanged(game, [&] { return game.restore(invalid); });
  }
  Game seed(small_dictionary());
  auto both_out = seed.state();
  exhaust_inventory_onto_board(both_out, 0, 4 * jot::kBoardSize);
  exhaust_inventory_onto_board(both_out, 1, 8 * jot::kBoardSize);
  both_out.over = true;
  Game game(std::make_shared<const jot::Dictionary>(board_dictionary(both_out)));
  rejected_unchanged(game, [&] { return game.restore(both_out); });
}

void test_single_move_score_bound() {
  // A word's bonus product cannot exceed 16 anywhere on the board. At most
  // eight placements can produce sixteen words; each word has <=16 letters,
  // each worth <=10 points with a <=3 letter multiplier. Even this deliberately
  // loose bound plus the 100-point rack bonus stays below the score ceiling.
  int maximum_letter = 0;
  for (char letter = 'A'; letter <= 'Z'; ++letter) {
    maximum_letter = std::max(maximum_letter, jot::letter_score(letter));
  }
  CHECK(maximum_letter == 10);
  for (bool vertical : {false, true}) {
    for (int line = 0; line < jot::kBoardSize; ++line) {
      int word_multiplier = 1;
      for (int offset = 0; offset < jot::kBoardSize; ++offset) {
        const auto bonus = jot::bonus_at(vertical ? offset : line, vertical ? line : offset);
        CHECK(bonus.letter_multiplier <= 3);
        word_multiplier *= bonus.word_multiplier;
      }
      CHECK(word_multiplier <= 16);
    }
  }
  CHECK(2 * jot::kRackSize * jot::kBoardSize * maximum_letter * 3 * 16 + 100 < 1000000);
}

void test_private_abi_exception_boundary() {
  struct Probe {
    int calls = 0;
    int failures = 0;
    std::string response;
  } probe;
  const auto failure = +[](void* context, const char* message) noexcept -> const char* {
    auto& state = *static_cast<Probe*>(context);
    ++state.failures;
    state.response = message;
    return state.response.c_str();
  };
  const auto success = +[](void* context, const char* command) -> const char* {
    auto& state = *static_cast<Probe*>(context);
    ++state.calls;
    state.response = command;
    return state.response.c_str();
  };
  const auto standard_exception = +[](void* context, const char*) -> const char* {
    ++static_cast<Probe*>(context)->calls;
    throw std::runtime_error("Callback rejected the command.");
  };
  const auto nonstandard_exception = +[](void* context, const char*) -> const char* {
    ++static_cast<Probe*>(context)->calls;
    throw 7;
  };
  CHECK(std::string(jot::invoke_command(&probe, "forwarded command", success, failure)) ==
        "forwarded command");
  CHECK(probe.calls == 1 && probe.failures == 0);
  const char* standard = jot::invoke_command(&probe, nullptr, standard_exception, failure);
  CHECK(std::string(standard) == "Callback rejected the command.");
  CHECK(standard == probe.response.c_str());
  CHECK(probe.calls == 2 && probe.failures == 1);
  const char* unknown = jot::invoke_command(&probe, nullptr, nonstandard_exception, failure);
  CHECK(std::string(unknown) == "Unexpected native engine error.");
  CHECK(unknown == probe.response.c_str());
  CHECK(probe.calls == 3 && probe.failures == 2);
}

}  // namespace

int main() {
  try {
    test_bonus_and_scores();
    test_draw_and_seed();
    test_first_word_and_atomic_play();
    test_bridge_and_copy_isolation();
    test_crosswords_and_blanks();
    test_invalid_word_and_bingo();
    test_word_multipliers();
    test_pass_and_endgame();
    test_restore_rejects_corruption();
    test_validation_boundaries_and_tile_relocation();
    test_empty_bag_draw_and_fill_fallback();
    test_restore_all_structural_rejections();
    test_exhausted_game_restore_boundaries();
    test_single_move_score_bound();
    test_private_abi_exception_boundary();
    std::cout << "Jot core tests passed.\n";
    return 0;
  } catch (const std::exception& error) {
    std::cerr << error.what() << '\n';
    return 1;
  }
}
