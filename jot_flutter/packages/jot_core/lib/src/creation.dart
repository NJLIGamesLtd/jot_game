void validateCoreCreation(String dictionary, int seed, int robot) {
  if (seed < 0 || seed > 0xffffffff) {
    throw ArgumentError.value(
      seed,
      'seed',
      'Must be an unsigned 32-bit integer.',
    );
  }
  if (robot < -1 || robot > 1) {
    throw ArgumentError.value(robot, 'robot', 'Must be -1, 0, or 1.');
  }
  if (dictionary.contains('\u0000')) {
    throw ArgumentError.value(
      dictionary,
      'dictionary',
      'Contains a null byte.',
    );
  }
}
