package me.nexo.war.commands;

import me.nexo.clans.core.ClanManager;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import me.nexo.war.NexoWar;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ComandoWar implements CommandExecutor {

    private final NexoWar plugin;
    private final Map<UUID, DesafioPendiente> desafiosPendientes = new ConcurrentHashMap<>();

    public ComandoWar(NexoWar plugin) {
        this.plugin = plugin;
    }

    // 🌟 LECTOR MÁGICO DE MENSAJES (Arquitectura Omega)
    private String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    private record DesafioPendiente(UUID clanAtacanteId, BigDecimal apuesta) {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        NexoUser user = NexoAPI.getInstance().getUserLocal(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.sin-clan")));
            return true;
        }

        // En un culto, LIDER = Señor Oscuro, OFICIAL = Apóstol/Sacerdote
        if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
            player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.rango-insuficiente")));
            return true;
        }

        if (args.length == 0) {
            for (String line : plugin.getConfigManager().getMessages().getStringList("mensajes.ayuda-comando")) {
                player.sendMessage(NexoColor.parse(line));
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        Optional<ClanManager> clanManagerOpt = NexoAPI.getServices().get(ClanManager.class);
        if (clanManagerOpt.isEmpty()) {
            player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.servicio-clanes-offline")));
            return true;
        }
        ClanManager clanManager = clanManagerOpt.get();

        if (sub.equals("challenge")) {
            if (args.length < 3) {
                player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.sintaxis-challenge")));
                return true;
            }

            String targetTag = args[1].toUpperCase();
            BigDecimal apuesta;
            try {
                apuesta = new BigDecimal(args[2]);
                if (apuesta.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.apuesta-invalida")));
                return true;
            }

            Optional<NexoClan> atacanteOpt = clanManager.getClanFromCache(user.getClanId());
            if (atacanteOpt.isEmpty()) return true;
            NexoClan atacante = atacanteOpt.get();

            if (atacante.getBankBalance().compareTo(apuesta) < 0) {
                player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.fondos-insuficientes")));
                return true;
            }

            player.sendMessage(NexoColor.parse(getMessage("mensajes.procesos.escaneando-red")));
            CompletableFuture.runAsync(() -> {
                String sql = "SELECT id FROM nexo_clans WHERE tag = ?";
                try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, targetTag);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        UUID targetId = UUID.fromString(rs.getString("id"));
                        if (targetId.equals(atacante.getId())) {
                            player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.auto-ataque")));
                            return;
                        }

                        clanManager.loadClanAsync(targetId, defensor -> {
                            if (defensor == null) return;
                            if (defensor.getBankBalance().compareTo(apuesta) < 0) {
                                String msg = getMessage("mensajes.errores.objetivo-sin-fondos").replace("%apuesta%", apuesta.toPlainString());
                                player.sendMessage(NexoColor.parse(msg));
                                return;
                            }

                            desafiosPendientes.put(targetId, new DesafioPendiente(atacante.getId(), apuesta));
                            String msgEmitido = getMessage("mensajes.exito.contrato-emitido").replace("%defensor%", defensor.getName());
                            player.sendMessage(NexoColor.parse(msgEmitido));

                            // ALERTA AL CULTO DEFENSOR
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                NexoUser tu = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(p.getUniqueId());
                                if (tu != null && tu.getClanId() != null && tu.getClanId().equals(targetId) && (tu.getClanRole().equals("LIDER") || tu.getClanRole().equals("OFICIAL"))) {
                                    for (String line : plugin.getConfigManager().getMessages().getStringList("mensajes.alertas.declaracion-guerra")) {
                                        String alertMsg = line.replace("%atacante%", atacante.getName()).replace("%apuesta%", apuesta.toPlainString());
                                        p.sendMessage(NexoColor.parse(alertMsg));
                                    }
                                }
                            }
                        });
                    } else {
                        String msgNoEncontrado = getMessage("mensajes.errores.objetivo-no-encontrado").replace("%tag%", targetTag);
                        player.sendMessage(NexoColor.parse(msgNoEncontrado));
                    }
                } catch (Exception e) {
                    player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.error-base-datos")));
                }
            });
            return true;
        }

        if (sub.equals("accept")) {
            DesafioPendiente desafio = desafiosPendientes.remove(user.getClanId());
            if (desafio == null) {
                player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.sin-contratos")));
                return true;
            }

            player.sendMessage(NexoColor.parse(getMessage("mensajes.procesos.iniciando-despliegue")));
            clanManager.getClanFromCache(user.getClanId()).ifPresent(defensor -> {
                clanManager.loadClanAsync(desafio.clanAtacanteId(), atacante -> {
                    if (atacante != null) {
                        plugin.getWarManager().iniciarDesafio(player, atacante, defensor, desafio.apuesta());
                    }
                });
            });
            return true;
        }

        return true;
    }
}