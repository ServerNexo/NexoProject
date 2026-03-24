package me.nexo.economy.trade;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.nexo.core.utils.NexoColor;
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

    private final Map<UUID, TradeSession> activeSessions = new HashMap<>();

    // 🎨 PALETA HEX
    private static final String MSG_SENT = "&#a8ff78[✓] Petición de intercambio enviada a &#fbd72b%target%";
    private static final String MSG_RECEIVED = "&#fbd72b%sender% &#00fbffsolicita abrir un canal de intercambio comercial contigo. Escribe: &#a8ff78/trade accept %sender%";

    public TradeManager(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    public void enviarPeticion(Player sender, Player target) {
        tradeRequests.put(target.getUniqueId(), sender.getUniqueId());
        sender.sendMessage(NexoColor.parse(MSG_SENT.replace("%target%", target.getName())));
        target.sendMessage(NexoColor.parse(MSG_RECEIVED.replace("%sender%", sender.getName())));
    }

    public boolean tienePeticionDe(Player target, Player sender) {
        UUID savedSender = tradeRequests.getIfPresent(target.getUniqueId());
        return savedSender != null && savedSender.equals(sender.getUniqueId());
    }

    public void iniciarTrade(Player player1, Player player2) {
        tradeRequests.invalidate(player1.getUniqueId());
        tradeRequests.invalidate(player2.getUniqueId());

        TradeSession session = new TradeSession(player1, player2);
        activeSessions.put(player1.getUniqueId(), session);
        activeSessions.put(player2.getUniqueId(), session);

        session.open();
    }

    public TradeSession getSession(Player player) { return activeSessions.get(player.getUniqueId()); }

    public void removeSession(TradeSession session) {
        activeSessions.remove(session.getPlayer1().getUniqueId());
        activeSessions.remove(session.getPlayer2().getUniqueId());
    }
}