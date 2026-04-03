package me.nexo.core.commands;

import com.google.inject.Inject;
import me.nexo.core.NexoCore;
import me.nexo.core.menus.VoidBlessingMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * 🏛️ Nexo Network - Comando Void (Arquitectura Enterprise)
 * Cero CommandExecutor, cero chequeos 'instanceof Player'.
 */
public class ComandoVoid {

    // 💉 PILAR 3: Inyección de la instancia principal (ya que VoidBlessingMenu la requiere)
    private final NexoCore plugin;

    @Inject
    public ComandoVoid(NexoCore plugin) {
        this.plugin = plugin;
    }

    // 💡 PILAR 1: Framework de anotaciones
    @Command("void")
    @CommandPermission("nexocore.commands.void")
    public void invocarVacio(Player player) {
        // Al poner "Player" como parámetro, Lamp bloquea automáticamente a la consola.
        // Al poner @CommandPermission, Lamp bloquea a los usuarios sin permiso automáticamente.

        new VoidBlessingMenu(plugin, player).openMenu();
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
    }
}