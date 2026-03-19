package me.nexo.items.mochilas;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ComandoPV implements CommandExecutor {

    private final MochilaManager manager;

    public ComandoPV(MochilaManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        if (args.length != 1) {
            p.sendMessage("§cUso: /pv <número>");
            return true;
        }

        try {
            int id = Integer.parseInt(args[0]);

            if (id < 1 || id > 50) {
                p.sendMessage("§cEl número de mochila debe estar entre 1 y 50.");
                return true;
            }

            // Permiso dinámico (ej: nexo.pv.1, nexo.pv.2, etc.)
            if (!p.hasPermission("nexo.pv." + id) && !p.hasPermission("nexo.admin")) {
                p.sendMessage("§c§l🔒 §cNo tienes acceso a la Mochila #" + id + ".");
                return true;
            }

            p.sendMessage("§eAbriendo Mochila #" + id + "...");
            manager.abrirMochila(p, id);

        } catch (NumberFormatException e) {
            p.sendMessage("§cPor favor ingresa un número válido.");
        }

        return true;
    }
}