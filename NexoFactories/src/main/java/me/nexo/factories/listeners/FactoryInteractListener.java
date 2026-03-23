package me.nexo.factories.listeners;

import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.menu.FactoryMenu;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class FactoryInteractListener implements Listener {

    private final NexoFactories plugin;
    private final FactoryMenu menu;

    public FactoryInteractListener(NexoFactories plugin, FactoryMenu menu) {
        this.plugin = plugin;
        this.menu = menu;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        // Comprobamos al instante si ese bloque es el núcleo de una fábrica
        ActiveFactory factory = plugin.getFactoryManager().getFactoryAt(clicked.getLocation());

        if (factory != null) {
            event.setCancelled(true); // Cancelamos interactuar con hornos/mesas vanilla
            Player player = event.getPlayer();

            // 🌟 ABRIMOS LA INTERFAZ
            menu.openMenu(player, factory);
        }
    }
}