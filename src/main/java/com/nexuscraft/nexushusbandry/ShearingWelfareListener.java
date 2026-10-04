package com.nexuscraft.nexushusbandry;

import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SheepRegrowWoolEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

/** Two small, real, confirmed shearing-related hooks ({@code PlayerShearEntityEvent}/{@code
 *  SheepRegrowWoolEvent} -- see each stub's own comment) feeding this plugin's own welfare
 *  bucket: getting sheared is a real moment of stress (a small happiness penalty), and a real
 *  wool regrowth is read here as a sign of a comfortable, well-fed sheep (a small happiness
 *  boost) rather than this plugin trying to control the regrowth timer itself, which real
 *  vanilla already owns entirely on its own (grass-eating, not a plugin tick loop). */
final class ShearingWelfareListener implements Listener {

    private final HusbandryConfig config;
    private final WelfareStore welfareStore;

    ShearingWelfareListener(HusbandryConfig config, WelfareStore welfareStore) {
        this.config = config;
        this.welfareStore = welfareStore;
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent event) {
        if (!config.enabled) {
            return;
        }
        Animals animal = HusbandryAnimals.asLivestock(event.getEntity());
        if (animal == null) {
            return;
        }
        welfareStore.adjust(animal, -config.shearHappinessPenalty, System.currentTimeMillis());
    }

    @EventHandler
    public void onWoolRegrow(SheepRegrowWoolEvent event) {
        if (!config.enabled) {
            return;
        }
        welfareStore.adjust(event.getEntity(), config.woolRegrowHappinessBoost, System.currentTimeMillis());
    }
}
