package me.nexo.pvp.mechanics;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import me.nexo.pvp.NexoPvP;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DeathPenaltyListener implements Listener {

    private final NexoCore core;
    private final NexoPvP plugin;

    public DeathPenaltyListener() {
        this.core = NexoCore.getPlugin(NexoCore.class);
        this.plugin = NexoPvP.getPlugin(NexoPvP.class);
    }

    private String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        NexoUser user = core.getUserManager().getUserOrNull(player.getUniqueId());

        if (user == null) return;

        // 🌟 LECTURA DE PROTECCIÓN DIVINA
        boolean hasProtection = user.hasActiveBlessing("VOID_BLESSING") || user.isVoidBlessingActive();

        if (hasProtection) {
            // 🛡️ PROTECCIÓN DIVINA: Mantiene todo intacto
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            // 🌟 TEXTO DESDE CONFIG
            player.sendMessage(NexoColor.parse(getMessage("mensajes.penalizaciones.muerte-protegida")));

            // Consumir la bendición de 1 uso si la tiene
            if (user.hasActiveBlessing("VOID_BLESSING")) {
                user.removeBlessing("VOID_BLESSING");

                // 🚀 Llamada al nuevo DAO asíncrono (Pilar 4)
                core.getUserRepository().saveBlessings(user);
            }

        } else {
            // 🩸 HARDCORE PENALTY (Filosofía RPG Moderada)

            // 1. Pérdida de Niveles Vanilla (10%)
            int currentLevel = player.getLevel();
            event.setNewLevel(Math.max(0, currentLevel - (int)(currentLevel * 0.10)));

            // 2. Pérdida de 8% de XP de Profesiones (AuraSkills)
            try {
                SkillsUser skillsUser = AuraSkillsApi.get().getUser(player.getUniqueId());
                if (skillsUser != null) {
                    for (Skill skill : AuraSkillsApi.get().getGlobalRegistry().getSkills()) {
                        double currentXp = skillsUser.getSkillXp(skill);
                        if (currentXp > 0) {
                            double xpLost = currentXp * 0.08; // 8% de penalización
                            skillsUser.addSkillXp(skill, -xpLost);
                        }
                    }
                }
            } catch (Exception ignored) {}

            // 3. 💸 PENALIZACIÓN ECONÓMICA ASÍNCRONA (5% del Balance Total)
            NexoEconomy ecoPlugin = (NexoEconomy) Bukkit.getPluginManager().getPlugin("NexoEconomy");
            if (ecoPlugin != null) {
                ecoPlugin.getEconomyManager().getAccountAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER).thenAccept(account -> {
                    if (account != null) {
                        BigDecimal currentBalance = account.getCoins();

                        if (currentBalance != null && currentBalance.compareTo(BigDecimal.ZERO) > 0) {
                            // Calculamos el 5% de pérdida
                            BigDecimal loss = currentBalance.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);

                            // Ejecutamos el débito seguro
                            ecoPlugin.getEconomyManager().updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, loss, false).thenAccept(success -> {
                                if (success) {
                                    String msgCobro = getMessage("mensajes.penalizaciones.cobro-resurreccion").replace("%amount%", loss.toPlainString());
                                    player.sendMessage(NexoColor.parse(msgCobro));
                                }
                            });
                        }
                    }
                });
            }

            // 🌟 TEXTOS DESDE CONFIG
            player.sendMessage(NexoColor.parse(getMessage("mensajes.penalizaciones.perdida-progreso")));
            player.sendMessage(NexoColor.parse(getMessage("mensajes.penalizaciones.consejo-bendicion")));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
    }
}