package me.nexo.items.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 🎒 NexoItems - Comando de Desguace (Arquitectura Enterprise)
 */
@Singleton
@Command({"desguace", "recycle", "salvage"})
public class ComandoDesguace {

    private final NexoItems plugin;
    private final ConfigManager configManager;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoDesguace(NexoItems plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    // 🌟 Lamp se encarga automáticamente de verificar que el ejecutor sea un Player
    @Default
    public void openDesguace(Player player) {

        String titulo = configManager.getMessages().menus().desguace().titulo();
        Inventory inv = Bukkit.createInventory(null, 54, CrossplayUtils.parseCrossplay(player, titulo));

        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(CrossplayUtils.parseCrossplay(player, " "));
            glass.setItemMeta(glassMeta);
        }

        // Panel de cristal inferior protector
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        ItemStack btn = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta btnMeta = btn.getItemMeta();
        if (btnMeta != null) {
            btnMeta.displayName(CrossplayUtils.parseCrossplay(player, configManager.getMessages().menus().desguace().boton().titulo()));

            List<String> lore = configManager.getMessages().menus().desguace().boton().lore().stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line))
                    .collect(Collectors.toList());

            btnMeta.lore(lore);
            btn.setItemMeta(btnMeta);
        }
        inv.setItem(49, btn);

        player.openInventory(inv);
    }
}