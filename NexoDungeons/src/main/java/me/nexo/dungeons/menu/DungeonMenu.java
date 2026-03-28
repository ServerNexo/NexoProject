package me.nexo.dungeons.menu;

import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DungeonMenu {

    public static final String TITLE_PLAIN = "» Terminal de Incursiones";
    public static final String MENU_TITLE = "&#1c0f2a<bold>»</bold> &#00f5ffTerminal de Incursiones";

    public static void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse(MENU_TITLE));

        // 🏰 OPCIÓN 1: Instancias Privadas (Slot 11)
        ItemStack instanced = new ItemStack(Material.IRON_DOOR);
        ItemMeta instancedMeta = instanced.getItemMeta();
        if (instancedMeta != null) {
            instancedMeta.setDisplayName(serialize("&#00f5ff<bold>Fortalezas Instanciadas</bold>"));
            instancedMeta.setLore(serializeList(Arrays.asList(
                    "&#1c0f2aAventúrate en sectores generados",
                    "&#1c0f2aexclusivamente para tu escuadrón.",
                    " ",
                    "&#00f5ff✦ Rompecabezas lógicos",
                    "&#00f5ff✦ Aislamiento total garantizado",
                    " ",
                    "&#00f5ff▶ Clic para ver expediciones"
            )));
            instanced.setItemMeta(instancedMeta);
        }
        inv.setItem(11, instanced);

        // ⚔️ OPCIÓN 2: Arenas de Oleadas (Slot 13)
        ItemStack waves = new ItemStack(Material.IRON_SWORD);
        ItemMeta wavesMeta = waves.getItemMeta();
        if (wavesMeta != null) {
            wavesMeta.setDisplayName(serialize("&#8b0000<bold>Simulador de Supervivencia</bold>"));
            wavesMeta.setLore(serializeList(Arrays.asList(
                    "&#1c0f2aResiste interminables oleadas de",
                    "&#1c0f2aanomalías cada vez más letales.",
                    " ",
                    "&#8b0000✦ Dificultad Exponencial",
                    "&#8b0000✦ Puntos de control cada 5 rondas",
                    " ",
                    "&#00f5ff▶ Clic para ingresar a la cola"
            )));
            waves.setItemMeta(wavesMeta);
        }
        inv.setItem(13, waves);

        // 🐉 OPCIÓN 3: Bosses del Mundo (Slot 15)
        ItemStack worldBoss = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta bossMeta = worldBoss.getItemMeta();
        if (bossMeta != null) {
            bossMeta.setDisplayName(serialize("&#ff00ff<bold>Amenazas Globales</bold>"));
            bossMeta.setLore(serializeList(Arrays.asList(
                    "&#1c0f2aInformación sobre los altares",
                    "&#1c0f2ade invocación pública.",
                    " ",
                    "&#1c0f2a✦ Recompensas por rendimiento (Top 3)",
                    "&#1c0f2a✦ Botín encriptado (Anti-Robo)",
                    " ",
                    "&#00f5ff▶ Clic para triangular ubicaciones"
            )));
            worldBoss.setItemMeta(bossMeta);
        }
        inv.setItem(15, worldBoss);

        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    private static String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }

    private static List<String> serializeList(List<String> hexList) {
        return hexList.stream().map(DungeonMenu::serialize).collect(Collectors.toList());
    }
}