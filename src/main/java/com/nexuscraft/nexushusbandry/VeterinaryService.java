package com.nexuscraft.nexushusbandry;

import org.bukkit.entity.Animals;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Random;

/**
 * Ties {@link VeterinaryStore}'s PDC state to real, visible consequences: a sick animal carries a
 * real {@link PotionEffect} (see {@code LivingEntity#addPotionEffect}'s own "moved up from
 * Player" fix) for as long as it's diseased, an untreated case that progresses past a configured
 * severity genuinely costs the animal real health (through {@code LivingEntity#setHealth}, never
 * all the way to death -- see {@link #applyHealthToll}'s own comment on why), and a real {@code
 * EntityDamageEvent} raises injury, which in turn raises infection risk (through {@link
 * WelfareMath#infectionChanceMultiplier} and {@link DiseaseMath#infectionChance} together --
 * genetics, welfare, and veterinary care all feed one real outcome here, not three independent
 * systems that happen to share a plugin).
 */
final class VeterinaryService {

    private final HusbandryConfig config;
    private final VeterinaryStore veterinaryStore;
    private final WelfareStore welfareStore;

    VeterinaryService(HusbandryConfig config, VeterinaryStore veterinaryStore, WelfareStore welfareStore) {
        this.config = config;
        this.veterinaryStore = veterinaryStore;
        this.welfareStore = welfareStore;
    }

    void recordInjury(Animals animal, double damage, long nowMillis) {
        veterinaryStore.addInjury(animal, damage * config.injuryPerDamagePoint, nowMillis);
    }

    /** Rolled on real interactions with a currently-healthy animal (never on an already-sick
     *  one -- real vanilla doesn't re-infect an already-sick creature with the same illness
     *  twice over). */
    void rollInfection(Animals animal, Random random, long nowMillis) {
        if (veterinaryStore.diseaseOf(animal) != Disease.NONE) {
            return;
        }
        double injury = veterinaryStore.effectiveInjury(animal, nowMillis);
        double happiness = welfareStore.effectiveHappiness(animal, nowMillis);
        double resistance = WelfareMath.infectionChanceMultiplier(happiness, config.happinessDiseaseResistance);
        double chance = DiseaseMath.infectionChance(config.baseInfectionChance, injury,
                config.injuryInfectionRiskMultiplier, resistance);
        if (random.nextDouble() < chance) {
            LivestockSpecies species = LivestockSpecies.of(animal);
            if (species != null) {
                infect(animal, Disease.likelyFor(species), nowMillis);
            }
        }
    }

    void infect(Animals animal, Disease disease, long nowMillis) {
        veterinaryStore.infect(animal, disease, nowMillis);
        PotionEffectType symptom = disease.symptom();
        if (symptom != null) {
            animal.addPotionEffect(new PotionEffect(symptom, config.symptomDurationTicks, 0));
        }
    }

    /** Advances a sick animal's illness and applies its real-world toll -- called on any real
     *  touchpoint this plugin already has with the animal (same lazy-recompute convention as
     *  {@link WelfareStore}). Refreshes the symptom effect too, since this stub's potion effects
     *  don't simulate real tick-by-tick expiry (see {@code LivingEntity}'s own comment) and a
     *  long-untouched sick animal could otherwise have its real effect quietly lapse. */
    void progress(Animals animal, long nowMillis) {
        Disease disease = veterinaryStore.diseaseOf(animal);
        if (disease == Disease.NONE) {
            return;
        }
        double severity = veterinaryStore.effectiveSeverity(animal, nowMillis);
        PotionEffectType symptom = disease.symptom();
        if (symptom != null) {
            animal.addPotionEffect(new PotionEffect(symptom, config.symptomDurationTicks, 0));
        }
        if (DiseaseMath.isLethal(severity, config.lethalSeverityThreshold)) {
            applyHealthToll(animal, severity);
        }
    }

    /** Real disease in this plugin can ravage an animal but deliberately never finishes it off
     *  outright -- health is floored at {@link HusbandryConfig#diseaseHealthFloor}, never {@code
     *  0}. An actual death from there is left to real vanilla (starvation, a predator, a
     *  player) rather than this plugin silently killing an entity a player never saw take a real
     *  hit; see README's "known, deliberate design choices" for the full reasoning. */
    private void applyHealthToll(Animals animal, double severity) {
        double lost = DiseaseMath.healthLostToSeverity(severity, config.maxDiseaseHealthLoss);
        double updated = Math.max(config.diseaseHealthFloor, animal.getHealth() - lost);
        animal.setHealth(updated);
    }

    /** Clears the disease entirely, removes its symptom, and gives the animal a modest welfare
     *  and injury-healing boost -- a cured animal doesn't just stop being sick, it genuinely
     *  feels better, the real payoff for a player who bothered to treat it. */
    /** Returns the disease that was cured ({@link Disease#NONE} if the animal wasn't sick to
     *  begin with -- a caller can use that to tell "cured" from "wasn't sick" without a separate
     *  {@link #isSick} check first). */
    Disease cure(Animals animal, long nowMillis) {
        Disease disease = veterinaryStore.diseaseOf(animal);
        PotionEffectType symptom = disease.symptom();
        veterinaryStore.cure(animal, nowMillis);
        if (symptom != null) {
            animal.removePotionEffect(symptom);
        }
        welfareStore.adjust(animal, config.cureHappinessBoost, nowMillis);
        veterinaryStore.addInjury(animal, -config.cureInjuryHeal, nowMillis);
        return disease;
    }

    boolean isSick(Animals animal) {
        return veterinaryStore.diseaseOf(animal) != Disease.NONE;
    }

    /** Rolls contagion from {@code source} onto up to {@code nearbyHealthyAnimals.size()} other
     *  animals, infecting however many of them the roll hits (see {@link DiseaseMath#trySpread}'s
     *  own comment on why this takes an explicit list rather than ever querying the world
     *  itself). In this sandbox, every real call site hands this an empty list (this plugin's own
     *  stub of {@code World#getNearbyEntities} always is -- see that method's own comment), so
     *  this never actually infects anything here; on a real server with a real nearby-entity
     *  query wired in, the same method genuinely spreads disease through a crowded pen. */
    int trySpread(Animals source, List<Animals> nearbyHealthyAnimals, Random random, long nowMillis) {
        double severity = veterinaryStore.effectiveSeverity(source, nowMillis);
        int infected = DiseaseMath.trySpread(severity, config.contagionBaseChance, nearbyHealthyAnimals.size(), random);
        LivestockSpecies sourceSpecies = LivestockSpecies.of(source);
        if (sourceSpecies == null) {
            return 0;
        }
        Disease disease = Disease.likelyFor(sourceSpecies);
        for (int i = 0; i < infected && i < nearbyHealthyAnimals.size(); i++) {
            infect(nearbyHealthyAnimals.get(i), disease, nowMillis);
        }
        return infected;
    }
}
