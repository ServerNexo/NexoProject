package me.nexo.items.mochilas;

import me.nexo.core.NexoCore;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import me.nexo.core.utils.Base64Util;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MochilaManager {

    private final NexoItems plugin;

    public MochilaManager(NexoItems plugin) {
        this.plugin = plugin;
    }

    private String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }

    public void abrirMochila(Player p, int id) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            NexoCore nexoCore = (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
            if (nexoCore == null || nexoCore.getDatabaseManager() == null) {
                p.sendMessage(NexoColor.parse("&#FF5555[!] Error crítico: Enlace caído con la Base de Datos Central."));
                return;
            }
            String base64Data = null;
            String sql = "SELECT contenido FROM mochilas WHERE uuid = ? AND mochila_id = ?";
            try (Connection conn = nexoCore.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ps.setInt(2, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    base64Data = rs.getString("contenido");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String finalData = base64Data;
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, 54, serialize("&#555555<bold>»</bold> &#00E5FFMochila Virtual #" + id));
                if (finalData != null && !finalData.isEmpty()) {
                    ItemStack[] items = Base64Util.itemStackArrayFromBase64(finalData);
                    inv.setContents(items);
                }
                p.openInventory(inv);
                p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            });
        });
    }

    public void guardarMochila(Player p, int id, Inventory inv) {
        ItemStack[] contents = inv.getContents();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String base64Data = Base64Util.itemStackArrayToBase64(contents);
            guardarMochilaSync(p.getUniqueId().toString(), id, base64Data);
        });
    }

    public void guardarMochilaSync(String uuid, int id, String base64Data) {
        NexoCore nexoCore = (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
        if (nexoCore == null || nexoCore.getDatabaseManager() == null) return;
        String sql = "INSERT INTO mochilas (uuid, mochila_id, contenido) VALUES (?, ?, ?) ON CONFLICT (uuid, mochila_id) DO UPDATE SET contenido = EXCLUDED.contenido;";
        try (Connection conn = nexoCore.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setInt(2, id);
            ps.setString(3, base64Data);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}