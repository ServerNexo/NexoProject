package me.nexo.war.managers;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.war.NexoWar;
import me.nexo.war.core.WarContract;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarManager {

    private final NexoWar plugin;
    private final Map<UUID, WarContract> guerrasActivas = new ConcurrentHashMap<>();

    // 🌟 CONFIGURACIÓN DE LA GUERRA
    private final long GRACE_PERIOD_MILLIS = 5 * 60 * 1000L; // 5 Minutos
    private final int KILLS_TO_WIN = 20; // Bajas necesarias para ganar el pozo

    public WarManager(NexoWar plugin) {
        this.plugin = plugin;
        iniciarRelojDeGuerras();
    }

    public void iniciarDesafio(NexoClan atacante, NexoClan defensor, BigDecimal apuesta) {
        if (atacante.getBankBalance().compareTo(apuesta) < 0 || defensor.getBankBalance().compareTo(apuesta) < 0) return;

        // 1. Escrow: Retirar dinero
        atacante.withdrawMoney(apuesta.doubleValue());
        defensor.withdrawMoney(apuesta.doubleValue());
        NexoClans.getPlugin(NexoClans.class).getClanManager().saveBankAsync(atacante);
        NexoClans.getPlugin(NexoClans.class).getClanManager().saveBankAsync(defensor);

        // 2. Crear Contrato
        UUID warId = UUID.randomUUID();
        WarContract contrato = new WarContract(
                warId, atacante.getId(), defensor.getId(), apuesta,
                System.currentTimeMillis(), WarContract.WarStatus.GRACE_PERIOD, 0, 0
        );

        guerrasActivas.put(warId, contrato);
        saveWarToDatabase(contrato);

        // 3. Anuncio
        String msg = "<gold><bold>⚔ GUERRA DE HONOR:</bold> <white>El clan <yellow>" + atacante.getName() +
                "</yellow> y <yellow>" + defensor.getName() +
                "</yellow> han congelado <green>🪙 " + apuesta.multiply(BigDecimal.valueOf(2)) + "</green> en el Escrow!";
        Bukkit.broadcast(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg));
        Bukkit.broadcastMessage("§e⏳ Tienen 5 minutos de Gracia para prepararse antes de que caigan las Zonas Seguras.");
    }

    // ⏱️ EL RELOJ MAESTRO
    private void iniciarRelojDeGuerras() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (WarContract guerra : guerrasActivas.values()) {
                    if (guerra.status() == WarContract.WarStatus.GRACE_PERIOD) {
                        if (now - guerra.startTime() >= GRACE_PERIOD_MILLIS) {

                            // 🌟 ¡Empieza la Guerra! Actualizamos el estado
                            WarContract activa = new WarContract(
                                    guerra.warId(), guerra.clanAtacante(), guerra.clanDefensor(),
                                    guerra.apuestaMonedas(), now, WarContract.WarStatus.ACTIVE, 0, 0
                            );
                            guerrasActivas.put(guerra.warId(), activa);
                            actualizarGuerraEnBD(activa);

                            Bukkit.broadcastMessage("§4§l⚔ ¡LA GUERRA HA COMENZADO! ⚔");
                            Bukkit.broadcastMessage("§c¡Las Zonas Seguras enemigas han caído! A por las " + KILLS_TO_WIN + " bajas.");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Chequea cada segundo
    }

    // 🔎 Búsquedas Rápidas (Para el Listener de PvP)
    public Optional<WarContract> getGuerraEntre(UUID clan1, UUID clan2) {
        return guerrasActivas.values().stream()
                .filter(w -> (w.clanAtacante().equals(clan1) && w.clanDefensor().equals(clan2)) ||
                        (w.clanAtacante().equals(clan2) && w.clanDefensor().equals(clan1)))
                .findFirst();
    }

    public boolean estanEnGuerraActiva(UUID player1, UUID player2) {
        NexoUser u1 = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player1);
        NexoUser u2 = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player2);
        if (u1 == null || !u1.hasClan() || u2 == null || !u2.hasClan()) return false;

        Optional<WarContract> guerra = getGuerraEntre(u1.getClanId(), u2.getClanId());
        return guerra.isPresent() && guerra.get().status() == WarContract.WarStatus.ACTIVE;
    }

    // 💀 RASTREADOR DE BAJAS
    public void registrarBaja(WarContract guerra, UUID clanAsesino, Player asesino, Player victima) {
        boolean esAtacante = clanAsesino.equals(guerra.clanAtacante());
        int killsA = guerra.killsAtacante() + (esAtacante ? 1 : 0);
        int killsD = guerra.killsDefensor() + (!esAtacante ? 1 : 0);

        WarContract actualizada = new WarContract(
                guerra.warId(), guerra.clanAtacante(), guerra.clanDefensor(),
                guerra.apuestaMonedas(), guerra.startTime(), guerra.status(), killsA, killsD
        );
        guerrasActivas.put(guerra.warId(), actualizada);

        asesino.sendMessage("§a⚔ ¡Baja confirmada para tu clan! (" + (esAtacante ? killsA : killsD) + "/" + KILLS_TO_WIN + ")");

        if (killsA >= KILLS_TO_WIN || killsD >= KILLS_TO_WIN) {
            terminarGuerra(actualizada, clanAsesino);
        } else {
            actualizarGuerraEnBD(actualizada); // Progreso parcial
        }
    }

    private void terminarGuerra(WarContract guerra, UUID clanGanador) {
        // 🌟 CORRECCIÓN: Creamos una nueva variable (guerraFinalizada) en lugar de sobrescribir el parámetro
        WarContract guerraFinalizada = new WarContract(guerra.warId(), guerra.clanAtacante(), guerra.clanDefensor(), guerra.apuestaMonedas(), guerra.startTime(), WarContract.WarStatus.FINISHED, guerra.killsAtacante(), guerra.killsDefensor());
        guerrasActivas.remove(guerraFinalizada.warId());
        actualizarGuerraEnBD(guerraFinalizada);

        // 💰 Entregar el Premio
        NexoClans clansPlugin = NexoClans.getPlugin(NexoClans.class);
        clansPlugin.getClanManager().loadClanAsync(clanGanador, clan -> {
            if (clan != null) {
                // Usamos guerraFinalizada, que es segura y final para el bloque lambda
                BigDecimal premio = guerraFinalizada.apuestaMonedas().multiply(BigDecimal.valueOf(2));
                clan.depositMoney(premio.doubleValue());
                clansPlugin.getClanManager().saveBankAsync(clan);

                String msg = "<gold><bold>🏆 ¡GUERRA TERMINADA!</bold> <white>El clan <yellow>" + clan.getName() +
                        "</yellow> ha masacrado a sus enemigos y se lleva el botín de <green>🪙 " + premio + "</green>!";
                Bukkit.broadcast(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg));
            }
        });
    }

    // 💾 Persistencia
    private void saveWarToDatabase(WarContract war) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO nexo_wars (id, attacker_id, defender_id, bet_amount, status, kills_attacker, kills_defender) VALUES (CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID), ?, ?, ?, ?)";
            try (java.sql.Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, war.warId().toString());
                ps.setString(2, war.clanAtacante().toString());
                ps.setString(3, war.clanDefensor().toString());
                ps.setBigDecimal(4, war.apuestaMonedas());
                ps.setString(5, war.status().name());
                ps.setInt(6, war.killsAtacante());
                ps.setInt(7, war.killsDefensor());
                ps.executeUpdate();
            } catch (Exception e) {}
        });
    }

    private void actualizarGuerraEnBD(WarContract war) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE nexo_wars SET status = ?, kills_attacker = ?, kills_defender = ? WHERE id = CAST(? AS UUID)";
            try (java.sql.Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, war.status().name());
                ps.setInt(2, war.killsAtacante());
                ps.setInt(3, war.killsDefensor());
                ps.setString(4, war.warId().toString());
                ps.executeUpdate();
            } catch (Exception e) {}
        });
    }
}