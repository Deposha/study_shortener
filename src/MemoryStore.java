import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MemoryStore {
    public static class LinkEntry {
        final UUID ownerId;
        final String original;
        final String shortCode;
        int used;
        final int maxUses;
        final Instant expiresAt;

        LinkEntry(UUID ownerId, String original, String shortCode, int maxUses, Instant expiresAt) {
            this.ownerId = ownerId;
            this.original = original;
            this.shortCode = shortCode;
            this.used = 0;
            this.maxUses = maxUses;
            this.expiresAt = expiresAt;
        }
        boolean isExpired(Instant now) { return expiresAt != null && now.isAfter(expiresAt); }
        boolean canUse(Instant now) { return !isExpired(now) && used < maxUses; }
        void registerUse() { used++; }
        @Override
        public String toString() {
            String exp = (expiresAt == null) ? "неограниченно"
                    : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault()).format(expiresAt);
            return "short: suren.com/" + shortCode +
                    "\n  оригинал: " + original +
                    "\n  использ.: " + used + "/" + maxUses +
                    "\n  истекает: " + exp;
        }
    }

    final Set<UUID> users = new HashSet<>();
    final Map<String, LinkEntry> linksByCode = new HashMap<>();
    final Map<UUID, List<LinkEntry>> linksByOwner = new HashMap<>();

    UUID registerUser() {
        UUID id = UUID.randomUUID();
        users.add(id);
        linksByOwner.putIfAbsent(id, new ArrayList<>());
        return id;
    }
    boolean userExists(UUID id) { return users.contains(id); }
    LinkEntry createLink(UUID owner, String original, String shortCode, int maxUses, Instant expiresAt) {
        LinkEntry e = new LinkEntry(owner, original, shortCode, maxUses, expiresAt);
        linksByCode.put(shortCode, e);
        linksByOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(e);
        return e;
    }
    boolean deleteLink(UUID owner, String shortCode) {
        LinkEntry e = linksByCode.get(shortCode);
        if (e == null || !e.ownerId.equals(owner)) return false;
        linksByCode.remove(shortCode);
        List<LinkEntry> lst = linksByOwner.get(owner);
        if (lst != null) lst.removeIf(le -> le.shortCode.equals(shortCode));
        return true;
    }
    List<LinkEntry> getUserLinks(UUID owner) {
        cleanupExpired();
        return new ArrayList<>(linksByOwner.getOrDefault(owner, Collections.emptyList()));
    }
    Optional<String> tryUseShort(String code) {
        cleanupExpired();
        LinkEntry e = linksByCode.get(code);
        Instant now = Instant.now();
        if (e == null || !e.canUse(now)) return Optional.empty();
        e.registerUse();
        return Optional.of(e.original);
    }
    void cleanupExpired() {
        Instant now = Instant.now();
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, LinkEntry> en : linksByCode.entrySet()) {
            LinkEntry e = en.getValue();
            if (!e.canUse(now)) toRemove.add(en.getKey());
        }
        for (String code : toRemove) {
            LinkEntry e = linksByCode.remove(code);
            if (e != null) {
                List<LinkEntry> lst = linksByOwner.get(e.ownerId);
                if (lst != null) lst.removeIf(le -> le.shortCode.equals(code));
            }
        }
    }
}
