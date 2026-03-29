package me.nexo.dungeons.bosses;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LootDistributor {

    public static void distributeLoot(NexoDungeons plugin, String bossInternalName, Map<UUID, Double> damageMap) {
        List<Map.Entry<UUID, Double>> ranking = damageMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .toList();

        double totalDamage = ranking.stream().mapToDouble(Map.Entry::getValue).sum();
        NexoCore core = NexoCore.getPlugin(NexoCore.class);

        int rank = 1;
        for (Map.Entry<UUID, Double> entry : ranking) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;

            double percentage = (entry.getValue() / totalDamage) * 100;
            final int finalRank = rank;

            Bukkit.getScheduler().runTask(plugin, () -> {
                CrossplayUtils.sendMessage(p, core.getConfigManager().getMessage("dungeons_messages.yml", "eventos.loot.divisor"));
                CrossplayUtils.sendMessage(p, core.getConfigManager().getMessage("dungeons_messages.yml", "eventos.loot.jefe-derrotado").replace("%boss%", bossInternalName.toUpperCase()));
                CrossplayUtils.sendMessage(p, core.getConfigManager().getMessage("dungeons_messages.yml", "eventos.loot.stats-dano")
                        .replace("%dmg%", String.format("%.0f", entry.getValue()))
                        .replace("%pct%", String.format("%.1f", percentage)));

                if (finalRank == 1) {
                    CrossplayUtils.sendMessage(p, core.getConfigManager().getMessage("dungeons_messages.yml", "eventos.loot.top1"));
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    entregarItemSeguro(p, generarRecompensa("MITICO", bossInternalName));
                    NexoAPI.getServices().get(EconomyManager.class).ifPresent(eco -> eco.updateBalanceAsync(p.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, BigDecimal.valueOf(1000), true));
                } else if (finalRank <= 3) {
                    CrossplayUtils.sendMessage(p, core.getConfigManager().getMessage("dungeons_messages.yml", "eventos.loot.top3").replace("%rank%", String.valueOf(finalRank)));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    entregarItemSeguro(p, generarRecompensa("EPICO", bossInternalName));
                    NexoAPI.getServices().get(EconomyManager.class).ifPresent(eco -> eco.updateBalanceAsync(p.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, BigDecimal.valueOf(500), true));
                } else {
                    CrossplayUtils.sendMessage(p, core.getConfigManager().getMessage("dungeons_messages.yml", "eventos.loot.participacion"));
                    entregarItemSeguro(p, generarRecompensa("COMUN", bossInternalName));
                    NexoAPI.getServices().get(EconomyManager.class).ifPresent(eco -> eco.updateBalanceAsync(p.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, BigDecimal.valueOf(100), true));
                }

                CrossplayUtils.sendMessage(p, core.getConfigManager().getMessage("dungeons_messages.yml", "eventos.loot.recompensa-xp"));
                CrossplayUtils.sendMessage(p, core.getConfigManager().getMessage("dungeons_messages.yml", "eventos.loot.divisor"));
            });

            rank++;
        }
    }

    private static void entregarItemSeguro(Player p, ItemStack item) {
        if (item == null) return;
        me.nexo.dungeons.listeners.LootProtectionListener.dropProtectedItem(p.getLocation(), item, p);
    }

    private static ItemStack generarRecompensa(String tier, String bossName) {
        if (tier.equals("MITICO")) return new ItemStack(Material.NETHER_STAR, 1);
        if (tier.equals("EPICO")) return new ItemStack(Material.DIAMOND, 3);
        return new ItemStack(Material.GOLD_INGOT, 5);
    }
}