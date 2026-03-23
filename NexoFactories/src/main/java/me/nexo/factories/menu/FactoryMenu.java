package me.nexo.factories.menu;

import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.items.NexoItems; // 🌟 IMPORT AÑADIDO
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
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

        // 📊 PANEL DE INFORMACIÓN (Slot 11)
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

        // 🌟 MÓDULO CATALIZADOR (Slot 13)
        ItemStack catalyst = new ItemStack(Material.LODESTONE);
        ItemMeta catMeta = catalyst.getItemMeta();
        catMeta.setDisplayName("§b§lMódulo de Mejora");
        String catName = factory.getCatalystItem().equals("NONE") ? "§cNinguno" : "§a" + factory.getCatalystItem();
        catMeta.setLore(Arrays.asList(
                "§7Inserta una Tarjeta Lógica para",
                "§7aumentar el rendimiento.",
                " ",
                "§fInstalado: " + catName,
                " ",
                "§e(Próximamente: Inserción de ítems)"
        ));
        catalyst.setItemMeta(catMeta);
        inv.setItem(13, catalyst);

        // 📦 ALMACENAMIENTO DE PRODUCCIÓN (Slot 15)
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

        // 💻 BOTÓN DEL TERMINAL LÓGICO (Slot 22)
        ItemStack logicBtn = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta logicMeta = logicBtn.getItemMeta();
        logicMeta.setDisplayName("§d§lTerminal de Lógica");
        logicMeta.setLore(Arrays.asList(
                "§7Automatiza esta factoría",
                "§7creando reglas y condiciones.",
                " ",
                "§e¡Clic para programar!"
        ));
        logicBtn.setItemMeta(logicMeta);
        inv.setItem(22, logicBtn);

        player.openInventory(inv);
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "ACTIVE" -> "§a§lPRODUCIENDO";
            case "NO_ENERGY" -> "§c§lSIN ENERGÍA";
            case "SCRIPT_PAUSED" -> "§e§lEN ESPERA (SCRIPT)";
            default -> "§8§lAPAGADA";
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(MENU_TITLE)) return;
        event.setCancelled(true); // Evitar que roben los ítems del menú

        Player player = (Player) event.getWhoClicked();

        // 1. Recolectar Producción
        if (event.getSlot() == 15) {
            Block target = player.getTargetBlockExact(5);
            if (target == null) return;

            ActiveFactory factory = plugin.getFactoryManager().getFactoryAt(target.getLocation());
            if (factory == null) return;

            if (factory.getStoredOutput() <= 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            int amount = factory.getStoredOutput();

            // 🌟 INTEGRACIÓN: Conectar la industria con tu catálogo de Ítems
            ItemStack reward = null;
            String nexoItemId = factory.getFactoryType() + "_OUTPUT"; // Ej: MINA_T1_OUTPUT

            try {
                // Intentamos sacar el ítem custom desde el motor visual Nexo (Oraxen)
                if (com.nexomc.nexo.api.NexoItems.itemFromId(nexoItemId) != null) {
                    reward = com.nexomc.nexo.api.NexoItems.itemFromId(nexoItemId).build();
                }
            } catch (NoClassDefFoundError | Exception ignored) {
                // Fallback silencioso por si Nexo (Oraxen) no está cargado
            }

            // Fallback de seguridad: Si no existe el ítem custom, damos Hierro Vanilla
            if (reward == null) {
                reward = new ItemStack(Material.IRON_INGOT);
            }

            reward.setAmount(amount);

            factory.clearOutput();
            plugin.getFactoryManager().saveFactoryStatusAsync(factory);

            HashMap<Integer, ItemStack> left = player.getInventory().addItem(reward);
            if (!left.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left.get(0));
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            player.sendMessage("§a§l¡RECOLECCIÓN EXITOSA! §7Has obtenido §e" + amount + " ítems§7.");
            player.closeInventory();
        }

        // 2. Abrir Terminal de Lógica
        else if (event.getSlot() == 22) {
            Block target = player.getTargetBlockExact(5);
            if (target == null) return;

            ActiveFactory factory = plugin.getFactoryManager().getFactoryAt(target.getLocation());
            if (factory == null) return;

            plugin.getLogicMenu().openMenu(player, factory);
        }
    }
}