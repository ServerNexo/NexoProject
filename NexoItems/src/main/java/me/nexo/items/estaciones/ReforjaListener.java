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

    // 🎨 Títulos limpios y seguros para Paper 1.21
    public static final String TITLE_PLAIN = "» Mesa de Reforjas";
    public static final String MENU_TITLE = "&#555555<bold>»</bold> &#00E5FFMesa de Reforjas";

    public ReforjaListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    private String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }

    public void abrirMenu(Player jugador) {
        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse(MENU_TITLE));

        // Decoración
        ItemStack cristal = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        if (metaCristal != null) {
            metaCristal.setDisplayName(serialize(" "));
            cristal.setItemMeta(metaCristal);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, cristal);
        }

        // Espacios para el jugador
        inv.setItem(11, new ItemStack(Material.AIR)); // Arma o Herramienta
        inv.setItem(15, new ItemStack(Material.AIR)); // Polvo Estelar

        // Botón Central
        ItemStack yunque = new ItemStack(Material.SMITHING_TABLE);
        ItemMeta metaYunque = yunque.getItemMeta();
        if (metaYunque != null) {
            metaYunque.setDisplayName(serialize("&#00E5FF<bold>APLICAR REFORJA ALEATORIA</bold>"));
            metaYunque.setLore(Arrays.asList(
                    serialize("&#AAAAAAAplica modificadores extra a tu activo"),
                    serialize("&#AAAAAAdependiendo de su clase/profesión."),
                    serialize(" "),
                    serialize("&#FFAA00Requiere: &#FFFFFF1x Polvo Estelar")
            ));
            yunque.setItemMeta(metaYunque);
        }
        inv.setItem(13, yunque);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        // 🌟 Validación segura del título del menú
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!tituloLimpio.equals(TITLE_PLAIN)) return;

        Player jugador = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot >= 27) return; // Permitir mover cosas en su inventario

        if (slot != 11 && slot != 15 && slot != 13) {
            event.setCancelled(true);
            return;
        }

        // CLIC EN EL BOTÓN DE REFORJAR
        if (slot == 13) {
            event.setCancelled(true);
            Inventory inv = event.getInventory();
            ItemStack arma = inv.getItem(11);
            ItemStack material = inv.getItem(15);

            if (arma == null || arma.getType() == Material.AIR) {
                jugador.sendMessage(NexoColor.parse("&#FF5555[!] Inserta un activo en la bahía izquierda."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (material == null || !material.hasItemMeta() ||
                    !material.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveMaterialMejora, PersistentDataType.BYTE)) {
                jugador.sendMessage(NexoColor.parse("&#FF5555[!] Necesitas &#FFAA00Polvo Estelar &#FF5555en la bahía derecha."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            var pdc = arma.getItemMeta().getPersistentDataContainer();

            // 1. Verificar si es un Arma o una Herramienta
            boolean esArma = pdc.has(ItemManager.llaveWeaponId, PersistentDataType.STRING);
            boolean esHerramienta = pdc.has(ItemManager.llaveHerramientaId, PersistentDataType.STRING);

            if (!esArma && !esHerramienta) {
                jugador.sendMessage(NexoColor.parse("&#FF5555[!] Este activo no soporta matrices de reforja."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // 2. Obtener la clase o profesión del ítem
            String claseItem = "Cualquiera";
            if (esArma) {
                WeaponDTO armaDto = plugin.getFileManager().getWeaponDTO(pdc.get(ItemManager.llaveWeaponId, PersistentDataType.STRING));
                if (armaDto != null) claseItem = armaDto.claseRequerida();
            } else if (esHerramienta) {
                ToolDTO toolDto = plugin.getFileManager().getToolDTO(pdc.get(ItemManager.llaveHerramientaId, PersistentDataType.STRING));
                if (toolDto != null) claseItem = toolDto.profesion();
            }

            // 3. Filtrar las reforjas que sirvan para la clase o profesión
            List<ReforgeDTO> reforjasCompatibles = new ArrayList<>();
            for (String key : plugin.getFileManager().getReforjas().getConfigurationSection("reforjas").getKeys(false)) {
                ReforgeDTO dto = plugin.getFileManager().getReforgeDTO(key);
                if (dto != null && (dto.aplicaAClase(claseItem) || dto.aplicaAClase("Cualquiera"))) {
                    reforjasCompatibles.add(dto);
                }
            }

            if (reforjasCompatibles.isEmpty()) {
                jugador.sendMessage(NexoColor.parse("&#FF5555[!] Base de datos vacía: No hay reforjas descubiertas para la clase " + claseItem + "."));
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // 4. Cobrar el material
            material.setAmount(material.getAmount() - 1);

            // 5. Elegir una reforja al azar (RNG)
            ReforgeDTO reforjaElegida = reforjasCompatibles.get(random.nextInt(reforjasCompatibles.size()));

            // 6. ¡Aplicar la magia!
            ItemStack armaReforjada = ItemManager.aplicarReforja(arma, reforjaElegida.id());
            inv.setItem(11, armaReforjada);

            // 🎨 Formato dinámico combinando el Hex de la reforja con el texto de éxito
            jugador.sendMessage(NexoColor.parse("&#55FF55[✓] <bold>¡FUSIÓN EXITOSA!</bold> Modificador aplicado: " + reforjaElegida.prefijoColor() + reforjaElegida.nombre()));
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
        }
    }

    // ANTI-DUPE
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