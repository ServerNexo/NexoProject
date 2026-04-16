package me.nexo.factories.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.menu.FactoryMenu;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * 🏭 NexoFactories - Listener de Interacción con Máquinas (Arquitectura Enterprise)
 */
@Singleton
public class FactoryInteractListener implements Listener {

    private final NexoFactories plugin;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public FactoryInteractListener(NexoFactories plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        // Ignoramos la mano secundaria para que no se ejecute dos veces el código
        if (event.getHand() != EquipmentSlot.HAND) return;

        // Solo nos interesa si hace clic derecho en un bloque
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        // Comprobamos al instante si ese bloque es el núcleo de una fábrica
        ActiveFactory factory = plugin.getFactoryManager().getFactoryAt(clicked.getLocation());

        if (factory != null) {
            event.setCancelled(true); // Cancelamos interactuar con hornos/mesas vanilla
            Player player = event.getPlayer();

            // 🌟 ABRIMOS LA INTERFAZ MODERNIZADA
            // Creamos el menú en el acto y lo abrimos
            new FactoryMenu(player, plugin, factory).open();
        }
    }
}