package me.nexo.items.estaciones;

import me.nexo.items.managers.ItemManager;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.EnchantDTO;
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

public class YunqueListener implements Listener {

    // 🟢 ARQUITECTURA: Ahora usamos la clase principal de NexoItems
    private final NexoItems plugin;
    private final String TITULO_MENU = "§8🪄 Yunque Mágico";

    public YunqueListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    public void abrirMenu(Player jugador) {
        Inventory inv = Bukkit.createInventory(null, 27, TITULO_MENU);

        ItemStack cristal = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        metaCristal.setDisplayName(" ");
        cristal.setItemMeta(metaCristal);

        for (int i = 0; i < 27; i++) inv.setItem(i, cristal);

        inv.setItem(11, new ItemStack(Material.AIR)); // Ítem a encantar
        inv.setItem(15, new ItemStack(Material.AIR)); // Libro Mágico

        ItemStack yunque = new ItemStack(Material.ANVIL);
        ItemMeta metaYunque = yunque.getItemMeta();
        metaYunque.setDisplayName("§d§lFUSIONAR MAGIA");
        metaYunque.setLore(List.of("§7Aplica el encantamiento del libro", "§7a tu arma, herramienta o armadura."));
        yunque.setItemMeta(metaYunque);
        inv.setItem(13, yunque);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MENU)) return;

        Player jugador = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot >= 27) return;

        if (slot != 11 && slot != 15 && slot != 13) {
            event.setCancelled(true);
            return;
        }

        if (slot == 13) {
            event.setCancelled(true);
            Inventory inv = event.getInventory();
            ItemStack itemObj = inv.getItem(11);
            ItemStack libro = inv.getItem(15);

            if (itemObj == null || itemObj.getType() == Material.AIR) {
                jugador.sendMessage("§cPon un arma, herramienta o armadura en la ranura izquierda.");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (libro == null || !libro.hasItemMeta() || !libro.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveEnchantId, PersistentDataType.STRING)) {
                jugador.sendMessage("§cNecesitas un §dLibro de Encantamiento §cen la ranura derecha.");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            var pdcItem = itemObj.getItemMeta().getPersistentDataContainer();
            // Verificamos de qué tipo de ítem se trata
            boolean esArma = pdcItem.has(ItemManager.llaveWeaponId, PersistentDataType.STRING);
            boolean esHerramienta = pdcItem.has(ItemManager.llaveHerramientaId, PersistentDataType.STRING);
            boolean esArmadura = pdcItem.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING);

            if (!esArma && !esHerramienta && !esArmadura) {
                jugador.sendMessage("§cEse ítem no soporta encantamientos del Nexo.");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            String idEnchant = libro.getItemMeta().getPersistentDataContainer().get(ItemManager.llaveEnchantId, PersistentDataType.STRING);
            int nivel = libro.getItemMeta().getPersistentDataContainer().get(ItemManager.llaveEnchantNivel, PersistentDataType.INTEGER);
            EnchantDTO enchantDTO = plugin.getFileManager().getEnchantDTO(idEnchant);

            // Determinamos el tipo para validarlo con el DTO
            String tipoItem = "Desconocido";
            if (esArma) tipoItem = "Arma";
            else if (esHerramienta) tipoItem = "Herramienta";
            else if (esArmadura) tipoItem = "Armadura";

            // Asegúrate de que tu EnchantDTO tiene este método, o cambia la lógica si no lo tiene.
            // Asumiendo que `esCompatible` verifica si la lista `aplicaA` contiene el tipo.
            // Si te da error aquí, es porque en EnchantDTO no creaste el método `esCompatible`.
            if (enchantDTO != null && !enchantDTO.esCompatible(tipoItem)) {
                jugador.sendMessage("§cEl encantamiento " + enchantDTO.nombre() + " §cno se puede aplicar a " + tipoItem + "s.");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // ¡Magia aplicada!
            ItemStack itemEncantado = ItemManager.aplicarEncantamiento(itemObj, idEnchant, nivel);
            inv.setItem(11, itemEncantado);
            inv.setItem(15, new ItemStack(Material.AIR)); // Consumir el libro

            jugador.sendMessage("§a§l¡ÉXITO! §7Has aplicado " + org.bukkit.ChatColor.translateAlternateColorCodes('&', enchantDTO.nombre()) + " §7a tu ítem.");
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MENU)) return;
        Player jugador = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        ItemStack item = inv.getItem(11);
        ItemStack libro = inv.getItem(15);

        if (item != null && item.getType() != Material.AIR) jugador.getInventory().addItem(item);
        if (libro != null && libro.getType() != Material.AIR) jugador.getInventory().addItem(libro);
    }
}