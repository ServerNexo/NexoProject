package me.nexo.factories.menu;

import me.nexo.core.utils.NexoColor;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FactoryMenu implements Listener {

    private final NexoFactories plugin;
    public static final String TITLE_PLAIN = "» Panel de Control";
    public static final String MENU_TITLE = "&#434343<bold>»</bold> &#00fbffPanel de Control";

    public FactoryMenu(NexoFactories plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, ActiveFactory factory) {
        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse(MENU_TITLE));

        ProtectionStone stone = NexoProtections.getClaimManager().getStoneById(factory.getStoneId());
        String energyStatus = (stone != null) ? "&#a8ff78" + stone.getCurrentEnergy() + " ⚡" : "&#ff4b2bDesconectada";

        // 📊 PANEL DE INFORMACIÓN (Slot 11)
        ItemStack info = new ItemStack(Material.REPEATER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(serialize("&#fbd72b<bold>" + factory.getFactoryType() + "</bold>"));
            infoMeta.setLore(serializeList(Arrays.asList(
                    "&#434343Nivel de Estructura: &#00fbff" + factory.getLevel(),
                    "&#434343Estado: " + getStatusColor(factory.getCurrentStatus()),
                    "&#434343Red Eléctrica: " + energyStatus,
                    " ",
                    "&#434343Autoridad: &#e0e0e0" + Bukkit.getOfflinePlayer(factory.getOwnerId()).getName()
            )));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(11, info);

        // 🌟 MÓDULO CATALIZADOR (Slot 13)
        ItemStack catalyst = new ItemStack(Material.LODESTONE);
        ItemMeta catMeta = catalyst.getItemMeta();
        if (catMeta != null) {
            catMeta.setDisplayName(serialize("&#00fbff<bold>Módulo de Mejora</bold>"));
            String catName = factory.getCatalystItem().equals("NONE") ? "&#ff4b2bVacante" : "&#a8ff78" + factory.getCatalystItem();
            catMeta.setLore(serializeList(Arrays.asList(
                    "&#434343Inserta una Tarjeta Lógica para",
                    "&#434343aumentar el rendimiento base.",
                    " ",
                    "&#434343Instalado: " + catName,
                    " ",
                    "&#fbd72b(Próximamente: Inserción de módulos)"
            )));
            catalyst.setItemMeta(catMeta);
        }
        inv.setItem(13, catalyst);

        // 📦 ALMACENAMIENTO DE PRODUCCIÓN (Slot 15)
        ItemStack output = new ItemStack(Material.CHEST);
        ItemMeta outputMeta = output.getItemMeta();
        if (outputMeta != null) {
            outputMeta.setDisplayName(serialize("&#fbd72b<bold>Producción Almacenada</bold>"));
            outputMeta.setLore(serializeList(Arrays.asList(
                    "&#434343Activos listos para extraer: &#a8ff78" + factory.getStoredOutput(),
                    " ",
                    factory.getStoredOutput() > 0 ? "&#fbd72b▶ Haz clic para recolectar" : "&#ff4b2bBandeja de salida vacía."
            )));
            output.setItemMeta(outputMeta);
        }
        inv.setItem(15, output);

        // 💻 BOTÓN DEL TERMINAL LÓGICO (Slot 22)
        ItemStack logicBtn = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta logicMeta = logicBtn.getItemMeta();
        if (logicMeta != null) {
            logicMeta.setDisplayName(serialize("&#8b008b<bold>Terminal de Lógica</bold>"));
            logicMeta.setLore(serializeList(Arrays.asList(
                    "&#434343Automatiza la operativa mediante",
                    "&#434343condiciones y scripts visuales.",
                    " ",
                    "&#00fbff▶ Clic para programar rutinas"
            )));
            logicBtn.setItemMeta(logicMeta);
        }
        inv.setItem(22, logicBtn);

        player.openInventory(inv);
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "ACTIVE" -> "&#a8ff78<bold>PRODUCIENDO</bold>";
            case "NO_ENERGY" -> "&#ff4b2b<bold>SIN ENERGÍA</bold>";
            case "SCRIPT_PAUSED" -> "&#fbd72b<bold>EN ESPERA (SCRIPT)</bold>";
            default -> "&#434343<bold>SISTEMA APAGADO</bold>";
        };
    }

    private String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }

    private List<String> serializeList(List<String> hexList) {
        List<String> out = new ArrayList<>();
        for (String s : hexList) out.add(serialize(s));
        return out;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!plainTitle.equals(TITLE_PLAIN)) return;

        event.setCancelled(true);

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
            player.sendMessage(NexoColor.parse("&#a8ff78<bold>¡EXTRACCIÓN EXITOSA!</bold> &#434343Has obtenido &#fbd72b" + amount + " activos&#434343."));
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