#pragma once
#include "engine.hpp"
#include <cstddef>

namespace jot {
// Search uses only const snapshots; reaching the budget still returns the best
// legal candidate found. UI code must not infer that no legal move exists.
std::optional<Move> find_best_move(const Game& game,
                                 std::size_t node_budget = 1500000);
}
