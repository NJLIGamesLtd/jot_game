#pragma once

namespace jot {
// A private, testable exception boundary used by the public C ABI. The error
// callback must always return a borrowed response and must never throw.
using CommandCallback = const char* (*)(void* context, const char* command);
using ErrorCallback = const char* (*)(void* context, const char* message) noexcept;

const char* invoke_command(void* context, const char* command,
                          CommandCallback execute, ErrorCallback failure) noexcept;
}
