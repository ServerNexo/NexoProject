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
    private final NexoCore core;

    public ComandoChatClan(NexoClans plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    private String getMessage(String path) {
        return core.getConfigManager().getMessage("clans_messages.yml", path);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        NexoUser user = core.getUserManager().getUserOrNull(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            CrossplayUtils.sendMessage(player, getMessage("comandos.chat.errores.sin-clan"));
            return true;
        }

        if (args.length == 0) {
            CrossplayUtils.sendMessage(player, getMessage("comandos.chat.errores.uso"));
            return true;
        }

        String mensaje = String.join(" ", args);

        Optional<NexoClan> clanOpt = plugin.getClanManager().getClanFromCache(user.getClanId());
        if (clanOpt.isEmpty()) {
            CrossplayUtils.sendMessage(player, getMessage("comandos.chat.errores.sin-clan"));
            return true;
        }

        NexoClan clan = clanOpt.get();

        String formato = getMessage("comandos.chat.formato")
                .replace("%clan_name%", clan.getName())
                .replace("%player%", player.getName())
                .replace("%message%", mensaje);

        for (Player p : Bukkit.getOnlinePlayers()) {
            NexoUser tUser = core.getUserManager().getUserOrNull(p.getUniqueId());
            if (tUser != null && tUser.hasClan() && tUser.getClanId().equals(user.getClanId())) {
                CrossplayUtils.sendMessage(p, formato);
            }
        }

        return true;
    }
}