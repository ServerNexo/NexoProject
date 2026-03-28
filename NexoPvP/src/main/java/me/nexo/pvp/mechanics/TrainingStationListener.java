package me.nexo.pvp.mechanics;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrainingStationListener implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // ⚖️ BALANCE: Ahora que romper tarda más que hacer clic, subimos la XP
    private final int MAX_TRAINING_LEVEL = 15;
    private final double XP_PER_BREAK = 10.0;

    @EventHandler
    public void onTrainingBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        Skill targetSkill = null;

        // 🌟 INTEGRACIÓN DE BLOQUES CUSTOM (O VANILLA)
        // Nota: Si usas el plugin premium "Nexo" para bloques custom, aquí podrías
        // validar el ID del bloque con NexoBlocks.isCustomBlock(block)

        Material blockType = block.getType();

        switch (blockType) {
            case TARGET: // ⚔️ Combate (Tardan en romperlo con la espada)
                targetSkill = Skills.FIGHTING;
                break;
            case COAL_ORE: // ⛏️ Minería (Cambié Bedrock porque Bedrock es irrompible en survival)
                targetSkill = Skills.MINING;
                break;
            case OAK_LOG: // 🪓 Tala
                targetSkill = Skills.FORAGING;
                break;
            case HAY_BLOCK: // 🌾 Agricultura
                targetSkill = Skills.FARMING;
                break;
            case BOOKSHELF: // 🔮 Encantamiento (Cambiado a Librería para que puedan romperlo con hacha/mano)
                targetSkill = Skills.ENCHANTING;
                break;
            case CAULDRON: // 🧪 Alquimia
                targetSkill = Skills.ALCHEMY;
                break;
            case BARREL: // 🎣 Pesca
                targetSkill = Skills.FISHING;
                break;
            default:
                return; // Si no es un bloque de entrenamiento, no hacemos nada
        }

        // 🛑 MAGIA CORPORATIVA: Cancelamos el evento para que el bloque NO se rompa.
        // Esto crea un "Dummy Infinito" sin tener que regenerarlo o usar bases de datos.
        event.setCancelled(true);

        if (cooldowns.containsKey(id) && (now - cooldowns.get(id)) < 500) {
            return; // Cooldown anti-lag por si usan instamine
        }

        try {
            SkillsUser skillsUser = AuraSkillsApi.get().getUser(id);
            if (skillsUser == null) return;

            // 🛑 SISTEMA ANTI-AFK INFINITO (Hard-Cap)
            if (skillsUser.getSkillLevel(targetSkill) >= MAX_TRAINING_LEVEL) {
                if (!cooldowns.containsKey(id) || (now - cooldowns.get(id)) > 3000) {
                    player.sendMessage(NexoColor.parse("&#8b0000[!] Ya eres muy avanzado (" + MAX_TRAINING_LEVEL + "+). ¡Sal al mundo real!"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    cooldowns.put(id, now);
                }
                return;
            }

            // 🌟 Otorgar XP y Feedback
            skillsUser.addSkillXp(targetSkill, XP_PER_BREAK);
            playTrainingFeedback(player, blockType, block.getLocation());

            cooldowns.put(id, now);

        } catch (Exception ignored) {}
    }

    private void playTrainingFeedback(Player player, Material blockType, org.bukkit.Location loc) {
        org.bukkit.Location center = loc.add(0.5, 0.5, 0.5);
        String icon = "[+]";

        // 🌟 PARTÍCULAS ACTUALIZADAS PARA PAPER 1.21
        switch (blockType) {
            case TARGET -> {
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, center, 5);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.5f);
                icon = "[⚔]";
            }
            case COAL_ORE -> {
                player.getWorld().spawnParticle(Particle.BLOCK, center, 10, Bukkit.createBlockData(Material.COAL_ORE));
                player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 0.5f, 0.8f);
                icon = "[⛏]";
            }
            case OAK_LOG -> {
                player.getWorld().spawnParticle(Particle.BLOCK, center, 10, Bukkit.createBlockData(Material.OAK_LOG));
                player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 0.5f, 0.8f);
                icon = "[🪓]";
            }
            case HAY_BLOCK -> {
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 5);
                player.playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.5f, 1.2f);
                icon = "[🌾]";
            }
            case BOOKSHELF -> {
                player.getWorld().spawnParticle(Particle.ENCHANT, center, 15);
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 2.0f);
                icon = "[🔮]";
            }
            case CAULDRON -> {
                player.getWorld().spawnParticle(Particle.WITCH, center, 10);
                player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.5f, 1.5f);
                icon = "[🧪]";
            }
            case BARREL -> {
                player.getWorld().spawnParticle(Particle.SPLASH, center, 15);
                player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.5f, 1.5f);
                icon = "[🎣]";
            }
        }

        player.sendActionBar(NexoColor.parse("&#ff00ff" + icon + " Entrenamiento: &#00f5ff+" + XP_PER_BREAK + " XP"));
    }
}