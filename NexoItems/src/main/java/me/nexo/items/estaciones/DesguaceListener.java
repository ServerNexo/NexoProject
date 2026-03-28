package me.nexo.items.estaciones;

import me.nexo.core.utils.NexoColor;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.NexoItems;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashMap;

public class DesguaceListener implements Listener {

    private final NexoItems plugin;

    public static final String TITLE_PLAIN = "» Desguace del Nexo";
    public static final String MENU_TITLE = "&#1c0f2a<bold>»</bold> &#8b0000Desguace del Nexo";

    public DesguaceListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    private String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }

    public void abrirMenu(Player jugador) {
        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse(MENU_TITLE));

        ItemStack cristal = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        if (metaCristal != null) {
            metaCristal.setDisplayName(serialize(" "));
            cristal.setItemMeta(metaCristal);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, cristal);
        }

        inv.setItem(11, new ItemStack(Material.AIR));

        ItemStack trituradora = new ItemStack(Material.BLAST_FURNACE);
        ItemMeta metaTrituradora = trituradora.getItemMeta();
        if (metaTrituradora != null) {
            metaTrituradora.setDisplayName(serialize("&#8b0000<bold>DESTRUIR ACTIVO</bold>"));
            metaTrituradora.setLore(Arrays.asList(
                    serialize("&#1c0f2aHaz clic aquí para destruir el"),
                    serialize("&#1c0f2aactivo de la izquierda y reciclarlo"),
                    serialize("&#1c0f2aen &#ff00ffPolvo Estelar&#1c0f2a."),
                    serialize(" "),
                    serialize("&#8b0000<bold>⚠ ESTA ACCIÓN ES IRREVERSIBLE ⚠</bold>")
            ));
            trituradora.setItemMeta(metaTrituradora);
        }
        inv.setItem(15, trituradora);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.equals(TITLE_PLAIN)) return;

        Player jugador = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot >= 27) return;

        if (slot != 11 && slot != 15) {
            event.setCancelled(true);
            return;
        }

        if (slot == 15) {
            event.setCancelled(true);
            Inventory inv = event.getInventory();
            ItemStack arma = inv.getItem(11);

            if (arma == null || arma.getType() == Material.AIR) {
                jugador.sendMessage(NexoColor.parse("&#8b0000[!] Inserta un activo en la bahía de desguace (izquierda)."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (!arma.hasItemMeta() || !arma.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER)) {
                jugador.sendMessage(NexoColor.parse("&#8b0000[!] Activo incompatible. No posee la firma mágica del Nexo requerida para reciclar."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            int nivel = arma.getItemMeta().getPersistentDataContainer().getOrDefault(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER, 0);
            int cantidadPolvo = 1 + nivel;

            inv.setItem(11, new ItemStack(Material.AIR));

            ItemStack recompensa = ItemManager.crearPolvoEstelar();
            recompensa.setAmount(cantidadPolvo);

            HashMap<Integer, ItemStack> sobrante = jugador.getInventory().addItem(recompensa);
            if (!sobrante.isEmpty()) {
                jugador.getWorld().dropItemNaturally(jugador.getLocation(), sobrante.get(0));
            }

            jugador.sendMessage(NexoColor.parse("&#00f5ff[✓] <bold>¡RECICLAJE EXITOSO!</bold> Materia recuperada: &#ff00ff" + cantidadPolvo + "x Polvo Estelar."));
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.equals(TITLE_PLAIN)) return;

        Player jugador = (Player) event.getPlayer();
        ItemStack arma = event.getInventory().getItem(11);

        if (arma != null && arma.getType() != Material.AIR) {
            jugador.getInventory().addItem(arma);
        }
    }
}