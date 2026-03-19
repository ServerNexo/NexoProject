package me.nexo.items.accesorios;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ComandoAccesorios implements CommandExecutor {

    private final AccesoriosManager manager;

    public ComandoAccesorios(AccesoriosManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player p) {
            manager.abrirBolsa(p);
        }
        return true;
    }
}