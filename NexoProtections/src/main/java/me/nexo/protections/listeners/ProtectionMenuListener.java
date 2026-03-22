package me.nexo.protections.listeners;

import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ProtectionMenuListener implements Listener {

    private final ClaimManager claimManager;

    public ProtectionMenuListener(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§8Piedra de Protección")) {
            event.setCancelled(true); // Evitamos que se roben los ítems del menú

            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();

            // Si hizo clic en el botón de recarga (Esmeralda en el slot 15)
            if (event.getSlot() == 15 && event.getCurrentItem().getType() == Material.EMERALD) {

                // Buscamos en qué piedra está parado el jugador
                ProtectionStone stone = claimManager.getStoneAt(player.getLocation());
                if (stone == null) {
                    player.closeInventory();
                    return;
                }

                if (stone.getCurrentEnergy() >= stone.getMaxEnergy()) {
                    player.sendMessage("§cLa piedra ya tiene su energía al máximo.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                boolean recargado = false;

                // 🌟 LÓGICA DE ALIMENTACIÓN
                if (stone.getClanId() == null) {
                    // Piedra Solitaria: Consume Vainilla
                    if (player.getInventory().contains(Material.DIAMOND)) {
                        quitarItem(player, Material.DIAMOND, 1);
                        stone.addEnergy(100);
                        recargado = true;
                    } else if (player.getInventory().contains(Material.IRON_INGOT)) {
                        quitarItem(player, Material.IRON_INGOT, 1);
                        stone.addEnergy(10);
                        recargado = true;
                    } else if (player.getInventory().contains(Material.COAL)) {
                        quitarItem(player, Material.COAL, 1);
                        stone.addEnergy(2);
                        recargado = true;
                    }
                } else {
                    // Piedra de Clan: Aquí conectarías con tu ItemManager de NexoItems
                    // if (NexoItems.getManager().hasItem(player, "polvo_estelar")) { ... }
                    // Por ahora dejaremos un placeholder con Esmeraldas Vanilla:
                    if (player.getInventory().contains(Material.EMERALD)) {
                        quitarItem(player, Material.EMERALD, 1);
                        stone.addEnergy(50);
                        recargado = true;
                    }
                }

                if (recargado) {
                    player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 2f);
                    player.sendMessage("§a¡Has inyectado energía a la Piedra de Protección!");
                    player.closeInventory(); // Cerramos para que vean los efectos
                } else {
                    player.sendMessage("§cNo tienes los materiales necesarios para recargar esta piedra.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
        }
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