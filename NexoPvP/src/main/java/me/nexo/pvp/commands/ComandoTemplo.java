package me.nexo.pvp.commands;

import com.google.inject.Inject;
import me.nexo.pvp.menus.BlessingMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;

/**
 * 🏛️ NexoPvP - Comando Templo (Arquitectura Enterprise)
 */
public class ComandoTemplo {

    @Inject
    public ComandoTemplo() {
        // Inyectaremos herramientas aquí cuando refactoricemos BlessingMenu a Type-Safe
    }

    @Command("templo")
    public void abrirTemplo(Player player) {
        new BlessingMenu(player).open();
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2.0f);
    }
}