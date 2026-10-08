#ifndef JOT_API_H
#define JOT_API_H
#include <stdint.h>
#if defined(_WIN32)
#define JOT_EXPORT __declspec(dllexport)
#else
#define JOT_EXPORT __attribute__((visibility("default"))) __attribute__((used))
#endif
#ifdef __cplusplus
extern "C" {
#endif
// A handle has one owner. Returned UTF-8 strings are borrowed until its next
// execute call or destruction. Callers must copy them before proceeding.
JOT_EXPORT void* jot_create(const char* dictionary, uint32_t seed, int32_t robot_player);
// Online servers must authorize commands and request {"op":"view","player":N}
// using an authenticated player index. Only that successful view's state may
// be sent to clients: ordinary command/error state and saves contain private
// current-player tiles or full engine state and are intended for trusted hosts.
JOT_EXPORT const char* jot_execute(void* handle, const char* command_json);
JOT_EXPORT void jot_destroy(void* handle);
#ifdef __cplusplus
}
#endif
#endif
