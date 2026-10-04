package com.nexuscraft.nexushusbandry;

/**
 * One showcase-worthy birth, as displayed by {@code /nexushusbandry hof} -- see
 * {@link HallOfFameRegistry}'s own comment for why this is in-memory only, not PDC/disk-backed.
 * {@code breederName} is {@code null} when the birth wasn't directly caused by a player feeding
 * the parents (real {@code EntityBreedEvent#getBreeder()} is legitimately nullable -- see that
 * event's own stub comment).
 */
record HallOfFameEntry(LivestockSpecies species, int generation, double qualityScore,
                        boolean primeSpecimen, String breederName, long recordedAtMillis) {
}
