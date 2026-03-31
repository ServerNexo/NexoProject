package me.nexo.core.menus;

import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class VoidBlessingMenu implements InventoryHolder {

    private final NexoCore plugin;
    private final Player player;
    private Inventory inventory;

    public VoidBlessingMenu(NexoCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openMenu() {
        NexoUser user = plugin.getUserManager().getUserOrNull(player.getUniqueId());
        if (user == null) return;

        // Título Vivid Void con parser Crossplay
        String title = "&#ff00ff✧ &#00f5ffEstado del Vacío";
        this.inventory = Bukkit.createInventory(this, 27, CrossplayUtils.parseCrossplay(player, title));

        // 🟪 FONDO VIVID VOID (Púrpura Profundo)
        ItemStack bg = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(Component.empty());
            bg.setItemMeta(bgMeta);
        }
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, bg);
        }

        // 🔮 ÍTEM CENTRAL: ESTADO DE LA BENDICIÓN
        ItemStack statusItem = new ItemStack(Material.AMETHYST_CLUSTER);
        ItemMeta statusMeta = statusItem.getItemMeta();
        List<Component> lore = new ArrayList<>();

        if (user.isVoidBlessingActive()) {
            statusMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>Bendición del Vacío</bold>"));

            // Cálculos matemáticos locales (Cero Lag)
            long remainingMillis = user.getVoidBlessingUntil() - System.currentTimeMillis();
            String timeFormatted = formatTime(remainingMillis);

            lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFEstado: &#00f5ffACTIVO"));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFTiempo restante: &#ff00ff" + timeFormatted));
            lore.add(Component.empty());
            lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff[✧] Beneficios Canalizados:"));
            // 🔥 CORRECCIÓN: Beneficios activos en Carmesí como solicitaste
            lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000 ▶ Protección Hardcore (0% pérdida de XP/Skills)"));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000 ▶ Void Greed (+15% Nexo Coins en toda la red)"));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000 ▶ Void Reach (Acceso remoto al mercado negro)"));
        } else {
            statusItem.setType(Material.COAL); // Cambia el ítem si está inactivo
            statusMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#8b0000<bold>Bendición Inactiva</bold>"));

            lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFEstado: &#8b0000DESACTIVADO"));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFTu alma se encuentra vulnerable."));
            lore.add(Component.empty());
            lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000[!] Consigue una Esencia del Vacío"));
            lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000    para proteger tu progreso."));
        }

        statusMeta.lore(lore);
        statusItem.setItemMeta(statusMeta);

        // Colocamos el ítem en el centro del menú
        inventory.setItem(13, statusItem);

        player.openInventory(inventory);
    }

    // Formateador visual de tiempo (HH:MM:SS)
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02dh %02dm %02ds", h, m, s);
    }

    @Override
    public Inventory getInventory() { return inventory; }
}