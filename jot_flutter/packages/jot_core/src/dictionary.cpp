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
  std::size_t longest_word = 0;
  std::string_view previous;
  for (const auto& word : words_) {
    std::size_t common = 0;
    while (common < previous.size() && common < word.size() &&
           previous[common] == word[common]) {
      ++common;
    }
    node_count += word.size() - common;
    longest_word = std::max(longest_word, word.size());
    previous = word;
  }
  nodes_.reserve(node_count);
  nodes_.emplace_back();
  // Sorted words create each parent's children in alphabetical order. Retain
  // just their parent indices while constructing, rather than a dense scratch
  // trie. Node IDs match the original insertion traversal exactly.
  std::vector<int> parents;
  parents.reserve(node_count - 1);
  std::vector<int> path;
  path.reserve(longest_word + 1);
  path.push_back(0);
  previous = {};
  for (const auto& word : words_) {
    std::size_t common = 0;
    while (common < previous.size() && common < word.size() &&
           previous[common] == word[common]) {
      ++common;
    }
    path.resize(common + 1);
    for (std::size_t offset = common; offset < word.size(); ++offset) {
      const int parent = path.back();
      nodes_[parent].child_mask |= 1u << (word[offset] - 'A');
      parents.push_back(parent);
      path.push_back(static_cast<int>(nodes_.size()));
      nodes_.emplace_back();
    }
    nodes_[path.back()].terminal = true;
    previous = word;
  }
  std::uint32_t first_child = 0;
  for (auto& node : nodes_) {
    node.first_child = first_child;
    node.byte_prefix[0] = child_count(node.child_mask & 0xffu);
    node.byte_prefix[1] = child_count(node.child_mask & 0xffffu);
    node.byte_prefix[2] = child_count(node.child_mask & 0xffffffu);
    first_child += child_count(node.child_mask);
  }
  children_.resize(first_child);
  for (std::size_t index = 0; index < parents.size(); ++index) {
    children_[nodes_[parents[index]].first_child++] = static_cast<int>(index + 1);
  }
  // first_child acted as a write cursor above; restore each immutable offset.
  for (auto& node : nodes_) {
    node.first_child -= child_count(node.child_mask);
  }
}

bool Dictionary::contains(std::string_view word) const {
  if (word.empty()) return false;
  int node = 0;
  for (const char letter : word) {
    if (letter < 'A' || letter > 'Z') return false;
    node = child(node, letter - 'A');
    if (node == -1) return false;
  }
  return nodes_[node].terminal;
}

}  // namespace jot
