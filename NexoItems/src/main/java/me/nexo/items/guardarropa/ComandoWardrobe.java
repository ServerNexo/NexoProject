package me.nexo.items.guardarropa;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ComandoWardrobe implements CommandExecutor {

    private final GuardarropaListener listener;

    public ComandoWardrobe(GuardarropaListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player p) {
            if (!p.hasPermission("nexo.wardrobe")) {
                p.sendMessage("§cNo tienes permiso para usar el Guardarropa.");
                return true;
            }
            listener.abrirMenu(p);
        }
        return true;
    }
}