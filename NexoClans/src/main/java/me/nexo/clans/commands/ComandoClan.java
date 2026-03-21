package me.nexo.clans.commands;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class ComandoClan implements CommandExecutor {

    private final NexoClans plugin;

    public ComandoClan(NexoClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando es solo para jugadores.");
            return true;
        }
        Player player = (Player) sender;
        NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());

        if (user == null) {
            player.sendMessage("§cTus datos aún están cargando. Intenta de nuevo en unos segundos.");
            return true;
        }

        // 🌟 /clan (Ver stats)
        if (args.length == 0) {
            if (user.hasClan()) {
                Optional<NexoClan> clanOpt = plugin.getClanManager().getClanFromCache(user.getClanId());
                if (clanOpt.isPresent()) {
                    NexoClan clan = clanOpt.get();
                    player.sendMessage("§8=========================");
                    player.sendMessage("§6§l CLAN: §e" + clan.getName() + " §8[§7" + clan.getTag() + "§8]");
                    player.sendMessage("§8=========================");
                    player.sendMessage("§7Nivel del Monolito: §a" + clan.getMonolithLevel());
                    player.sendMessage("§7Banco de Clan: §2$" + clan.getBankBalance());
                    player.sendMessage("§7Tu Rango: §b" + user.getClanRole());
                    player.sendMessage("§8=========================");
                } else {
                    player.sendMessage("§cCargando datos de tu clan desde la nube... Intenta de nuevo.");
                }
            } else {
                player.sendMessage("§cActualmente no perteneces a ningún clan.");
                player.sendMessage("§7Usa: §e/clan create <tag> <nombre> §7para fundar uno.");
            }
            return true;
        }

        // 🌟 /clan create <tag> <nombre>
        if (args[0].equalsIgnoreCase("create")) {
            if (user.hasClan()) {
                player.sendMessage("§cYa perteneces a un clan. Primero debes abandonarlo.");
                return true;
            }
            if (args.length < 3) {
                player.sendMessage("§cUso correcto: /clan create <Tag> <Nombre>");
                return true;
            }

            String tag = args[1].toUpperCase();
            // Juntamos el resto de los argumentos por si el nombre tiene espacios
            String nombre = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));

            if (tag.length() > 5) {
                player.sendMessage("§cEl Tag no puede tener más de 5 letras.");
                return true;
            }
            if (nombre.length() > 30) {
                player.sendMessage("§cEl Nombre no puede exceder las 30 letras.");
                return true;
            }

            player.sendMessage("§eForjando el clan en la base de datos...");
            plugin.getClanManager().crearClanAsync(player, user, tag, nombre);
            return true;
        }

        player.sendMessage("§7Comando no reconocido. Sub-comandos en construcción 🏗️");
        return true;
    }
}