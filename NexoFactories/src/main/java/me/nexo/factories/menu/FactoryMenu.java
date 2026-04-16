package me.nexo.factories.menu;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.utils.NexoColor;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🏭 NexoFactories - Interfaz de la Fábrica (Arquitectura Enterprise)
 * Nota: Los menús son instanciados por jugador, NO usan @Singleton.
 */
public class FactoryMenu extends NexoMenu {

    private final NexoFactories plugin;
    private final ActiveFactory factory;

    public FactoryMenu(Player player, NexoFactories plugin, ActiveFactory factory) {
        super(player);
        this.plugin = plugin;
        this.factory = factory;
    }

    @Override
    public String getMenuName() {
        // 🌟 FIX: Texto directo serializado para evitar errores en Bedrock y quitar getMessage
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#ff00ff🏭 <bold>FÁBRICA: " + factory.getFactoryType() + "</bold>"));
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // 🌟 FIX: Obtenemos el ClaimManager usando el ServiceManager de la Arquitectura Enterprise
        ProtectionStone stone = null;
        ClaimManager claimManager = NexoAPI.getServices().get(ClaimManager.class).orElse(null);

        if (claimManager != null) {
            stone = claimManager.getStoneById(factory.getStoneId());
        }

        String energyStatus = (stone != null) ? "&#00f5ff" + stone.getCurrentEnergy() + " ⚡" : "&#8b0000Desconectada";

        // Ítem 1: Información de la Fábrica
        List<String> infoLoreRaw = List.of(
                "&#E6CCFFNivel del Núcleo: &#00f5ff" + factory.getLevel(),
                "&#E6CCFFEstado: " + getStatusColor(factory.getCurrentStatus()),
                "&#E6CCFFRed Eléctrica: " + energyStatus,
                " ",
                "&#E6CCFFAutoridad: &#ff00ff" + Bukkit.getOfflinePlayer(factory.getOwnerId()).getName()
        );
        setItem(11, Material.REPEATER, "&#00f5ff📊 <bold>ESTADO DEL SISTEMA</bold>",
                infoLoreRaw.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line))).collect(Collectors.toList()));

        // Ítem 2: Catalizador / Mejora
        String catName = factory.getCatalystItem().equals("NONE") ? "&#8b0000Vacante" : "&#00f5ff" + factory.getCatalystItem();
        List<String> catLoreRaw = List.of(
                "&#E6CCFFMódulo Actual: " + catName,
                "",
                "&#FF5555(Sistema de inserción en desarrollo)"
        );
        setItem(13, Material.LODESTONE, "&#FFAA00✨ <bold>MÓDULO CATALIZADOR</bold>",
                catLoreRaw.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line))).collect(Collectors.toList()));

        // Ítem 3: Salida de Producción
        String action = factory.getStoredOutput() > 0 ? "&#55FF55► Clic para Recolectar" : "&#FF5555[!] La bandeja está vacía";
        List<String> outputLoreRaw = List.of(
                "&#E6CCFFRecursos Procesados: &#55FF55" + factory.getStoredOutput() + " und(s).",
                "",
                action
        );
        setItem(15, Material.CHEST, "&#55FF55📦 <bold>BANDEJA DE SALIDA</bold>",
                outputLoreRaw.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line))).collect(Collectors.toList()));

        // Ítem 4: Botón Lógico (Terminal)
        List<String> terminalLoreRaw = List.of(
                "&#E6CCFFPrograma el comportamiento",
                "&#E6CCFFautónomo de esta maquinaria.",
                "",
                "&#00f5ff► Clic para abrir el compilador"
        );
        setItem(22, Material.COMMAND_BLOCK, "&#FF5555⚙ <bold>TERMINAL LÓGICA</bold>",
                terminalLoreRaw.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line))).collect(Collectors.toList()));
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "ACTIVE" -> "&#00f5ff<bold>PRODUCIENDO</bold>";
            case "NO_ENERGY" -> "&#8b0000<bold>SIN ENERGÍA</bold>";
            case "SCRIPT_PAUSED" -> "&#ff00ff<bold>EN ESPERA (SCRIPT)</bold>";
            default -> "&#E6CCFF<bold>SISTEMA APAGADO</bold>";
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

            // Integración con el plugin externo Nexo (Oraxen)
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

            // Dar ítems de forma segura
            HashMap<Integer, ItemStack> left = player.getInventory().addItem(reward);
            if (!left.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left.get(0));
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);

            // 🌟 FIX: Mensaje directo sin getMessage
            CrossplayUtils.sendMessage(player, "&#55FF55[✓] Has extraído " + amount + " unidades de la fábrica.");
            player.closeInventory();

        } else if (slot == 22) {
            // Abrimos el nuevo LogicMenu y pasamos los datos
            new LogicMenu(player, plugin, factory).open();
        }
    }
}