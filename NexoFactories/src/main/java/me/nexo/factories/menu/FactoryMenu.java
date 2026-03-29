package me.nexo.factories.menu;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
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

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class FactoryMenu implements Listener {

    private final NexoFactories plugin;
    private final NexoCore core;

    public FactoryMenu(NexoFactories plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    private String getMessage(String path) {
        return core.getConfigManager().getMessage("factories_messages.yml", path);
    }

    public void openMenu(Player player, ActiveFactory factory) {
        Inventory inv = Bukkit.createInventory(null, 27, CrossplayUtils.parseCrossplay(player, getMessage("menus.factory.titulo")));

        ProtectionStone stone = NexoProtections.getClaimManager().getStoneById(factory.getStoneId());
        String energyStatus = (stone != null) ? "&#00f5ff" + stone.getCurrentEnergy() + " ⚡" : "&#8b0000Desconectada";

        ItemStack info = new ItemStack(Material.REPEATER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>" + factory.getFactoryType() + "</bold>"));
            List<String> lore = List.of(
                    getMessage("menus.factory.info.nivel").replace("%level%", String.valueOf(factory.getLevel())),
                    getMessage("menus.factory.info.estado").replace("%status%", getStatusColor(factory.getCurrentStatus())),
                    getMessage("menus.factory.info.red-electrica").replace("%energy_status%", energyStatus),
                    " ",
                    getMessage("menus.factory.info.autoridad").replace("%owner_name%", Bukkit.getOfflinePlayer(factory.getOwnerId()).getName())
            );
            infoMeta.lore(lore.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(11, info);

        ItemStack catalyst = new ItemStack(Material.LODESTONE);
        ItemMeta catMeta = catalyst.getItemMeta();
        if (catMeta != null) {
            catMeta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.factory.mejora.titulo")));
            String catName = factory.getCatalystItem().equals("NONE") ? "&#8b0000Vacante" : "&#00f5ff" + factory.getCatalystItem();
            List<String> loreConfig = core.getConfigManager().getMessages().getStringList("menus.factory.mejora.lore");
            catMeta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%catalyst_name%", catName))).collect(Collectors.toList()));
            catalyst.setItemMeta(catMeta);
        }
        inv.setItem(13, catalyst);

        ItemStack output = new ItemStack(Material.CHEST);
        ItemMeta outputMeta = output.getItemMeta();
        if (outputMeta != null) {
            outputMeta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.factory.produccion.titulo")));
            List<String> loreConfig = core.getConfigManager().getMessages().getStringList("menus.factory.produccion.lore");
            String action = factory.getStoredOutput() > 0 ? getMessage("menus.factory.produccion.accion-recolectar") : getMessage("menus.factory.produccion.bandeja-vacia");
            loreConfig.add(action);
            outputMeta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%stored_output%", String.valueOf(factory.getStoredOutput())))).collect(Collectors.toList()));
            output.setItemMeta(outputMeta);
        }
        inv.setItem(15, output);

        ItemStack logicBtn = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta logicMeta = logicBtn.getItemMeta();
        if (logicMeta != null) {
            logicMeta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.factory.terminal.titulo")));
            List<String> loreConfig = core.getConfigManager().getMessages().getStringList("menus.factory.terminal.lore");
            logicMeta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));
            logicBtn.setItemMeta(logicMeta);
        }
        inv.setItem(22, logicBtn);

        player.openInventory(inv);
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "ACTIVE" -> "&#00f5ff<bold>PRODUCIENDO</bold>";
            case "NO_ENERGY" -> "&#8b0000<bold>SIN ENERGÍA</bold>";
            case "SCRIPT_PAUSED" -> "&#ff00ff<bold>EN ESPERA (SCRIPT)</bold>";
            default -> "&#1c0f2a<bold>SISTEMA APAGADO</bold>";
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!CrossplayUtils.getChatPlain((Player) event.getWhoClicked(), event.getView().title()).equals(getMessage("menus.factory.titulo").replaceAll("<[^>]*>", ""))) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

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
            ItemStack reward = null;
            String nexoItemId = factory.getFactoryType() + "_OUTPUT";
            try {
                if (com.nexomc.nexo.api.NexoItems.itemFromId(nexoItemId) != null) {
                    reward = com.nexomc.nexo.api.NexoItems.itemFromId(nexoItemId).build();
                }
            } catch (NoClassDefFoundError | Exception ignored) {}
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
            CrossplayUtils.sendMessage(player, getMessage("eventos.extraccion-exitosa").replace("%amount%", String.valueOf(amount)));
            player.closeInventory();
        } else if (event.getSlot() == 22) {
            Block target = player.getTargetBlockExact(5);
            if (target == null) return;
            ActiveFactory factory = plugin.getFactoryManager().getFactoryAt(target.getLocation());
            if (factory == null) return;
            plugin.getLogicMenu().openMenu(player, factory);
        }
    }
}