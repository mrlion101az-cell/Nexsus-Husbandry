package com.nexuscraft.nexushusbandry;

/** Pure pedigree math -- generation numbering and the "is this a standout, showcase-worthy
 *  animal" determination, kept separate from {@link PedigreeStore}'s PDC I/O the same way every
 *  other bucket in this plugin splits pure math from storage. */
final class PedigreeMath {

    private PedigreeMath() {
    }

    /** A child's generation is always one past the more-advanced of its two parents -- so a
     *  generation-3 mother bred with a generation-1 father produces a generation-4 child, not a
     *  generation-2 one; lineage never resets just because one side of the pairing was less
     *  refined. */
    static int nextGeneration(int motherGeneration, int fatherGeneration) {
        return Math.max(motherGeneration, fatherGeneration) + 1;
    }

    /** An animal is a showcase-worthy "prime specimen" either because this breeding's rare
     *  mutation roll already hit (see {@code GenomeMath#rollRareMutation}), or because its
     *  overall genome quality independently cleared {@code qualityThreshold} on its own merits --
     *  either is a real, separate reason to be proud of the animal, so either alone qualifies. */
    static boolean isPrimeSpecimen(double qualityScore, double qualityThreshold, boolean rareMutationHit) {
        return rareMutationHit || qualityScore >= qualityThreshold;
    }
}
