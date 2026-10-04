package com.nexuscraft.nexushusbandry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

/**
 * A server-wide, in-memory leaderboard of this plugin's own best/rarest animals ever bred, kept
 * to the configured capacity (lowest-quality entry evicted once full). Deliberately NOT
 * PDC-backed -- PDC only ever attaches to one specific entity's own persistent data (see every
 * other store in this plugin), and a cross-animal leaderboard has no single entity to live on;
 * it's also deliberately NOT written to its own save file on disk, so a server restart clears it
 * -- a genuine, flagged scope trim for this v1 (see README's "known, deliberate design choices"):
 * a real next step would be a small JSON/YAML file in the plugin's own data folder, saved on
 * each new entry and reloaded on {@code onEnable}, but that's additional surface judged out of
 * scope for this pass rather than an oversight.
 */
final class HallOfFameRegistry {

    private final int capacity;
    private final List<HallOfFameEntry> entries = new ArrayList<>();

    HallOfFameRegistry(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    void record(HallOfFameEntry entry) {
        entries.add(entry);
        entries.sort(Comparator.comparingDouble(HallOfFameEntry::qualityScore).reversed());
        while (entries.size() > capacity) {
            entries.remove(entries.size() - 1);
        }
    }

    List<HallOfFameEntry> top(int count) {
        int limit = Math.max(0, Math.min(count, entries.size()));
        return Collections.unmodifiableList(entries.subList(0, limit));
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }
}
