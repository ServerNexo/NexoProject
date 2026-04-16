package me.nexo.factories.menu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.utils.NexoColor;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🏭 NexoFactories - Compilador Lógico de Máquinas (Arquitectura Enterprise)
 * Nota: Los menús son instanciados por jugador, NO usan @Singleton.
 */
public class LogicMenu extends NexoMenu {

    private final NexoFactories plugin;
    private final ActiveFactory factory;

    private final List<String> conditions = Arrays.asList("NONE", "ENERGY_>_50", "ENERGY_>_20", "STORAGE_<_100", "STORAGE_<_500");
    private final List<String> actions = Arrays.asList("NONE", "START_MACHINE", "PAUSE_MACHINE");

    private int currentConditionIndex = 0;
    private int currentActionIndex = 0;

    public LogicMenu(Player player, NexoFactories plugin, ActiveFactory factory) {
        super(player);
        this.plugin = plugin;
        this.factory = factory;

        // Cargamos la configuración anterior de esta máquina en específico
        try {
            if (factory.getJsonLogic() != null && !factory.getJsonLogic().equals("NONE")) {
                JsonObject json = JsonParser.parseString(factory.getJsonLogic()).getAsJsonObject();
                if (json.has("condition")) currentConditionIndex = Math.max(0, conditions.indexOf(json.get("condition").getAsString()));
                if (json.has("action")) currentActionIndex = Math.max(0, actions.indexOf(json.get("action").getAsString()));
            }
        } catch (Exception ignored) {}
    }

    @Override
    public String getMenuName() {
        // 🌟 FIX: Título serializado compatible con Java y Bedrock
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#FF5555⚙ <bold>COMPILADOR LÓGICO</bold>"));
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        String cond = conditions.get(currentConditionIndex);
        String act = actions.get(currentActionIndex);

        // 🌟 FIX: Lores con Hexadecimal directo, sin getMessageList
        List<String> sensorLoreRaw = List.of(
                "&#E6CCFFSelecciona la condición que",
                "&#E6CCFFdisparará el evento.",
                "",
                "&#E6CCFFActual: &#00f5ff" + cond,
                "",
                "&#00f5ff► Clic para alternar"
        );
        setItem(11, Material.COMPARATOR, "&#00f5ff📡 <bold>SENSOR DE ENTRADA</bold>",
                sensorLoreRaw.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line))).collect(Collectors.toList()));

        List<String> actionLoreRaw = List.of(
                "&#E6CCFFSelecciona lo que hará la máquina",
                "&#E6CCFFal cumplirse la condición.",
                "",
                "&#E6CCFFActual: &#FFAA00" + act,
                "",
                "&#FFAA00► Clic para alternar"
        );
        setItem(15, Material.REDSTONE_TORCH, "&#FFAA00⚡ <bold>OPERACIÓN DE RESPUESTA</bold>",
                actionLoreRaw.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line))).collect(Collectors.toList()));

        List<String> saveLoreRaw = List.of(
                "&#E6CCFFGuarda los cambios en el chip",
                "&#E6CCFFlógico de esta maquinaria."
        );
        setItem(22, Material.LIME_DYE, "&#55FF55[✓] <bold>COMPILAR SCRIPT</bold>",
                saveLoreRaw.stream().map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line))).collect(Collectors.toList()));
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 11) {
            currentConditionIndex = (currentConditionIndex + 1) % conditions.size();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            setMenuItems(); // Actualizamos la vista sin cerrar el inventario

        } else if (slot == 15) {
            currentActionIndex = (currentActionIndex + 1) % actions.size();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            setMenuItems(); // Actualizamos la vista sin cerrar el inventario

        } else if (slot == 22) {
            String cond = conditions.get(currentConditionIndex);
            String act = actions.get(currentActionIndex);

            if (cond.equals("NONE") || act.equals("NONE")) {
                factory.setJsonLogic("NONE");
            } else {
                JsonObject json = new JsonObject();
                json.addProperty("condition", cond);
                json.addProperty("action", act);
                factory.setJsonLogic(json.toString());
            }

            plugin.getFactoryManager().saveFactoryStatusAsync(factory);

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);

            // 🌟 FIX: Mensaje Directo
            CrossplayUtils.sendMessage(player, "&#55FF55[✓] Script lógico compilado e inyectado en el procesador de la máquina.");
        }
    }
}