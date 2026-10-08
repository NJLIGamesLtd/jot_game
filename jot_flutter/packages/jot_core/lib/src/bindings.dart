import 'dart:ffi';
import 'package:ffi/ffi.dart';

@Native<Pointer<Void> Function(Pointer<Utf8>, Uint32, Int32)>(
  symbol: 'jot_create',
  assetId: 'package:jot_core/jot_core.dart',
)
external Pointer<Void> jotCreate(Pointer<Utf8> dictionary, int seed, int robot);
@Native<Pointer<Utf8> Function(Pointer<Void>, Pointer<Utf8>)>(
  symbol: 'jot_execute',
  assetId: 'package:jot_core/jot_core.dart',
)
external Pointer<Utf8> jotExecute(Pointer<Void> handle, Pointer<Utf8> command);
@Native<Void Function(Pointer<Void>)>(
  symbol: 'jot_destroy',
  assetId: 'package:jot_core/jot_core.dart',
)
external void jotDestroy(Pointer<Void> handle);
