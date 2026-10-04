package com.nexuscraft.nexushusbandry;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/** Parses config.yml's tunables. Same "config drives shared numbers, code holds the fixed shape"
 *  split this whole family already uses (compare {@code MomentumConfig}) -- the fixed shape here
 *  is the {@link Trait}/{@link Disease} catalogs themselves, which stay in code since they're
 *  structural, not tunable numbers. */
final class HusbandryConfig {

    private final Plugin plugin;

    boolean enabled = true;

    // -- Genetics (Genome / GenomeMath / GeneticsService) --
    double wildGenomeCenter = 0.5;
    double wildGenomeSpread = 0.3;
    double mutationChance = 0.08;
    double mutationStrength = 0.12;
    /** Real, symmetric {@code ADD_SCALAR} spread applied around vanilla's own attribute default
     *  -- see {@link GeneticsService}'s own comment for the exact formula. */
    double traitAttributeScalarRange = 0.5;
    double twinBaseChance = 0.02;
    double twinFertilityBonusChance = 0.18;

    // -- Welfare (WelfareMath / WelfareStore) --
    double happinessDecayPerMinute = 0.5;
    double happinessFloor = 10.0;
    double feedHappinessBoost = 15.0;
    double shearHappinessPenalty = 5.0;
    double woolRegrowHappinessBoost = 4.0;
    double breedingHappinessThreshold = 40.0;
    /** A modest happiness boost both real parents get after a successful real breeding --
     *  parenthood is good for morale too. */
    double breedingHappinessBoost = 8.0;
    /** Max fraction of infection-chance resistance a fully happy ({@code 100}) animal gets. */
    double happinessDiseaseResistance = 0.6;

    // -- Veterinary care (DiseaseMath / VeterinaryStore / VeterinaryService) --
    double baseInfectionChance = 0.01;
    double injuryPerDamagePoint = 0.05;
    double injuryInfectionRiskMultiplier = 2.0;
    double injuryHealPerMinute = 0.02;
    double initialDiseaseSeverity = 0.1;
    double diseaseProgressionPerMinute = 0.05;
    double lethalSeverityThreshold = 0.75;
    double maxDiseaseHealthLoss = 12.0;
    /** Disease alone never finishes an animal off -- see {@code VeterinaryService#applyHealthToll}. */
    double diseaseHealthFloor = 2.0;
    int symptomDurationTicks = 200;
    double cureHappinessBoost = 10.0;
    double cureInjuryHeal = 0.3;
    double contagionBaseChance = 0.15;

    // -- Pedigree & rare mutations (GenomeMath#rollRareMutation / PedigreeMath / PedigreeService) --
    double rareMutationChance = 0.03;
    double rareMutationFloor = 0.9;
    double primeSpecimenQualityThreshold = 0.8;
    int hallOfFameSize = 10;

    HusbandryConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    void load(FileConfiguration c) {
        Logger log = plugin != null ? plugin.getLogger() : null;

        enabled = c.getBoolean("enabled", enabled);

        ConfigurationSection genetics = c.getConfigurationSection("genetics");
        if (genetics != null) {
            wildGenomeCenter = clamp01(genetics.getDouble("wild-genome-center", wildGenomeCenter));
            wildGenomeSpread = clampNonNegative(genetics.getDouble("wild-genome-spread", wildGenomeSpread));
            mutationChance = clamp01(genetics.getDouble("mutation-chance", mutationChance));
            mutationStrength = clampNonNegative(genetics.getDouble("mutation-strength", mutationStrength));
            traitAttributeScalarRange = clampNonNegative(
                    genetics.getDouble("trait-attribute-scalar-range", traitAttributeScalarRange));
            twinBaseChance = clamp01(genetics.getDouble("twin-base-chance", twinBaseChance));
            twinFertilityBonusChance = clamp01(genetics.getDouble("twin-fertility-bonus-chance", twinFertilityBonusChance));
        }

        ConfigurationSection welfare = c.getConfigurationSection("welfare");
        if (welfare != null) {
            happinessDecayPerMinute = clampNonNegative(welfare.getDouble("happiness-decay-per-minute", happinessDecayPerMinute));
            happinessFloor = clamp(welfare.getDouble("happiness-floor", happinessFloor), 0.0, 100.0);
            feedHappinessBoost = clampNonNegative(welfare.getDouble("feed-happiness-boost", feedHappinessBoost));
            shearHappinessPenalty = clampNonNegative(welfare.getDouble("shear-happiness-penalty", shearHappinessPenalty));
            woolRegrowHappinessBoost = clampNonNegative(welfare.getDouble("wool-regrow-happiness-boost", woolRegrowHappinessBoost));
            breedingHappinessThreshold = clamp(welfare.getDouble("breeding-happiness-threshold", breedingHappinessThreshold), 0.0, 100.0);
            breedingHappinessBoost = clampNonNegative(welfare.getDouble("breeding-happiness-boost", breedingHappinessBoost));
            happinessDiseaseResistance = clamp01(welfare.getDouble("happiness-disease-resistance", happinessDiseaseResistance));
        }

        ConfigurationSection vet = c.getConfigurationSection("veterinary");
        if (vet != null) {
            baseInfectionChance = clamp01(vet.getDouble("base-infection-chance", baseInfectionChance));
            injuryPerDamagePoint = clampNonNegative(vet.getDouble("injury-per-damage-point", injuryPerDamagePoint));
            injuryInfectionRiskMultiplier = clampNonNegative(vet.getDouble("injury-infection-risk-multiplier", injuryInfectionRiskMultiplier));
            injuryHealPerMinute = clampNonNegative(vet.getDouble("injury-heal-per-minute", injuryHealPerMinute));
            initialDiseaseSeverity = clamp01(vet.getDouble("initial-disease-severity", initialDiseaseSeverity));
            diseaseProgressionPerMinute = clampNonNegative(vet.getDouble("disease-progression-per-minute", diseaseProgressionPerMinute));
            lethalSeverityThreshold = clamp01(vet.getDouble("lethal-severity-threshold", lethalSeverityThreshold));
            maxDiseaseHealthLoss = clampNonNegative(vet.getDouble("max-disease-health-loss", maxDiseaseHealthLoss));
            diseaseHealthFloor = clampNonNegative(vet.getDouble("disease-health-floor", diseaseHealthFloor));
            symptomDurationTicks = Math.max(1, vet.getInt("symptom-duration-ticks", symptomDurationTicks));
            cureHappinessBoost = clampNonNegative(vet.getDouble("cure-happiness-boost", cureHappinessBoost));
            cureInjuryHeal = clampNonNegative(vet.getDouble("cure-injury-heal", cureInjuryHeal));
            contagionBaseChance = clamp01(vet.getDouble("contagion-base-chance", contagionBaseChance));
        }

        ConfigurationSection pedigree = c.getConfigurationSection("pedigree");
        if (pedigree != null) {
            rareMutationChance = clamp01(pedigree.getDouble("rare-mutation-chance", rareMutationChance));
            rareMutationFloor = clamp01(pedigree.getDouble("rare-mutation-floor", rareMutationFloor));
            primeSpecimenQualityThreshold = clamp01(pedigree.getDouble("prime-specimen-quality-threshold", primeSpecimenQualityThreshold));
            hallOfFameSize = Math.max(1, pedigree.getInt("hall-of-fame-size", hallOfFameSize));
        }

        if (!enabled && log != null) {
            log.info("NexusHusbandry is disabled via config.yml.");
        }
    }

    private static double clampNonNegative(double value) {
        return Math.max(0.0, value);
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
