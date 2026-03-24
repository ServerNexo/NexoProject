package me.nexo.war.commands;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
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
            player.sendMessage("§cDebes pertenecer a un clan para participar en Guerras de Honor.");
            return true;
        }

        if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
            player.sendMessage("§cSolo los Líderes y Oficiales pueden gestionar las guerras.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e⚔ §lComandos de Guerra:");
            player.sendMessage("§f/war challenge <TagClan> <Apuesta> §7- Desafía a un clan rival.");
            player.sendMessage("§f/war accept §7- Acepta el desafío pendiente y comienza el Período de Gracia.");
            return true;
        }

        String sub = args[0].toLowerCase();
        NexoClans clansPlugin = NexoClans.getPlugin(NexoClans.class);

        // ==========================================
        // ⚔️ /war challenge <Tag> <Apuesta>
        // ==========================================
        if (sub.equals("challenge")) {
            if (args.length < 3) {
                player.sendMessage("§cUso correcto: /war challenge <TagClanEnemigo> <Apuesta>");
                return true;
            }

            String targetTag = args[1].toUpperCase();
            BigDecimal apuesta;
            try {
                apuesta = new BigDecimal(args[2]);
                if (apuesta.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage("§cLa apuesta debe ser un número válido mayor a 0.");
                return true;
            }

            Optional<NexoClan> atacanteOpt = clansPlugin.getClanManager().getClanFromCache(user.getClanId());
            if (atacanteOpt.isEmpty()) return true;
            NexoClan atacante = atacanteOpt.get();

            // 1. Verificamos fondos del atacante
            if (atacante.getBankBalance().compareTo(apuesta) < 0) {
                player.sendMessage("§cEl banco de tu clan no tiene fondos suficientes para respaldar esta apuesta.");
                return true;
            }

            // 2. Buscamos al clan enemigo en la base de datos (Asíncrono para no dar lag)
            player.sendMessage("§7Buscando clan rival y verificando sus fondos...");
            CompletableFuture.runAsync(() -> {
                String sql = "SELECT id FROM nexo_clans WHERE tag = ?";
                try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, targetTag);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        UUID targetId = UUID.fromString(rs.getString("id"));
                        if (targetId.equals(atacante.getId())) {
                            player.sendMessage("§cNo puedes declarar la guerra a tu propio clan. ¿Estás loco?");
                            return;
                        }

                        // 3. Cargamos el clan defensor para comprobar su dinero
                        clansPlugin.getClanManager().loadClanAsync(targetId, defensor -> {
                            if (defensor == null) return;
                            if (defensor.getBankBalance().compareTo(apuesta) < 0) {
                                player.sendMessage("§cEl clan enemigo es demasiado pobre. No pueden cubrir la apuesta de 🪙 " + apuesta);
                                return;
                            }

                            // 4. Registrar la propuesta
                            desafiosPendientes.put(targetId, new DesafioPendiente(atacante.getId(), apuesta));
                            player.sendMessage("§a¡El desafío de sangre ha sido enviado a " + defensor.getName() + "!");

                            // 5. Notificar a los líderes del clan enemigo conectados
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                NexoUser tu = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(p.getUniqueId());
                                if (tu != null && tu.getClanId() != null && tu.getClanId().equals(targetId) && (tu.getClanRole().equals("LIDER") || tu.getClanRole().equals("OFICIAL"))) {
                                    p.sendMessage(" ");
                                    p.sendMessage("§4§l⚔ ¡DECLARACIÓN DE GUERRA!");
                                    p.sendMessage("§eEl clan §c" + atacante.getName() + " §eha puesto §6🪙 " + apuesta + " §esobre la mesa.");
                                    p.sendMessage("§eSi aceptan, el dinero de ambos bancos será congelado. El ganador se lo lleva TODO.");
                                    p.sendMessage("§7Usa §a/war accept §7para aceptar el desafío.");
                                    p.sendMessage(" ");
                                }
                            }
                        });
                    } else {
                        player.sendMessage("§cNo se encontró ningún clan con el tag " + targetTag);
                    }
                } catch (Exception e) {
                    player.sendMessage("§cError de base de datos al buscar el clan.");
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
                player.sendMessage("§cTu clan no tiene ningún desafío de guerra pendiente.");
                return true;
            }

            player.sendMessage("§7Iniciando preparativos de guerra...");
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