package me.nexo.dungeons.bosses;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LootDistributor {

    public static void distributeLoot(String bossInternalName, Map<UUID, Double> damageMap) {

        // 1. Ordenamos la lista de mayor daño a menor daño
        List<Map.Entry<UUID, Double>> ranking = damageMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .toList();

        // 2. Calculamos el daño total para sacar los porcentajes
        double totalDamage = ranking.stream().mapToDouble(Map.Entry::getValue).sum();

        int rank = 1;
        for (Map.Entry<UUID, Double> entry : ranking) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;

            double percentage = (entry.getValue() / totalDamage) * 100;

            // Volvemos al hilo principal para enviarle mensajes y darle ítems
            final int finalRank = rank;
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("NexoDungeons"), () -> {
                p.sendMessage("§8====================================");
                p.sendMessage("§d☠ §l" + bossInternalName.toUpperCase() + " DERROTADO");
                p.sendMessage("§7Tu daño infligido: §c" + String.format("%.0f", entry.getValue()) + " §8(" + String.format("%.1f", percentage) + "%)");

                // 🎁 REPARTO ANTI-ROBO (Directo al inventario)
                if (finalRank == 1) {
                    p.sendMessage("§e🏆 §l¡TOP #1 DE DAÑO! §7Recibes Botín Mítico.");
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    entregarItemSeguro(p, generarRecompensa("MITICO", bossInternalName));
                }
                else if (finalRank <= 3) {
                    p.sendMessage("§6🥈 §l¡TOP #" + finalRank + "! §7Recibes Botín Épico.");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    entregarItemSeguro(p, generarRecompensa("EPICO", bossInternalName));
                }
                else {
                    p.sendMessage("§b🎖 §lParticipación. §7Recibes recompensa base.");
                    entregarItemSeguro(p, generarRecompensa("COMUN", bossInternalName));
                }

                // 🌟 Integración con NexoCore (Exp de Combate / AuraSkills)
                // me.nexo.core.user.NexoAPI.getInstance().addProfessionXp(p.getUniqueId(), "fighting", 500);
                p.sendMessage("§3✨ +500 XP de Combate");
                p.sendMessage("§8====================================");
            });

            rank++;
        }
    }

    // Método para inyectar el botín. ESTILO HYPIXEL: Cae al suelo protegido
    private static void entregarItemSeguro(Player p, ItemStack item) {
        if (item == null) return;

        // En lugar de meterlo al inventario, lo tiramos a sus pies con protección
        me.nexo.dungeons.listeners.LootProtectionListener.dropProtectedItem(p.getLocation(), item, p);
    }

    // Aquí conectarías con tu ItemManager de NexoItems (Ej: ItemManager.generarArmaRPG("ESPADA_DRAGON"))
    private static ItemStack generarRecompensa(String tier, String bossName) {
        if (tier.equals("MITICO")) {
            return new ItemStack(Material.NETHER_STAR, 1); // Placeholder
        } else if (tier.equals("EPICO")) {
            return new ItemStack(Material.DIAMOND, 3); // Placeholder
        }
        return new ItemStack(Material.GOLD_INGOT, 5); // Placeholder Comun
    }
}