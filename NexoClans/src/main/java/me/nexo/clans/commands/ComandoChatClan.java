package me.nexo.clans.commands;

import me.nexo.clans.NexoClans;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoChatClan implements CommandExecutor {

    private final NexoClans plugin;

    // 🎨 PALETA HEX - CONSTANTES DE TEXTO
    private static final String ERR_NO_CLAN = "&#ff4b2bDebes pertenecer a un clan para usar el chat privado.";
    private static final String ERR_USAGE = "&#ff4b2bUso correcto: /cc <mensaje>";
    private static final String CHAT_FORMAT = "&#434343[&#fbd72bClan&#434343] &#00fbff%player% &#434343» &#e0e0e0%message%";

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

        // Unimos el mensaje y aplicamos los colores dinámicamente
        String mensaje = String.join(" ", args);

        // 🌟 CAMBIO AQUÍ: Usamos Component en lugar de String
        net.kyori.adventure.text.Component formatoFinal = NexoColor.parse(CHAT_FORMAT
                .replace("%player%", player.getName())
                .replace("%message%", mensaje));

        // Lo enviamos SOLO a los miembros del clan conectados
        for (Player p : Bukkit.getOnlinePlayers()) {
            NexoUser tUser = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(p.getUniqueId());
            if (tUser != null && tUser.hasClan() && tUser.getClanId().equals(user.getClanId())) {
                p.sendMessage(formatoFinal); // Paper acepta Component sin problemas
            }
        }

        return true;
    }
}