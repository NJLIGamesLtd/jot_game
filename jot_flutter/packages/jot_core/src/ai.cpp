#include "ai.hpp"

#include <algorithm>
#include <array>
#include <cstdint>
#include <limits>
#include <utility>

namespace jot {
namespace {

constexpr std::uint32_t kAllLetters = (1u << 26) - 1u;
constexpr int kCells = kBoardSize * kBoardSize;

bool inside(int row, int col) {
  return row >= 0 && row < kBoardSize && col >= 0 && col < kBoardSize;
}

struct Start {
  int row;
  int col;
  int empties_to_anchor;
};

class Search {
 public:
  Search(const Game& game, std::size_t budget)
      : game_(game), state_(game.state()), trie_(game.dictionary().nodes()),
        rack_(state_.players[state_.current_player].rack), budget_(budget) {
    board_empty_ = std::none_of(state_.board.begin(), state_.board.end(),
                               [](const auto& tile) { return tile.has_value(); });
    for (int row = 0; row < kBoardSize; ++row) {
      for (int col = 0; col < kBoardSize; ++col) {
        if (tile_at(row, col)) continue;
        if (board_empty_) {
          // Jot has an even-sized board and four opening centre cells.
          anchors_[row * kBoardSize + col] =
              (row == 7 || row == 8) && (col == 7 || col == 8);
        } else {
          anchors_[row * kBoardSize + col] =
              tile_at(row - 1, col) || tile_at(row + 1, col) ||
              tile_at(row, col - 1) || tile_at(row, col + 1);
        }
      }
    }
  }

  std::optional<Move> run() {
    if (state_.over || rack_.empty()) return std::nullopt;
    for (int direction = 0; direction < 2 && !exhausted_; ++direction) {
      vertical_ = direction == 1;
      prepare_cross_checks();
      auto starts = legal_starts();
      // Search near connections first. A small budget is then more likely to
      // find a useful legal candidate before exploring long rack permutations.
      std::stable_sort(starts.begin(), starts.end(), [](const Start& a, const Start& b) {
        return a.empties_to_anchor < b.empties_to_anchor;
      });
      for (const auto& start : starts) {
        visit(start.row, start.col, 0, 0, false);
        if (exhausted_) break;
      }
    }
    if (best_) {
      best_->search_exhausted = exhausted_;
      return best_;
    }
    if (exhausted_) {
      // Distinguish "not found within the budget" from "no legal move". The
      // robot must retry with a larger/unlimited budget before passing.
      Move incomplete;
      incomplete.search_exhausted = true;
      incomplete.evaluation.message = "Search budget reached before a legal move was found.";
      return incomplete;
    }
    return std::nullopt;
  }

 private:
  const std::optional<Tile>& tile_at(int row, int col) const {
    static const std::optional<Tile> empty;
    if (!inside(row, col)) return empty;
    return state_.board[row * kBoardSize + col];
  }

  void prepare_cross_checks() {
    const int cross_row = vertical_ ? 0 : 1;
    const int cross_col = vertical_ ? 1 : 0;
    for (int row = 0; row < kBoardSize; ++row) {
      for (int col = 0; col < kBoardSize; ++col) {
        const int cell = row * kBoardSize + col;
        cross_checks_[cell] = kAllLetters;
        if (tile_at(row, col)) continue;
        std::string prefix;
        int r = row - cross_row;
        int c = col - cross_col;
        while (tile_at(r, c)) {
          prefix.push_back(tile_at(r, c)->letter);
          r -= cross_row;
          c -= cross_col;
        }
        std::reverse(prefix.begin(), prefix.end());
        std::string suffix;
        r = row + cross_row;
        c = col + cross_col;
        while (tile_at(r, c)) {
          suffix.push_back(tile_at(r, c)->letter);
          r += cross_row;
          c += cross_col;
        }
        if (prefix.empty() && suffix.empty()) continue;
        cross_checks_[cell] = 0;
        std::string word = prefix + 'A' + suffix;
        for (int letter = 0; letter < 26; ++letter) {
          word[prefix.size()] = static_cast<char>('A' + letter);
          if (game_.dictionary().contains(word)) cross_checks_[cell] |= 1u << letter;
        }
      }
    }
  }

  std::vector<Start> legal_starts() const {
    std::vector<Start> starts;
    const int dr = vertical_ ? 1 : 0;
    const int dc = vertical_ ? 0 : 1;
    for (int row = 0; row < kBoardSize; ++row) {
      for (int col = 0; col < kBoardSize; ++col) {
        // A dictionary word must include the complete existing word prefix.
        if (tile_at(row - dr, col - dc)) continue;
        int empty_count = 0;
        for (int r = row, c = col; inside(r, c); r += dr, c += dc) {
          if (!tile_at(r, c)) {
            if (++empty_count > static_cast<int>(rack_.size())) break;
            if (cross_checks_[r * kBoardSize + c] == 0) break;
          }
          if (anchors_[r * kBoardSize + c]) {
            starts.push_back({row, col, empty_count});
            break;
          }
        }
      }
    }
    return starts;
  }

  void consider() {
    const auto evaluation = game_.evaluate(placements_);
    if (!evaluation.valid) return;
    if (!best_ || evaluation.score > best_->evaluation.score ||
        (evaluation.score == best_->evaluation.score &&
         placements_.size() > best_->placements.size())) {
      best_ = Move{placements_, evaluation, false};
    }
  }

  void visit(int row, int col, int node, std::uint32_t used, bool connected) {
    if (exhausted_) return;
    if (visited_ >= budget_) {
      exhausted_ = true;
      return;
    }
    ++visited_;
    const auto& square = tile_at(row, col);
    if (trie_[node].terminal && !square && connected && !placements_.empty()) consider();
    if (!inside(row, col)) return;

    const int dr = vertical_ ? 1 : 0;
    const int dc = vertical_ ? 0 : 1;
    if (square) {
      const int letter = square->letter - 'A';
      if (letter < 0 || letter >= 26) return;
      const int next = game_.dictionary().child(node, letter);
      if (next != -1) visit(row + dr, col + dc, next, used, connected);
      return;
    }
    if (placements_.size() >= rack_.size()) return;
    const auto allowed = cross_checks_[row * kBoardSize + col];
    const bool new_connection = connected || anchors_[row * kBoardSize + col];
    std::uint32_t seen_letters = 0;
    bool seen_blank = false;
    for (std::size_t index = 0; index < rack_.size() && !exhausted_; ++index) {
      if (used & (1u << index)) continue;
      const auto& tile = rack_[index];
      if (tile.blank) {
        if (seen_blank) continue;
        seen_blank = true;
        for (int letter = 0; letter < 26 && !exhausted_; ++letter) {
          const int next = game_.dictionary().child(node, letter);
          if (next == -1 || !(allowed & (1u << letter))) continue;
          placements_.push_back({static_cast<int>(index), row, col,
                                 static_cast<char>('A' + letter)});
          visit(row + dr, col + dc, next, used | (1u << index), new_connection);
          placements_.pop_back();
        }
      } else {
        const int letter = tile.letter - 'A';
        if (letter < 0 || letter >= 26 || (seen_letters & (1u << letter))) continue;
        seen_letters |= 1u << letter;
        const int next = game_.dictionary().child(node, letter);
        if (next == -1 || !(allowed & (1u << letter))) continue;
        placements_.push_back({static_cast<int>(index), row, col, tile.letter});
        visit(row + dr, col + dc, next, used | (1u << index), new_connection);
        placements_.pop_back();
      }
    }
  }

  const Game& game_;
  const State& state_;
  const std::vector<TrieNode>& trie_;
  const std::vector<Tile>& rack_;
  std::size_t budget_;
  std::size_t visited_ = 0;
  bool board_empty_ = false;
  bool vertical_ = false;
  bool exhausted_ = false;
  std::array<bool, kCells> anchors_{};
  std::array<std::uint32_t, kCells> cross_checks_{};
  std::vector<Placement> placements_;
  std::optional<Move> best_;
};

}  // namespace

std::optional<Move> find_best_move(const Game& game, std::size_t node_budget) {
  return Search(game, node_budget).run();
}

}  // namespace jot
