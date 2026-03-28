package me.nexo.economy.bazar;

import me.nexo.core.crossplay.CrossplayUtils;
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

        event.setCancelled(true);
        OrderSession session = activeSessions.get(player.getUniqueId());
        String msg = event.getMessage().trim().toLowerCase();

        if (msg.equals("cancelar") || msg.equals("cancel")) {
            activeSessions.remove(player.getUniqueId());
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.bazar.chat.operacion-abortada"));
            return;
        }

        try {
            if (session.amount == -1) {
                int cant = Integer.parseInt(msg);
                if (cant <= 0) throw new NumberFormatException();

                session.amount = cant;
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.bazar.chat.cantidad-fijada").replace("%amount%", String.valueOf(cant)));
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.bazar.chat.solicitar-precio"));
            } else {
                BigDecimal price = new BigDecimal(msg);
                if (price.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();

                activeSessions.remove(player.getUniqueId());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (session.orderType.equals("BUY")) {
                        plugin.getBazaarManager().crearOrdenCompra(player, session.itemId, session.amount, price);
                    } else {
                        plugin.getBazaarManager().crearOrdenVenta(player, session.itemId, session.amount, price);
                    }
                });
            }
        } catch (NumberFormatException e) {
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.bazar.chat.valor-invalido"));
        }
    }
}