package com.nexuscraft.nexushusbandry;

/**
 * Pure disease/injury math -- same "no live Bukkit types, fully seeded-{@code Random} testable"
 * convention as {@link GenomeMath}/{@link WelfareMath}. {@link #trySpread} is deliberately written
 * to take an explicit list of nearby animals rather than ever calling {@code World#getNearbyEntities}
 * itself -- this sandbox's own stub of that method always reports an empty, untracked world (see
 * that method's own comment), so keeping the actual contagion math in a plain, list-taking pure
 * function is what lets {@code NexusHusbandryLogicTest} genuinely exercise it against a
 * hand-built list, rather than it being an unverifiable guess that would only ever get exercised
 * for the first time against a real server.
 */
final class DiseaseMath {

    private DiseaseMath() {
    }

    static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /** {@code injury} and the welfare-driven {@code resistanceMultiplier} (see
     *  {@code WelfareMath#infectionChanceMultiplier}) both scale the same base roll: an injured,
     *  unhappy animal is meaningfully more likely to come down sick than a healthy, content one. */
    static double infectionChance(double baseChance, double injury, double injuryRiskMultiplier,
                                   double resistanceMultiplier) {
        double injuryFactor = 1.0 + clamp01(injury) * injuryRiskMultiplier;
        return clamp01(baseChance * injuryFactor * resistanceMultiplier);
    }

    static double progressedSeverity(double currentSeverity, long elapsedMillis, double progressionPerMinute) {
        if (elapsedMillis <= 0) {
            return clamp01(currentSeverity);
        }
        double minutesElapsed = elapsedMillis / 60000.0;
        return clamp01(currentSeverity + minutesElapsed * progressionPerMinute);
    }

    static double healedInjury(double currentInjury, long elapsedMillis, double healPerMinute) {
        if (elapsedMillis <= 0) {
            return clamp01(currentInjury);
        }
        double minutesElapsed = elapsedMillis / 60000.0;
        return clamp01(currentInjury - minutesElapsed * healPerMinute);
    }

    static boolean isLethal(double severity, double lethalThreshold) {
        return severity >= lethalThreshold;
    }

    /** Health lost to disease at a given severity -- scales linearly so a barely-sick animal
     *  loses almost nothing while a fully progressed case genuinely threatens it. */
    static double healthLostToSeverity(double severity, double maxHealthLoss) {
        return clamp01(severity) * maxHealthLoss;
    }

    /** One contagion roll from an already-sick {@code source} onto one nearby, currently-healthy
     *  animal: the chance scales with how far the source's own disease has progressed (a barely
     *  sick animal is barely contagious yet). */
    static boolean trySpreadOnce(double sourceSeverity, double baseSpreadChance, java.util.Random random) {
        double chance = clamp01(baseSpreadChance * clamp01(sourceSeverity));
        return random.nextDouble() < chance;
    }

    /** Rolls contagion from {@code source} against every entry of {@code nearbyHealthyAnimals},
     *  returning how many caught it. Takes a plain count rather than mutating the list in place
     *  so this stays trivially testable with a hand-built list of any size, including zero (the
     *  honest outcome every real call site gets in this sandbox today -- see class comment). */
    static int trySpread(double sourceSeverity, double baseSpreadChance, int nearbyHealthyAnimalCount,
                          java.util.Random random) {
        int infected = 0;
        for (int i = 0; i < nearbyHealthyAnimalCount; i++) {
            if (trySpreadOnce(sourceSeverity, baseSpreadChance, random)) {
                infected++;
            }
        }
        return infected;
    }
}
