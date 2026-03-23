package me.nexo.factories.menu;

import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;

public class FactoryMenu implements Listener {

    private final NexoFactories plugin;
    public static final String MENU_TITLE = "§8⚙ §lPanel de Control";

    public FactoryMenu(NexoFactories plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, ActiveFactory factory) {
        Inventory inv = Bukkit.createInventory(null, 27, MENU_TITLE);

        ProtectionStone stone = NexoProtections.getClaimManager().getStoneById(factory.getStoneId());
        String energyStatus = (stone != null) ? "§a" + stone.getCurrentEnergy() + " ⚡" : "§cDesconectada";

        // 📊 PANEL DE INFORMACIÓN
        ItemStack info = new ItemStack(Material.REPEATER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§e§l" + factory.getFactoryType());
        infoMeta.setLore(Arrays.asList(
                "§7Nivel Actual: §b" + factory.getLevel(),
                "§7Estado: " + getStatusColor(factory.getCurrentStatus()),
                "§7Red Eléctrica: " + energyStatus,
                " ",
                "§7Dueño: §f" + Bukkit.getOfflinePlayer(factory.getOwnerId()).getName()
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(11, info);

        // 📦 ALMACENAMIENTO DE PRODUCCIÓN
        ItemStack output = new ItemStack(Material.CHEST);
        ItemMeta outputMeta = output.getItemMeta();
        outputMeta.setDisplayName("§6§lProducción Almacenada");
        outputMeta.setLore(Arrays.asList(
                "§7Ítems listos para recoger: §a" + factory.getStoredOutput(),
                " ",
                factory.getStoredOutput() > 0 ? "§e¡Haz clic para recolectar!" : "§cNo hay nada que recoger aún."
        ));
        output.setItemMeta(outputMeta);
        inv.setItem(15, output);

        player.openInventory(inv);
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "ACTIVE" -> "§a§lPRODUCIENDO";
            case "NO_ENERGY" -> "§c§lSIN ENERGÍA";
            default -> "§8§lAPAGADA";
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(MENU_TITLE)) return;
        event.setCancelled(true); // Evitar que roben los ítems del menú

        Player player = (Player) event.getWhoClicked();

        // Si hizo clic en Recolectar (Slot 15)
        if (event.getSlot() == 15) {
            // Buscamos la fábrica que está mirando usando su Target Block (O una sesión caché)
            Block target = player.getTargetBlockExact(5);
            if (target == null) return;

            ActiveFactory factory = plugin.getFactoryManager().getFactoryAt(target.getLocation());
            if (factory == null) return;

            if (factory.getStoredOutput() <= 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            int amount = factory.getStoredOutput();
            factory.clearOutput();
            plugin.getFactoryManager().saveFactoryStatusAsync(factory);

            // Entregamos los ítems (Placeholder: Lingotes de Hierro)
            // 🚧 Aquí conectaremos con tu NexoItems más adelante para dar el ítem exacto
            ItemStack reward = new ItemStack(Material.IRON_INGOT, amount);
            HashMap<Integer, ItemStack> left = player.getInventory().addItem(reward);
            if (!left.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left.get(0));
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            player.sendMessage("§a§l¡RECOLECCIÓN EXITOSA! §7Has obtenido §e" + amount + " ítems§7.");
            player.closeInventory();
        }
    }
}