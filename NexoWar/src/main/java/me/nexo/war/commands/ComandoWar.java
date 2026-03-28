package me.nexo.war.commands;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
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

        NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            player.sendMessage(NexoColor.parse("&#8b0000[!] Acceso Denegado: &#1c0f2aDebes pertenecer a un sindicato para participar en Conflictos Corporativos."));
            return true;
        }

        if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
            player.sendMessage(NexoColor.parse("&#8b0000[!] Autoridad Insuficiente: &#1c0f2aSolo Directores y Oficiales pueden gestionar Contratos de Exterminio."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(NexoColor.parse("&#ff00ff⚔ <bold>TERMINAL DE CONFLICTOS:</bold>"));
            player.sendMessage(NexoColor.parse("&#00f5ff/war challenge <Tag> <Fondos> &#1c0f2a- Inicia un contrato de hostilidad contra un sindicato rival."));
            player.sendMessage(NexoColor.parse("&#00f5ff/war accept &#1c0f2a- Firma el contrato pendiente e inicia el Período de Preparación."));
            return true;
        }

        String sub = args[0].toLowerCase();
        NexoClans clansPlugin = NexoClans.getPlugin(NexoClans.class);

        if (sub.equals("challenge")) {
            if (args.length < 3) {
                player.sendMessage(NexoColor.parse("&#8b0000[!] Sintaxis de Red: &#1c0f2a/war challenge <TagRival> <Fondos>"));
                return true;
            }

            String targetTag = args[1].toUpperCase();
            BigDecimal apuesta;
            try {
                apuesta = new BigDecimal(args[2]);
                if (apuesta.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(NexoColor.parse("&#8b0000[!] Error Financiero: &#1c0f2aLos fondos asignados deben ser mayores a 0."));
                return true;
            }

            Optional<NexoClan> atacanteOpt = clansPlugin.getClanManager().getClanFromCache(user.getClanId());
            if (atacanteOpt.isEmpty()) return true;
            NexoClan atacante = atacanteOpt.get();

            if (atacante.getBankBalance().compareTo(apuesta) < 0) {
                player.sendMessage(NexoColor.parse("&#8b0000[!] Fondos Insuficientes: &#1c0f2aLa bóveda de tu sindicato no puede respaldar esta operación."));
                return true;
            }

            player.sendMessage(NexoColor.parse("&#1c0f2a[⟳] Escaneando red en busca del sindicato rival y auditando sus reservas..."));
            CompletableFuture.runAsync(() -> {
                String sql = "SELECT id FROM nexo_clans WHERE tag = ?";
                try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, targetTag);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        UUID targetId = UUID.fromString(rs.getString("id"));
                        if (targetId.equals(atacante.getId())) {
                            player.sendMessage(NexoColor.parse("&#8b0000[!] Error Lógico: &#1c0f2aNo puedes emitir un contrato de exterminio contra tu propia facción."));
                            return;
                        }

                        clansPlugin.getClanManager().loadClanAsync(targetId, defensor -> {
                            if (defensor == null) return;
                            if (defensor.getBankBalance().compareTo(apuesta) < 0) {
                                player.sendMessage(NexoColor.parse("&#8b0000[!] Auditoría Fallida: &#1c0f2aEl objetivo no posee liquidez para cubrir la cuota de &#ff00ff🪙 " + apuesta));
                                return;
                            }

                            desafiosPendientes.put(targetId, new DesafioPendiente(atacante.getId(), apuesta));
                            player.sendMessage(NexoColor.parse("&#00f5ff[✓] <bold>CONTRATO EMITIDO:</bold> &#1c0f2aDeclaración de hostilidad enviada a " + defensor.getName() + "."));

                            for (Player p : Bukkit.getOnlinePlayers()) {
                                NexoUser tu = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(p.getUniqueId());
                                if (tu != null && tu.getClanId() != null && tu.getClanId().equals(targetId) && (tu.getClanRole().equals("LIDER") || tu.getClanRole().equals("OFICIAL"))) {
                                    p.sendMessage(" ");
                                    p.sendMessage(NexoColor.parse("&#8b0000<bold>⚔ ¡ALERTA DE CONFLICTO CORPORATIVO!</bold>"));
                                    p.sendMessage(NexoColor.parse("&#1c0f2aEl sindicato &#8b0000" + atacante.getName() + " &#1c0f2aha invertido &#ff00ff🪙 " + apuesta + " &#1c0f2apara financiar tu exterminio."));
                                    p.sendMessage(NexoColor.parse("&#1c0f2aSi firmas, los activos de ambas bóvedas serán congelados. El vencedor se lleva el total del fondo."));
                                    p.sendMessage(NexoColor.parse("&#1c0f2aEjecuta &#00f5ff/war accept &#1c0f2apara firmar el contrato."));
                                    p.sendMessage(" ");
                                }
                            }
                        });
                    } else {
                        player.sendMessage(NexoColor.parse("&#8b0000[!] Objetivo No Encontrado: &#1c0f2aNingún sindicato registrado con el tag " + targetTag));
                    }
                } catch (Exception e) {
                    player.sendMessage(NexoColor.parse("&#8b0000[!] Error de Conexión: &#1c0f2aFalla al consultar la base de datos central."));
                }
            });
            return true;
        }

        if (sub.equals("accept")) {
            DesafioPendiente desafio = desafiosPendientes.remove(user.getClanId());
            if (desafio == null) {
                player.sendMessage(NexoColor.parse("&#8b0000[!] Sin Contratos: &#1c0f2aTu sindicato no tiene solicitudes de hostilidad pendientes."));
                return true;
            }

            player.sendMessage(NexoColor.parse("&#1c0f2a[⟳] Procesando firmas e iniciando despliegue táctico..."));
            clansPlugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(defensor -> {
                clansPlugin.getClanManager().loadClanAsync(desafio.clanAtacanteId(), atacante -> {
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