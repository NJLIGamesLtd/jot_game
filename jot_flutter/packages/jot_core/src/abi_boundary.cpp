#include "abi_boundary.hpp"

#include <exception>

namespace jot {
const char* invoke_command(void* context, const char* command,
                          CommandCallback execute, ErrorCallback failure) noexcept {
  try {
    return execute(context, command);
  } catch (const std::exception& error) {
    return failure(context, error.what());
  } catch (...) {
    return failure(context, "Unexpected native engine error.");
  }
}
}
