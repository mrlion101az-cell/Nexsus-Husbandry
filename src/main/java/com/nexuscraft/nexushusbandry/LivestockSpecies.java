package com.nexuscraft.nexushusbandry;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

/**
 * The six real vanilla breedable livestock types this whole plugin is built around ({@code Cow},
 * {@code Pig}, {@code Sheep}, {@code Chicken}, {@code Rabbit}, {@code Horse} -- confirmed real
 * against 1.21.4 javadoc; see EntityType.java's own stub comment). Every one of this plugin's four
 * systems (genetics, welfare, veterinary care, pedigree) branches on species somewhere -- which
 * real breed-food list applies, which {@link Disease} an animal is at risk of, whether a bonus
 * trait even makes sense for that species -- so centralizing the {@code Entity -> species}
 * classification here, once, keeps every one of those branches consistent rather than four
 * separate {@code instanceof}/{@code getType()} chains that could quietly drift apart from each
 * other.
 */
enum LivestockSpecies {
    COW,
    PIG,
    SHEEP,
    CHICKEN,
    RABBIT,
    HORSE;

    /** {@code null} for any entity outside this plugin's own scope (a zombie, a villager, a
     *  player) -- every caller is written to treat that as "not a husbandry animal," not an
     *  error; see {@code HusbandryAnimals.isLivestock}. */
    static LivestockSpecies of(Entity entity) {
        if (entity == null) {
            return null;
        }
        EntityType type = entity.getType();
        return switch (type) {
            case COW -> COW;
            case PIG -> PIG;
            case SHEEP -> SHEEP;
            case CHICKEN -> CHICKEN;
            case RABBIT -> RABBIT;
            case HORSE -> HORSE;
            default -> null;
        };
    }
}
