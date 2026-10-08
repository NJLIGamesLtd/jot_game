/// Commands run on a worker; all rules and saved state belong to C++.
abstract interface class CoreTransport {
  Future<String> execute(String command);
  Future<void> close();
}
