package me.nexo.clans.commands;

import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoChatClan implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            player.sendMessage("§cDebes pertenecer a un clan para usar el chat privado.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUso correcto: /c <mensaje>");
            return true;
        }

        // Unimos el mensaje
        String mensaje = String.join(" ", args);
        String formato = "§8[§eClan§8] §b" + player.getName() + " §8» §f" + mensaje;

        // Lo enviamos SOLO a los miembros del clan conectados
        for (Player p : Bukkit.getOnlinePlayers()) {
            NexoUser tUser = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(p.getUniqueId());
            if (tUser != null && tUser.hasClan() && tUser.getClanId().equals(user.getClanId())) {
                p.sendMessage(formato);
            }
        }

        return true;
    }
}