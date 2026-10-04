package com.nexuscraft.nexushusbandry;

import java.util.Random;

/**
 * Standalone pure-logic test suite -- a plain {@code main} method, no JUnit, no Bukkit runtime.
 * Same convention as every other sibling in this family -- everything that talks to a live
 * Bukkit server (listeners, services that read a real {@code Animals}/{@code Player}/{@code
 * Event}) is left to a real {@code mvn clean package} build and real server testing. This suite
 * covers the plugin's actual math: genome inheritance/mutation/encoding, welfare decay and
 * breeding-gate thresholds, disease infection/progression/contagion, and pedigree generation and
 * prime-specimen determination.
 */
public final class NexusHusbandryLogicTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testGenomeClamping();
        testGenomeEncodeDecodeRoundTrip();
        testGenomeDecodeRejectsGarbage();
        testGenomeQualityScore();
        testMutateRespectsChance();
        testMutateStaysInBounds();
        testInheritIsAverageWhenNoMutation();
        testWildGenomeWithinSpread();
        testRollRareMutationReplacesExactlyOneGene();
        testRollRareMutationNoOpBelowChance();

        testWelfareClamp();
        testDecayedHappinessNeverBelowFloor();
        testDecayedHappinessZeroElapsedIsNoOp();
        testAdjustedHappinessClampsToRange();
        testBreedingAllowedThreshold();
        testInfectionChanceMultiplierRange();

        testInfectionChanceScalesWithInjuryAndResistance();
        testProgressedSeverityMonotonic();
        testHealedInjuryNeverNegative();
        testIsLethalThreshold();
        testHealthLostToSeverityScaling();
        testTrySpreadZeroWhenNoHealthyNeighbors();
        testTrySpreadAllInfectedAtCertainChance();

        testNextGenerationTakesMax();
        testPrimeSpecimenByQualityOrRareMutation();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // -- Genome / GenomeMath --

    private static void testGenomeClamping() {
        Genome genome = new Genome(-0.5, 1.5, 0.5, Double.NaN);
        expect(genome.vitality() == 0.0, "a below-zero gene is clamped to 0 by the record's own constructor");
        expect(genome.speed() == 1.0, "an above-one gene is clamped to 1 by the record's own constructor");
        expect(genome.strength() == 0.5, "an already-valid gene passes through unchanged");
        expect(genome.fertility() == 0.0, "NaN is treated as 0 rather than propagating");
    }

    private static void testGenomeEncodeDecodeRoundTrip() {
        Genome original = new Genome(0.1, 0.4, 0.6, 0.9);
        Genome decoded = Genome.decode(original.encode());
        expect(decoded != null, "a genome encoded then decoded is never null");
        expect(decoded.equals(original), "a genome round-trips through encode/decode unchanged");
    }

    private static void testGenomeDecodeRejectsGarbage() {
        expect(Genome.decode(null) == null, "decoding null returns null rather than throwing");
        expect(Genome.decode("") == null, "decoding a blank string returns null rather than throwing");
        expect(Genome.decode("not:a:valid:genome") == null, "decoding non-numeric parts returns null rather than throwing");
        expect(Genome.decode("0.1:0.2:0.3") == null, "decoding the wrong number of parts returns null rather than throwing");
    }

    private static void testGenomeQualityScore() {
        expect(new Genome(1.0, 1.0, 1.0, 1.0).qualityScore() == 1.0, "a flawless genome scores exactly 1.0");
        expect(new Genome(0.0, 0.0, 0.0, 0.0).qualityScore() == 0.0, "a runt genome scores exactly 0.0");
        expect(new Genome(1.0, 0.0, 1.0, 0.0).qualityScore() == 0.5, "quality score is the plain average of all four genes");
    }

    private static void testMutateRespectsChance() {
        Random neverMutates = new Random() {
            @Override
            public double nextDouble() {
                return 0.999;
            }
        };
        expect(GenomeMath.mutate(0.5, 0.1, 0.3, neverMutates) == 0.5,
                "a mutation roll at or above the configured chance leaves the gene unchanged");
    }

    private static void testMutateStaysInBounds() {
        Random alwaysMutatesUp = new Random() {
            private boolean first = true;

            @Override
            public double nextDouble() {
                if (first) {
                    first = false;
                    return 0.0; // passes the chance check
                }
                return 1.0; // maximal positive delta
            }
        };
        double mutated = GenomeMath.mutate(0.95, 1.0, 0.5, alwaysMutatesUp);
        expect(mutated <= 1.0, "a mutation that would overshoot 1.0 is clamped back down to it");
    }

    private static void testInheritIsAverageWhenNoMutation() {
        Random neverMutates = new Random() {
            @Override
            public double nextDouble() {
                return 0.999;
            }
        };
        double inherited = GenomeMath.inheritGene(0.2, 0.8, 0.1, 0.3, neverMutates);
        expect(inherited == 0.5, "with no mutation roll, an inherited gene is exactly the average of both parents");
    }

    private static void testWildGenomeWithinSpread() {
        Random random = new Random(42);
        for (int i = 0; i < 50; i++) {
            Genome wild = GenomeMath.wildGenome(0.5, 0.2, random);
            expect(wild.vitality() >= 0.3 && wild.vitality() <= 0.7,
                    "a wild genome's gene stays within the configured spread around its center");
        }
    }

    private static void testRollRareMutationReplacesExactlyOneGene() {
        Random alwaysHits = new Random() {
            private int call = 0;

            @Override
            public double nextDouble() {
                call++;
                return call == 1 ? 0.0 : 0.99; // passes the rare-mutation chance, rolls near-max exceptional value
            }

            @Override
            public int nextInt(int bound) {
                return 2; // always picks STRENGTH (index 2)
            }
        };
        Genome original = new Genome(0.3, 0.3, 0.3, 0.3);
        Genome mutated = GenomeMath.rollRareMutation(original, 1.0, 0.9, alwaysHits);
        expect(mutated.strength() > original.strength(), "a rare-mutation hit replaces the chosen gene with an exceptional value");
        expect(mutated.vitality() == original.vitality() && mutated.speed() == original.speed()
                        && mutated.fertility() == original.fertility(),
                "a rare-mutation hit leaves every other gene completely untouched");
    }

    private static void testRollRareMutationNoOpBelowChance() {
        Random neverHits = new Random() {
            @Override
            public double nextDouble() {
                return 0.999;
            }
        };
        Genome original = new Genome(0.3, 0.3, 0.3, 0.3);
        Genome result = GenomeMath.rollRareMutation(original, 0.05, 0.9, neverHits);
        expect(result.equals(original), "missing the rare-mutation roll returns the genome completely unchanged");
    }

    // -- WelfareMath --

    private static void testWelfareClamp() {
        expect(WelfareMath.clamp(150, 10, 100) == 100, "clamp never returns above max");
        expect(WelfareMath.clamp(-5, 10, 100) == 10, "clamp never returns below the floor");
    }

    private static void testDecayedHappinessNeverBelowFloor() {
        double decayed = WelfareMath.decayedHappiness(20.0, 10_000_000L, 5.0, 10.0);
        expect(decayed == 10.0, "happiness decay never drops an animal below its configured floor, however long the elapsed time");
    }

    private static void testDecayedHappinessZeroElapsedIsNoOp() {
        expect(WelfareMath.decayedHappiness(55.0, 0, 5.0, 10.0) == 55.0,
                "zero elapsed time leaves happiness completely unchanged");
    }

    private static void testAdjustedHappinessClampsToRange() {
        expect(WelfareMath.adjusted(95.0, 20.0) == 100.0, "a happiness boost never overshoots 100");
        expect(WelfareMath.adjusted(5.0, -20.0) == 0.0, "a happiness penalty never undershoots 0");
    }

    private static void testBreedingAllowedThreshold() {
        expect(WelfareMath.breedingAllowed(40.0, 40.0), "happiness exactly at the breeding threshold is allowed");
        expect(!WelfareMath.breedingAllowed(39.9, 40.0), "happiness just under the breeding threshold is not allowed");
    }

    private static void testInfectionChanceMultiplierRange() {
        expect(WelfareMath.infectionChanceMultiplier(0.0, 0.6) == 1.0,
                "zero happiness gives no disease-resistance discount at all");
        expect(WelfareMath.infectionChanceMultiplier(100.0, 0.6) == 0.4,
                "full happiness applies the full configured resistance discount");
    }

    // -- DiseaseMath --

    private static void testInfectionChanceScalesWithInjuryAndResistance() {
        double healthy = DiseaseMath.infectionChance(0.1, 0.0, 2.0, 1.0);
        double injured = DiseaseMath.infectionChance(0.1, 1.0, 2.0, 1.0);
        expect(injured > healthy, "a fully injured animal has a strictly higher infection chance than an uninjured one");

        double resistant = DiseaseMath.infectionChance(0.1, 0.0, 2.0, 0.4);
        expect(resistant < healthy, "a disease-resistant (happy) animal has a strictly lower infection chance");
    }

    private static void testProgressedSeverityMonotonic() {
        double after1Minute = DiseaseMath.progressedSeverity(0.1, 60_000L, 0.05);
        double after2Minutes = DiseaseMath.progressedSeverity(0.1, 120_000L, 0.05);
        expect(after2Minutes > after1Minute, "untreated disease severity strictly increases with more elapsed time");
        expect(DiseaseMath.progressedSeverity(0.99, 10_000_000L, 0.05) == 1.0,
                "severity never exceeds 1.0 no matter how long it's left untreated");
    }

    private static void testHealedInjuryNeverNegative() {
        double healed = DiseaseMath.healedInjury(0.1, 10_000_000L, 0.05);
        expect(healed == 0.0, "injury healing never goes negative, however long the elapsed time");
    }

    private static void testIsLethalThreshold() {
        expect(DiseaseMath.isLethal(0.75, 0.75), "severity exactly at the lethal threshold counts as lethal");
        expect(!DiseaseMath.isLethal(0.74, 0.75), "severity just under the lethal threshold does not count as lethal");
    }

    private static void testHealthLostToSeverityScaling() {
        expect(DiseaseMath.healthLostToSeverity(1.0, 12.0) == 12.0, "full severity applies the full configured health loss");
        expect(DiseaseMath.healthLostToSeverity(0.5, 12.0) == 6.0, "half severity applies exactly half the configured health loss");
        expect(DiseaseMath.healthLostToSeverity(0.0, 12.0) == 0.0, "zero severity applies no health loss at all");
    }

    private static void testTrySpreadZeroWhenNoHealthyNeighbors() {
        int infected = DiseaseMath.trySpread(0.9, 0.9, 0, new Random());
        expect(infected == 0, "contagion can't infect a neighbor that doesn't exist, however high the chance");
    }

    private static void testTrySpreadAllInfectedAtCertainChance() {
        Random alwaysHits = new Random() {
            @Override
            public double nextDouble() {
                return 0.0;
            }
        };
        int infected = DiseaseMath.trySpread(1.0, 1.0, 5, alwaysHits);
        expect(infected == 5, "a certain spread chance against five healthy neighbors infects all five");
    }

    // -- PedigreeMath --

    private static void testNextGenerationTakesMax() {
        expect(PedigreeMath.nextGeneration(0, 0) == 1, "two founders (generation 0) produce a generation-1 child");
        expect(PedigreeMath.nextGeneration(3, 1) == 4,
                "a child's generation is one past the MORE advanced parent, not the less advanced one");
    }

    private static void testPrimeSpecimenByQualityOrRareMutation() {
        expect(PedigreeMath.isPrimeSpecimen(0.9, 0.8, false),
                "a genome clearing the quality threshold on its own merits is a prime specimen");
        expect(!PedigreeMath.isPrimeSpecimen(0.5, 0.8, false),
                "a genome below the quality threshold with no rare mutation is not a prime specimen");
        expect(PedigreeMath.isPrimeSpecimen(0.1, 0.8, true),
                "a rare-mutation hit alone qualifies as a prime specimen regardless of overall quality");
    }

    private static void expect(boolean condition, String description) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + description);
        } else {
            failed++;
            System.out.println("FAIL: " + description);
        }
    }
}
