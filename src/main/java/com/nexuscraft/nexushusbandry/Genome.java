package com.nexuscraft.nexushusbandry;

/**
 * One animal's full set of {@link Trait} gene values, each clamped {@code 0.0-1.0}. Plain,
 * immutable data -- all the actual inheritance/mutation math lives in {@link GenomeMath}, same
 * "pure math in its own class, a small immutable holder next to it" split this family already
 * uses elsewhere (compare {@code MomentumMath} next to the player-state classes it operates on).
 * {@link #encode()}/{@link #decode(String)} are this plugin's own serialization for
 * {@link GenomeStore}'s single packed-string PDC value -- deliberately one PDC entry for all four
 * genes rather than four separate keys, so a partially-written genome (a crash mid-write) can
 * never leave three genes updated and one stale.
 */
record Genome(double vitality, double speed, double strength, double fertility) {

    Genome {
        vitality = GenomeMath.clamp01(vitality);
        speed = GenomeMath.clamp01(speed);
        strength = GenomeMath.clamp01(strength);
        fertility = GenomeMath.clamp01(fertility);
    }

    double get(Trait trait) {
        return switch (trait) {
            case VITALITY -> vitality;
            case SPEED -> speed;
            case STRENGTH -> strength;
            case FERTILITY -> fertility;
        };
    }

    /** This plugin's own "how good a specimen is this, overall" number -- the plain average of
     *  all four genes. Used by {@link GeneticsService} for breeding-outcome flavor and reused by
     *  {@code PedigreeService} to help decide whether a newborn qualifies as a rare, standout
     *  "prime specimen" (see that class's own comment) -- one formula, not two independently
     *  tuned ones that could drift apart. */
    double qualityScore() {
        return (vitality + speed + strength + fertility) / 4.0;
    }

    String encode() {
        return vitality + ":" + speed + ":" + strength + ":" + fertility;
    }

    /** Returns {@code null} for a blank/malformed value rather than throwing -- a corrupt or
     *  hand-edited PDC string is this plugin's own data, not real server data, but treating a
     *  read failure as "no genome yet" (same as a brand-new animal) is the safer failure mode
     *  than crashing a listener over it. */
    static Genome decode(String packed) {
        if (packed == null || packed.isBlank()) {
            return null;
        }
        String[] parts = packed.split(":", -1);
        if (parts.length != 4) {
            return null;
        }
        try {
            return new Genome(
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
