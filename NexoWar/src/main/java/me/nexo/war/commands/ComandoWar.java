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

    private record DesafioPendiente(UUID clanAtacanteId, BigDecimal apuesta) {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        NexoUser user = NexoAPI.getInstance().getUserLocal(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            player.sendMessage(NexoColor.parse("&#8b0000[!] Acceso Denegado: &#E6CCFFDebes pertenecer a un sindicato para participar en Conflictos Corporativos."));
            return true;
        }

        if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
            player.sendMessage(NexoColor.parse("&#8b0000[!] Autoridad Insuficiente: &#E6CCFFSolo Directores y Oficiales pueden gestionar Contratos de Exterminio."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(NexoColor.parse("&#ff00ff⚔ <bold>TERMINAL DE CONFLICTOS:</bold>"));
            player.sendMessage(NexoColor.parse("&#00f5ff/war challenge <Tag> <Fondos> &#E6CCFF- Inicia un contrato de hostilidad contra un sindicato rival."));
            player.sendMessage(NexoColor.parse("&#00f5ff/war accept &#E6CCFF- Firma el contrato pendiente e inicia el Período de Preparación."));
            return true;
        }

        String sub = args[0].toLowerCase();
        Optional<ClanManager> clanManagerOpt = NexoAPI.getServices().get(ClanManager.class);
        if (clanManagerOpt.isEmpty()) {
            player.sendMessage(NexoColor.parse("&#8b0000[!] Error Crítico: El servicio de Clanes no está disponible."));
            return true;
        }
        ClanManager clanManager = clanManagerOpt.get();

        if (sub.equals("challenge")) {
            if (args.length < 3) {
                player.sendMessage(NexoColor.parse("&#8b0000[!] Sintaxis de Red: &#E6CCFF/war challenge <TagRival> <Fondos>"));
                return true;
            }

            String targetTag = args[1].toUpperCase();
            BigDecimal apuesta;
            try {
                apuesta = new BigDecimal(args[2]);
                if (apuesta.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(NexoColor.parse("&#8b0000[!] Error Financiero: &#E6CCFFLos fondos asignados deben ser mayores a 0."));
                return true;
            }

            Optional<NexoClan> atacanteOpt = clanManager.getClanFromCache(user.getClanId());
            if (atacanteOpt.isEmpty()) return true;
            NexoClan atacante = atacanteOpt.get();

            if (atacante.getBankBalance().compareTo(apuesta) < 0) {
                player.sendMessage(NexoColor.parse("&#8b0000[!] Fondos Insuficientes: &#E6CCFFLa bóveda de tu sindicato no puede respaldar esta operación."));
                return true;
            }

            player.sendMessage(NexoColor.parse("&#E6CCFF[⟳] Escaneando red en busca del sindicato rival y auditando sus reservas..."));
            CompletableFuture.runAsync(() -> {
                String sql = "SELECT id FROM nexo_clans WHERE tag = ?";
                try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, targetTag);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        UUID targetId = UUID.fromString(rs.getString("id"));
                        if (targetId.equals(atacante.getId())) {
                            player.sendMessage(NexoColor.parse("&#8b0000[!] Error Lógico: &#E6CCFFNo puedes emitir un contrato de exterminio contra tu propia facción."));
                            return;
                        }

                        clanManager.loadClanAsync(targetId, defensor -> {
                            if (defensor == null) return;
                            if (defensor.getBankBalance().compareTo(apuesta) < 0) {
                                player.sendMessage(NexoColor.parse("&#8b0000[!] Auditoría Fallida: &#E6CCFFEl objetivo no posee liquidez para cubrir la cuota de &#ff00ff🪙 " + apuesta));
                                return;
                            }

                            desafiosPendientes.put(targetId, new DesafioPendiente(atacante.getId(), apuesta));
                            player.sendMessage(NexoColor.parse("&#00f5ff[✓] <bold>CONTRATO EMITIDO:</bold> &#E6CCFFDeclaración de hostilidad enviada a " + defensor.getName() + "."));

                            for (Player p : Bukkit.getOnlinePlayers()) {
                                NexoUser tu = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(p.getUniqueId());
                                if (tu != null && tu.getClanId() != null && tu.getClanId().equals(targetId) && (tu.getClanRole().equals("LIDER") || tu.getClanRole().equals("OFICIAL"))) {
                                    p.sendMessage(" ");
                                    p.sendMessage(NexoColor.parse("&#8b0000<bold>⚔ ¡ALERTA DE CONFLICTO CORPORATIVO!</bold>"));
                                    p.sendMessage(NexoColor.parse("&#E6CCFFEl sindicato &#8b0000" + atacante.getName() + " &#E6CCFFha invertido &#ff00ff🪙 " + apuesta + " &#E6CCFFpara financiar tu exterminio."));
                                    p.sendMessage(NexoColor.parse("&#E6CCFFSi firmas, los activos de ambas bóvedas serán congelados. El vencedor se lleva el total del fondo."));
                                    p.sendMessage(NexoColor.parse("&#E6CCFFEjecuta &#00f5ff/war accept &#E6CCFFpara firmar el contrato."));
                                    p.sendMessage(" ");
                                }
                            }
                        });
                    } else {
                        player.sendMessage(NexoColor.parse("&#8b0000[!] Objetivo No Encontrado: &#E6CCFFNingún sindicato registrado con el tag " + targetTag));
                    }
                } catch (Exception e) {
                    player.sendMessage(NexoColor.parse("&#8b0000[!] Error de Conexión: &#E6CCFFFalla al consultar la base de datos central."));
                }
            });
            return true;
        }

        if (sub.equals("accept")) {
            DesafioPendiente desafio = desafiosPendientes.remove(user.getClanId());
            if (desafio == null) {
                player.sendMessage(NexoColor.parse("&#8b0000[!] Sin Contratos: &#E6CCFFTu sindicato no tiene solicitudes de hostilidad pendientes."));
                return true;
            }

            player.sendMessage(NexoColor.parse("&#E6CCFF[⟳] Procesando firmas e iniciando despliegue táctico..."));
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