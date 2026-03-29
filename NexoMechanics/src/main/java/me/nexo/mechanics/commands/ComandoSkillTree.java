package me.nexo.mechanics.commands;

import me.nexo.mechanics.skills.SkillTreeMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ComandoSkillTree implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por un jugador.");
            return true;
        }

        // Aquí asumimos que el menú del árbol de habilidades es estático o se obtiene de un manager
        // Si SkillTreeMenu necesita una instancia del plugin, habría que pasarla en el constructor.
        // Por ahora, lo mantenemos simple.
        // new SkillTreeMenu(plugin).openMenu(player);
        player.sendMessage("Abriendo el árbol de habilidades..."); // Placeholder

        return true;
    }
}