package me.nexo.economy.trade;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 💰 NexoEconomy - Manager de Intercambios (Arquitectura Enterprise)
 */
@Singleton
public class TradeManager {

    private final NexoEconomy plugin;

    // Caché inteligente: Las peticiones expiran automáticamente a los 60 segundos
    private final Cache<UUID, UUID> tradeRequests = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    private final Map<UUID, TradeSession> activeSessions = new HashMap<>();

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public TradeManager(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    public void enviarPeticion(Player sender, Player target) {
        tradeRequests.put(target.getUniqueId(), sender.getUniqueId());

        // 🌟 FIX: Mensajes Hexadecimales directos. Cero lag de I/O y 100% seguros para Bedrock.
        CrossplayUtils.sendMessage(sender, "&#00f5ff[🤝] <bold>TRADE:</bold> &#E6CCFFHas enviado una petición de intercambio a &#ff00ff" + target.getName() + "&#E6CCFF.");
        CrossplayUtils.sendMessage(target, "&#00f5ff[🤝] <bold>TRADE:</bold> &#ff00ff" + sender.getName() + " &#E6CCFFquiere intercambiar contigo. Usa &#55FF55/trade " + sender.getName() + " &#E6CCFFpara aceptar.");
    }

    public boolean tienePeticionDe(Player target, Player sender) {
        UUID savedSender = tradeRequests.getIfPresent(target.getUniqueId());
        return savedSender != null && savedSender.equals(sender.getUniqueId());
    }

    public void iniciarTrade(Player player1, Player player2) {
        // Limpiamos las peticiones pendientes para evitar spam
        tradeRequests.invalidate(player1.getUniqueId());
        tradeRequests.invalidate(player2.getUniqueId());

        // La TradeSession es instanciada por trade, NO lleva inyección
        TradeSession session = new TradeSession(plugin, player1, player2);
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