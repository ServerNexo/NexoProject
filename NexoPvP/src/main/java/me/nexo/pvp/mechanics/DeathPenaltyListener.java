package me.nexo.pvp.mechanics;

import com.google.inject.Inject;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.core.user.UserRepository;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import me.nexo.pvp.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 🏛️ NexoPvP - Penalización de Muerte (Arquitectura Enterprise)
 * Inyección de Dependencias, Type-Safe Configs, y Guardado Asíncrono.
 */
public class DeathPenaltyListener implements Listener {

    // 💉 PILAR 3: Inyectamos exactamente lo que necesitamos, nada de getPlugin()
    private final UserManager userManager;
    private final UserRepository userRepository;
    private final ConfigManager configManager;

    @Inject
    public DeathPenaltyListener(UserManager userManager, UserRepository userRepository, ConfigManager configManager) {
        this.userManager = userManager;
        this.userRepository = userRepository;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());

        if (user == null) return;

        // 🌟 LECTURA DE PROTECCIÓN DIVINA
        boolean hasProtection = user.hasActiveBlessing("VOID_BLESSING") || user.isVoidBlessingActive();

        if (hasProtection) {
            // 🛡️ PROTECCIÓN DIVINA: Mantiene todo intacto
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            // 💡 PILAR 2 & 6: Texto Type-Safe y Crossplay
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().penalizaciones().muerteProtegida());

            // Consumir la bendición de 1 uso si la tiene
            if (user.hasActiveBlessing("VOID_BLESSING")) {
                user.removeBlessing("VOID_BLESSING");

                // 🚀 PILAR 4: Llamada al nuevo DAO asíncrono del Core
                userRepository.saveBlessings(user);
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
                            double xpLost = currentXp * 0.08;
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
                                    // Reemplazo seguro de variables
                                    String msgCobro = configManager.getMessages().mensajes().penalizaciones().cobroResurreccion().replace("%amount%", loss.toPlainString());
                                    CrossplayUtils.sendMessage(player, msgCobro);
                                }
                            });
                        }
                    }
                });
            }

            // 🌟 TEXTOS DESDE CONFIG TYPE-SAFE
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().penalizaciones().perdidaProgreso());
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().penalizaciones().consejoBendicion());
        }

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
    }
}