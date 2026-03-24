package me.nexo.items.guardarropa;

import me.nexo.core.NexoCore;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import me.nexo.core.utils.Base64Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GuardarropaManager {

    private final NexoItems plugin;

    public GuardarropaManager(NexoItems plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // 💾 GUARDAR ARMADURA ACTUAL EN UN PRESET
    // ==========================================
    public void guardarPreset(Player p, int presetId) {
        ItemStack[] armadura = p.getInventory().getArmorContents();

        // Verificamos que al menos lleve una pieza puesta
        boolean estaDesnudo = true;
        for (ItemStack item : armadura) {
            if (item != null && item.getType() != Material.AIR) {
                estaDesnudo = false;
                break;
            }
        }

        if (estaDesnudo) {
            p.sendMessage(NexoColor.parse("&#FF5555[!] No tienes ninguna armadura equipada para guardar."));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        String base64Data = Base64Util.itemStackArrayToBase64(armadura);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            NexoCore nexoCore = (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
            if (nexoCore == null || nexoCore.getDatabaseManager() == null) return;

            String sql = "INSERT INTO guardarropa (uuid, preset_id, contenido) VALUES (?, ?, ?) " +
                    "ON CONFLICT (uuid, preset_id) DO UPDATE SET contenido = EXCLUDED.contenido;";

            try (Connection conn = nexoCore.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ps.setInt(2, presetId);
                ps.setString(3, base64Data);
                ps.executeUpdate();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    p.sendMessage(NexoColor.parse("&#55FF55[✓] 👕 ¡Armadura guardada exitosamente en el Preset #" + presetId + "!"));
                    p.playSound(p.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1f);
                    p.closeInventory();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ==========================================
    // 🛡️ EQUIPAR PRESET DESDE LA NUBE
    // ==========================================
    public void equiparPreset(Player p, int presetId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            NexoCore nexoCore = (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
            if (nexoCore == null || nexoCore.getDatabaseManager() == null) return;

            String sql = "SELECT contenido FROM guardarropa WHERE uuid = ? AND preset_id = ?";
            String base64Data = null;

            try (Connection conn = nexoCore.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ps.setInt(2, presetId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    base64Data = rs.getString("contenido");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String finalData = base64Data;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalData == null || finalData.isEmpty()) {
                    p.sendMessage(NexoColor.parse("&#FF5555[!] No hay ninguna armadura guardada en el Preset #" + presetId + "."));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                ItemStack[] nuevaArmadura = Base64Util.itemStackArrayFromBase64(finalData);
                ItemStack[] armaduraActual = p.getInventory().getArmorContents();

                // Calculamos cuántos espacios libres necesita
                int espaciosNecesarios = 0;
                for (ItemStack item : armaduraActual) {
                    if (item != null && item.getType() != Material.AIR) espaciosNecesarios++;
                }

                int espaciosLibres = 0;
                for (ItemStack item : p.getInventory().getStorageContents()) {
                    if (item == null || item.getType() == Material.AIR) espaciosLibres++;
                }

                if (espaciosLibres < espaciosNecesarios) {
                    p.sendMessage(NexoColor.parse("&#FF5555<bold>[!] ¡INVENTARIO LLENO!</bold> &#AAAAAANecesitas " + espaciosNecesarios + " espacios libres para guardar tu armadura actual."));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                // Movemos la armadura actual al inventario
                for (ItemStack item : armaduraActual) {
                    if (item != null && item.getType() != Material.AIR) {
                        p.getInventory().addItem(item);
                    }
                }

                // Equipamos la nueva y vaciamos el preset de la BD
                p.getInventory().setArmorContents(nuevaArmadura);
                borrarPreset(p, presetId);

                p.sendMessage(NexoColor.parse("&#00E5FF✨ ¡Te has equipado el Preset #" + presetId + " rápidamente!"));
                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1f);
                p.closeInventory();
            });
        });
    }

    private void borrarPreset(Player p, int presetId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            NexoCore nexoCore = (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
            if (nexoCore == null || nexoCore.getDatabaseManager() == null) return;

            String sql = "DELETE FROM guardarropa WHERE uuid = ? AND preset_id = ?";
            try (Connection conn = nexoCore.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ps.setInt(2, presetId);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}