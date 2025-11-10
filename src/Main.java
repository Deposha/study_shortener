import java.awt.Desktop;
import java.net.*;
import java.time.*;
import java.util.*;

public class Main {
    private static final Scanner SC = new Scanner(System.in);
    private static final MemoryStore STORE = new MemoryStore();
    private static final ShortCodeGenerator GEN = new ShortCodeGenerator();
    private static final String PREFIX = "suren.com/";
    private static final int SHORT_LEN = 8;

    public static void main(String[] args) {
        System.out.println("=== Шортнер ссылок (in-memory) ===");
        System.out.println("Подсказка: в любой момент можно ввести suren.com/[код] — ссылка будет использована и открыта в браузере.");
        while (true) {
            STORE.cleanupExpired();
            System.out.println();
            System.out.println("Меню:");
            System.out.println("1) Войти по UUID");
            System.out.println("2) Создать UUID (регистрация + вход)");
            System.out.println("0) Выход");
            System.out.print("Ваш выбор: ");
            String choice = readLineWithGlobalShortUse();
            switch (choice) {
                case "1" -> {
                    System.out.print("Введите ваш UUID: ");
                    String s = readLineWithGlobalShortUse().trim();
                    try {
                        UUID id = UUID.fromString(s);
                        if (STORE.userExists(id)) {
                            accountMenu(id);
                        } else {
                            System.out.println("Пользователь не найден.");
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.println("Неверный формат UUID.");
                    }
                }
                case "2" -> {
                    UUID id = STORE.registerUser();
                    System.out.println("Создан новый пользователь: " + id);
                    accountMenu(id);
                }
                case "0" -> {
                    System.out.println("Пока!");
                    return;
                }
                default -> System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }

    private static void accountMenu(UUID userId) {
        while (true) {
            STORE.cleanupExpired();
            System.out.println();
            System.out.println("UUID: " + userId);
            System.out.println("1) Посмотреть мои ссылки");
            System.out.println("2) Создать ссылку");
            System.out.println("3) Удалить ссылку");
            System.out.println("0) Выйти из аккаунта");
            System.out.print("Ваш выбор: ");
            String choice = readLineWithGlobalShortUse();
            switch (choice) {
                case "1" -> listMyLinks(userId);
                case "2" -> createLinkFlow(userId);
                case "3" -> deleteLinkFlow(userId);
                case "0" -> { System.out.println("Вы вышли из аккаунта."); return; }
                default -> System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }

    private static void listMyLinks(UUID userId) {
        List<MemoryStore.LinkEntry> links = STORE.getUserLinks(userId);
        if (links.isEmpty()) { System.out.println("У вас нет ссылок."); return; }
        System.out.println("Ваши ссылки:");
        for (int i = 0; i < links.size(); i++) {
            System.out.println("[" + (i + 1) + "]");
            System.out.println(links.get(i));
        }
    }

    private static void createLinkFlow(UUID userId) {
        System.out.print("Введите ссылку (подойдет https://, http:// или без протокола): ");
        String originalInput = readLineWithGlobalShortUse().trim();
        if (!isAcceptableUrl(originalInput)) {
            System.out.println("Неверный формат ссылки.");
            return;
        }
        String shortCode;
        while (true) {
            shortCode = GEN.nextCode(SHORT_LEN);
            if (!STORE.linksByCode.containsKey(shortCode)) break;
        }
        Instant expiresAt = chooseExpiryInstant();
        int uses = readInt("Введите максимальное число использований (>0, максимум " + Integer.MAX_VALUE + "): ", 1, Integer.MAX_VALUE);
        MemoryStore.LinkEntry e = STORE.createLink(userId, originalInput, shortCode, uses, expiresAt);
        System.out.println("Создано:");
        System.out.println(e);
    }

    private static boolean isAcceptableUrl(String s) {
        if (s == null || s.isBlank() || s.contains(" ")) return false;
        if (s.contains("://")) {
            try {
                URL u = new URL(s);
                String host = u.getHost();
                if (host == null || host.isEmpty()) return false;
                return isHostValid(host);
            } catch (MalformedURLException e) {
                return false;
            }
        } else {
            String hostPart = s;
            int slash = s.indexOf('/');
            if (slash >= 0) hostPart = s.substring(0, slash);
            if (hostPart.isEmpty()) return false;
            return isHostValid(hostPart);
        }
    }

    private static boolean isHostValid(String host) {
        if (host.equalsIgnoreCase("localhost")) return true;
        if (host.matches("^\\d{1,3}(?:\\.\\d{1,3}){3}$")) {
            String[] parts = host.split("\\.");
            for (String p : parts) {
                int v = Integer.parseInt(p);
                if (v < 0 || v > 255) return false;
            }
            return true;
        }
        if (!host.contains(".")) return false;
        String tld = host.substring(host.lastIndexOf('.') + 1);
        return tld.length() >= 2;
    }

    private static Instant chooseExpiryInstant() {
        System.out.println("Выберите длительность существования:");
        System.out.println("1) 5 минут");
        System.out.println("2) 30 минут");
        System.out.println("3) 60 минут");
        System.out.println("4) Неделя");
        System.out.println("5) Месяц");
        System.out.println("6) Без ограничений");
        System.out.print("Ваш выбор: ");
        String ch = readLineWithGlobalShortUse();
        Instant now = Instant.now();
        return switch (ch) {
            case "1" -> now.plus(Duration.ofMinutes(5));
            case "2" -> now.plus(Duration.ofMinutes(30));
            case "3" -> now.plus(Duration.ofMinutes(60));
            case "4" -> now.plus(Duration.ofDays(7));
            case "5" -> now.plus(Duration.ofDays(30));
            case "6" -> null;
            default -> null;
        };
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String s = readLineWithGlobalShortUse().trim();
            try {
                long valLong = Long.parseLong(s);
                if (valLong < min || valLong > max) System.out.println("Число вне диапазона.");
                else return (int) valLong;
            } catch (NumberFormatException e) {
                System.out.println("Ожидается число.");
            }
        }
    }

    private static void deleteLinkFlow(UUID userId) {
        List<MemoryStore.LinkEntry> links = STORE.getUserLinks(userId);
        if (links.isEmpty()) { System.out.println("У вас нет ссылок для удаления."); return; }
        for (int i = 0; i < links.size(); i++) {
            System.out.println((i + 1) + ") suren.com/" + links.get(i).shortCode + "  ->  " + links.get(i).original);
        }
        System.out.print("Выберите номер для удаления (0 — отмена): ");
        String s = readLineWithGlobalShortUse().trim();
        int idx;
        try { idx = Integer.parseInt(s); } catch (NumberFormatException e) { System.out.println("Неверный ввод."); return; }
        if (idx == 0) { System.out.println("Отмена."); return; }
        if (idx < 1 || idx > links.size()) { System.out.println("Неверный номер."); return; }
        String code = links.get(idx - 1).shortCode;
        boolean ok = STORE.deleteLink(userId, code);
        System.out.println(ok ? "Ссылка удалена." : "Не удалось удалить ссылку.");
    }

    private static String readLineWithGlobalShortUse() {
        while (true) {
            String line = SC.nextLine();
            String code = extractShortCodeFromText(line);
            if (code != null) {
                Optional<String> res = STORE.tryUseShort(code);
                if (res.isPresent()) {
                    String url = res.get();
                    System.out.println("Открываю...");
                    openInBrowser(url);
                    continue;
                } else {
                    System.out.println("Код недействителен.");
                }
            }
            return line;
        }
    }

    private static void openInBrowser(String url) {
        String toOpen = url;
        if (!url.contains("://")) toOpen = "https://" + url;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(toOpen));
            } else {
                System.out.println("Откройте вручную: " + toOpen);
            }
        } catch (Exception e) {
            System.out.println("Не удалось открыть: " + toOpen);
        }
    }

    private static String extractShortCodeFromText(String text) {
        if (text == null) return null;
        int idx = text.indexOf(PREFIX);
        if (idx < 0) return null;
        int start = idx + PREFIX.length();
        if (start >= text.length()) return null;
        int end = start;
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) end++;
        String code = text.substring(start, end).trim();
        return code.isEmpty() ? null : code;
    }
}
