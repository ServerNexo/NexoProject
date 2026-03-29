package me.nexo.items.estaciones;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager;
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

import java.util.Random;
import java.util.stream.Collectors;

public class HerreriaListener implements Listener {

    private final NexoItems plugin;
    private final Random random = new Random();

    public HerreriaListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    public void abrirMenu(Player jugador) {
        Inventory inv = Bukkit.createInventory(null, 27, CrossplayUtils.parseCrossplay(jugador, plugin.getConfigManager().getMessage("menus.herreria.titulo")));

        ItemStack cristal = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        if (metaCristal != null) {
            metaCristal.displayName(CrossplayUtils.parseCrossplay(jugador, " "));
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
            metaYunque.displayName(CrossplayUtils.parseCrossplay(jugador, plugin.getConfigManager().getMessage("menus.herreria.boton.titulo")));
            metaYunque.lore(plugin.getConfigManager().getMessages().getStringList("menus.herreria.boton.lore").stream()
                    .map(line -> CrossplayUtils.parseCrossplay(jugador, line))
                    .collect(Collectors.toList()));
            yunque.setItemMeta(metaYunque);
        }
        inv.setItem(13, yunque);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.equals(plugin.getConfigManager().getMessage("menus.herreria.titulo").replaceAll("<[^>]*>", ""))) return;

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
                CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.herreria.inserta-activo"));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (material == null || !material.hasItemMeta() ||
                    !material.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveMaterialMejora, PersistentDataType.BYTE)) {
                CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.herreria.necesitas-polvo"));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            ItemMeta metaArma = arma.getItemMeta();
            if (!metaArma.getPersistentDataContainer().has(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER)) {
                CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.herreria.no-soporta-mejoras"));
                return;
            }

            int nivelActual = metaArma.getPersistentDataContainer().getOrDefault(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER, 0);

            if (nivelActual >= 10) {
                CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.herreria.mejora-maxima"));
                return;
            }

            material.setAmount(material.getAmount() - 1);

            int chanceExito = 100 - (nivelActual * 10);
            int tiro = random.nextInt(100) + 1;

            if (tiro <= chanceExito) {
                nivelActual++;
                metaArma.getPersistentDataContainer().set(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER, nivelActual);

                String nombreViejo = PlainTextComponentSerializer.plainText().serialize(metaArma.displayName());
                String nombreNuevo = nombreViejo.replaceAll("\\[\\+\\d+\\]", "[+" + nivelActual + "]");
                metaArma.displayName(CrossplayUtils.parseCrossplay(jugador, nombreNuevo));

                arma.setItemMeta(metaArma);

                CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.herreria.forja-exitosa").replace("%level%", String.valueOf(nivelActual)));
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            } else {
                CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.herreria.forja-fallida"));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.equals(plugin.getConfigManager().getMessage("menus.herreria.titulo").replaceAll("<[^>]*>", ""))) return;

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