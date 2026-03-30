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

import java.util.ArrayList;
import java.util.List;

public class ProtectionMenu extends NexoMenu {

    private final ProtectionStone stone;
    private final NexoProtections plugin;

    public ProtectionMenu(Player player, NexoProtections plugin, ProtectionStone stone) {
        super(player);
        this.plugin = plugin;
        this.stone = stone;
    }

    @Override
    public String getMenuName() {
        return "&#1c0f2a<bold>»</bold> &#00f5ffMonolito del Vacío";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Cristal morado por defecto del NexoMenu

        // SLOT 11: ACÓLITOS
        List<String> memLore = List.of(
                "&#1c0f2aExplora y destierra a las almas",
                "&#1c0f2avinculadas a este Monolito.",
                " ",
                "&#00f5ff► Clic para abrir el registro"
        );
        setItem(11, Material.WITHER_SKELETON_SKULL, "&#ff00ff<bold>ACÓLITOS DEL PACTO</bold>", memLore);

        // SLOT 13: NÚCLEO
        List<String> infoLore = new ArrayList<>();
        infoLore.add("&#1c0f2aSeñor Oscuro: &#ff00ff" + Bukkit.getOfflinePlayer(stone.getOwnerId()).getName());
        infoLore.add("&#1c0f2aTipo de Culto: &#ff00ff" + (stone.getClanId() == null ? "Solitario" : "Sindicato"));
        infoLore.add(" ");
        double porcentaje = (stone.getCurrentEnergy() / stone.getMaxEnergy()) * 100;
        String colorEnergia = porcentaje > 50 ? "&#00f5ff" : (porcentaje > 20 ? "&#ff00ff" : "&#8b0000");
        infoLore.add("&#1c0f2aEsencia Consumida: " + colorEnergia + String.format("%.1f", stone.getCurrentEnergy()) + " &#1c0f2a/ &#00f5ff" + stone.getMaxEnergy());
        setItem(13, Material.LODESTONE, "&#ff00ff<bold>NÚCLEO DEL MONOLITO</bold>", infoLore);

        // SLOT 15: FLAGS
        List<String> flagLore = List.of(
                "&#1c0f2aAltera las leyes naturales y",
                "&#1c0f2afísicas de los forasteros.",
                " ",
                "&#00f5ff► Clic para dictar las leyes"
        );
        setItem(15, Material.SOUL_TORCH, "&#ff00ff<bold>LEYES DEL DOMINIO</bold>", flagLore);

        // SLOT 22: RECARGA
        List<String> rechargeLore = List.of(
                "&#1c0f2aOfrece sacrificios (Diamantes o Ecos)",
                "&#1c0f2apara alimentar el vacío del Monolito."
        );
        setItem(22, Material.ECHO_SHARD, "&#ff00ff<bold>[ INFUNDIR ESENCIA ]</bold>", rechargeLore);
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
                player.sendMessage(NexoColor.parse("&#FF3366[!] Solo el Señor del Dominio puede alterar las leyes naturales."));
                return;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1f);
            new ProtectionFlagsMenu(player, plugin, stone).open();
        }
        else if (slot == 22) { // Recarga
            if (stone.getCurrentEnergy() >= stone.getMaxEnergy()) {
                player.sendMessage(NexoColor.parse("&#FF3366[!] El Monolito está completamente saciado."));
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
                player.sendMessage(NexoColor.parse("&#FF3366[!] Ofrenda Rechazada: &#E6CCFFNecesitas Diamantes o Fragmentos de Eco para alimentar el vacío."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    private void recargaExitosa() {
        player.sendMessage(NexoColor.parse("&#CC66FF[✓] <bold>ESENCIA DEVORADA:</bold> &#E6CCFFEl Monolito ha absorbido tu ofrenda."));
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