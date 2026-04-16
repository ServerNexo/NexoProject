package me.nexo.economy.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import me.nexo.economy.trade.TradeManager;
import me.nexo.economy.trade.TradeSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

/**
 * 💰 NexoEconomy - Listener de Intercambios (Arquitectura Enterprise Inhackeable)
 */
@Singleton
public class TradeListener implements Listener {

    private final NexoEconomy plugin;
    private final TradeManager tradeManager;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public TradeListener(NexoEconomy plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTradeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        TradeSession session = tradeManager.getSession(player);
        if (session == null) return;

        // 🛡️ PATRÓN ENTERPRISE SEGURO: Validación por instancia en memoria (Adiós validación por título)
        if (!event.getView().getTopInventory().equals(session.getInventory())) return;

        // Si hace clic en su propio inventario en lugar del menú superior
        if (event.getClickedInventory() != session.getInventory()) {
            session.unready(); // Si añade/quita un ítem de abajo, quitamos el estado de "listo"
            return;
        }

        int slot = event.getSlot();
        boolean isPlayer1 = player.equals(session.getPlayer1());

        // Fila central (Divisor y botones de dinero)
        if (slot % 9 == 4) {
            event.setCancelled(true); // Nadie puede robar los divisores
            if (slot == 13) session.addCurrency(player, NexoAccount.Currency.COINS, new BigDecimal("1000"));
            else if (slot == 22) session.addCurrency(player, NexoAccount.Currency.GEMS, new BigDecimal("100"));
            else if (slot == 31) session.addCurrency(player, NexoAccount.Currency.MANA, new BigDecimal("10"));
            return;
        }

        boolean isLeftSide = (slot % 9 < 4);

        // Evitar que toquen el lado del otro jugador
        if (isPlayer1 && !isLeftSide) event.setCancelled(true);
        else if (!isPlayer1 && isLeftSide) event.setCancelled(true);
        else session.unready(); // Si movió un ítem válido, desmarcamos el botón de listo

        // Botón de Listo P1
        if (slot == 45 && isPlayer1) {
            event.setCancelled(true);
            session.toggleReady(player);
        }

        // Botón de Listo P2
        if (slot == 53 && !isPlayer1) {
            event.setCancelled(true);
            session.toggleReady(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTradeClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        TradeSession session = tradeManager.getSession(player);

        // 🛡️ Asegurarnos de que el inventario cerrado fue el de tradeo (y no otro)
        if (session != null && event.getInventory().equals(session.getInventory())) {
            devolverItems(session);
            tradeManager.removeSession(session);

            Player other = player.equals(session.getPlayer1()) ? session.getPlayer2() : session.getPlayer1();
            if (other.getOpenInventory().getTopInventory().equals(session.getInventory())) {
                other.closeInventory();
                CrossplayUtils.sendMessage(other, "&#ff4b2b[!] La otra parte ha abortado el proceso de intercambio.");
            }
            CrossplayUtils.sendMessage(player, "&#ff4b2b[!] Canal de intercambio cerrado.");
        }
    }

    private void devolverItems(TradeSession session) {
        Inventory inv = session.getInventory();
        // Recorremos la cuadrícula del tradeo (ignora los botones de listo)
        for (int i = 0; i < 45; i++) {
            if (i % 9 == 4) continue; // Salta la columna del medio

            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            Player owner = (i % 9 < 4) ? session.getPlayer1() : session.getPlayer2();

            // Evita borrar ítems si el inventario está lleno
            if (owner.getInventory().firstEmpty() == -1) {
                owner.getWorld().dropItemNaturally(owner.getLocation(), item);
            } else {
                owner.getInventory().addItem(item);
            }
            inv.setItem(i, null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TradeSession session = tradeManager.getSession(player);

        if (session != null) {
            devolverItems(session);
            tradeManager.removeSession(session);

            Player other = player.equals(session.getPlayer1()) ? session.getPlayer2() : session.getPlayer1();
            if (other.getOpenInventory().getTopInventory().equals(session.getInventory())) {
                other.closeInventory();
                CrossplayUtils.sendMessage(other, "&#ff4b2b[!] El otro jugador ha abandonado la realidad física. Intercambio abortado.");
            }
        }
    }
}