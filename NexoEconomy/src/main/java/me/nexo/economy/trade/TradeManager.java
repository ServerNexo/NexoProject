package me.nexo.economy.trade;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.nexo.economy.NexoEconomy;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TradeManager {

    private final NexoEconomy plugin;

    private final Cache<UUID, UUID> tradeRequests = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    // 🌟 AQUÍ GUARDAMOS LOS INTERCAMBIOS ACTIVOS
    private final Map<UUID, TradeSession> activeSessions = new HashMap<>();

    public TradeManager(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    public void enviarPeticion(Player sender, Player target) {
        tradeRequests.put(target.getUniqueId(), sender.getUniqueId());
        sender.sendMessage("§aPetición de intercambio enviada a §e" + target.getName() + "§a.");
        target.sendMessage("§e" + sender.getName() + " §adesea iniciar un intercambio contigo. Usa §e/trade accept " + sender.getName());
    }

    public boolean tienePeticionDe(Player target, Player sender) {
        UUID savedSender = tradeRequests.getIfPresent(target.getUniqueId());
        return savedSender != null && savedSender.equals(sender.getUniqueId());
    }

    public void iniciarTrade(Player player1, Player player2) {
        tradeRequests.invalidate(player1.getUniqueId());
        tradeRequests.invalidate(player2.getUniqueId());

        // 🌟 CREAMOS Y ABRIMOS LA SESIÓN
        TradeSession session = new TradeSession(player1, player2);
        activeSessions.put(player1.getUniqueId(), session);
        activeSessions.put(player2.getUniqueId(), session);

        session.open();
    }

    public TradeSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public void removeSession(TradeSession session) {
        activeSessions.remove(session.getPlayer1().getUniqueId());
        activeSessions.remove(session.getPlayer2().getUniqueId());
    }
}