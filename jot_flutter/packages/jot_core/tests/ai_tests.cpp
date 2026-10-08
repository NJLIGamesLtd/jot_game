#include "ai.hpp"
#include "dictionary.hpp"

#include <algorithm>
#include <array>
#include <chrono>
#include <fstream>
#include <functional>
#include <iostream>
#include <iterator>
#include <limits>
#include <sstream>
#include <stdexcept>

namespace {

void require(bool condition, const std::string& message) {
  if (!condition) throw std::runtime_error(message);
}

bool vowel(char letter) {
  return std::string("AEIOU@").find(letter) != std::string::npos;
}

void take(jot::State& state, char letter, int owner) {
  auto& bag = vowel(letter) ? state.players[owner].vowels : state.players[owner].consonants;
  const auto found = std::find(bag.begin(), bag.end(), letter);
  require(found != bag.end(), "Fixture exceeds the original tile inventory");
  bag.erase(found);
}

void rack(jot::State& state, const std::string& letters) {
  const int owner = state.current_player;
  for (char letter : letters) {
    take(state, letter, owner);
    state.players[owner].rack.push_back({letter, letter == '@', owner});
  }
}

void board_word(jot::State& state, const std::string& letters,
                int row, int col, bool vertical, int owner) {
  for (char letter : letters) {
    take(state, letter, owner);
    state.board[row * jot::kBoardSize + col] = jot::Tile{letter, false, owner};
    row += vertical ? 1 : 0;
    col += vertical ? 0 : 1;
  }
}

std::string snapshot(const jot::State& state) {
  std::ostringstream out;
  out << state.current_player << state.robot_player << state.rng_state
      << state.revision << state.consecutive_passes << state.over;
  for (const auto& tile : state.board) {
    if (tile) out << tile->letter << tile->blank << tile->owner;
    else out << '.';
  }
  for (int player = 0; player < 2; ++player) {
    out << state.scores[player] << '|';
    for (const auto& tile : state.players[player].rack) out << tile.letter << tile.blank << tile.owner;
    out << '|';
    for (char letter : state.players[player].vowels) out << letter;
    out << '|';
    for (char letter : state.players[player].consonants) out << letter;
  }
  for (const auto& placement : state.pending) {
    out << placement.rack_index << ':' << placement.row << ':' << placement.col << ':' << placement.letter;
  }
  return out.str();
}

// Independent, unpruned oracle: lay down each dictionary word at every board
// coordinate and try all matching rack assignments. This catches missing starts,
// crossing moves, repeated-letter choices and blank substitutions in the trie AI.
int brute_best_score(const jot::Game& game) {
  int best = -1;
  const auto& state = game.state();
  const auto& tiles = state.players[state.current_player].rack;
  for (const auto& word : game.dictionary().words()) {
    for (bool vertical : {false, true}) {
      for (int row = 0; row < jot::kBoardSize; ++row) {
        for (int col = 0; col < jot::kBoardSize; ++col) {
          const int end_row = row + (vertical ? static_cast<int>(word.size()) - 1 : 0);
          const int end_col = col + (vertical ? 0 : static_cast<int>(word.size()) - 1);
          if (end_row >= jot::kBoardSize || end_col >= jot::kBoardSize) continue;
          const int dr = vertical ? 1 : 0, dc = vertical ? 0 : 1;
          const auto occupied = [&](int r, int c) {
            return r >= 0 && r < jot::kBoardSize && c >= 0 && c < jot::kBoardSize &&
                   state.board[r * jot::kBoardSize + c].has_value();
          };
          if (occupied(row - dr, col - dc) || occupied(end_row + dr, end_col + dc)) continue;
          std::vector<jot::Placement> placements;
          std::function<void(std::size_t, unsigned)> assign = [&](std::size_t offset, unsigned used) {
            if (offset == word.size()) {
              const auto evaluation = game.evaluate(placements);
              if (evaluation.valid) best = std::max(best, evaluation.score);
              return;
            }
            const int r = row + dr * static_cast<int>(offset);
            const int c = col + dc * static_cast<int>(offset);
            const auto& existing = state.board[r * jot::kBoardSize + c];
            if (existing) {
              if (existing->letter == word[offset]) assign(offset + 1, used);
              return;
            }
            for (std::size_t index = 0; index < tiles.size(); ++index) {
              if ((used & (1u << index)) ||
                  (!tiles[index].blank && tiles[index].letter != word[offset])) continue;
              placements.push_back({static_cast<int>(index), r, c, word[offset]});
              assign(offset + 1, used | (1u << index));
              placements.pop_back();
            }
          };
          assign(0, 0);
        }
      }
    }
  }
  return best;
}

void verify_optimum(jot::Game& game) {
  const auto before = snapshot(game.state());
  const auto move = jot::find_best_move(game, std::numeric_limits<std::size_t>::max());
  const int expected = brute_best_score(game);
  require(snapshot(game.state()) == before, "AI search mutated the live game state");
  require(move.has_value() == (expected >= 0), "AI move availability differs from oracle");
  if (move) {
    require(!move->search_exhausted, "Unlimited small-dictionary search was truncated");
    require(move->evaluation.valid && game.evaluate(move->placements).valid,
            "AI returned an invalid move");
    require(move->evaluation.score == expected, "AI did not find the oracle's best score");
  }
}

void compact_dictionary_tests() {
  // A separately built dense reference verifies every transition and terminal
  // marker, so a packing/rank error cannot hide behind a few accepted words.
  struct DenseNode {
    std::array<int, 26> children;
    bool terminal = false;
    DenseNode() { children.fill(-1); }
  };
  std::vector<std::string> words = {"A", "AB", "ABC", "ABZ", "AZ", "B", "BA", "Z", "ZZ"};
  std::uint32_t random = 123;
  for (int sample = 0; sample < 1000; ++sample) {
    std::string word;
    for (int letter = 0; letter < 1 + sample % 12; ++letter) {
      random = random * 1664525u + 1013904223u;
      word += static_cast<char>('A' + random % 26);
    }
    words.push_back(word);
  }
  std::string lines;
  // Deliberately unsorted, duplicated and CRLF input exercises normalization.
  for (const auto& word : words) lines += word + "\r\n";
  lines += "AB\n\nABC";
  jot::Dictionary dictionary(lines);
  words.push_back("AB");
  words.push_back("ABC");
  std::sort(words.begin(), words.end());
  words.erase(std::unique(words.begin(), words.end()), words.end());
  std::vector<DenseNode> reference(1);
  for (const auto& word : words) {
    int node = 0;
    for (char letter : word) {
      int next = reference[node].children[letter - 'A'];
      if (next == -1) {
        next = static_cast<int>(reference.size());
        reference[node].children[letter - 'A'] = next;
        reference.emplace_back();
      }
      node = next;
    }
    reference[node].terminal = true;
  }
  require(dictionary.words() == words && dictionary.nodes().size() == reference.size(),
          "Compact trie changed normalized words or node numbering");
  for (std::size_t node = 0; node < reference.size(); ++node) {
    require(dictionary.nodes()[node].terminal == reference[node].terminal,
            "Compact trie changed a terminal marker");
    for (int letter = 0; letter < 26; ++letter) {
      require(dictionary.child(static_cast<int>(node), letter) == reference[node].children[letter],
              "Compact trie changed an A-Z transition");
    }
  }
  require(dictionary.trie_storage_bytes() <= reference.size() * 16,
          "Compact trie regained dense child storage");
  for (const auto& word : words) require(dictionary.contains(word), "Compact trie lost a generated word");
}

void small_dictionary_tests() {
  auto dictionary = std::make_shared<jot::Dictionary>(
      "AT\nAS\nCAT\nCATS\nSAT\nCAST\nSCAT\nACT\nACTS\n"
      "RAT\nRATS\nRAN\nRANT\nRANTS\nIT\nIS\nIN\nTIN\nTINS\n"
      "SIT\nSIN\nLINE\nLINES\nLIE\nLIES\nAN\nTA\nAA\n");
  for (bool vertical : {false, true}) {
    for (const auto& letters : {std::string("CATSLINE"), std::string("C@SEINRT"), std::string("AATTSSIN")}) {
      jot::Game game(dictionary, 123, -1);
      auto state = game.state();
      state.current_player = 1;
      rack(state, letters);
      board_word(state, "AT", 7, 7, vertical, 0);
      require(game.restore(state).ok, "Could not restore valid crossing fixture");
      verify_optimum(game);
    }
  }
  {
    jot::Game game(dictionary, 123, -1);
    auto state = game.state();
    rack(state, "C@SEINRT");
    require(game.restore(state).ok, "Could not restore opening fixture");
    verify_optimum(game);
    const auto incomplete = jot::find_best_move(game, 0);
    require(incomplete && incomplete->search_exhausted && incomplete->placements.empty(),
            "Exhausted search must be distinguishable from no legal move");
    const auto bounded = jot::find_best_move(game, 40);
    require(bounded && bounded->search_exhausted, "Low-budget search should report truncation");
    if (!bounded->placements.empty()) require(game.evaluate(bounded->placements).valid,
                                            "Truncated search returned an invalid move");
  }
  {
    jot::Game game(dictionary, 123, -1);
    auto state = game.state();
    rack(state, "QJXZ");
    require(game.restore(state).ok, "Could not restore no-move fixture");
    verify_optimum(game);
  }
  {
    jot::Dictionary deduplicated("CAT\r\nAT\nCAT\n");
    require(deduplicated.words().size() == 2 && deduplicated.contains("CAT") &&
            !deduplicated.contains("cat") && !deduplicated.contains("CA") &&
            !deduplicated.contains(""), "Dictionary strict lookup/dedup failed");
    for (const auto& bad : {"cat\n", "C AT\n", "CAT!\n", "\n"}) {
      bool rejected = false;
      try { jot::Dictionary invalid(bad); }
      catch (const std::invalid_argument&) { rejected = true; }
      require(rejected, "Dictionary accepted malformed input");
    }
    jot::Dictionary trailing("\nA\nAB\nABC");
    require(trailing.words().size() == 3 && trailing.nodes().size() == 4 &&
            trailing.contains("A") && trailing.contains("AB") && trailing.contains("ABC"),
            "Dictionary without final newline or with word prefixes failed");
    require(!trailing.contains("!") && !trailing.contains("["),
            "Dictionary accepted letters outside A-Z");
  }
  {
    jot::Game game(dictionary);
    require(!jot::find_best_move(game), "An empty rack must have no suggested move");
    require(game.fill_rack().ok && game.pass().ok && game.pass().ok, "Could not finish AI fixture");
    require(!jot::find_best_move(game), "A completed game must have no suggested move");
  }
  {
    jot::Game game(dictionary);
    auto state = game.state();
    rack(state, "AT");
    state.scores[0] = 1000000;
    require(game.restore(state).ok, "Could not restore the supported score boundary");
    verify_optimum(game);
    require(!jot::find_best_move(game), "AI must reject moves that exceed the game score limit");
  }
  {
    // Every prefix is possible with this rack, but every complete dictionary
    // word needs a ninth tile. The normal budget therefore expires before a
    // candidate exists, which must remain distinct from a complete no-move search.
    std::string letters = "ABCDEFGH";
    std::string words;
    do { words += letters + "Z\n"; }
    while (std::next_permutation(letters.begin(), letters.end()));
    jot::Game game(std::make_shared<const jot::Dictionary>(words));
    auto state = game.state();
    rack(state, "ABCDEFGH");
    require(game.restore(state).ok, "Could not restore the dense no-move rack");
    const auto before = snapshot(game.state());
    const auto incomplete = jot::find_best_move(game);
    require(incomplete && incomplete->search_exhausted && incomplete->placements.empty(),
            "Default-budget no-move search did not report incompleteness");
    require(snapshot(game.state()) == before, "An incomplete search mutated the game");
  }
}

void original_dictionary_tests(const std::string& path) {
  std::ifstream input(path);
  require(input.good(), "Could not open original dictionary asset");
  const std::string words((std::istreambuf_iterator<char>(input)), std::istreambuf_iterator<char>());
  const auto started = std::chrono::steady_clock::now();
  auto dictionary = std::make_shared<jot::Dictionary>(words);
  require(dictionary->words().size() == 143722, "Original dictionary count changed");
  require(dictionary->nodes().size() == 293227 &&
          dictionary->trie_storage_bytes() <= dictionary->nodes().size() * 16,
          "Original dictionary trie layout lost its compact storage");
  std::uint64_t graph_hash = 14695981039346656037ull;
  const auto hash_value = [&](std::uint32_t value) {
    for (int byte = 0; byte < 4; ++byte) {
      graph_hash ^= (value >> (8 * byte)) & 255u;
      graph_hash *= 1099511628211ull;
    }
  };
  for (std::size_t node = 0; node < dictionary->nodes().size(); ++node) {
    hash_value(dictionary->nodes()[node].terminal);
    for (int letter = 0; letter < 26; ++letter) {
      hash_value(static_cast<std::uint32_t>(dictionary->child(static_cast<int>(node), letter)));
    }
  }
  require(graph_hash == 669227770809285337ull,
          "Original dictionary node IDs, terminal markers or A-Z transitions changed");
  for (const auto& word : dictionary->words()) require(dictionary->contains(word), "Trie lookup failed");
  struct HintFixture {
    const char* rack;
    int board_direction;
    const char* word;
    int score;
    int row;
    int col;
    bool vertical;
    std::vector<int> rack_order;
  };
  // Captured from the dense-trie baseline: equal-scoring moves must retain the
  // exact word, position, blank substitution and rack-index selection.
  for (const auto& fixture : std::vector<HintFixture>{
           {"CATSLINE", -1, "CATLINS", 30, 7, 7, false, {0, 1, 2, 4, 5, 6, 3}},
           {"C@SEINRT", -1, "CENTRIST", 130, 7, 7, false, {0, 3, 5, 1, 6, 4, 2, 7}},
           {"CATSLINE", 0, "LAITANCES", 126, 6, 7, true, {4, 5, 2, 1, 6, 0, 7, 3}},
           {"C@SEINRT", 1, "BACTERINS", 130, 7, 6, false, {1, 0, 7, 3, 6, 4, 5, 2}}}) {
    jot::Game fixed(dictionary, 123, -1);
    auto state = fixed.state();
    state.current_player = fixture.board_direction < 0 ? 0 : 1;
    rack(state, fixture.rack);
    if (fixture.board_direction >= 0) board_word(state, "AT", 7, 7, fixture.board_direction == 1, 0);
    require(fixed.restore(state).ok, "Could not restore baseline hint fixture");
    const auto move = jot::find_best_move(fixed);
    require(move && !move->search_exhausted && move->evaluation.valid &&
            move->evaluation.score == fixture.score && move->evaluation.words.size() == 1 &&
            move->evaluation.words.front().word == fixture.word &&
            move->placements.size() == fixture.rack_order.size(),
            "Full-dictionary hint changed its baseline word or score");
    std::size_t placement_index = 0;
    for (std::size_t offset = 0; offset < std::string(fixture.word).size(); ++offset) {
      const int row = fixture.row + (fixture.vertical ? static_cast<int>(offset) : 0);
      const int col = fixture.col + (fixture.vertical ? 0 : static_cast<int>(offset));
      if (state.board[row * jot::kBoardSize + col]) continue;
      const auto& placed = move->placements[placement_index];
      require(placed.rack_index == fixture.rack_order[placement_index] && placed.row == row &&
              placed.col == col && placed.letter == fixture.word[offset],
              "Full-dictionary hint changed its baseline position or rack selection");
      ++placement_index;
    }
  }
  const auto loaded = std::chrono::steady_clock::now();
  jot::Game game(dictionary, 123, -1);
  int moves = 0;
  int passes = 0;
  int truncated = 0;
  int turns = 0;
  for (; turns < 160 && !game.state().over; ++turns) {
    require(game.fill_rack().ok, "AI could not fill the rack");
    const auto before = snapshot(game.state());
    auto move = jot::find_best_move(game);
    if (move && move->search_exhausted) ++truncated;
    if (move && move->placements.empty() && move->search_exhausted) {
      move = jot::find_best_move(game, std::numeric_limits<std::size_t>::max());
    }
    require(snapshot(game.state()) == before, "Full dictionary search mutated live game state");
    if (move) {
      require(move->evaluation.valid && game.evaluate(move->placements).valid,
              "Full dictionary AI returned an invalid move");
      require(game.stage(*move).ok && game.play().ok, "Full dictionary AI could not commit move");
      ++moves;
    } else {
      require(game.pass().ok, "AI could not pass");
      ++passes;
    }
  }
  require(moves >= 10, "Full dictionary self-play unexpectedly stalled");
  require(game.state().over, "Full dictionary self-play did not finish a complete game");
  const auto completed = std::chrono::steady_clock::now();
  std::cout << "Original dictionary: " << dictionary->words().size() << " words, "
            << dictionary->nodes().size() << " trie nodes; load "
            << std::chrono::duration_cast<std::chrono::milliseconds>(loaded - started).count()
            << " ms; " << turns << "-turn complete self-play "
            << std::chrono::duration_cast<std::chrono::milliseconds>(completed - loaded).count()
            << " ms, " << moves << " moves, " << passes << " passes, "
            << truncated << " budget-limited turns\n";
}

}  // namespace

int main(int argc, char** argv) {
  try {
    small_dictionary_tests();
    compact_dictionary_tests();
    if (argc > 1) original_dictionary_tests(argv[1]);
    std::cout << "Dictionary and AI tests passed.\n";
    return 0;
  } catch (const std::exception& error) {
    std::cerr << error.what() << '\n';
    return 1;
  }
}
