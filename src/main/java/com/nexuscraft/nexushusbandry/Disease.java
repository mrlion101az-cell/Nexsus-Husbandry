package com.nexuscraft.nexushusbandry;

import org.bukkit.potion.PotionEffectType;

/**
 * This plugin's own three invented livestock ailments -- real vanilla has no disease system of
 * its own at all for passive mobs (checked; nothing in {@code EntityDamageEvent}, {@code
 * EntityRegainHealthEvent}, or anywhere else in real Bukkit models a progressive illness), so
 * unlike {@link Trait}/real {@code Attribute} or {@link #symptom}/real {@code PotionEffectType},
 * the disease catalog itself is this plugin's own content, flavor-matched to each species the
 * same way a real farm sim would (hoofed animals get foot rot, poultry gets a pox, fur-bearing
 * animals get mange). Each one DOES apply a real, confirmed {@code PotionEffectType} as its
 * visible symptom though -- see {@link #symptom()} and {@code LivingEntity#addPotionEffect}'s own
 * "moved up from Player" fix.
 */
enum Disease {
    NONE,
    /** Cows, sheep, and pigs -- hoofed livestock. */
    FOOT_ROT,
    /** Chickens. */
    FOWL_POX,
    /** Rabbits and horses -- fur-/coat-bearing animals. */
    MANGE;

    static Disease likelyFor(LivestockSpecies species) {
        return switch (species) {
            case COW, SHEEP, PIG -> FOOT_ROT;
            case CHICKEN -> FOWL_POX;
            case RABBIT, HORSE -> MANGE;
        };
    }

    /** {@code null} for {@link #NONE} -- a healthy animal has no symptom to apply or remove. */
    PotionEffectType symptom() {
        return switch (this) {
            case NONE -> null;
            case FOOT_ROT -> PotionEffectType.SLOWNESS;
            case FOWL_POX -> PotionEffectType.WEAKNESS;
            case MANGE -> PotionEffectType.SLOWNESS;
        };
    }

    String displayName() {
        return switch (this) {
            case NONE -> "Healthy";
            case FOOT_ROT -> "Foot Rot";
            case FOWL_POX -> "Fowl Pox";
            case MANGE -> "Mange";
        };
    }
}
