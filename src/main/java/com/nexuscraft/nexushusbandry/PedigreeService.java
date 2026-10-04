package com.nexuscraft.nexushusbandry;

import org.bukkit.entity.Animals;

import java.util.Random;

/**
 * Orchestrates everything this plugin's own pedigree/rare-mutation bucket does at the moment of
 * a real {@code EntityBreedEvent}: computes the child's real generation number, rolls the rare
 * mutation chance on top of {@link GeneticsService}'s already-inherited genome, decides whether
 * the result is a showcase-worthy "prime specimen," records the full lineage to {@link
 * PedigreeStore}, and -- for a prime specimen -- logs it to the server-wide {@link
 * HallOfFameRegistry}. Called once per real breeding from {@code BreedingListener}, after
 * {@link GeneticsService} has already produced the plain inherited genome; this class only ever
 * adds the rare-mutation layer on top; it never recomputes inheritance itself.
 */
final class PedigreeService {

    private final HusbandryConfig config;
    private final PedigreeStore pedigreeStore;
    private final HallOfFameRegistry hallOfFame;

    PedigreeService(HusbandryConfig config, PedigreeStore pedigreeStore, HallOfFameRegistry hallOfFame) {
        this.config = config;
        this.pedigreeStore = pedigreeStore;
        this.hallOfFame = hallOfFame;
    }

    /** Returns the child's final genome (the inherited genome, possibly with one gene replaced
     *  by the rare-mutation roll) -- the caller is responsible for actually applying it via
     *  {@link GeneticsService#applyToEntity} and persisting it via {@link GenomeStore}; this
     *  method's own job is lineage and the showcase decision, not genome application. */
    Genome resolveChildGenomeAndRecord(Animals child, Animals mother, Animals father, Genome inheritedGenome,
                                        String breederName, Random random) {
        int generation = PedigreeMath.nextGeneration(pedigreeStore.generationOf(mother), pedigreeStore.generationOf(father));

        Genome finalGenome = GenomeMath.rollRareMutation(
                inheritedGenome, config.rareMutationChance, config.rareMutationFloor, random);
        boolean rareMutationHit = !finalGenome.equals(inheritedGenome);

        double quality = finalGenome.qualityScore();
        boolean primeSpecimen = PedigreeMath.isPrimeSpecimen(quality, config.primeSpecimenQualityThreshold, rareMutationHit);

        pedigreeStore.recordParentage(child, mother, father, generation, primeSpecimen);

        if (primeSpecimen) {
            LivestockSpecies species = LivestockSpecies.of(child);
            if (species != null) {
                hallOfFame.record(new HallOfFameEntry(
                        species, generation, quality, true, breederName, System.currentTimeMillis()));
            }
        }

        return finalGenome;
    }
}
