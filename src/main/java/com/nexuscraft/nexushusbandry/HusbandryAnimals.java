package com.nexuscraft.nexushusbandry;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;

/** Tiny shared helper: is this entity one of this plugin's own six in-scope species at all.
 *  Every listener in this plugin checks this before doing any real work -- see each listener's
 *  own top comment for why a plain {@code instanceof Animals} alone isn't quite enough (real
 *  Bukkit's {@code Animals} family also covers things like a real Wolf's tamed-pet cousins that
 *  this plugin deliberately never touches; see README's "known, deliberate design choices"). */
final class HusbandryAnimals {

    private HusbandryAnimals() {
    }

    static boolean isLivestock(Entity entity) {
        return entity instanceof Animals && LivestockSpecies.of(entity) != null;
    }

    /** Returns the entity cast to {@link Animals}, or {@code null} if it isn't one of this
     *  plugin's own six in-scope species. Saves every caller its own repeat of
     *  {@code isLivestock} + an unchecked cast. */
    static Animals asLivestock(Entity entity) {
        return isLivestock(entity) ? (Animals) entity : null;
    }
}
