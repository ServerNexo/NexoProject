package me.nexo.economy.listeners;

import me.nexo.economy.NexoEconomy;
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
        if (session == null) return; // No está en un tradeo

        // Si están clickeando en su propio inventario (el de abajo), cancelamos el "Listo"
        if (event.getClickedInventory() != session.getInventory()) {
            session.unready();
            return;
        }

        int slot = event.getSlot();
        boolean isPlayer1 = player.equals(session.getPlayer1());

        // 🛡️ Bloqueamos el cristal central (Columna 4) y manejamos los botones de dinero
        if (slot % 9 == 4) {
            event.setCancelled(true);

            // 💰 CLIC EN BOTONES DE DINERO
            if (slot == 13) { // Añadir Monedas
                session.addMoney(player, new BigDecimal("1000"));
            } else if (slot == 22) { // Añadir Gemas (Próximamente)
                // session.addGems(...)
            } else if (slot == 31) { // Añadir Maná (Próximamente)
                // session.addMana(...)
            }
            return;
        }

        // 🛡️ Lógica de Zonas: Evitar que toquen el lado del otro
        boolean isLeftSide = (slot % 9 < 4);

        if (isPlayer1 && !isLeftSide) {
            event.setCancelled(true); // P1 intentando tocar lado derecho
        } else if (!isPlayer1 && isLeftSide) {
            event.setCancelled(true); // P2 intentando tocar lado izquierdo
        } else {
            // Si mueven un ítem válido en su zona, quitamos el "Listo" por seguridad
            session.unready();
        }

        // 🖱️ Botón Confirmar P1 (Slot 45)
        if (slot == 45 && isPlayer1) {
            event.setCancelled(true);
            session.toggleReady(player);
        }

        // 🖱️ Botón Confirmar P2 (Slot 53)
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
            // 1. DEVOLVEMOS LOS ÍTEMS A SUS DUEÑOS ANTES DE CERRAR
            devolverItems(session);

            // 2. Eliminamos la sesión del sistema
            plugin.getTradeManager().removeSession(session);

            // 3. Forzamos al otro jugador a cerrar su menú también
            Player other = player.equals(session.getPlayer1()) ? session.getPlayer2() : session.getPlayer1();
            if (other.getOpenInventory().getTopInventory().equals(session.getInventory())) {
                other.closeInventory();
                other.sendMessage("§cEl otro jugador canceló el intercambio.");
            }
            player.sendMessage("§cIntercambio cancelado.");
        }
    }

    // 🛡️ MÉTODO PARA DEVOLVER ÍTEMS SI SE CANCELA EL TRADE
    private void devolverItems(TradeSession session) {
        Inventory inv = session.getInventory();

        // Recorremos todo el inventario superior (excepto la última fila de botones)
        for (int i = 0; i < 45; i++) {
            // Ignoramos la columna central de cristales
            if (i % 9 == 4) continue;

            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            // ¿De quién era este ítem?
            Player owner = (i % 9 < 4) ? session.getPlayer1() : session.getPlayer2();

            // Si tiene el inventario lleno, lo tiramos al piso, sino se lo damos a la mano
            if (owner.getInventory().firstEmpty() == -1) {
                owner.getWorld().dropItemNaturally(owner.getLocation(), item);
            } else {
                owner.getInventory().addItem(item);
            }

            // Borramos el ítem del menú de tradeo para que no se duplique
            inv.setItem(i, null);
        }
    }
}