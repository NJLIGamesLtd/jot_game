export 'src/transport.dart';
export 'src/bridge_stub.dart'
    if (dart.library.io) 'src/bridge_native.dart'
    if (dart.library.js_interop) 'src/bridge_web.dart';
