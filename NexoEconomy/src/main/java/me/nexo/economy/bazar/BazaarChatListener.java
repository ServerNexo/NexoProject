package me.nexo.economy.bazar;

import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BazaarChatListener implements Listener {

    private final NexoEconomy plugin;
    public static final Map<UUID, OrderSession> activeSessions = new HashMap<>();

    public BazaarChatListener(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    public static class OrderSession {
        public String itemId;
        public String orderType;
        public int amount = -1;

        public OrderSession(String itemId, String orderType) {
            this.itemId = itemId;
            this.orderType = orderType;
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        event.setCancelled(true); // Bloqueamos el mensaje para que nadie más lo lea
        OrderSession session = activeSessions.get(player.getUniqueId());
        String msg = event.getMessage().trim().toLowerCase();

        if (msg.equals("cancelar") || msg.equals("cancel")) {
            activeSessions.remove(player.getUniqueId());
            player.sendMessage(NexoColor.parse("&#ff4b2b[!] Operación financiera abortada."));
            return;
        }

        try {
            if (session.amount == -1) {
                // Paso 1: Leer Cantidad
                int cant = Integer.parseInt(msg);
                if (cant <= 0) throw new NumberFormatException();

                session.amount = cant;
                player.sendMessage(NexoColor.parse("&#a8ff78[✓] Cantidad fijada en " + cant + " unidades."));
                player.sendMessage(NexoColor.parse("&#00fbff[NEXO] Escribe en el chat el &#fbd72bPRECIO POR UNIDAD&#00fbff que ofreces:"));
            } else {
                // Paso 2: Leer Precio
                BigDecimal price = new BigDecimal(msg);
                if (price.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();

                activeSessions.remove(player.getUniqueId());

                // Volver al hilo principal para crear la orden
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (session.orderType.equals("BUY")) {
                        plugin.getBazaarManager().crearOrdenCompra(player, session.itemId, session.amount, price);
                    } else {
                        plugin.getBazaarManager().crearOrdenVenta(player, session.itemId, session.amount, price);
                    }
                });
            }
        } catch (NumberFormatException e) {
            player.sendMessage(NexoColor.parse("&#ff4b2b[!] Valor inválido. Ingresa un número válido o escribe 'cancelar'."));
        }
    }
}