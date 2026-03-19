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

import java.util.List;
import java.util.Random;

public class HerreriaListener implements Listener {

    // 🟢 ARQUITECTURA: Usamos la clase principal de NexoItems
    private final NexoItems plugin;
    private final String TITULO_MENU = "§8⚒ Herrería del Nexo";
    private final Random random = new Random();

    public HerreriaListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    // Método para abrir el menú
    public void abrirMenu(Player jugador) {
        Inventory inv = Bukkit.createInventory(null, 27, TITULO_MENU);

        // Decoración de cristal negro
        ItemStack cristal = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        metaCristal.setDisplayName(" ");
        cristal.setItemMeta(metaCristal);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, cristal);
        }

        // Dejamos 2 huecos vacíos: Slot 11 (Arma) y Slot 15 (Material)
        inv.setItem(11, new ItemStack(Material.AIR));
        inv.setItem(15, new ItemStack(Material.AIR));

        // El Botón de Forjar (Slot 13)
        ItemStack yunque = new ItemStack(Material.ANVIL);
        ItemMeta metaYunque = yunque.getItemMeta();
        metaYunque.setDisplayName("§a§lFORJAR MEJORA");
        metaYunque.setLore(List.of("§7Haz clic para intentar mejorar tu arma.", "§eRequiere: §f1x Polvo Estelar"));
        yunque.setItemMeta(metaYunque);
        inv.setItem(13, yunque);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MENU)) return;

        Player jugador = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Si hacen clic en su propio inventario, les dejamos mover sus cosas
        if (slot >= 27) return;

        // Si hacen clic en los cristales, cancelamos
        if (slot != 11 && slot != 15 && slot != 13) {
            event.setCancelled(true);
            return;
        }

        // SI HACEN CLIC EN EL BOTÓN DE FORJAR
        if (slot == 13) {
            event.setCancelled(true);
            Inventory inv = event.getInventory();
            ItemStack arma = inv.getItem(11);
            ItemStack material = inv.getItem(15);

            if (arma == null || arma.getType() == Material.AIR) {
                jugador.sendMessage("§cPon un arma en la ranura izquierda.");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (material == null || !material.hasItemMeta() ||
                    !material.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveMaterialMejora, PersistentDataType.BYTE)) {
                jugador.sendMessage("§cNecesitas §ePolvo Estelar §cen la ranura derecha.");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Leemos el nivel actual del arma
            ItemMeta metaArma = arma.getItemMeta();
            if (!metaArma.getPersistentDataContainer().has(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER)) {
                jugador.sendMessage("§cEsta arma no se puede mejorar.");
                return;
            }

            int nivelActual = metaArma.getPersistentDataContainer().get(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER);

            if (nivelActual >= 10) {
                jugador.sendMessage("§c¡Tu arma ya está al nivel máximo!");
                return;
            }

            // GASTAMOS 1 POLVO ESTELAR
            material.setAmount(material.getAmount() - 1);

            // MATEMÁTICAS DE PROBABILIDAD (RNG)
            int chanceExito = 100 - (nivelActual * 10);
            int tiro = random.nextInt(100) + 1;

            if (tiro <= chanceExito) {
                // ¡ÉXITO!
                nivelActual++;
                metaArma.getPersistentDataContainer().set(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER, nivelActual);

                String nombreViejo = metaArma.getDisplayName();
                String nombreNuevo = nombreViejo.replaceAll("\\[\\+\\d+\\]", "[+" + nivelActual + "]");
                metaArma.setDisplayName(nombreNuevo);

                arma.setItemMeta(metaArma);

                jugador.sendMessage("§a§l¡ÉXITO! §7Tu arma subió a nivel §e+" + nivelActual);
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            } else {
                // ¡FALLO!
                jugador.sendMessage("§c§l¡FALLO! §7La mejora no funcionó y el polvo se destruyó.");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            }
        }
    }

    // ANTI-DUPE: Devolver ítems al cerrar el menú
    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MENU)) return;

        Player jugador = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        ItemStack arma = inv.getItem(11);
        ItemStack material = inv.getItem(15);

        if (arma != null && arma.getType() != Material.AIR) {
            jugador.getInventory().addItem(arma);
        }
        if (material != null && material.getType() != Material.AIR) {
            jugador.getInventory().addItem(material);
        }
    }
}