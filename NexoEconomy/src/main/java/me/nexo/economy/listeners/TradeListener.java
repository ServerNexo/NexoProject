package me.nexo.economy.listeners;

import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount; // 🌟 IMPORTANTE
import me.nexo.economy.trade.TradeSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

public class TradeListener implements Listener {

    private final NexoEconomy plugin;

    public TradeListener(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTradeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        TradeSession session = plugin.getTradeManager().getSession(player);
        if (session == null) return;

        if (event.getClickedInventory() != session.getInventory()) {
            session.unready();
            return;
        }

        int slot = event.getSlot();
        boolean isPlayer1 = player.equals(session.getPlayer1());

        if (slot % 9 == 4) {
            event.setCancelled(true);

            // 🌟 CLIC EN BOTONES MULTIDIVISA
            if (slot == 13) {
                session.addCurrency(player, NexoAccount.Currency.COINS, new BigDecimal("1000"));
            } else if (slot == 22) {
                session.addCurrency(player, NexoAccount.Currency.GEMS, new BigDecimal("100"));
            } else if (slot == 31) {
                session.addCurrency(player, NexoAccount.Currency.MANA, new BigDecimal("10"));
            }
            return;
        }

        boolean isLeftSide = (slot % 9 < 4);

        if (isPlayer1 && !isLeftSide) {
            event.setCancelled(true);
        } else if (!isPlayer1 && isLeftSide) {
            event.setCancelled(true);
        } else {
            session.unready();
        }

        if (slot == 45 && isPlayer1) {
            event.setCancelled(true);
            session.toggleReady(player);
        }

        if (slot == 53 && !isPlayer1) {
            event.setCancelled(true);
            session.toggleReady(player);
        }
    }

    @EventHandler
    public void onTradeClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        TradeSession session = plugin.getTradeManager().getSession(player);
        if (session != null) {
            devolverItems(session);
            plugin.getTradeManager().removeSession(session);

            Player other = player.equals(session.getPlayer1()) ? session.getPlayer2() : session.getPlayer1();
            if (other.getOpenInventory().getTopInventory().equals(session.getInventory())) {
                other.closeInventory();
                other.sendMessage("§cEl otro jugador canceló el intercambio.");
            }
            player.sendMessage("§cIntercambio cancelado.");
        }
    }

    private void devolverItems(TradeSession session) {
        Inventory inv = session.getInventory();
        for (int i = 0; i < 45; i++) {
            if (i % 9 == 4) continue;

            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            Player owner = (i % 9 < 4) ? session.getPlayer1() : session.getPlayer2();

            if (owner.getInventory().firstEmpty() == -1) {
                owner.getWorld().dropItemNaturally(owner.getLocation(), item);
            } else {
                owner.getInventory().addItem(item);
            }
            inv.setItem(i, null);
        }
    }
}