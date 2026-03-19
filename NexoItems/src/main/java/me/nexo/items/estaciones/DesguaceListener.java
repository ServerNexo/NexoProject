package me.nexo.items.estaciones;

import me.nexo.items.managers.ItemManager;
import me.nexo.items.NexoItems;
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

import java.util.HashMap;
import java.util.List;

public class DesguaceListener implements Listener {

    // 🟢 ARQUITECTURA: Usamos la clase principal de NexoItems
    private final NexoItems plugin;
    private final String TITULO_MENU = "§8♻ Desguace del Nexo";

    public DesguaceListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    public void abrirMenu(Player jugador) {
        Inventory inv = Bukkit.createInventory(null, 27, TITULO_MENU);

        // Decoración
        ItemStack cristal = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        metaCristal.setDisplayName(" ");
        cristal.setItemMeta(metaCristal);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, cristal);
        }

        // Slot de entrada
        inv.setItem(11, new ItemStack(Material.AIR));

        // Botón de Destruir
        ItemStack trituradora = new ItemStack(Material.BLAST_FURNACE);
        ItemMeta metaTrituradora = trituradora.getItemMeta();
        metaTrituradora.setDisplayName("§c§lDESTRUIR ÍTEM");
        metaTrituradora.setLore(List.of(
                "§7Haz clic aquí para destruir el",
                "§7arma de la izquierda y convertirla",
                "§7en §ePolvo Estelar§7.",
                "",
                "§4⚠ ¡ESTA ACCIÓN NO SE PUEDE DESHACER ⚠"
        ));
        trituradora.setItemMeta(metaTrituradora);
        inv.setItem(15, trituradora);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MENU)) return;

        Player jugador = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Inventario del jugador
        if (slot >= 27) return;

        // Bloquear cristales
        if (slot != 11 && slot != 15) {
            event.setCancelled(true);
            return;
        }

        // CLIC EN DESTRUIR
        if (slot == 15) {
            event.setCancelled(true);
            Inventory inv = event.getInventory();
            ItemStack arma = inv.getItem(11);

            if (arma == null || arma.getType() == Material.AIR) {
                jugador.sendMessage("§cColoca un arma RPG en la ranura vacía.");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (!arma.hasItemMeta() || !arma.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER)) {
                jugador.sendMessage("§cEse ítem no es un arma mágica del Nexo. No se puede reciclar.");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Calculamos cuánto polvo dar (1 base + 1 por cada nivel que tuviera el arma)
            int nivel = arma.getItemMeta().getPersistentDataContainer().get(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER);
            int cantidadPolvo = 1 + nivel;

            // Destruimos el arma
            inv.setItem(11, new ItemStack(Material.AIR));

            // Creamos y damos el polvo
            ItemStack recompensa = ItemManager.crearPolvoEstelar();
            recompensa.setAmount(cantidadPolvo);

            // Si tiene el inventario lleno, lo tiramos al piso
            HashMap<Integer, ItemStack> sobrante = jugador.getInventory().addItem(recompensa);
            if (!sobrante.isEmpty()) {
                jugador.getWorld().dropItemNaturally(jugador.getLocation(), sobrante.get(0));
            }

            jugador.sendMessage("§a§l¡RECICLAJE EXITOSO! §7Obtuviste §e" + cantidadPolvo + "x Polvo Estelar§7.");
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
        }
    }

    // ANTI-DUPE
    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MENU)) return;
        Player jugador = (Player) event.getPlayer();
        ItemStack arma = event.getInventory().getItem(11);

        if (arma != null && arma.getType() != Material.AIR) {
            jugador.getInventory().addItem(arma);
        }
    }
}