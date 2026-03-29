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

import java.util.HashMap;
import java.util.stream.Collectors;

public class DesguaceListener implements Listener {

    private final NexoItems plugin;

    public DesguaceListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    public void abrirMenu(Player jugador) {
        Inventory inv = Bukkit.createInventory(null, 27, CrossplayUtils.parseCrossplay(jugador, plugin.getConfigManager().getMessage("menus.desguace.titulo")));

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

        ItemStack btn = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta btnMeta = btn.getItemMeta();
        if (btnMeta != null) {
            btnMeta.displayName(CrossplayUtils.parseCrossplay(jugador, plugin.getConfigManager().getMessage("menus.desguace.boton.titulo")));
            btnMeta.lore(plugin.getConfigManager().getMessages().getStringList("menus.desguace.boton.lore").stream()
                    .map(line -> CrossplayUtils.parseCrossplay(jugador, line))
                    .collect(Collectors.toList()));
            btn.setItemMeta(btnMeta);
        }
        inv.setItem(15, btn);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.equals(plugin.getConfigManager().getMessage("menus.desguace.titulo").replaceAll("<[^>]*>", ""))) return;

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
                CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.desguace.inserta-activo"));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (!arma.hasItemMeta() || !arma.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER)) {
                CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.desguace.activo-incompatible"));
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

            CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.desguace.reciclaje-exitoso").replace("%amount%", String.valueOf(cantidadPolvo)));
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.equals(plugin.getConfigManager().getMessage("menus.desguace.titulo").replaceAll("<[^>]*>", ""))) return;

        Player jugador = (Player) event.getPlayer();
        ItemStack arma = event.getInventory().getItem(11);

        if (arma != null && arma.getType() != Material.AIR) {
            jugador.getInventory().addItem(arma);
        }
    }
}