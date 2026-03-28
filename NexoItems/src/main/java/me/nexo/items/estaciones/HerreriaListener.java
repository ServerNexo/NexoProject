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
import java.util.Random;

public class HerreriaListener implements Listener {

    private final NexoItems plugin;
    private final Random random = new Random();

    public static final String TITLE_PLAIN = "» Herrería del Nexo";
    public static final String MENU_TITLE = "&#1c0f2a<bold>»</bold> &#ff00ffHerrería del Nexo";

    public HerreriaListener(NexoItems plugin) {
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
        inv.setItem(15, new ItemStack(Material.AIR));

        ItemStack yunque = new ItemStack(Material.ANVIL);
        ItemMeta metaYunque = yunque.getItemMeta();
        if (metaYunque != null) {
            metaYunque.setDisplayName(serialize("&#00f5ff<bold>FORJAR MEJORA</bold>"));
            metaYunque.setLore(Arrays.asList(
                    serialize("&#1c0f2aHaz clic para intentar mejorar tu activo."),
                    serialize("&#ff00ffRequiere: &#1c0f2a1x Polvo Estelar")
            ));
            yunque.setItemMeta(metaYunque);
        }
        inv.setItem(13, yunque);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.equals(TITLE_PLAIN)) return;

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
            ItemStack arma = inv.getItem(11);
            ItemStack material = inv.getItem(15);

            if (arma == null || arma.getType() == Material.AIR) {
                jugador.sendMessage(NexoColor.parse("&#8b0000[!] Inserta un activo en la bahía izquierda."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (material == null || !material.hasItemMeta() ||
                    !material.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveMaterialMejora, PersistentDataType.BYTE)) {
                jugador.sendMessage(NexoColor.parse("&#8b0000[!] Necesitas &#ff00ffPolvo Estelar &#8b0000en la bahía derecha."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            ItemMeta metaArma = arma.getItemMeta();
            if (!metaArma.getPersistentDataContainer().has(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER)) {
                jugador.sendMessage(NexoColor.parse("&#8b0000[!] Este activo no soporta mejoras de herrería estelar."));
                return;
            }

            int nivelActual = metaArma.getPersistentDataContainer().getOrDefault(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER, 0);

            if (nivelActual >= 10) {
                jugador.sendMessage(NexoColor.parse("&#ff00ff[!] ¡El activo ya ha alcanzado la mejora máxima (+10)!"));
                return;
            }

            material.setAmount(material.getAmount() - 1);

            int chanceExito = 100 - (nivelActual * 10);
            int tiro = random.nextInt(100) + 1;

            if (tiro <= chanceExito) {
                nivelActual++;
                metaArma.getPersistentDataContainer().set(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER, nivelActual);

                String nombreViejo = metaArma.getDisplayName();
                String nombreNuevo = nombreViejo.replaceAll("\\[\\+\\d+\\]", "[+" + nivelActual + "]");
                metaArma.setDisplayName(nombreNuevo);

                arma.setItemMeta(metaArma);

                jugador.sendMessage(NexoColor.parse("&#00f5ff[✓] <bold>¡FORJA EXITOSA!</bold> El activo ascendió a Nivel +" + nivelActual));
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            } else {
                jugador.sendMessage(NexoColor.parse("&#8b0000[!] <bold>¡FORJA FALLIDA!</bold> La integridad del polvo estelar colapsó."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.equals(TITLE_PLAIN)) return;

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