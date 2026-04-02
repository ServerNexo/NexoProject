package me.nexo.war.managers;

import me.nexo.clans.core.ClanManager;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import me.nexo.war.NexoWar;
import me.nexo.war.core.WarContract;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarManager {

    private final NexoWar plugin;
    private final Map<UUID, WarContract> guerrasActivas = new ConcurrentHashMap<>();

    // 🌟 TEMÁTICA GOTHIC VOID: Requerimos Fragmentos de Almas o Ítems del Vacío para la guerra
    private final NamespacedKey voidEssenceKey;

    private final long GRACE_PERIOD_MILLIS = 5 * 60 * 1000L; // 5 Minutos
    private final int KILLS_TO_WIN = 20;
    private final int COSTO_SUMINISTROS = 100;

    public WarManager(NexoWar plugin) {
        this.plugin = plugin;
        this.voidEssenceKey = new NamespacedKey(plugin, "void_essence"); // Ajustado a la temática
        iniciarRelojDeGuerras();
    }

    // 🌟 LECTOR MÁGICO DE MENSAJES
    private String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    public void iniciarDesafio(Player leader, NexoClan atacante, NexoClan defensor, BigDecimal apuesta) {
        if (atacante.getBankBalance().compareTo(apuesta) < 0 || defensor.getBankBalance().compareTo(apuesta) < 0) {
            leader.sendMessage(NexoColor.parse(getMessage("mensajes.errores.objetivo-sin-fondos").replace("%apuesta%", apuesta.toPlainString())));
            return;
        }

        int contadorSuministros = 0;
        for (ItemStack item : leader.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(voidEssenceKey, PersistentDataType.BYTE)) {
                contadorSuministros += item.getAmount();
            }
        }

        if (contadorSuministros < COSTO_SUMINISTROS) {
            leader.sendMessage(NexoColor.parse(getMessage("mensajes.errores.sin-esencia-guerra")));
            leader.sendMessage(NexoColor.parse("&#E6CCFFRequerido: &#ff00ff" + COSTO_SUMINISTROS + "x Esencia del Vacío &#E6CCFF(Obtenible en Monolitos)."));
            return;
        }

        // Consumir el costo
        int faltanPorCobrar = COSTO_SUMINISTROS;
        for (ItemStack item : leader.getInventory().getContents()) {
            if (faltanPorCobrar <= 0) break;

            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(voidEssenceKey, PersistentDataType.BYTE)) {
                if (item.getAmount() <= faltanPorCobrar) {
                    faltanPorCobrar -= item.getAmount();
                    item.setAmount(0);
                } else {
                    item.setAmount(item.getAmount() - faltanPorCobrar);
                    faltanPorCobrar = 0;
                }
            }
        }

        NexoAPI.getServices().get(ClanManager.class).ifPresent(clanManager -> {
            atacante.withdrawMoney(apuesta.doubleValue());
            defensor.withdrawMoney(apuesta.doubleValue());
            clanManager.saveBankAsync(atacante);
            clanManager.saveBankAsync(defensor);
        });

        UUID warId = UUID.randomUUID();
        WarContract contrato = new WarContract(
                warId, atacante.getId(), defensor.getId(), apuesta,
                System.currentTimeMillis(), WarContract.WarStatus.GRACE_PERIOD, 0, 0
        );

        guerrasActivas.put(warId, contrato);
        saveWarToDatabase(contrato);

        // 🌟 ANUNCIO GLOBAL DEL PACTO
        for (String line : plugin.getConfigManager().getMessages().getStringList("mensajes.alertas.pacto-iniciado")) {
            String broadcast = line.replace("%atacante%", atacante.getName())
                    .replace("%defensor%", defensor.getName())
                    .replace("%total%", apuesta.multiply(BigDecimal.valueOf(2)).toPlainString());
            Bukkit.broadcast(NexoColor.parse(broadcast));
        }
    }

    private void iniciarRelojDeGuerras() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (WarContract guerra : guerrasActivas.values()) {
                    if (guerra.status() == WarContract.WarStatus.GRACE_PERIOD) {
                        if (now - guerra.startTime() >= GRACE_PERIOD_MILLIS) {

                            WarContract activa = new WarContract(
                                    guerra.warId(), guerra.clanAtacante(), guerra.clanDefensor(),
                                    guerra.apuestaMonedas(), now, WarContract.WarStatus.ACTIVE, 0, 0
                            );
                            guerrasActivas.put(guerra.warId(), activa);
                            actualizarGuerraEnBD(activa);

                            // 🌟 ANUNCIO DE QUE LA GUERRA EMPEZÓ
                            for (String line : plugin.getConfigManager().getMessages().getStringList("mensajes.alertas.guerra-activa")) {
                                Bukkit.broadcast(NexoColor.parse(line.replace("%kills%", String.valueOf(KILLS_TO_WIN))));
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public Optional<WarContract> getGuerraEntre(UUID clan1, UUID clan2) {
        return guerrasActivas.values().stream()
                .filter(w -> (w.clanAtacante().equals(clan1) && w.clanDefensor().equals(clan2)) ||
                        (w.clanAtacante().equals(clan2) && w.clanDefensor().equals(clan1)))
                .findFirst();
    }

    public boolean estanEnGuerraActiva(UUID player1, UUID player2) {
        NexoUser u1 = NexoAPI.getInstance().getUserLocal(player1);
        NexoUser u2 = NexoAPI.getInstance().getUserLocal(player2);
        if (u1 == null || !u1.hasClan() || u2 == null || !u2.hasClan()) return false;

        Optional<WarContract> guerra = getGuerraEntre(u1.getClanId(), u2.getClanId());
        return guerra.isPresent() && guerra.get().status() == WarContract.WarStatus.ACTIVE;
    }

    public void registrarBaja(WarContract guerra, UUID clanAsesino, Player asesino, Player victima) {
        boolean esAtacante = clanAsesino.equals(guerra.clanAtacante());
        int killsA = guerra.killsAtacante() + (esAtacante ? 1 : 0);
        int killsD = guerra.killsDefensor() + (!esAtacante ? 1 : 0);

        WarContract actualizada = new WarContract(
                guerra.warId(), guerra.clanAtacante(), guerra.clanDefensor(),
                guerra.apuestaMonedas(), guerra.startTime(), guerra.status(), killsA, killsD
        );
        guerrasActivas.put(guerra.warId(), actualizada);

        // 🌟 MENSAJE DE BAJA CONFIRMADA
        int killsActuales = esAtacante ? killsA : killsD;
        asesino.sendMessage(NexoColor.parse(getMessage("mensajes.exito.baja-confirmada").replace("%actual%", String.valueOf(killsActuales)).replace("%meta%", String.valueOf(KILLS_TO_WIN))));

        if (killsA >= KILLS_TO_WIN || killsD >= KILLS_TO_WIN) {
            terminarGuerra(actualizada, clanAsesino);
        } else {
            actualizarGuerraEnBD(actualizada);
        }
    }

    private void terminarGuerra(WarContract guerra, UUID clanGanador) {
        WarContract guerraFinalizada = new WarContract(guerra.warId(), guerra.clanAtacante(), guerra.clanDefensor(), guerra.apuestaMonedas(), guerra.startTime(), WarContract.WarStatus.FINISHED, guerra.killsAtacante(), guerra.killsDefensor());
        guerrasActivas.remove(guerraFinalizada.warId());
        actualizarGuerraEnBD(guerraFinalizada);

        NexoAPI.getServices().get(ClanManager.class).ifPresent(clanManager -> {
            clanManager.loadClanAsync(clanGanador, clan -> {
                if (clan != null) {
                    BigDecimal premio = guerraFinalizada.apuestaMonedas().multiply(BigDecimal.valueOf(2));
                    clan.depositMoney(premio.doubleValue());
                    clanManager.saveBankAsync(clan);

                    // 🌟 ANUNCIO DE VICTORIA
                    for (String line : plugin.getConfigManager().getMessages().getStringList("mensajes.alertas.victoria")) {
                        Bukkit.broadcast(NexoColor.parse(line.replace("%ganador%", clan.getName()).replace("%premio%", premio.toPlainString())));
                    }
                }
            });
        });
    }

    // ... (Mantén aquí abajo tus métodos saveWarToDatabase y actualizarGuerraEnBD intactos) ...

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