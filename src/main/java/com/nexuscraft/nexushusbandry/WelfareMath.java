package com.nexuscraft.nexushusbandry;

/**
 * Pure welfare math. Happiness is tracked as a lazily-decayed value rather than a scheduled
 * world-scanning tick loop -- this stub sandbox's own {@code World#getNearbyEntities}/{@code
 * World#getEntities} always report an empty, untracked world (see those methods' own comments),
 * so a periodic "scan every loaded animal" service would be a guaranteed no-op here and an
 * unverifiable guess for a real server. Lazy decay sidesteps that entirely: every real touchpoint
 * this plugin already has with a specific animal (a player feeding it, shearing it, it taking
 * damage, a breeding attempt) recomputes that one animal's current happiness from how much real
 * wall-clock time has passed since it was last computed, via {@link #decayedHappiness}, then
 * stores the fresh value and timestamp right back (see {@code WelfareStore#effectiveHappiness}).
 * No animal's happiness is ever "forgotten" between visits -- it just isn't continuously
 * recomputed for animals nobody is currently interacting with, which is also exactly how a real
 * player would describe an animal's welfare: unknown and unchanging until you go check on it.
 */
final class WelfareMath {

    private WelfareMath() {
    }

    static double clamp(double value, double floor, double max) {
        return Math.max(floor, Math.min(max, value));
    }

    /** {@code decayPerMinute} is happiness lost per real minute elapsed; never decays below
     *  {@code floor} (a neglected animal gets miserable, not literally zero and inert). */
    static double decayedHappiness(double current, long elapsedMillis, double decayPerMinute, double floor) {
        if (elapsedMillis <= 0) {
            return clamp(current, floor, 100.0);
        }
        double minutesElapsed = elapsedMillis / 60000.0;
        return clamp(current - minutesElapsed * decayPerMinute, floor, 100.0);
    }

    static double adjusted(double current, double delta) {
        return clamp(current + delta, 0.0, 100.0);
    }

    static boolean breedingAllowed(double happiness, double breedingThreshold) {
        return happiness >= breedingThreshold;
    }

    /** A happier animal resists disease better -- this plugin's own cross-system link between the
     *  welfare bucket and the veterinary bucket (see {@code DiseaseMath#infectionChance}'s own
     *  comment for the other side of it). Returns a multiplier in {@code (0, 1]}: {@code 1.0} at
     *  zero happiness (no resistance bonus at all), down to {@code 1.0 - maxResistance} at full
     *  happiness. */
    static double infectionChanceMultiplier(double happiness, double maxResistance) {
        double resistance = (happiness / 100.0) * clamp(maxResistance, 0.0, 1.0);
        return 1.0 - resistance;
    }
}
