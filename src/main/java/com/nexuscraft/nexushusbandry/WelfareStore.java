package com.nexuscraft.nexushusbandry;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * An animal's current happiness plus the real wall-clock timestamp it was last computed at --
 * see {@link WelfareMath}'s own class comment for why lazy, on-touch decay replaces a periodic
 * tick loop in this plugin. A freshly-seen animal (no stored value yet) starts at full, {@code
 * 100.0} happiness, not zero -- same "a fresh join starts full, not empty" reasoning
 * {@code StaminaStore} already uses on the player side.
 */
final class WelfareStore {

    private final NamespacedKey happinessKey;
    private final NamespacedKey lastUpdateKey;
    private final HusbandryConfig config;

    WelfareStore(Plugin plugin, HusbandryConfig config) {
        this.happinessKey = new NamespacedKey(plugin, "husbandry_happiness");
        this.lastUpdateKey = new NamespacedKey(plugin, "husbandry_happiness_updated");
        this.config = config;
    }

    /** Recomputes, persists, and returns this animal's happiness as of {@code nowMillis} --
     *  every caller should go through this rather than reading the raw stored value directly, or
     *  it'll see a stale number for an animal nobody has touched in a while. */
    double effectiveHappiness(Animals animal, long nowMillis) {
        Double storedHappiness = animal.getPersistentDataContainer().get(happinessKey, PersistentDataType.DOUBLE);
        Long lastUpdate = animal.getPersistentDataContainer().get(lastUpdateKey, PersistentDataType.LONG);
        double current = storedHappiness == null ? 100.0 : storedHappiness;
        long elapsed = lastUpdate == null ? 0L : Math.max(0L, nowMillis - lastUpdate);
        double decayed = WelfareMath.decayedHappiness(current, elapsed, config.happinessDecayPerMinute,
                config.happinessFloor);
        store(animal, decayed, nowMillis);
        return decayed;
    }

    /** Applies {@code delta} on top of the freshly-decayed current value, and persists the
     *  result. Returns the new happiness. */
    double adjust(Animals animal, double delta, long nowMillis) {
        double current = effectiveHappiness(animal, nowMillis);
        double updated = WelfareMath.adjusted(current, delta);
        store(animal, updated, nowMillis);
        return updated;
    }

    private void store(Animals animal, double happiness, long nowMillis) {
        animal.getPersistentDataContainer().set(happinessKey, PersistentDataType.DOUBLE, happiness);
        animal.getPersistentDataContainer().set(lastUpdateKey, PersistentDataType.LONG, nowMillis);
    }
}
