package me.nexo.war.commands;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor; // 🌟 IMPORT AÑADIDO PARA LA PALETA CIBERPUNK
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
    // Memoria RAM temporal para las propuestas de guerra (Se borran al reiniciar, es intencional)
    // Llave: UUID del Clan Defensor | Valor: Record con datos del atacante
    private final Map<UUID, DesafioPendiente> desafiosPendientes = new ConcurrentHashMap<>();

    public ComandoWar(NexoWar plugin) {
        this.plugin = plugin;
    }

    // Un pequeño record interno para guardar la propuesta
    private record DesafioPendiente(UUID clanAtacanteId, BigDecimal apuesta) {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            player.sendMessage(NexoColor.parse("&#FF5555[!] Acceso Denegado: &#AAAAAADebes pertenecer a un sindicato para participar en Conflictos Corporativos."));
            return true;
        }

        if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
            player.sendMessage(NexoColor.parse("&#FF5555[!] Autoridad Insuficiente: &#AAAAAASolo Directores y Oficiales pueden gestionar Contratos de Exterminio."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(NexoColor.parse("&#FFAA00⚔ <bold>TERMINAL DE CONFLICTOS:</bold>"));
            player.sendMessage(NexoColor.parse("&#00E5FF/war challenge <Tag> <Fondos> &#AAAAAA- Inicia un contrato de hostilidad contra un sindicato rival."));
            player.sendMessage(NexoColor.parse("&#00E5FF/war accept &#AAAAAA- Firma el contrato pendiente e inicia el Período de Preparación."));
            return true;
        }

        String sub = args[0].toLowerCase();
        NexoClans clansPlugin = NexoClans.getPlugin(NexoClans.class);

        // ==========================================
        // ⚔️ /war challenge <Tag> <Apuesta>
        // ==========================================
        if (sub.equals("challenge")) {
            if (args.length < 3) {
                player.sendMessage(NexoColor.parse("&#FF5555[!] Sintaxis de Red: &#AAAAAA/war challenge <TagRival> <Fondos>"));
                return true;
            }

            String targetTag = args[1].toUpperCase();
            BigDecimal apuesta;
            try {
                apuesta = new BigDecimal(args[2]);
                if (apuesta.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(NexoColor.parse("&#FF5555[!] Error Financiero: &#AAAAAALos fondos asignados deben ser mayores a 0."));
                return true;
            }

            Optional<NexoClan> atacanteOpt = clansPlugin.getClanManager().getClanFromCache(user.getClanId());
            if (atacanteOpt.isEmpty()) return true;
            NexoClan atacante = atacanteOpt.get();

            // 1. Verificamos fondos del atacante
            if (atacante.getBankBalance().compareTo(apuesta) < 0) {
                player.sendMessage(NexoColor.parse("&#FF5555[!] Fondos Insuficientes: &#AAAAAALa bóveda de tu sindicato no puede respaldar esta operación."));
                return true;
            }

            // 2. Buscamos al clan enemigo en la base de datos (Asíncrono para no dar lag)
            player.sendMessage(NexoColor.parse("&#AAAAAA[⟳] Escaneando red en busca del sindicato rival y auditando sus reservas..."));
            CompletableFuture.runAsync(() -> {
                String sql = "SELECT id FROM nexo_clans WHERE tag = ?";
                try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, targetTag);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        UUID targetId = UUID.fromString(rs.getString("id"));
                        if (targetId.equals(atacante.getId())) {
                            player.sendMessage(NexoColor.parse("&#FF5555[!] Error Lógico: &#AAAAAANo puedes emitir un contrato de exterminio contra tu propia facción."));
                            return;
                        }

                        // 3. Cargamos el clan defensor para comprobar su dinero
                        clansPlugin.getClanManager().loadClanAsync(targetId, defensor -> {
                            if (defensor == null) return;
                            if (defensor.getBankBalance().compareTo(apuesta) < 0) {
                                player.sendMessage(NexoColor.parse("&#FF5555[!] Auditoría Fallida: &#AAAAAAEl objetivo no posee liquidez para cubrir la cuota de &#FFAA00🪙 " + apuesta));
                                return;
                            }

                            // 4. Registrar la propuesta
                            desafiosPendientes.put(targetId, new DesafioPendiente(atacante.getId(), apuesta));
                            player.sendMessage(NexoColor.parse("&#55FF55[✓] <bold>CONTRATO EMITIDO:</bold> &#AAAAAADeclaración de hostilidad enviada a " + defensor.getName() + "."));

                            // 5. Notificar a los líderes del clan enemigo conectados
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                NexoUser tu = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(p.getUniqueId());
                                if (tu != null && tu.getClanId() != null && tu.getClanId().equals(targetId) && (tu.getClanRole().equals("LIDER") || tu.getClanRole().equals("OFICIAL"))) {
                                    p.sendMessage(" ");
                                    p.sendMessage(NexoColor.parse("&#FF5555<bold>⚔ ¡ALERTA DE CONFLICTO CORPORATIVO!</bold>"));
                                    p.sendMessage(NexoColor.parse("&#AAAAAAEl sindicato &#FF5555" + atacante.getName() + " &#AAAAAAha invertido &#FFAA00🪙 " + apuesta + " &#AAAAAApara financiar tu exterminio."));
                                    p.sendMessage(NexoColor.parse("&#AAAAAASi firmas, los activos de ambas bóvedas serán congelados. El vencedor se lleva el total del fondo."));
                                    p.sendMessage(NexoColor.parse("&#AAAAAAEjecuta &#00E5FF/war accept &#AAAAAApara firmar el contrato."));
                                    p.sendMessage(" ");
                                }
                            }
                        });
                    } else {
                        player.sendMessage(NexoColor.parse("&#FF5555[!] Objetivo No Encontrado: &#AAAAAANingún sindicato registrado con el tag " + targetTag));
                    }
                } catch (Exception e) {
                    player.sendMessage(NexoColor.parse("&#FF5555[!] Error de Conexión: &#AAAAAAFalla al consultar la base de datos central."));
                }
            });
            return true;
        }

        // ==========================================
        // 🛡️ /war accept
        // ==========================================
        if (sub.equals("accept")) {
            DesafioPendiente desafio = desafiosPendientes.remove(user.getClanId());
            if (desafio == null) {
                player.sendMessage(NexoColor.parse("&#FF5555[!] Sin Contratos: &#AAAAAATu sindicato no tiene solicitudes de hostilidad pendientes."));
                return true;
            }

            player.sendMessage(NexoColor.parse("&#AAAAAA[⟳] Procesando firmas e iniciando despliegue táctico..."));
            clansPlugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(defensor -> {
                clansPlugin.getClanManager().loadClanAsync(desafio.clanAtacanteId(), atacante -> {
                    if (atacante != null) {
                        // 🌟 ¡AQUÍ ESTALLA LA GUERRA! Llamamos a nuestro WarManager
                        // Pasamos 'player' como primer argumento para que pague los suministros
                        plugin.getWarManager().iniciarDesafio(player, atacante, defensor, desafio.apuesta());
                    }
                });
            });
            return true;
        }

        return true;
    }
}