package com.nexuscraft.nexushusbandry;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * The one real event every real player touchpoint with a specific animal funnels through --
 * {@code PlayerInteractEntityEvent} -- bundled into a single listener the same way {@code
 * MovementListener} bundles several of NexusMomentum's own systems onto one real move event,
 * rather than four separate listeners all independently re-validating/re-casting the same
 * right-clicked entity. Order here matters and is deliberate: genetics gets a chance to assign a
 * genome first (every other system below assumes one already exists), then veterinary care
 * (using a {@link RemedyItems#isRemedy} potion) takes priority over an ordinary feeding
 * interaction (a remedy potion is never also a real vanilla breed item, so there's no real
 * conflict, but checking it first keeps the branches readable), and a passive infection-risk roll
 * happens on every interaction regardless -- real exposure doesn't wait for a player's specific
 * intent.
 */
final class AnimalInteractionListener implements Listener {

    private final HusbandryConfig config;
    private final GeneticsService geneticsService;
    private final WelfareStore welfareStore;
    private final VeterinaryService veterinaryService;
    private final RemedyItems remedyItems;
    private final Random random = new Random();

    AnimalInteractionListener(HusbandryConfig config, GeneticsService geneticsService, WelfareStore welfareStore,
                               VeterinaryService veterinaryService, RemedyItems remedyItems) {
        this.config = config;
        this.geneticsService = geneticsService;
        this.welfareStore = welfareStore;
        this.veterinaryService = veterinaryService;
        this.remedyItems = remedyItems;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!config.enabled) {
            return;
        }
        Animals animal = HusbandryAnimals.asLivestock(event.getRightClicked());
        if (animal == null) {
            return;
        }

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        geneticsService.ensureGenome(animal, random);

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (remedyItems.isRemedy(handItem)) {
            handleRemedy(player, animal, handItem, now);
        } else if (handItem != null && animal.isBreedItem(handItem.getType())) {
            welfareStore.adjust(animal, config.feedHappinessBoost, now);
        }

        veterinaryService.rollInfection(animal, random, now);
        veterinaryService.progress(animal, now);
    }

    private void handleRemedy(Player player, Animals animal, ItemStack remedy, long now) {
        if (!veterinaryService.isSick(animal)) {
            player.sendMessage("§7This animal looks perfectly healthy already.");
            return;
        }
        Disease cured = veterinaryService.cure(animal, now);
        remedy.setAmount(remedy.getAmount() - 1);
        player.sendMessage("§aYou treat the animal's " + cured.displayName() + " -- it looks much better already.");
    }
}
