package me.nexo.mechanics.commands;

import me.nexo.core.utils.NexoColor;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.skills.SkillTreeMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ComandoSkillTree implements CommandExecutor {

    private final NexoMechanics plugin;

    // 🌟 Requerimos el plugin en el constructor para poder abrir el menú
    public ComandoSkillTree(NexoMechanics plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            // 🌟 LECTURA DESDE CONFIGURACIÓN
            sender.sendMessage(NexoColor.parse(plugin.getConfigManager().getMessage("mensajes.errores.solo-jugadores")));
            return true;
        }

        // 🌟 ABRIMOS EL MENÚ OMEGA
        new SkillTreeMenu(player, plugin).open();
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);

        return true;
    }
}