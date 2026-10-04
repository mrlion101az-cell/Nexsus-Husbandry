package com.nexuscraft.nexushusbandry;

import java.util.Random;

/**
 * Pure genetics math -- no {@code Entity}, no {@code Event}, nothing that touches a live Bukkit
 * runtime, so every formula here is directly exercised by {@code NexusHusbandryLogicTest} with a
 * seeded {@link Random}, same "pure math class, independently testable" convention as
 * {@code MomentumMath}/{@code StaminaMath} in NexusMomentum.
 *
 * <p>The inheritance model: a child's gene is the average of its two parents' genes, then rolled
 * for mutation independently per gene -- a real, simplified version of real-world polygenic
 * blending inheritance (deliberately not a full dominant/recessive allele-pair model; see
 * README's "known, deliberate design choices" for why that was judged out of scope for a
 * Minecraft plugin). A wild-caught/freshly-spawned animal with no recorded parents instead gets a
 * {@link #wildGenome} -- randomized around a configurable center, not a flat {@code 0.5} for
 * every single one, so the wild population itself has real variance for players to selectively
 * breed up from.
 */
final class GenomeMath {

    private GenomeMath() {
    }

    static double clamp01(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    /** One gene's mutation roll: with probability {@code mutationChance}, nudges {@code gene} by
     *  a random amount in {@code [-mutationStrength, +mutationStrength]}; otherwise returns it
     *  unchanged. Always clamped to {@code [0, 1]} either way. */
    static double mutate(double gene, double mutationChance, double mutationStrength, Random random) {
        if (random.nextDouble() >= mutationChance) {
            return clamp01(gene);
        }
        double delta = (random.nextDouble() * 2.0 - 1.0) * mutationStrength;
        return clamp01(gene + delta);
    }

    /** One gene's inherited value: the two parents' average, then an independent mutation roll. */
    static double inheritGene(double motherGene, double fatherGene, double mutationChance,
                               double mutationStrength, Random random) {
        double averaged = (motherGene + fatherGene) / 2.0;
        return mutate(averaged, mutationChance, mutationStrength, random);
    }

    static Genome inherit(Genome mother, Genome father, double mutationChance, double mutationStrength,
                           Random random) {
        return new Genome(
                inheritGene(mother.vitality(), father.vitality(), mutationChance, mutationStrength, random),
                inheritGene(mother.speed(), father.speed(), mutationChance, mutationStrength, random),
                inheritGene(mother.strength(), father.strength(), mutationChance, mutationStrength, random),
                inheritGene(mother.fertility(), father.fertility(), mutationChance, mutationStrength, random));
    }

    /** A fresh genome for an animal with no recorded parents (a wild spawn, or one that existed
     *  before this plugin was ever installed on the server). Each gene is independently
     *  randomized around {@code center} within {@code +/- spread}, not copied from a single roll
     *  -- a wild cow's vitality and speed aren't correlated with each other in real life either. */
    static Genome wildGenome(double center, double spread, Random random) {
        return new Genome(
                clamp01(center + (random.nextDouble() * 2.0 - 1.0) * spread),
                clamp01(center + (random.nextDouble() * 2.0 - 1.0) * spread),
                clamp01(center + (random.nextDouble() * 2.0 - 1.0) * spread),
                clamp01(center + (random.nextDouble() * 2.0 - 1.0) * spread));
    }

    /** This plugin's own rare-mutation roll, used by {@code PedigreeService} at the moment a
     *  child's genome is first assigned: independent of the per-gene mutation rolls above, a
     *  separate, much rarer roll that -- if it hits -- replaces one randomly-chosen gene outright
     *  with a near-perfect value instead of nudging it. This is what actually produces this
     *  plugin's own "rare mutation" showcase animals, not just an unusually lucky ordinary roll. */
    static Genome rollRareMutation(Genome genome, double rareMutationChance, double rareMutationFloor,
                                    Random random) {
        if (random.nextDouble() >= rareMutationChance) {
            return genome;
        }
        double exceptional = clamp01(rareMutationFloor + random.nextDouble() * (1.0 - rareMutationFloor));
        int which = random.nextInt(4);
        return switch (which) {
            case 0 -> new Genome(exceptional, genome.speed(), genome.strength(), genome.fertility());
            case 1 -> new Genome(genome.vitality(), exceptional, genome.strength(), genome.fertility());
            case 2 -> new Genome(genome.vitality(), genome.speed(), exceptional, genome.fertility());
            default -> new Genome(genome.vitality(), genome.speed(), genome.strength(), exceptional);
        };
    }
}
