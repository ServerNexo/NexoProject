package me.nexo.items;

import me.nexo.items.estaciones.DesguaceListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ComandoDesguace implements CommandExecutor {

    // 🟢 ARQUITECTURA: Ahora usamos NexoItems
    private final NexoItems plugin;

    public ComandoDesguace(NexoItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p) {
            DesguaceListener desguace = new DesguaceListener(plugin);
            desguace.abrirMenu(p);
        }
        return true;
    }
}