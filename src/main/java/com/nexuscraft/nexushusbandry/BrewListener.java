package com.nexuscraft.nexushusbandry;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.ItemStack;

/** Tags a real, ordinary brewed Potion of Healing as this plugin's own "Veterinary Remedy" the
 *  moment real vanilla produces one from the real {@code Material.GLISTERING_MELON_SLICE}
 *  ingredient -- see {@link RemedyItems}'s own class comment for why this is purely additive
 *  (never cancels the event, never edits {@code BrewEvent#getResults()} itself, just mutates each
 *  resulting item's own meta in place) and therefore safe to coexist with any other plugin's own
 *  unrelated {@code BrewEvent} listener. Only plain {@code Material.POTION} results are tagged,
 *  not {@code SPLASH_POTION}/{@code LINGERING_POTION} -- a thrown remedy makes little sense for
 *  this plugin's own right-click-to-cure interaction model (see {@code AnimalInteractionListener}),
 *  a deliberate scope trim, not an oversight. */
final class BrewListener implements Listener {

    private final HusbandryConfig config;
    private final RemedyItems remedyItems;

    BrewListener(HusbandryConfig config, RemedyItems remedyItems) {
        this.config = config;
        this.remedyItems = remedyItems;
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (!config.enabled) {
            return;
        }
        ItemStack ingredient = event.getContents().getIngredient();
        if (ingredient == null || ingredient.getType() != Material.GLISTERING_MELON_SLICE) {
            return;
        }
        for (ItemStack result : event.getResults()) {
            if (result != null && result.getType() == Material.POTION) {
                remedyItems.tagAsRemedy(result);
            }
        }
    }
}
