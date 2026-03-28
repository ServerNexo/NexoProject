package me.nexo.items.estaciones;

import me.nexo.core.utils.NexoColor;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ReforgeDTO;
import me.nexo.items.dtos.ToolDTO;
import me.nexo.items.dtos.WeaponDTO;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ReforjaListener implements Listener {

    private final NexoItems plugin;
    private final Random random = new Random();

    public static final String TITLE_PLAIN = "» Mesa de Reforjas";
    public static final String MENU_TITLE = "&#1c0f2a<bold>»</bold> &#00f5ffMesa de Reforjas";

    public ReforjaListener(NexoItems plugin) {
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

        ItemStack yunque = new ItemStack(Material.SMITHING_TABLE);
        ItemMeta metaYunque = yunque.getItemMeta();
        if (metaYunque != null) {
            metaYunque.setDisplayName(serialize("&#00f5ff<bold>APLICAR REFORJA ALEATORIA</bold>"));
            metaYunque.setLore(Arrays.asList(
                    serialize("&#1c0f2aAplica modificadores extra a tu activo"),
                    serialize("&#1c0f2adependiendo de su clase/profesión."),
                    serialize(" "),
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

            var pdc = arma.getItemMeta().getPersistentDataContainer();

            boolean esArma = pdc.has(ItemManager.llaveWeaponId, PersistentDataType.STRING);
            boolean esHerramienta = pdc.has(ItemManager.llaveHerramientaId, PersistentDataType.STRING);

            if (!esArma && !esHerramienta) {
                jugador.sendMessage(NexoColor.parse("&#8b0000[!] Este activo no soporta matrices de reforja."));
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
                jugador.sendMessage(NexoColor.parse("&#8b0000[!] Base de datos vacía: No hay reforjas descubiertas para la clase " + claseItem + "."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            material.setAmount(material.getAmount() - 1);

            ReforgeDTO reforjaElegida = reforjasCompatibles.get(random.nextInt(reforjasCompatibles.size()));

            ItemStack armaReforjada = ItemManager.aplicarReforja(arma, reforjaElegida.id());
            inv.setItem(11, armaReforjada);

            jugador.sendMessage(NexoColor.parse("&#00f5ff[✓] <bold>¡FUSIÓN EXITOSA!</bold> Modificador aplicado: " + reforjaElegida.prefijoColor() + reforjaElegida.nombre()));
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
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