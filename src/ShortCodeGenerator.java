import java.util.*;

public class ShortCodeGenerator {
    private static final String ALPHABET;
    static {
        StringBuilder sb = new StringBuilder();
        for (char c = 'A'; c <= 'Z'; c++) sb.append(c);
        for (char c = 'a'; c <= 'z'; c++) sb.append(c);
        for (char c = '0'; c <= '9'; c++) sb.append(c);
        sb.append('_').append('-');
        ALPHABET = sb.toString();
    }
    private final Random rnd = new Random();
    String nextCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
