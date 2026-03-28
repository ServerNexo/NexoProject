package me.nexo.items.estaciones;

import me.nexo.core.utils.NexoColor;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.EnchantDTO;
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
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class YunqueListener implements Listener {

    private final NexoItems plugin;

    public static final String TITLE_PLAIN = "» Yunque Mágico";
    public static final String MENU_TITLE = "&#1c0f2a<bold>»</bold> &#ff00ffYunque Mágico";

    public YunqueListener(NexoItems plugin) {
        this.plugin = plugin;
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
        inv.setItem(15, new ItemStack(Material.AIR));

        ItemStack yunque = new ItemStack(Material.ANVIL);
        ItemMeta metaYunque = yunque.getItemMeta();
        if (metaYunque != null) {
            metaYunque.setDisplayName(serialize("&#ff00ff<bold>FUSIONAR MAGIA</bold>"));
            metaYunque.setLore(Arrays.asList(
                    serialize("&#1c0f2aAplica el encantamiento del libro"),
                    serialize("&#1c0f2aa tu arma, herramienta o armadura.")
            ));
            yunque.setItemMeta(metaYunque);
        }
        inv.setItem(13, yunque);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!plainTitle.equals(TITLE_PLAIN)) return;

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
                jugador.sendMessage(NexoColor.parse("&#8b0000[!] Inserta un arma, herramienta o armadura en la bahía izquierda."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (libro == null || !libro.hasItemMeta() || !libro.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveEnchantId, PersistentDataType.STRING)) {
                jugador.sendMessage(NexoColor.parse("&#8b0000[!] Necesitas un Módulo de Encantamiento válido en la bahía derecha."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            var pdcItem = itemObj.getItemMeta().getPersistentDataContainer();

            boolean esArma = pdcItem.has(ItemManager.llaveWeaponId, PersistentDataType.STRING);
            boolean esHerramienta = pdcItem.has(ItemManager.llaveHerramientaId, PersistentDataType.STRING);
            boolean esArmadura = pdcItem.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING);

            if (!esArma && !esHerramienta && !esArmadura) {
                jugador.sendMessage(NexoColor.parse("&#8b0000[!] Este activo no soporta encantamientos corporativos."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            String idEnchant = libro.getItemMeta().getPersistentDataContainer().get(ItemManager.llaveEnchantId, PersistentDataType.STRING);
            int nivel = libro.getItemMeta().getPersistentDataContainer().getOrDefault(ItemManager.llaveEnchantNivel, PersistentDataType.INTEGER, 1);
            EnchantDTO enchantDTO = plugin.getFileManager().getEnchantDTO(idEnchant);

            String tipoItem = esArma ? "Arma" : (esHerramienta ? "Herramienta" : "Armadura");

            if (enchantDTO != null && !enchantDTO.aplicaA().contains(tipoItem) && !enchantDTO.aplicaA().contains("Cualquiera")) {
                jugador.sendMessage(NexoColor.parse("&#8b0000[!] Incompatibilidad: " + enchantDTO.nombre() + " no puede aplicarse a " + tipoItem + "s."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            ItemStack itemEncantado = ItemManager.aplicarEncantamiento(itemObj, idEnchant, nivel);
            ItemManager.sincronizarItemAsync(itemEncantado);

            inv.setItem(11, itemEncantado);
            inv.setItem(15, new ItemStack(Material.AIR));

            jugador.sendMessage(NexoColor.parse("&#00f5ff[✓] FUSIÓN EXITOSA. Módulo " + enchantDTO.nombre() + " inyectado en el activo."));
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!plainTitle.equals(TITLE_PLAIN)) return;

        Player jugador = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        ItemStack item = inv.getItem(11);
        ItemStack libro = inv.getItem(15);

        if (item != null && item.getType() != Material.AIR) jugador.getInventory().addItem(item);
        if (libro != null && libro.getType() != Material.AIR) jugador.getInventory().addItem(libro);
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack item1 = event.getInventory().getItem(0);
        ItemStack result = event.getResult();

        if (item1 != null && item1.hasItemMeta() && result != null && result.hasItemMeta()) {
            ItemMeta meta1 = item1.getItemMeta();

            if (meta1.getPersistentDataContainer().has(ItemManager.llaveWeaponId, PersistentDataType.STRING) ||
                    meta1.getPersistentDataContainer().has(ItemManager.llaveHerramientaId, PersistentDataType.STRING) ||
                    meta1.getPersistentDataContainer().has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {

                String oldName = meta1.getDisplayName();
                String newName = result.getItemMeta().getDisplayName();

                if (!oldName.equals(newName)) {
                    event.setResult(null);
                }
            }
        }
    }

    private String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }
}