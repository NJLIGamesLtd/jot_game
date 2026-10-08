import com.jot.decomp.ScanWords;
import com.jot.decomp.TreeWords;
import com.jot.decomp.Words;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;

/** Exports the bundled dictionary using the original project's decompressor. */
public final class ExportDictionary {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      throw new IllegalArgumentException("Usage: ExportDictionary input.jot output.txt");
    }
    Words tree = new TreeWords(args[0]);
    Words lookup = tree.clone();
    TreeSet<String> words = new TreeSet<>();
    tree.scan((letters, index) -> {
      int length = 0;
      while (length < letters.length && letters[length] != '\0') ++length;
      String word = new String(letters, 0, length);
      if (!word.matches("[A-Z]+") || !lookup.isWord(word)) {
        throw new IllegalStateException("Invalid dictionary entry: " + word);
      }
      if (!words.add(word)) throw new IllegalStateException("Duplicate entry: " + word);
      return ScanWords.SCAN_LEVEL_CONTINUE;
    }, ScanWords.SCAN_FLAG_QUICK);
    if (words.size() != tree.getWordCount()) {
      throw new IllegalStateException("Dictionary header count does not match exported count");
    }
    Path output = Path.of(args[1]);
    Files.createDirectories(output.toAbsolutePath().getParent());
    Files.writeString(output, String.join("\n", words) + "\n", StandardCharsets.US_ASCII);
    System.out.println("Exported " + words.size() + " original dictionary entries to " + output);
  }
}
