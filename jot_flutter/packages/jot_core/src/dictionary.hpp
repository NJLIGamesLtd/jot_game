#pragma once
#include <array>
#include <cstdint>
#include <string>
#include <string_view>
#include <vector>

namespace jot {
struct TrieNode {
  std::uint32_t child_mask = 0;
  std::uint32_t first_child = 0;
  std::array<std::uint8_t, 3> byte_prefix{};
  bool terminal = false;
};
class Dictionary {
 public:
  explicit Dictionary(std::string_view word_lines);
  bool contains(std::string_view word) const;
  // Node and letter indices are internal validated trie-search indices. The
  // bitmap ranks A-Z edges into one packed immutable child array.
  int child(int node, int letter) const {
    const auto& entry = nodes_[node];
    const std::uint32_t bit = 1u << letter;
    if (!(entry.child_mask & bit)) return -1;
    const unsigned byte = static_cast<unsigned>(letter) / 8;
    const unsigned prefix = byte ? entry.byte_prefix[byte - 1] : 0;
    const unsigned lower = (entry.child_mask & (bit - 1)) >> (byte * 8);
    return children_[entry.first_child + prefix + byte_counts_[lower]];
  }
  const std::vector<TrieNode>& nodes() const { return nodes_; }
  const std::vector<std::string>& words() const { return words_; }
  std::size_t trie_storage_bytes() const {
    return nodes_.capacity() * sizeof(TrieNode) + children_.capacity() * sizeof(int);
  }
 private:
  // Three prefix counts occupy the node's former padding. One byte lookup then
  // ranks an edge without optional native CPU instructions or a full popcount.
  inline static constexpr std::array<std::uint8_t, 256> byte_counts_ = {
      0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4,
      1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
      1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
      2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
      1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
      2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
      2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
      3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
      1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
      2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
      2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
      3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
      2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
      3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
      3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
      4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7, 7, 8
  };
  // Portable C++17 population count. Optimizing compilers recognize this idiom;
  // it also works on native targets without an optional CPU popcount instruction.
  static unsigned child_count(std::uint32_t mask) {
    mask -= (mask >> 1) & 0x55555555u;
    mask = (mask & 0x33333333u) + ((mask >> 2) & 0x33333333u);
    mask = (mask + (mask >> 4)) & 0x0f0f0f0fu;
    return (mask * 0x01010101u) >> 24;
  }
  std::vector<TrieNode> nodes_;
  std::vector<int> children_;
  std::vector<std::string> words_;
};
}
