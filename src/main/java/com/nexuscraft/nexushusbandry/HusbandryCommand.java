package com.nexuscraft.nexushusbandry;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;

/** {@code /nexushusbandry <inspect|hof|reload>} -- same "small, read-only status/admin surface"
 *  convention as every other sibling's own command (compare {@code MomentumCommand}). {@code
 *  inspect} rides on real {@code World#rayTraceEntities} to find the animal a player is actually
 *  looking at, rather than requiring the player to be holding right-click on it -- this stub's own
 *  always-{@code null} ray trace (see that method's own comment) means {@code inspect} always
 *  reports "nothing in your sights" in this sandbox, same honest limitation as every other
 *  "doesn't simulate a live world" caveat in this family; a real server's real ray trace genuinely
 *  resolves it. */
final class HusbandryCommand implements CommandExecutor {

    private static final double INSPECT_RANGE = 8.0;

    private final GenomeStore genomeStore;
    private final WelfareStore welfareStore;
    private final VeterinaryStore veterinaryStore;
    private final PedigreeStore pedigreeStore;
    private final HallOfFameRegistry hallOfFame;
    private final Runnable reloadAction;

    HusbandryCommand(GenomeStore genomeStore, WelfareStore welfareStore, VeterinaryStore veterinaryStore,
                      PedigreeStore pedigreeStore, HallOfFameRegistry hallOfFame, Runnable reloadAction) {
        this.genomeStore = genomeStore;
        this.welfareStore = welfareStore;
        this.veterinaryStore = veterinaryStore;
        this.pedigreeStore = pedigreeStore;
        this.hallOfFame = hallOfFame;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7/nexushusbandry <inspect|hof|reload>");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "inspect" -> handleInspect(sender);
            case "hof" -> handleHallOfFame(sender, args);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage("§7/nexushusbandry <inspect|hof|reload>");
        }
        return true;
    }

    private void handleInspect(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly a player can inspect an animal.");
            return;
        }
        Animals animal = findTargetAnimal(player);
        if (animal == null) {
            player.sendMessage("§7Nothing in your sights looks like livestock.");
            return;
        }

        long now = System.currentTimeMillis();
        Genome genome = genomeStore.genomeOf(animal);
        double happiness = welfareStore.effectiveHappiness(animal, now);
        Disease disease = veterinaryStore.diseaseOf(animal);
        double severity = veterinaryStore.effectiveSeverity(animal, now);
        int generation = pedigreeStore.generationOf(animal);
        boolean primeSpecimen = pedigreeStore.isPrimeSpecimen(animal);

        player.sendMessage("§b" + LivestockSpecies.of(animal) + (primeSpecimen ? " §6(Prime Specimen)" : ""));
        player.sendMessage("§7Generation " + generation);
        if (genome != null) {
            player.sendMessage(String.format(Locale.ROOT,
                    "§7Vitality %.2f -- Speed %.2f -- Strength %.2f -- Fertility %.2f",
                    genome.vitality(), genome.speed(), genome.strength(), genome.fertility()));
        }
        player.sendMessage("§7Happiness " + Math.round(happiness) + " / 100");
        player.sendMessage(disease == Disease.NONE
                ? "§aHealthy"
                : "§c" + disease.displayName() + " §7(severity " + Math.round(severity * 100) + "%)");
    }

    private Animals findTargetAnimal(Player player) {
        Vector direction = player.getLocation().getDirection();
        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), direction, INSPECT_RANGE);
        if (result == null) {
            return null;
        }
        Entity hit = result.getHitEntity();
        return HusbandryAnimals.asLivestock(hit);
    }

    private void handleHallOfFame(CommandSender sender, String[] args) {
        int count = 5;
        if (args.length > 1) {
            try {
                count = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {
                // keep the default
            }
        }
        List<HallOfFameEntry> top = hallOfFame.top(count);
        if (top.isEmpty()) {
            sender.sendMessage("§7No prime specimens have been bred yet.");
            return;
        }
        sender.sendMessage("§6-- NexusHusbandry Hall of Fame --");
        for (HallOfFameEntry entry : top) {
            String breeder = entry.breederName() == null ? "unknown" : entry.breederName();
            sender.sendMessage(String.format(Locale.ROOT,
                    "§7%s §7(gen %d, quality %.2f) -- bred by %s",
                    entry.species(), entry.generation(), entry.qualityScore(), breeder));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("nexushusbandry.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return;
        }
        reloadAction.run();
        sender.sendMessage("§aNexusHusbandry config reloaded.");
    }
}
