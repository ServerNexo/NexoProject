package me.nexo.items.estaciones;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ReforgeDTO;
import me.nexo.items.dtos.ToolDTO;
import me.nexo.items.dtos.WeaponDTO;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ReforjaListener implements Listener {

    private final NexoItems plugin;
    private final Random random = new Random();

    public ReforjaListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    public void abrirMenu(Player jugador) {
        Inventory inv = Bukkit.createInventory(null, 27, CrossplayUtils.parseCrossplay(jugador, plugin.getConfigManager().getMessage("menus.reforja.titulo")));

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

        ItemStack yunque = new ItemStack(Material.SMITHING_TABLE);
        ItemMeta metaYunque = yunque.getItemMeta();
        if (metaYunque != null) {
            metaYunque.displayName(CrossplayUtils.parseCrossplay(jugador, plugin.getConfigManager().getMessage("menus.reforja.boton.titulo")));
            metaYunque.lore(plugin.getConfigManager().getMessages().getStringList("menus.reforja.boton.lore").stream()
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
        if (!tituloLimpio.equals(plugin.getConfigManager().getMessage("menus.reforja.titulo").replaceAll("<[^>]*>", ""))) return;

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
                CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.reforja.inserta-activo"));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (material == null || !material.hasItemMeta() ||
                    !material.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveMaterialMejora, PersistentDataType.BYTE)) {
                CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.reforja.necesitas-polvo"));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            var pdc = arma.getItemMeta().getPersistentDataContainer();

            boolean esArma = pdc.has(ItemManager.llaveWeaponId, PersistentDataType.STRING);
            boolean esHerramienta = pdc.has(ItemManager.llaveHerramientaId, PersistentDataType.STRING);

            if (!esArma && !esHerramienta) {
                CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.reforja.no-soporta-reforja"));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            String claseItem = "Cualquiera";
            if (esArma) {
                WeaponDTO armaDto = plugin.getFileManager().getWeaponDTO(pdc.get(ItemManager.llaveWeaponId, PersistentDataType.STRING));
                if (armaDto != null) claseItem = armaDto.claseRequerida();
            } else if (esHerramienta) {
                ToolDTO toolDto = plugin.getFileManager().getToolDTO(pdc.get(ItemManager.llaveHerramientaId, PersistentDataType.STRING));
                if (toolDto != null) claseItem = toolDto.profesion();
            }

            List<ReforgeDTO> reforjasCompatibles = new ArrayList<>();
            for (String key : plugin.getFileManager().getReforjas().getConfigurationSection("reforjas").getKeys(false)) {
                ReforgeDTO dto = plugin.getFileManager().getReforgeDTO(key);
                if (dto != null && (dto.aplicaAClase(claseItem) || dto.aplicaAClase("Cualquiera"))) {
                    reforjasCompatibles.add(dto);
                }
            }

            if (reforjasCompatibles.isEmpty()) {
                CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.reforja.sin-reforjas-compatibles").replace("%class%", claseItem));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            material.setAmount(material.getAmount() - 1);

            ReforgeDTO reforjaElegida = reforjasCompatibles.get(random.nextInt(reforjasCompatibles.size()));

            ItemStack armaReforjada = ItemManager.aplicarReforja(arma, reforjaElegida.id());
            inv.setItem(11, armaReforjada);

            CrossplayUtils.sendMessage(jugador, plugin.getConfigManager().getMessage("eventos.reforja.fusion-exitosa")
                    .replace("%prefix%", reforjaElegida.prefijoColor())
                    .replace("%name%", reforjaElegida.nombre()));
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.equals(plugin.getConfigManager().getMessage("menus.reforja.titulo").replaceAll("<[^>]*>", ""))) return;

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