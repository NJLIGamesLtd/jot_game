#include "dictionary.hpp"

#include <algorithm>
#include <stdexcept>

namespace jot {

Dictionary::Dictionary(std::string_view word_lines) {
  // Keep input validation separate from trie construction so malformed assets
  // never produce a partially usable dictionary.
  words_.reserve(static_cast<std::size_t>(
      std::count(word_lines.begin(), word_lines.end(), '\n')) + 1);
  std::size_t cursor = 0;
  while (cursor < word_lines.size()) {
    auto end = word_lines.find('\n', cursor);
    if (end == std::string_view::npos) end = word_lines.size();
    auto line = word_lines.substr(cursor, end - cursor);
    if (!line.empty() && line.back() == '\r') line.remove_suffix(1);
    if (!line.empty()) {
      for (const char letter : line) {
        if (letter < 'A' || letter > 'Z') {
          throw std::invalid_argument("Dictionary words must contain uppercase A-Z only.");
        }
      }
      words_.emplace_back(line);
    }
    cursor = end + 1;
  }
  std::sort(words_.begin(), words_.end());
  words_.erase(std::unique(words_.begin(), words_.end()), words_.end());
  if (words_.empty()) throw std::invalid_argument("Dictionary contains no words.");

  // Sorted adjacent words share every already-created prefix. Count exactly
  // how many nodes will be needed before allocating the trie. This avoids a
  // large transient vector copy and unused capacity in constrained runtimes.
  std::size_t node_count = 1;
  std::string_view previous;
  for (const auto& word : words_) {
    std::size_t common = 0;
    while (common < previous.size() && common < word.size() &&
           previous[common] == word[common]) {
      ++common;
    }
    node_count += word.size() - common;
    previous = word;
  }
  nodes_.reserve(node_count);
  nodes_.emplace_back();
  for (const auto& word : words_) {
    int node = 0;
    for (const char letter : word) {
      const auto index = static_cast<std::size_t>(letter - 'A');
      int next = nodes_[node].children[index];
      if (next == -1) {
        next = static_cast<int>(nodes_.size());
        // Growing the vector invalidates references, so assign the child by
        // index before allocating the new node.
        nodes_[node].children[index] = next;
        nodes_.emplace_back();
      }
      node = next;
    }
    nodes_[node].terminal = true;
  }
}

bool Dictionary::contains(std::string_view word) const {
  if (word.empty()) return false;
  int node = 0;
  for (const char letter : word) {
    if (letter < 'A' || letter > 'Z') return false;
    node = nodes_[node].children[static_cast<std::size_t>(letter - 'A')];
    if (node == -1) return false;
  }
  return nodes_[node].terminal;
}

}  // namespace jot
