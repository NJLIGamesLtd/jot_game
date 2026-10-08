#pragma once
#include <array>
#include <string>
#include <string_view>
#include <vector>

namespace jot {
struct TrieNode {
  std::array<int, 26> children{};
  bool terminal = false;
  TrieNode() { children.fill(-1); }
};
class Dictionary {
 public:
  explicit Dictionary(std::string_view word_lines);
  bool contains(std::string_view word) const;
  const std::vector<TrieNode>& nodes() const { return nodes_; }
  const std::vector<std::string>& words() const { return words_; }
 private:
  std::vector<TrieNode> nodes_;
  std::vector<std::string> words_;
};
}
