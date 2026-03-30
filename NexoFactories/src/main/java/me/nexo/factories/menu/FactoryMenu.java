package me.nexo.factories.menu;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

// 🌟 Heredamos del sistema base de menús de NexoCore
public class FactoryMenu extends NexoMenu {

    private final NexoFactories plugin;
    private final NexoCore core;
    private final ActiveFactory factory;

    public FactoryMenu(Player player, NexoFactories plugin, ActiveFactory factory) {
        super(player);
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
        this.factory = factory;
    }

    private String getMessage(String path) {
        return core.getConfigManager().getMessage("factories_messages.yml", path);
    }

    private List<String> getMessageList(String path) {
        return core.getConfigManager().getConfig("factories_messages.yml").getStringList(path);
    }

    @Override
    public String getMenuName() {
        return getMessage("menus.factory.titulo");
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        ProtectionStone stone = NexoProtections.getClaimManager().getStoneById(factory.getStoneId());
        String energyStatus = (stone != null) ? "&#00f5ff" + stone.getCurrentEnergy() + " ⚡" : "&#8b0000Desconectada";

        // Ítem 1: Información de la Fábrica
        List<String> infoLore = List.of(
                getMessage("menus.factory.info.nivel").replace("%level%", String.valueOf(factory.getLevel())),
                getMessage("menus.factory.info.estado").replace("%status%", getStatusColor(factory.getCurrentStatus())),
                getMessage("menus.factory.info.red-electrica").replace("%energy_status%", energyStatus),
                " ",
                getMessage("menus.factory.info.autoridad").replace("%owner_name%", Bukkit.getOfflinePlayer(factory.getOwnerId()).getName())
        );
        setItem(11, Material.REPEATER, "&#ff00ff<bold>" + factory.getFactoryType() + "</bold>", infoLore);

        // Ítem 2: Catalizador / Mejora
        String catName = factory.getCatalystItem().equals("NONE") ? "&#8b0000Vacante" : "&#00f5ff" + factory.getCatalystItem();
        List<String> catLore = getMessageList("menus.factory.mejora.lore").stream()
                .map(line -> line.replace("%catalyst_name%", catName)).collect(Collectors.toList());
        setItem(13, Material.LODESTONE, getMessage("menus.factory.mejora.titulo"), catLore);

        // Ítem 3: Salida de Producción
        List<String> outputLore = new ArrayList<>(getMessageList("menus.factory.produccion.lore"));
        String action = factory.getStoredOutput() > 0 ? getMessage("menus.factory.produccion.accion-recolectar") : getMessage("menus.factory.produccion.bandeja-vacia");
        outputLore.add(action);

        List<String> finalOutputLore = outputLore.stream()
                .map(line -> line.replace("%stored_output%", String.valueOf(factory.getStoredOutput())))
                .collect(Collectors.toList());
        setItem(15, Material.CHEST, getMessage("menus.factory.produccion.titulo"), finalOutputLore);

        // Ítem 4: Botón Lógico (Terminal)
        setItem(22, Material.COMMAND_BLOCK, getMessage("menus.factory.terminal.titulo"), getMessageList("menus.factory.terminal.lore"));
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "ACTIVE" -> "&#00f5ff<bold>PRODUCIENDO</bold>";
            case "NO_ENERGY" -> "&#8b0000<bold>SIN ENERGÍA</bold>";
            case "SCRIPT_PAUSED" -> "&#ff00ff<bold>EN ESPERA (SCRIPT)</bold>";
            default -> "&#1c0f2a<bold>SISTEMA APAGADO</bold>";
        };
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 15) {
            // Lógica de recolección de ítems
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

        } else if (slot == 22) {
            // Abrimos el nuevo LogicMenu y pasamos los datos
            new LogicMenu(player, plugin, factory).open();
        }
    }
}