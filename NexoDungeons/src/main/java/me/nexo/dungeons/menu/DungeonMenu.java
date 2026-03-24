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
    public static final String MENU_TITLE = "&#434343<bold>»</bold> &#00fbffTerminal de Incursiones";

    public static void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse(MENU_TITLE));

        // 🏰 OPCIÓN 1: Instancias Privadas (Slot 11)
        ItemStack instanced = new ItemStack(Material.IRON_DOOR);
        ItemMeta instancedMeta = instanced.getItemMeta();
        if (instancedMeta != null) {
            instancedMeta.setDisplayName(serialize("&#00fbff<bold>Fortalezas Instanciadas</bold>"));
            instancedMeta.setLore(serializeList(Arrays.asList(
                    "&#434343Aventúrate en sectores generados",
                    "&#434343exclusivamente para tu escuadrón.",
                    " ",
                    "&#a8ff78✦ Rompecabezas lógicos",
                    "&#a8ff78✦ Aislamiento total garantizado",
                    " ",
                    "&#fbd72b▶ Clic para ver expediciones"
            )));
            instanced.setItemMeta(instancedMeta);
        }
        inv.setItem(11, instanced);

        // ⚔️ OPCIÓN 2: Arenas de Oleadas (Slot 13)
        ItemStack waves = new ItemStack(Material.IRON_SWORD);
        ItemMeta wavesMeta = waves.getItemMeta();
        if (wavesMeta != null) {
            wavesMeta.setDisplayName(serialize("&#ff4b2b<bold>Simulador de Supervivencia</bold>"));
            wavesMeta.setLore(serializeList(Arrays.asList(
                    "&#434343Resiste interminables oleadas de",
                    "&#434343anomalías cada vez más letales.",
                    " ",
                    "&#ff4b2b✦ Dificultad Exponencial",
                    "&#ff4b2b✦ Puntos de control cada 5 rondas",
                    " ",
                    "&#fbd72b▶ Clic para ingresar a la cola"
            )));
            waves.setItemMeta(wavesMeta);
        }
        inv.setItem(13, waves);

        // 🐉 OPCIÓN 3: Bosses del Mundo (Slot 15)
        ItemStack worldBoss = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta bossMeta = worldBoss.getItemMeta();
        if (bossMeta != null) {
            bossMeta.setDisplayName(serialize("&#8b008b<bold>Amenazas Globales</bold>"));
            bossMeta.setLore(serializeList(Arrays.asList(
                    "&#434343Información sobre los altares",
                    "&#434343de invocación pública.",
                    " ",
                    "&#e0e0e0✦ Recompensas por rendimiento (Top 3)",
                    "&#e0e0e0✦ Botín encriptado (Anti-Robo)",
                    " ",
                    "&#fbd72b▶ Clic para triangular ubicaciones"
            )));
            worldBoss.setItemMeta(bossMeta);
        }
        inv.setItem(15, worldBoss);

        // Cristal negro de fondo
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
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

    // Funciones utilitarias para evitar el error de "null components" en ItemMeta
    private static String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }

    private static List<String> serializeList(List<String> hexList) {
        return hexList.stream().map(DungeonMenu::serialize).collect(Collectors.toList());
    }
}