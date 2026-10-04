package com.nexuscraft.nexushusbandry;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * An animal's lineage -- both real parents' UUIDs (when known; a wild-spawned founder has
 * neither), its generation number, and whether it was flagged a rare "prime specimen" at birth
 * (see {@link PedigreeService}). PDC-backed, same convention as every other store in this plugin.
 * A real parent {@code LivingEntity} can die, be removed, or simply not be reachable again by
 * UUID alone in this sandbox (no "look up any entity anywhere by UUID" API exists in real Bukkit
 * either, short of a full-world scan) -- this store deliberately only ever records the parent
 * UUIDs as a historical fact, it never promises a live, dereferenceable handle back to them.
 */
final class PedigreeStore {

    private final NamespacedKey motherKey;
    private final NamespacedKey fatherKey;
    private final NamespacedKey generationKey;
    private final NamespacedKey primeSpecimenKey;

    PedigreeStore(Plugin plugin) {
        this.motherKey = new NamespacedKey(plugin, "husbandry_mother");
        this.fatherKey = new NamespacedKey(plugin, "husbandry_father");
        this.generationKey = new NamespacedKey(plugin, "husbandry_generation");
        this.primeSpecimenKey = new NamespacedKey(plugin, "husbandry_prime_specimen");
    }

    /** {@code 0} for a founder (no recorded parents, wild or pre-existing) -- every bred child is
     *  at least generation {@code 1}. */
    int generationOf(Animals animal) {
        Integer stored = animal.getPersistentDataContainer().get(generationKey, PersistentDataType.INTEGER);
        return stored == null ? 0 : stored;
    }

    boolean isPrimeSpecimen(Animals animal) {
        Boolean flag = animal.getPersistentDataContainer().get(primeSpecimenKey, PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(flag);
    }

    UUID motherOf(Animals animal) {
        String id = animal.getPersistentDataContainer().get(motherKey, PersistentDataType.STRING);
        return parseOrNull(id);
    }

    UUID fatherOf(Animals animal) {
        String id = animal.getPersistentDataContainer().get(fatherKey, PersistentDataType.STRING);
        return parseOrNull(id);
    }

    void recordParentage(Animals child, Animals mother, Animals father, int generation, boolean primeSpecimen) {
        child.getPersistentDataContainer().set(motherKey, PersistentDataType.STRING, mother.getUniqueId().toString());
        child.getPersistentDataContainer().set(fatherKey, PersistentDataType.STRING, father.getUniqueId().toString());
        child.getPersistentDataContainer().set(generationKey, PersistentDataType.INTEGER, generation);
        child.getPersistentDataContainer().set(primeSpecimenKey, PersistentDataType.BOOLEAN, primeSpecimen);
    }

    private static UUID parseOrNull(String id) {
        if (id == null) {
            return null;
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
