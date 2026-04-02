package me.nexo.protections.menu;

import me.nexo.core.menus.NexoMenu;
import me.nexo.core.utils.NexoColor;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class ProtectionMenu extends NexoMenu {

    private final ProtectionStone stone;
    private final NexoProtections plugin;

    public ProtectionMenu(Player player, NexoProtections plugin, ProtectionStone stone) {
        super(player);
        this.plugin = plugin;
        this.stone = stone;
    }

    // 🌟 MÉTODOS PARA LEER LA CONFIGURACIÓN LOCAL DE FORMA RÁPIDA
    private String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    private List<String> getMessageList(String path) {
        return plugin.getConfigManager().getMessages().getStringList(path);
    }

    @Override
    public String getMenuName() {
        return getMessage("menus.principal.titulo");
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Cristal morado por defecto del NexoMenu

        // SLOT 11: ACÓLITOS
        setItem(11, Material.WITHER_SKELETON_SKULL, getMessage("menus.principal.acolitos.nombre"), getMessageList("menus.principal.acolitos.lore"));

        // SLOT 13: NÚCLEO
        double porcentaje = (stone.getCurrentEnergy() / stone.getMaxEnergy()) * 100;
        String colorEnergia = porcentaje > 50 ? "&#00f5ff" : (porcentaje > 20 ? "&#ff00ff" : "&#8b0000");
        String ownerName = Bukkit.getOfflinePlayer(stone.getOwnerId()).getName();

        List<String> infoLore = getMessageList("menus.principal.nucleo.lore").stream()
                .map(line -> line.replace("%owner%", ownerName != null ? ownerName : "Desconocido")
                        .replace("%type%", stone.getClanId() == null ? "Solitario" : "Sindicato")
                        .replace("%energy_color%", colorEnergia)
                        .replace("%current_energy%", String.format("%.1f", stone.getCurrentEnergy()))
                        .replace("%max_energy%", String.valueOf(stone.getMaxEnergy())))
                .collect(Collectors.toList());

        setItem(13, Material.LODESTONE, getMessage("menus.principal.nucleo.nombre"), infoLore);

        // SLOT 15: FLAGS
        setItem(15, Material.SOUL_TORCH, getMessage("menus.principal.leyes.nombre"), getMessageList("menus.principal.leyes.lore"));

        // SLOT 22: RECARGA
        setItem(22, Material.ECHO_SHARD, getMessage("menus.principal.recarga.nombre"), getMessageList("menus.principal.recarga.lore"));
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto para que no roben ítems
        int slot = event.getRawSlot();

        if (slot == 11) { // Acólitos
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            new ProtectionMembersMenu(player, plugin, stone).open();
        }
        else if (slot == 15) { // Leyes
            if (!stone.getOwnerId().equals(player.getUniqueId())) {
                player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.solo-dueno")));
                return;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1f);
            new ProtectionFlagsMenu(player, plugin, stone).open();
        }
        else if (slot == 22) { // Recarga
            if (stone.getCurrentEnergy() >= stone.getMaxEnergy()) {
                player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.monolito-lleno")));
                return;
            }
            if (player.getInventory().contains(Material.ECHO_SHARD)) {
                quitarItem(player, Material.ECHO_SHARD, 1);
                stone.addEnergy(500);
                recargaExitosa();
            } else if (player.getInventory().contains(Material.DIAMOND)) {
                quitarItem(player, Material.DIAMOND, 1);
                stone.addEnergy(100);
                recargaExitosa();
            } else {
                player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.ofrenda-rechazada")));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    private void recargaExitosa() {
        player.sendMessage(NexoColor.parse(getMessage("mensajes.exito.recarga-exitosa")));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.5f);
        setMenuItems(); // Actualizamos la visual de la energía sin cerrar el menú
    }

    private void quitarItem(Player player, Material mat, int cantidad) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                if (item.getAmount() > cantidad) {
                    item.setAmount(item.getAmount() - cantidad);
                    break;
                } else {
                    cantidad -= item.getAmount();
                    player.getInventory().remove(item);
                    if (cantidad <= 0) break;
                }
            }
        }
    }
}