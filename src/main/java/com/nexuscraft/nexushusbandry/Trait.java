package com.nexuscraft.nexushusbandry;

import org.bukkit.attribute.Attribute;

/**
 * The four heritable genes this plugin's own genetics system tracks per animal, each a plain
 * {@code 0.0-1.0} value (0 = runt of the litter, 1 = flawless specimen) read and written by
 * {@link Genome}/{@link GenomeMath}. Three of the four ride directly on a real vanilla {@link
 * Attribute}, applied through {@link GeneticsService} via {@code LivingEntity#getAttribute()} --
 * the same real-attribute-modifier pattern NexusLode's own mining skill and NexusMomentum's own
 * traversal skill both already established, just on an animal instead of a player this time.
 *
 * <p>{@link #FERTILITY} deliberately has no backing {@code Attribute} -- real vanilla has no
 * "fertility" attribute at all (checked; the 32-constant list in {@code Attribute.java} has
 * nothing resembling one), so this trait is this plugin's own invented genetic concept, applied
 * through plain breeding-outcome math in {@link GeneticsService} instead (a higher-fertility
 * parent pair has a real chance at a bonus second offspring -- see that class's own comment).
 */
enum Trait {
    /** Backs {@code Attribute.MAX_HEALTH} (real vanilla base {@code 20.0}). */
    VITALITY(Attribute.MAX_HEALTH),
    /** Backs {@code Attribute.MOVEMENT_SPEED} (real vanilla base {@code 0.1} for most animals). */
    SPEED(Attribute.MOVEMENT_SPEED),
    /** Backs {@code Attribute.ATTACK_DAMAGE} (real vanilla base {@code 1.0}, mostly cosmetic on a
     *  passive animal that never attacks -- kept anyway since it's a real, genuine genetic trait
     *  on the real entity, not a no-op; a tamed, ridden {@code Horse} is the one species in this
     *  plugin's own scope where this could ever matter in practice, via a kick). */
    STRENGTH(Attribute.ATTACK_DAMAGE),
    /** No backing {@code Attribute} -- see class comment. */
    FERTILITY(null);

    private final Attribute attribute;

    Trait(Attribute attribute) {
        this.attribute = attribute;
    }

    /** {@code null} for {@link #FERTILITY}; every caller that applies traits onto real attributes
     *  is written to skip a {@code null} result rather than assume every trait has one. */
    Attribute attribute() {
        return attribute;
    }
}
