package me.nexo.dungeons.bosses;

import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LootDistributor {

    // 🎨 PALETA VIVID VOID
    private static final String BC_DIVIDER = "&#1c0f2a=======================================";
    private static final String MSG_BOSS_DEFEATED = "&#8b0000<bold>☠ %boss% ELIMINADO</bold>";
    private static final String MSG_DAMAGE_STAT = "&#1c0f2aDaño registrado: &#8b0000%dmg% &#1c0f2a(%pct%%)";
    private static final String MSG_TOP_1 = "&#ff00ff🏆 <bold>¡RENDIMIENTO ÓPTIMO (TOP #1)!</bold> &#1c0f2aExtrayendo Botín Mítico.";
    private static final String MSG_TOP_3 = "&#00f5ff🥈 <bold>¡SOBRESALIENTE (TOP #%rank%)!</bold> &#1c0f2aExtrayendo Botín Épico.";
    private static final String MSG_PARTICIPATION = "&#00f5ff🎖 Participación confirmada. &#1c0f2aExtrayendo Botín Estándar.";
    private static final String MSG_XP_REWARD = "&#00f5ff✨ Transferencia de +500 XP de Combate completada.";

    public static void distributeLoot(String bossInternalName, Map<UUID, Double> damageMap) {
        List<Map.Entry<UUID, Double>> ranking = damageMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .toList();

        double totalDamage = ranking.stream().mapToDouble(Map.Entry::getValue).sum();

        int rank = 1;
        for (Map.Entry<UUID, Double> entry : ranking) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;

            double percentage = (entry.getValue() / totalDamage) * 100;
            final int finalRank = rank;

            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("NexoDungeons"), () -> {
                p.sendMessage(NexoColor.parse(BC_DIVIDER));
                p.sendMessage(NexoColor.parse(MSG_BOSS_DEFEATED.replace("%boss%", bossInternalName.toUpperCase())));
                p.sendMessage(NexoColor.parse(MSG_DAMAGE_STAT.replace("%dmg%", String.format("%.0f", entry.getValue())).replace("%pct%", String.format("%.1f", percentage))));

                if (finalRank == 1) {
                    p.sendMessage(NexoColor.parse(MSG_TOP_1));
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    entregarItemSeguro(p, generarRecompensa("MITICO", bossInternalName));
                }
                else if (finalRank <= 3) {
                    p.sendMessage(NexoColor.parse(MSG_TOP_3.replace("%rank%", String.valueOf(finalRank))));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    entregarItemSeguro(p, generarRecompensa("EPICO", bossInternalName));
                }
                else {
                    p.sendMessage(NexoColor.parse(MSG_PARTICIPATION));
                    entregarItemSeguro(p, generarRecompensa("COMUN", bossInternalName));
                }

                p.sendMessage(NexoColor.parse(MSG_XP_REWARD));
                p.sendMessage(NexoColor.parse(BC_DIVIDER));
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