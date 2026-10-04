package com.nexuscraft.nexushusbandry;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * An animal's {@link Genome}, stored directly on its own real PDC -- same PDC-tagging convention
 * this whole family already uses for per-player skill data (compare {@code TraversalSkillStore}),
 * just on a non-player {@code LivingEntity} this time (real Bukkit's {@code Entity} is a real
 * {@code PersistentDataHolder} too -- see {@code Entity.java}'s own stub comment). One packed
 * {@code STRING} key for the whole genome -- see {@link Genome#encode()}'s own comment for why
 * that's one key instead of four.
 */
final class GenomeStore {

    private final NamespacedKey genomeKey;

    GenomeStore(Plugin plugin) {
        this.genomeKey = new NamespacedKey(plugin, "husbandry_genome");
    }

    boolean hasGenome(Animals animal) {
        return animal.getPersistentDataContainer().has(genomeKey, PersistentDataType.STRING);
    }

    /** {@code null} if this animal has never had a genome assigned (a wild spawn this plugin
     *  hasn't yet seen, or a genome string that failed to decode -- see {@link Genome#decode}). */
    Genome genomeOf(Animals animal) {
        String packed = animal.getPersistentDataContainer().get(genomeKey, PersistentDataType.STRING);
        return Genome.decode(packed);
    }

    void setGenome(Animals animal, Genome genome) {
        animal.getPersistentDataContainer().set(genomeKey, PersistentDataType.STRING, genome.encode());
    }
}
