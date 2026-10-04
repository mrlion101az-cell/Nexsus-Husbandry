package com.nexuscraft.nexushusbandry;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * This plugin's own craftable cure item: a "Veterinary Remedy" -- not a new {@code Material} (no
 * such thing as a custom item type in real Bukkit without a resource pack this project doesn't
 * ship), but a real, ordinary vanilla Potion of Healing that this plugin's own {@code
 * BrewListener} additionally tags via PDC the moment real vanilla brews one from real {@code
 * Material.GLISTERING_MELON_SLICE} (the real, confirmed vanilla ingredient for that exact potion
 * -- see {@code Material.java}'s own NexusAlchemy comment block). The tag is purely additive: the
 * potion is still a completely ordinary, real Potion of Healing a player can drink themselves
 * (the real vanilla brew already decided that), this plugin just also recognizes that specific
 * tagged stack as usable on a sick animal (see {@code VeterinaryInteractionListener}). Nothing
 * here cancels the brew or edits {@code BrewEvent#getResults()} -- a hypothetical sibling plugin
 * with its own, unrelated {@code BrewEvent} listener (NexusAlchemy, referenced throughout this
 * stub tree's own comments) is never fought over whose tag wins, since this plugin never removes
 * or replaces anything, only adds its own marker on top of whatever real item vanilla (or another
 * plugin) already decided the brew produces.
 */
final class RemedyItems {

    private final NamespacedKey remedyKey;

    RemedyItems(Plugin plugin) {
        this.remedyKey = new NamespacedKey(plugin, "husbandry_remedy");
    }

    void tagAsRemedy(ItemStack potion) {
        ItemMeta meta = potion.getItemMeta();
        meta.getPersistentDataContainer().set(remedyKey, PersistentDataType.BOOLEAN, true);
        meta.setDisplayName("§aVeterinary Remedy");
        meta.setLore(List.of(
                "§7A brewed Potion of Healing set aside",
                "§7for treating a sick animal.",
                "§7Right-click a sick animal to cure it."));
        potion.setItemMeta(meta);
    }

    boolean isRemedy(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        Boolean flag = item.getItemMeta().getPersistentDataContainer().get(remedyKey, PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(flag);
    }
}
