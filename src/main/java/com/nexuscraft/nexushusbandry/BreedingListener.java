package com.nexuscraft.nexushusbandry;

import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;

import java.util.Random;

/**
 * Where all four of this plugin's own systems actually meet: a real {@code EntityEnterLoveModeEvent}
 * is this plugin's own welfare gate (an unhappy animal isn't in the mood -- see
 * {@link WelfareMath#breedingAllowed}'s own comment), and a real {@code EntityBreedEvent} is
 * where genetics resolves the child's genome, pedigree records its lineage/rare-mutation roll,
 * and welfare gives both real parents a small happiness boost for a successful breeding, all in
 * one real moment.
 */
final class BreedingListener implements Listener {

    private final HusbandryConfig config;
    private final GenomeStore genomeStore;
    private final GeneticsService geneticsService;
    private final WelfareStore welfareStore;
    private final PedigreeService pedigreeService;
    private final Random random = new Random();

    BreedingListener(HusbandryConfig config, GenomeStore genomeStore, GeneticsService geneticsService,
                      WelfareStore welfareStore, PedigreeService pedigreeService) {
        this.config = config;
        this.genomeStore = genomeStore;
        this.geneticsService = geneticsService;
        this.welfareStore = welfareStore;
        this.pedigreeService = pedigreeService;
    }

    @EventHandler
    public void onEnterLoveMode(EntityEnterLoveModeEvent event) {
        if (!config.enabled) {
            return;
        }
        Animals animal = HusbandryAnimals.asLivestock(event.getEntity());
        if (animal == null) {
            return;
        }
        long now = System.currentTimeMillis();
        geneticsService.ensureGenome(animal, random);
        double happiness = welfareStore.effectiveHappiness(animal, now);
        if (!WelfareMath.breedingAllowed(happiness, config.breedingHappinessThreshold)) {
            // This plugin's own deliberate welfare gate: a neglected animal won't enter real
            // love mode at all until a player brings its happiness back up, a real, visible
            // incentive to actually care for livestock rather than just farming XP off them.
            event.setCancelled(true);
            if (event.getHumanEntity() instanceof Player player) {
                player.sendMessage("§7That animal doesn't seem interested right now -- it could use some care.");
            }
        }
    }

    @EventHandler
    public void onBreed(EntityBreedEvent event) {
        if (!config.enabled) {
            return;
        }
        Animals mother = HusbandryAnimals.asLivestock(event.getMother());
        Animals father = HusbandryAnimals.asLivestock(event.getFather());
        Animals child = HusbandryAnimals.asLivestock(event.getEntity());
        if (mother == null || father == null || child == null) {
            return;
        }

        long now = System.currentTimeMillis();
        geneticsService.ensureGenome(mother, random);
        geneticsService.ensureGenome(father, random);
        Genome motherGenome = genomeStore.genomeOf(mother);
        Genome fatherGenome = genomeStore.genomeOf(father);

        String breederName = event.getBreeder() instanceof Player player ? player.getName() : null;

        applyChildGenome(child, mother, father, motherGenome, fatherGenome, breederName, now);

        welfareStore.adjust(mother, config.breedingHappinessBoost, now);
        welfareStore.adjust(father, config.breedingHappinessBoost, now);

        if (geneticsService.rollBonusTwin(motherGenome, fatherGenome, random)) {
            spawnBonusTwin(child, mother, father, motherGenome, fatherGenome, breederName, now);
        }
    }

    private void applyChildGenome(Animals child, Animals mother, Animals father, Genome motherGenome,
                                   Genome fatherGenome, String breederName, long now) {
        Genome inherited = geneticsService.resolveOffspringGenome(motherGenome, fatherGenome, random);
        Genome finalGenome = pedigreeService.resolveChildGenomeAndRecord(
                child, mother, father, inherited, breederName, random);
        genomeStore.setGenome(child, finalGenome);
        geneticsService.applyToEntity(child, finalGenome);
    }

    /** Real vanilla breeding only ever produces one child per {@code EntityBreedEvent} -- a
     *  fertility-driven "bonus twin" has to be this plugin's own additional, separate spawn (real
     *  {@code World#spawn}, same real entity type as the first child), not a second breed event.
     *  See {@link GeneticsService#rollBonusTwin}'s own comment. */
    private void spawnBonusTwin(Animals child, Animals mother, Animals father, Genome motherGenome,
                                 Genome fatherGenome, String breederName, long now) {
        Location location = child.getLocation();
        if (location == null) {
            return;
        }
        Class<? extends Animals> species = child.getClass();
        LivingEntity spawned = location.getWorld().spawn(location, species);
        Animals twin = HusbandryAnimals.asLivestock(spawned);
        if (twin == null) {
            return;
        }
        twin.setBaby();
        applyChildGenome(twin, mother, father, motherGenome, fatherGenome, breederName, now);
    }
}
