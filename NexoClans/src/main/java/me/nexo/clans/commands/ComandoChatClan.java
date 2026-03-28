package me.nexo.clans.commands;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class ComandoChatClan implements CommandExecutor {

    private final NexoClans plugin;

    private static final String ERR_NO_CLAN = "&#8b0000[!] Comunicación Fallida: No tienes un enlace de clan activo.";
    private static final String ERR_USAGE = "&#8b0000[!] Sintaxis de red: /cc <mensaje>";

    public ComandoChatClan(NexoClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            CrossplayUtils.sendMessage(player, ERR_NO_CLAN);
            return true;
        }

        if (args.length == 0) {
            CrossplayUtils.sendMessage(player, ERR_USAGE);
            return true;
        }

        String mensaje = String.join(" ", args);

        Optional<NexoClan> clanOpt = plugin.getClanManager().getClanFromCache(user.getClanId());
        if (clanOpt.isEmpty()) {
            CrossplayUtils.sendMessage(player, ERR_NO_CLAN);
            return true;
        }

        NexoClan clan = clanOpt.get();

        String formato = "&#1c0f2a[Clan] " + clan.getName() + " &#1c0f2a| &#ff00ff%player% &#1c0f2a» &#00f5ff%message%"
                .replace("%player%", player.getName())
                .replace("%message%", mensaje);

        for (Player p : Bukkit.getOnlinePlayers()) {
            NexoUser tUser = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(p.getUniqueId());
            if (tUser != null && tUser.hasClan() && tUser.getClanId().equals(user.getClanId())) {
                CrossplayUtils.sendMessage(p, formato);
            }
        }

        return true;
    }
}