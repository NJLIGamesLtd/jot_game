// Reproducible engine benchmark, deliberately separate from timing-sensitive
// unit tests. From jot_flutter, compile and run with the same release compiler:
// clang++ -std=c++17 -O3 -DNDEBUG -Ipackages/jot_core/src \
//   packages/jot_core/tests/performance_benchmark.cpp \
//   packages/jot_core/src/{dictionary,engine,ai}.cpp -o /tmp/jot-core-benchmark
// /tmp/jot-core-benchmark assets/dictionary/words.txt > /tmp/jot-core-result.json
#include "ai.hpp"
#include "third_party/json.hpp"

#include <algorithm>
#include <chrono>
#include <fstream>
#include <iostream>
#include <iterator>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>
#if defined(__APPLE__) || defined(__linux__)
#include <sys/resource.h>
#endif

namespace {
using Json = nlohmann::json;
using Clock = std::chrono::steady_clock;

void require(bool condition, const char* message) {
  if (!condition) throw std::runtime_error(message);
}

std::uint64_t peak_rss_bytes() {
#if defined(__APPLE__) || defined(__linux__)
  struct rusage usage {};
  require(getrusage(RUSAGE_SELF, &usage) == 0, "Cannot read process peak RSS");
#if defined(__APPLE__)
  return usage.ru_maxrss;
#else
  return static_cast<std::uint64_t>(usage.ru_maxrss) * 1024;
#endif
#else
  return 0;  // RSS is supplementary; portable timings and byte counts remain.
#endif
}

double milliseconds(Clock::time_point start) {
  return std::chrono::duration<double, std::milli>(Clock::now() - start).count();
}

double median(std::vector<double> samples) {
  std::sort(samples.begin(), samples.end());
  return samples[samples.size() / 2];
}

// Fingerprint every transition, including missing edges and terminal markers.
// This detects changes to node numbering or traversal order, not just membership.
std::uint64_t graph_fingerprint(const jot::Dictionary& dictionary) {
  std::uint64_t hash = 14695981039346656037ull;
  const auto add = [&](std::uint32_t value) {
    for (int byte = 0; byte < 4; ++byte) {
      hash ^= (value >> (8 * byte)) & 255u;
      hash *= 1099511628211ull;
    }
  };
  for (std::size_t node = 0; node < dictionary.nodes().size(); ++node) {
    add(dictionary.nodes()[node].terminal);
    for (int letter = 0; letter < 26; ++letter) {
#ifdef JOT_BENCHMARK_DENSE_TRIE
      add(static_cast<std::uint32_t>(dictionary.nodes()[node].children[letter]));
#else
      add(static_cast<std::uint32_t>(dictionary.child(static_cast<int>(node), letter)));
#endif
    }
  }
  return hash;
}

void take(jot::State& state, char letter, int owner) {
  auto& player = state.players[owner];
  auto& bag = std::string("AEIOU@").find(letter) == std::string::npos
                  ? player.consonants : player.vowels;
  const auto found = std::find(bag.begin(), bag.end(), letter);
  require(found != bag.end(), "Fixture exceeds tile inventory");
  bag.erase(found);
}

jot::State fixture(const jot::Game& initial, const std::string& rack, int direction) {
  auto state = initial.state();
  state.current_player = direction < 0 ? 0 : 1;
  for (char letter : rack) {
    take(state, letter, state.current_player);
    state.players[state.current_player].rack.push_back(
        {letter, letter == '@', state.current_player});
  }
  if (direction >= 0) {
    int row = 7, col = 7;
    for (char letter : std::string("AT")) {
      take(state, letter, 0);
      state.board[row * jot::kBoardSize + col] = jot::Tile{letter, false, 0};
      row += direction;
      col += 1 - direction;
    }
  }
  return state;
}

Json tile_json(const jot::Tile& tile) {
  return {{"letter", std::string(1, tile.letter)},
          {"blank", tile.blank}, {"owner", tile.owner}};
}

// Persist the fixed states in the public save schema so other runtimes can run
// these exact fixtures rather than approximate a rack or reconstructed board.
Json save_json(const jot::State& state) {
  Json board = Json::array(), players = Json::array();
  for (const auto& tile : state.board) {
    board.push_back(tile ? tile_json(*tile) : Json(nullptr));
  }
  for (const auto& player : state.players) {
    Json rack = Json::array();
    for (const auto& tile : player.rack) rack.push_back(tile_json(tile));
    players.push_back({{"rack", rack},
                      {"consonants", std::string(player.consonants.begin(), player.consonants.end())},
                      {"vowels", std::string(player.vowels.begin(), player.vowels.end())}});
  }
  return {{"version", 1}, {"fullState", {
      {"board", board}, {"players", players}, {"scores", state.scores},
      {"pending", Json::array()}, {"currentPlayer", state.current_player},
      {"robotPlayer", state.robot_player}, {"consecutivePasses", state.consecutive_passes},
      {"over", state.over}, {"rngState", state.rng_state}, {"revision", state.revision}}}};
}

Json move_json(const std::optional<jot::Move>& move) {
  if (!move) return nullptr;
  Json placements = Json::array(), words = Json::array();
  for (const auto& placement : move->placements) {
    placements.push_back({{"rackIndex", placement.rack_index}, {"row", placement.row},
                          {"col", placement.col}, {"letter", std::string(1, placement.letter)}});
  }
  for (const auto& word : move->evaluation.words) {
    words.push_back({{"word", word.word}, {"score", word.score}, {"row", word.row},
                     {"col", word.col}, {"vertical", word.vertical}});
  }
  return {{"score", move->evaluation.score}, {"valid", move->evaluation.valid},
          {"searchExhausted", move->search_exhausted},
          {"placements", placements}, {"words", words}};
}
}  // namespace

int main(int argc, char** argv) {
  try {
    require(argc == 2, "Usage: jot-core-benchmark path/to/words.txt");
    std::ifstream input(argv[1], std::ios::binary);
    require(input.good(), "Cannot read dictionary asset");
    const std::string text((std::istreambuf_iterator<char>(input)),
                           std::istreambuf_iterator<char>());
    const auto rss_before = peak_rss_bytes();
    std::vector<double> cold_samples;
    std::shared_ptr<const jot::Dictionary> dictionary;
    std::uint64_t rss_first_construction = 0;
    for (int repeat = 0; repeat < 5; ++repeat) {
      dictionary.reset();
      const auto start = Clock::now();
      dictionary = std::make_shared<const jot::Dictionary>(text);
      cold_samples.push_back(milliseconds(start));
      if (repeat == 0) rss_first_construction = peak_rss_bytes();
    }
    const auto rss_after_construction = peak_rss_bytes();
    const auto graph_hash = graph_fingerprint(*dictionary);

    constexpr int lookup_repeats = 7;
    std::vector<double> lookup_samples;
    std::uint64_t successful_lookups = 0;
    for (int repeat = 0; repeat < lookup_repeats; ++repeat) {
      const auto start = Clock::now();
      for (const auto& word : dictionary->words()) successful_lookups += dictionary->contains(word);
      lookup_samples.push_back(milliseconds(start));
    }
    require(successful_lookups == dictionary->words().size() * lookup_repeats,
            "Dictionary membership changed");
    // Sorted words favor contiguous dense-trie cache lines. Also measure a
    // deterministic shuffled workload, closer to unrelated game validations.
    std::vector<std::size_t> order(dictionary->words().size());
    for (std::size_t index = 0; index < order.size(); ++index) order[index] = index;
    std::uint32_t shuffle_seed = 123;
    for (std::size_t remaining = order.size(); remaining > 1; --remaining) {
      shuffle_seed = shuffle_seed * 1664525u + 1013904223u;
      std::swap(order[remaining - 1], order[shuffle_seed % remaining]);
    }
    std::vector<double> shuffled_lookup_samples;
    successful_lookups = 0;
    for (int repeat = 0; repeat < lookup_repeats; ++repeat) {
      const auto start = Clock::now();
      for (auto index : order) successful_lookups += dictionary->contains(dictionary->words()[index]);
      shuffled_lookup_samples.push_back(milliseconds(start));
    }
    require(successful_lookups == dictionary->words().size() * lookup_repeats,
            "Shuffled dictionary membership changed");

    Json fixtures = Json::array();
    for (const auto& item : std::vector<std::pair<std::string, int>>{
             {"CATSLINE", -1}, {"C@SEINRT", -1}, {"CATSLINE", 0}, {"C@SEINRT", 1}}) {
      jot::Game game(dictionary, 123, -1);
      const auto saved = fixture(game, item.first, item.second);
      require(game.restore(saved).ok, "Cannot restore benchmark fixture");
      std::vector<double> samples;
      Json expected;
      for (int repeat = 0; repeat < 5; ++repeat) {
        const auto start = Clock::now();
        const auto move = jot::find_best_move(game);
        samples.push_back(milliseconds(start));
        const auto actual = move_json(move);
        if (repeat == 0) expected = actual;
        require(actual == expected, "Hint is not deterministic across repetitions");
        require(move && move->evaluation.valid, "Benchmark fixture has no legal hint");
      }
      fixtures.push_back({{"rack", item.first}, {"direction", item.second},
                          {"save", save_json(saved)}, {"move", expected},
                          {"samplesMs", samples}, {"medianMs", median(samples)}});
    }
    const Json result = {
        {"dictionaryBytes", text.size()}, {"words", dictionary->words().size()},
        {"nodes", dictionary->nodes().size()}, {"nodeBytes", sizeof(jot::TrieNode)},
#ifdef JOT_BENCHMARK_DENSE_TRIE
        {"trieBytes", dictionary->nodes().capacity() * sizeof(jot::TrieNode)},
#else
        {"trieBytes", dictionary->trie_storage_bytes()},
#endif
        {"graphFingerprint", std::to_string(graph_hash)},
        {"coldConstructionSamplesMs", cold_samples}, {"coldConstructionMedianMs", median(cold_samples)},
        {"containsSamplesMs", lookup_samples},
        {"containsNsPerWord", median(lookup_samples) * 1000000 / dictionary->words().size()},
        {"shuffledContainsSamplesMs", shuffled_lookup_samples},
        {"shuffledContainsNsPerWord", median(shuffled_lookup_samples) * 1000000 / dictionary->words().size()},
        {"peakRssBeforeBytes", rss_before}, {"peakRssFirstConstructionBytes", rss_first_construction},
        {"peakRssAfterConstructionBytes", rss_after_construction},
        {"peakRssAfterAiBytes", peak_rss_bytes()}, {"fixtures", fixtures}};
    std::cout << result.dump(2) << '\n';
    return 0;
  } catch (const std::exception& error) {
    std::cerr << error.what() << '\n';
    return 1;
  }
}
