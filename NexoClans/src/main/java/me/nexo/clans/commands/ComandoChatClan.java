package me.nexo.clans.commands;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class ComandoChatClan implements CommandExecutor {

    private final NexoClans plugin;

    // 🎨 PALETA HEX - CONSTANTES DE TEXTO
    private static final String ERR_NO_CLAN = "&#FF5555[!] Comunicación Fallida: No tienes un enlace de clan activo.";
    private static final String ERR_USAGE = "&#FFAA00[!] Sintaxis de red: /cc <mensaje>";

    public ComandoChatClan(NexoClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            player.sendMessage(NexoColor.parse(ERR_NO_CLAN));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(NexoColor.parse(ERR_USAGE));
            return true;
        }

        String mensaje = String.join(" ", args);

        // Obtenemos el clan de la caché para recuperar su nombre (con sus códigos HEX intactos)
        Optional<NexoClan> clanOpt = plugin.getClanManager().getClanFromCache(user.getClanId());
        if (clanOpt.isEmpty()) {
            player.sendMessage(NexoColor.parse(ERR_NO_CLAN));
            return true;
        }

        NexoClan clan = clanOpt.get();

        // 🌟 INYECCIÓN HEX: El nombre del clan se inyecta directamente. El procesador traducirá su código interno.
        net.kyori.adventure.text.Component formatoFinal = NexoColor.parse("&#555555[Clan] " + clan.getName() + " &#555555| &#AAAAAA%player% &#555555» &#00E5FF%message%"
                .replace("%player%", player.getName())
                .replace("%message%", mensaje));

        // Lo enviamos SOLO a los miembros del clan conectados
        for (Player p : Bukkit.getOnlinePlayers()) {
            NexoUser tUser = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(p.getUniqueId());
            if (tUser != null && tUser.hasClan() && tUser.getClanId().equals(user.getClanId())) {
                p.sendMessage(formatoFinal);
            }
        }

        return true;
    }
}