package me.nexo.war.managers;

import me.nexo.clans.core.NexoClan;
import me.nexo.clans.NexoClans;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import me.nexo.war.NexoWar;
import me.nexo.war.core.WarContract;
import org.bukkit.Bukkit;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarManager {

    private final NexoWar plugin;
    private final Map<UUID, WarContract> guerrasActivas = new ConcurrentHashMap<>();

    public WarManager(NexoWar plugin) {
        this.plugin = plugin;
        // 🚧 Aquí llamaríamos a cargarGuerrasDesdeDB() en el futuro
    }

    public void iniciarDesafio(NexoClan atacante, NexoClan defensor, BigDecimal apuesta) {
        // 1. Verificar fondos en ambos bancos (NexoClans)
        if (atacante.getBankBalance().compareTo(apuesta) < 0 ||
                defensor.getBankBalance().compareTo(apuesta) < 0) {
            return;
        }

        // 2. ESCROW: Retirar dinero de los bancos y "congelarlo"
        atacante.withdrawMoney(apuesta.doubleValue());
        defensor.withdrawMoney(apuesta.doubleValue());

        // Actualizar base de datos de clanes asíncronamente
        NexoClans.getPlugin(NexoClans.class).getClanManager().saveBankAsync(atacante);
        NexoClans.getPlugin(NexoClans.class).getClanManager().saveBankAsync(defensor);

        // 3. Crear el Contrato
        UUID warId = UUID.randomUUID();
        WarContract contrato = new WarContract(
                warId, atacante.getId(), defensor.getId(), apuesta,
                System.currentTimeMillis(), WarContract.WarStatus.GRACE_PERIOD, 0, 0
        );

        guerrasActivas.put(warId, contrato);
        saveWarToDatabase(contrato);

        // 4. Anuncio Global usando Adventure (MiniMessage)
        String msg = "<gold><bold>⚔ GUERRA DE HONOR:</bold> <white>El clan <yellow>" + atacante.getName() +
                "</yellow> ha desafiado a <yellow>" + defensor.getName() +
                "</yellow> por un pozo de <green>🪙 " + apuesta.multiply(new BigDecimal(2)) + "</green>!";
        Bukkit.broadcast(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg));
    }

    private void saveWarToDatabase(WarContract war) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO nexo_wars (id, attacker_id, defender_id, bet_amount, status) VALUES (CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID), ?, ?)";
            try (java.sql.Connection conn = me.nexo.core.NexoCore.getPlugin(me.nexo.core.NexoCore.class).getDatabaseManager().getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, war.warId().toString());
                ps.setString(2, war.clanAtacante().toString());
                ps.setString(3, war.clanDefensor().toString());
                ps.setBigDecimal(4, war.apuestaMonedas());
                ps.setString(5, war.status().name());
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    public Map<UUID, WarContract> getGuerrasActivas() { return guerrasActivas; }
}