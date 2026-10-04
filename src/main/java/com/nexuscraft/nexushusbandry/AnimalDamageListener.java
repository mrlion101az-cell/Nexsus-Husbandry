package com.nexuscraft.nexushusbandry;

import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Real, confirmed trauma -- any {@code EntityDamageEvent} against one of this plugin's own six
 * species raises its injury level, which in turn raises its future infection risk (see {@link
 * VeterinaryService#rollInfection}'s own cross-system comment). Registered on the base {@code
 * EntityDamageEvent} rather than {@code EntityDamageByEntityEvent} specifically -- real Bukkit's
 * {@code EntityDamageByEntityEvent} never declares its own separate handler list, so a listener
 * registered on the base event type already genuinely catches both a mob attack and plain
 * environmental damage (fall, fire, drowning) alike; narrowing to the subclass would have quietly
 * dropped the environmental half for no real benefit. */
final class AnimalDamageListener implements Listener {

    private final HusbandryConfig config;
    private final VeterinaryService veterinaryService;

    AnimalDamageListener(HusbandryConfig config, VeterinaryService veterinaryService) {
        this.config = config;
        this.veterinaryService = veterinaryService;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!config.enabled) {
            return;
        }
        Animals animal = HusbandryAnimals.asLivestock(event.getEntity());
        if (animal == null) {
            return;
        }
        veterinaryService.recordInjury(animal, event.getFinalDamage(), System.currentTimeMillis());
    }
}
