package me.nexo.items.accesorios;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoAccesorios implements CommandExecutor {

    private final NexoItems plugin;

    public ComandoAccesorios(NexoItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            CrossplayUtils.sendMessage(null, plugin.getConfigManager().getMessage("comandos.accesorios.no-jugador"));
            return true;
        }

        if (args.length == 0) {
            plugin.getAccesoriosManager().abrirBolsa(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.accesorios.ayuda.divisor"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.accesorios.ayuda.titulo"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.accesorios.ayuda.abrir"));
            if (player.hasPermission("nexo.admin")) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.accesorios.ayuda.give"));
            }
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.accesorios.ayuda.divisor"));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!player.hasPermission("nexo.admin")) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.accesorios.errores.sin-permiso"));
                return true;
            }

            if (args.length < 3) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.accesorios.errores.uso-give"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.accesorios.errores.offline"));
                return true;
            }

            String accId = args[2].toLowerCase();
            org.bukkit.inventory.ItemStack item = plugin.getAccesoriosManager().generarAccesorio(accId);

            if (item == null) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.accesorios.errores.no-encontrado").replace("%id%", accId));
                return true;
            }

            target.getInventory().addItem(item);
            CrossplayUtils.sendMessage(target, plugin.getConfigManager().getMessage("comandos.accesorios.exito.recibir"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.accesorios.exito.give"));
            return true;
        }

        return true;
    }
}