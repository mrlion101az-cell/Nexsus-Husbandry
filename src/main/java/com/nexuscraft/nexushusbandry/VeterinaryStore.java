package com.nexuscraft.nexushusbandry;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** An animal's disease and injury state, PDC-backed the same way as {@link GenomeStore}/{@link
 *  WelfareStore}. Disease severity and injury both use the same lazy, on-touch recompute pattern
 *  {@link WelfareStore} already established -- see that class's own comment for why. */
final class VeterinaryStore {

    private final NamespacedKey diseaseKey;
    private final NamespacedKey severityKey;
    private final NamespacedKey severityUpdatedKey;
    private final NamespacedKey injuryKey;
    private final NamespacedKey injuryUpdatedKey;
    private final HusbandryConfig config;

    VeterinaryStore(Plugin plugin, HusbandryConfig config) {
        this.diseaseKey = new NamespacedKey(plugin, "husbandry_disease");
        this.severityKey = new NamespacedKey(plugin, "husbandry_disease_severity");
        this.severityUpdatedKey = new NamespacedKey(plugin, "husbandry_disease_updated");
        this.injuryKey = new NamespacedKey(plugin, "husbandry_injury");
        this.injuryUpdatedKey = new NamespacedKey(plugin, "husbandry_injury_updated");
        this.config = config;
    }

    Disease diseaseOf(Animals animal) {
        String name = animal.getPersistentDataContainer().get(diseaseKey, PersistentDataType.STRING);
        if (name == null) {
            return Disease.NONE;
        }
        try {
            return Disease.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Disease.NONE;
        }
    }

    /** Recomputes and persists disease severity as of {@code nowMillis}, progressing it forward
     *  if the animal is currently sick. Returns the up-to-date severity (always {@code 0.0} for
     *  a healthy animal). */
    double effectiveSeverity(Animals animal, long nowMillis) {
        if (diseaseOf(animal) == Disease.NONE) {
            return 0.0;
        }
        Double stored = animal.getPersistentDataContainer().get(severityKey, PersistentDataType.DOUBLE);
        Long lastUpdate = animal.getPersistentDataContainer().get(severityUpdatedKey, PersistentDataType.LONG);
        double current = stored == null ? config.initialDiseaseSeverity : stored;
        long elapsed = lastUpdate == null ? 0L : Math.max(0L, nowMillis - lastUpdate);
        double progressed = DiseaseMath.progressedSeverity(current, elapsed, config.diseaseProgressionPerMinute);
        storeSeverity(animal, progressed, nowMillis);
        return progressed;
    }

    void infect(Animals animal, Disease disease, long nowMillis) {
        animal.getPersistentDataContainer().set(diseaseKey, PersistentDataType.STRING, disease.name());
        storeSeverity(animal, config.initialDiseaseSeverity, nowMillis);
    }

    void cure(Animals animal, long nowMillis) {
        animal.getPersistentDataContainer().set(diseaseKey, PersistentDataType.STRING, Disease.NONE.name());
        storeSeverity(animal, 0.0, nowMillis);
    }

    private void storeSeverity(Animals animal, double severity, long nowMillis) {
        animal.getPersistentDataContainer().set(severityKey, PersistentDataType.DOUBLE, severity);
        animal.getPersistentDataContainer().set(severityUpdatedKey, PersistentDataType.LONG, nowMillis);
    }

    /** Recomputes, persists, and returns this animal's injury level as of {@code nowMillis},
     *  healing it slowly toward zero the same lazy way {@link #effectiveSeverity} progresses
     *  disease upward. */
    double effectiveInjury(Animals animal, long nowMillis) {
        Double stored = animal.getPersistentDataContainer().get(injuryKey, PersistentDataType.DOUBLE);
        Long lastUpdate = animal.getPersistentDataContainer().get(injuryUpdatedKey, PersistentDataType.LONG);
        double current = stored == null ? 0.0 : stored;
        long elapsed = lastUpdate == null ? 0L : Math.max(0L, nowMillis - lastUpdate);
        double healed = DiseaseMath.healedInjury(current, elapsed, config.injuryHealPerMinute);
        storeInjury(animal, healed, nowMillis);
        return healed;
    }

    double addInjury(Animals animal, double amount, long nowMillis) {
        double current = effectiveInjury(animal, nowMillis);
        double updated = DiseaseMath.clamp01(current + amount);
        storeInjury(animal, updated, nowMillis);
        return updated;
    }

    private void storeInjury(Animals animal, double injury, long nowMillis) {
        animal.getPersistentDataContainer().set(injuryKey, PersistentDataType.DOUBLE, injury);
        animal.getPersistentDataContainer().set(injuryUpdatedKey, PersistentDataType.LONG, nowMillis);
    }
}
