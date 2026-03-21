package me.nexo.clans.commands;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class ComandoClan implements CommandExecutor {

    private final NexoClans plugin;

    public ComandoClan(NexoClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());

        if (user == null) { player.sendMessage("§cTus datos aún están cargando."); return true; }

        if (args.length == 0) {
            if (user.hasClan()) {
                Optional<NexoClan> clanOpt = plugin.getClanManager().getClanFromCache(user.getClanId());
                clanOpt.ifPresentOrElse(
                        clan -> me.nexo.clans.menu.ClanMenu.abrirMenu(player, clan, user),
                        () -> player.sendMessage("§cCargando datos...")
                );
            } else {
                player.sendMessage("§cNo tienes clan. Usa /clan create <tag> <nombre>");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // (Mantenemos los comandos de siempre)
        if (subCommand.equals("create")) { /* Tu lógica de create que ya estaba... copiada abreviada o usa la tuya */
            if (user.hasClan()) { player.sendMessage("§cYa perteneces a un clan."); return true; }
            if (args.length < 3) { player.sendMessage("§cUso: /clan create <Tag> <Nombre>"); return true; }
            String tag = args[1].toUpperCase();
            String nombre = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            plugin.getClanManager().crearClanAsync(player, user, tag, nombre);
            return true;
        }

        if (subCommand.equals("invite")) {
            if (!user.hasClan() || (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL"))) return true;
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target != null) plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> plugin.getClanManager().invitarJugador(player, target, clan));
            return true;
        }

        if (subCommand.equals("join")) {
            UUID inv = plugin.getClanManager().getInvitacionPendiente(player);
            if (inv != null) plugin.getClanManager().loadClanAsync(inv, clan -> plugin.getClanManager().unirseClanAsync(player, user, clan));
            return true;
        }

        if (subCommand.equals("leave")) {
            if (user.hasClan() && !user.getClanRole().equals("LIDER")) plugin.getClanManager().abandonarClanAsync(player, user);
            return true;
        }

        if (subCommand.equals("ff") || subCommand.equals("friendlyfire")) {
            if (user.hasClan() && (user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL"))) {
                plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> plugin.getClanManager().toggleFriendlyFireAsync(clan, player, !clan.isFriendlyFire()));
            }
            return true;
        }

        if (subCommand.equals("kick")) {
            if (!user.hasClan() || (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL"))) return true;
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target != null) {
                NexoUser tUser = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(target.getUniqueId());
                if (tUser != null && tUser.getClanId().equals(user.getClanId())) plugin.getClanManager().expulsarJugadorAsync(player, target, tUser);
            }
            return true;
        }

        if (subCommand.equals("disband")) {
            if (user.hasClan() && user.getClanRole().equals("LIDER")) plugin.getClanManager().disolverClanAsync(player, user, user.getClanId());
            return true;
        }

        // 🌟 NUEVO: /clan sethome
        if (subCommand.equals("sethome")) {
            if (!user.hasClan() || !user.getClanRole().equals("LIDER")) {
                player.sendMessage("§cSolo el Líder puede establecer la base del Monolito.");
                return true;
            }
            plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                plugin.getClanManager().setClanHomeAsync(clan, player, player.getLocation());
            });
            return true;
        }

        // 🌟 NUEVO: /clan home
        if (subCommand.equals("home")) {
            if (!user.hasClan()) {
                player.sendMessage("§cNo perteneces a ningún clan.");
                return true;
            }
            plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                if (clan.getPublicHome() == null || clan.getPublicHome().isEmpty()) {
                    player.sendMessage("§cTu clan aún no ha establecido una base. Dile al líder que use /clan sethome.");
                    return;
                }
                try {
                    String[] parts = clan.getPublicHome().split(";");
                    World w = Bukkit.getWorld(parts[0]);
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    float yaw = Float.parseFloat(parts[4]);
                    float pitch = Float.parseFloat(parts[5]);
                    player.teleport(new Location(w, x, y, z, yaw, pitch));
                    player.sendMessage("§aTeletransportado a la base del clan. 🏛️");
                } catch (Exception e) {
                    player.sendMessage("§cError leyendo las coordenadas de la base.");
                }
            });
            return true;
        }

        player.sendMessage("§7Sub-comandos: §ecreate, invite, join, leave, ff, kick, disband, sethome, home.");
        return true;
    }
}