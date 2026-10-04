package com.nexuscraft.nexushusbandry;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * NexusHusbandry -- a massive overhaul of animal breeding and husbandry, the next entry in the
 * ongoing engine-overhaul series. Real vanilla breeding has had essentially the same shape since
 * it was added: feed two of the same real breedable animal, they enter real love mode, and a
 * fresh, genetically-blank baby appears a few seconds later. This plugin keeps every one of those
 * real moments recognizable -- real breed items, real love mode, a real baby -- and builds four
 * systems on top of real vanilla's six breedable livestock types ({@code Cow}, {@code Pig},
 * {@code Sheep}, {@code Chicken}, {@code Rabbit}, {@code Horse}), exactly the four scope buckets
 * asked for when this project started:
 *
 * <p>Real genetic trait inheritance ({@link Genome}/{@link GenomeMath}/{@link GenomeStore}): four
 * heritable genes per animal, blended from both real parents at the moment of a real {@code
 * EntityBreedEvent} with an independent mutation roll per gene, and applied as real, live
 * modifiers onto real vanilla's own {@code Attribute.MAX_HEALTH}/{@code MOVEMENT_SPEED}/{@code
 * ATTACK_DAMAGE} attributes ({@link GeneticsService}) -- not a cosmetic number, the actual
 * attributes a real animal's health, speed, and strength are built on, the same real-attribute-
 * modifier pattern NexusLode's mining skill and NexusMomentum's traversal skill both already
 * established for players.
 *
 * <p>A real animal welfare system ({@link WelfareStore}/{@link WelfareMath}): a lazily-decayed
 * happiness value raised by real feeding (a real vanilla breed item), real wool regrowth, and
 * successful breeding, lowered by real shearing and plain neglect over real wall-clock time, and
 * genuinely gating whether an animal will even enter real love mode at all -- a neglected animal
 * won't breed until a player brings its happiness back up.
 *
 * <p>Real disease and veterinary care ({@link Disease}/{@link DiseaseMath}/{@link
 * VeterinaryService}): three species-flavored illnesses an animal can catch (worse odds the more
 * injured and unhappy it is), each with a real, visible {@code PotionEffect} symptom and a real
 * health toll if left untreated, cured by a real, ordinary vanilla Potion of Healing this
 * plugin's own {@link BrewListener} recognizes and tags the moment it's brewed from real {@code
 * Material.GLISTERING_MELON_SLICE}.
 *
 * <p>Pedigree and rare mutations ({@link PedigreeService}/{@link PedigreeStore}/{@link
 * HallOfFameRegistry}): every bred animal's real generation number and both real parents' UUIDs
 * are recorded, a rare, separate mutation roll can produce a near-perfect gene outright, and a
 * resulting "prime specimen" is logged to a server-wide hall of fame, viewable via {@code
 * /nexushusbandry hof}.
 *
 * <p>A standalone plugin, no shared compiled dependency with any other sibling -- same convention
 * as every other plugin in this family; see README.md's own section for the full reasoning.
 */
public final class NexusHusbandryPlugin extends JavaPlugin {

    private HusbandryConfig config;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new HusbandryConfig(this);
        config.load(getConfig());

        GenomeStore genomeStore = new GenomeStore(this);
        GeneticsService geneticsService = new GeneticsService(this, config, genomeStore);

        WelfareStore welfareStore = new WelfareStore(this, config);

        VeterinaryStore veterinaryStore = new VeterinaryStore(this, config);
        VeterinaryService veterinaryService = new VeterinaryService(config, veterinaryStore, welfareStore);
        RemedyItems remedyItems = new RemedyItems(this);

        PedigreeStore pedigreeStore = new PedigreeStore(this);
        HallOfFameRegistry hallOfFame = new HallOfFameRegistry(config.hallOfFameSize);
        PedigreeService pedigreeService = new PedigreeService(config, pedigreeStore, hallOfFame);

        getServer().getPluginManager().registerEvents(
                new BreedingListener(config, genomeStore, geneticsService, welfareStore, pedigreeService), this);
        getServer().getPluginManager().registerEvents(
                new AnimalInteractionListener(config, geneticsService, welfareStore, veterinaryService, remedyItems), this);
        getServer().getPluginManager().registerEvents(new AnimalDamageListener(config, veterinaryService), this);
        getServer().getPluginManager().registerEvents(new ShearingWelfareListener(config, welfareStore), this);
        getServer().getPluginManager().registerEvents(new BrewListener(config, remedyItems), this);

        var command = getCommand("nexushusbandry");
        if (command != null) {
            command.setExecutor(new HusbandryCommand(
                    genomeStore, welfareStore, veterinaryStore, pedigreeStore, hallOfFame, this::reload));
        }

        getLogger().info("NexusHusbandry enabled -- animal breeding and husbandry have been overhauled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NexusHusbandry disabled.");
    }

    private void reload() {
        reloadConfig();
        config.load(getConfig());
    }
}
