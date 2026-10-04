package com.nexuscraft.nexushusbandry;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Animals;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Applies a {@link Genome} onto an animal's own real {@link Attribute} instances, and resolves
 * what a newborn's genome should be at the moment of {@code EntityBreedEvent}. Real
 * attribute-modifier application (not a potion effect, not a raw stat overwrite) -- the same
 * pattern NexusLode's mining skill and NexusMomentum's traversal skill both already established
 * for players, just read through {@code LivingEntity#getAttribute()} on an animal this time (see
 * that method's own "moved up from Player" comment on {@code LivingEntity}).
 *
 * <p>Each of {@link Trait#VITALITY}/{@link Trait#SPEED}/{@link Trait#STRENGTH} maps to one real
 * {@code Attribute} via a single, fixed-key {@code ADD_SCALAR} modifier: a gene of exactly
 * {@code 0.5} contributes no scalar at all (the animal sits at real vanilla's own default for
 * that attribute), a gene of {@code 1.0} adds {@code +traitAttributeScalarRange} to the base
 * value, and a gene of {@code 0.0} subtracts the same amount -- so the configured range is a
 * symmetric spread around vanilla's own normal, not a one-sided buff. Using a fixed
 * {@link NamespacedKey} per trait (not a fresh one every call) is what lets this be called again
 * on config reload or a later re-application without ever stacking a second copy of the same
 * modifier -- {@code AttributeInstance#addModifier} already de-duplicates by key (see that
 * stub's own comment), the same reason {@code TraversalAttributeService} does this on the player
 * side.
 */
final class GeneticsService {

    private final HusbandryConfig config;
    private final GenomeStore genomeStore;
    private final Map<Trait, NamespacedKey> modifierKeys = new EnumMap<>(Trait.class);

    GeneticsService(Plugin plugin, HusbandryConfig config, GenomeStore genomeStore) {
        this.config = config;
        this.genomeStore = genomeStore;
        for (Trait trait : Trait.values()) {
            if (trait.attribute() != null) {
                modifierKeys.put(trait, new NamespacedKey(plugin, "husbandry_trait_" + trait.name().toLowerCase()));
            }
        }
    }

    /** Guarantees {@code animal} has a genome, assigning a fresh {@link GenomeMath#wildGenome} if
     *  it doesn't (a wild spawn, or one that predates this plugin), and applying it either way --
     *  called on every real interaction this plugin listens for, not just breeding, so a wild
     *  animal a player has never fed or bred still has a genuine genetic identity the moment any
     *  system here looks at it. */
    Genome ensureGenome(Animals animal, Random random) {
        Genome genome = genomeStore.genomeOf(animal);
        if (genome == null) {
            genome = GenomeMath.wildGenome(config.wildGenomeCenter, config.wildGenomeSpread, random);
            genomeStore.setGenome(animal, genome);
            applyToEntity(animal, genome);
        }
        return genome;
    }

    Genome resolveOffspringGenome(Genome motherGenome, Genome fatherGenome, Random random) {
        return GenomeMath.inherit(motherGenome, fatherGenome, config.mutationChance, config.mutationStrength, random);
    }

    void applyToEntity(Animals animal, Genome genome) {
        for (Trait trait : Trait.values()) {
            Attribute attribute = trait.attribute();
            if (attribute == null) {
                continue;
            }
            AttributeInstance instance = animal.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            double scalar = (genome.get(trait) - 0.5) * 2.0 * config.traitAttributeScalarRange;
            instance.addModifier(new AttributeModifier(
                    modifierKeys.get(trait), scalar, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
        }
    }

    /** This plugin's own payoff for a high-{@link Trait#FERTILITY} breeding pair: real vanilla
     *  breeding always produces exactly one child, so a "bonus twin" has to be this plugin's own
     *  separate, additional spawn -- real {@code World#spawn(Location, Class)}, not a second
     *  {@code EntityBreedEvent} (nothing fires one of those twice for a single real breeding).
     *  Rolled independently of everything else in this class; see
     *  {@code GenomeMath#rollRareMutation} for the unrelated rare-mutation roll this is sometimes
     *  confused with in design notes but is NOT the same mechanic. */
    boolean rollBonusTwin(Genome motherGenome, Genome fatherGenome, Random random) {
        double averageFertility = (motherGenome.fertility() + fatherGenome.fertility()) / 2.0;
        double chance = config.twinBaseChance + averageFertility * config.twinFertilityBonusChance;
        return random.nextDouble() < chance;
    }
}
