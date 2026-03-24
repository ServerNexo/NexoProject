package me.nexo.colecciones.menu;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        net.kyori.adventure.text.Component titleComp = event.getView().title();

        // Serializamos el Componente a texto plano (ej: "» Categorías de Colecciones") para hacer comprobaciones seguras
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(titleComp);

        // 🌟 1. MENÚS DE COLECCIONES
        if (titleComp.equals(NexoColor.parse(ColeccionesMenu.TITLE_MAIN)) || plainTitle.startsWith("» Colecciones:")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR || event.getCurrentItem().getItemMeta() == null) return;

            Player player = (Player) event.getWhoClicked();
            NexoColecciones plugin = NexoColecciones.getPlugin(NexoColecciones.class);

            // Clic en Menú Principal
            if (titleComp.equals(NexoColor.parse(ColeccionesMenu.TITLE_MAIN))) {
                String plainItemName = PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().getItemMeta().displayName());
                try {
                    CollectionCategory categoria = CollectionCategory.valueOf(plainItemName.toUpperCase());
                    ColeccionesMenu.abrirSubMenu(player, plugin.getCollectionManager(), categoria);
                } catch (IllegalArgumentException ignored) {}
            }
            // Clic en Sub-Menú
            else if (plainTitle.startsWith("» Colecciones:")) {
                if (event.getCurrentItem().getType() == Material.ARROW) {
                    ColeccionesMenu.abrirMenuPrincipal(player);
                }
            }
            return;
        }

        // 🌟 2. MENÚ DE SLAYERS
        if (titleComp.equals(NexoColor.parse(SlayerMenu.TITLE_MENU))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR || event.getCurrentItem().getItemMeta() == null) return;

            // Extraemos el nombre en plano (ej: "ZOMBIE TIER 1")
            String slayerName = PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().getItemMeta().displayName());
            Player player = (Player) event.getWhoClicked();
            NexoColecciones plugin = NexoColecciones.getPlugin(NexoColecciones.class);

            // Buscamos el ID correcto comparando los nombres en plano
            for (SlayerManager.SlayerTemplate template : plugin.getSlayerManager().getTemplates().values()) {
                // Serializamos también el nombre de la plantilla desde la config para evitar fallos de lectura de color
                String cleanTemplateName = PlainTextComponentSerializer.plainText().serialize(NexoColor.parse(template.name()));

                if (cleanTemplateName.equalsIgnoreCase(slayerName)) {
                    player.closeInventory();
                    plugin.getSlayerManager().iniciarSlayer(player, template.id());
                    break;
                }
            }
        }
    }
}