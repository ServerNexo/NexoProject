package me.nexo.factories.menu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.nexo.core.utils.NexoColor;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LogicMenu implements Listener {

    private final NexoFactories plugin;
    public static final String TITLE_PLAIN = "» Scripting Visual";
    public static final String MENU_TITLE = "&#434343<bold>»</bold> &#00fbffScripting Visual";

    private final List<String> conditions = Arrays.asList("NONE", "ENERGY_>_50", "ENERGY_>_20", "STORAGE_<_100", "STORAGE_<_500");
    private final List<String> actions = Arrays.asList("NONE", "START_MACHINE", "PAUSE_MACHINE");

    private final Map<UUID, ActiveFactory> editingFactory = new HashMap<>();
    private final Map<UUID, Integer> currentConditionIndex = new HashMap<>();
    private final Map<UUID, Integer> currentActionIndex = new HashMap<>();

    public LogicMenu(NexoFactories plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, ActiveFactory factory) {
        editingFactory.put(player.getUniqueId(), factory);

        int condIndex = 0;
        int actIndex = 0;
        try {
            if (factory.getJsonLogic() != null && !factory.getJsonLogic().equals("NONE")) {
                JsonObject json = JsonParser.parseString(factory.getJsonLogic()).getAsJsonObject();
                if (json.has("condition")) condIndex = conditions.indexOf(json.get("condition").getAsString());
                if (json.has("action")) actIndex = actions.indexOf(json.get("action").getAsString());
            }
        } catch (Exception ignored) {}

        currentConditionIndex.put(player.getUniqueId(), Math.max(0, condIndex));
        currentActionIndex.put(player.getUniqueId(), Math.max(0, actIndex));

        renderMenu(player);
    }

    private void renderMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, NexoColor.parse(MENU_TITLE));
        UUID id = player.getUniqueId();

        String cond = conditions.get(currentConditionIndex.get(id));
        String act = actions.get(currentActionIndex.get(id));

        // 📡 SENSOR (Condición)
        ItemStack sensor = new ItemStack(Material.COMPARATOR);
        ItemMeta sensorMeta = sensor.getItemMeta();
        if (sensorMeta != null) {
            sensorMeta.setDisplayName(serialize("&#fbd72b<bold>1. Sensor Lógico (IF)</bold>"));
            sensorMeta.setLore(serializeList(Arrays.asList(
                    "&#434343Si el entorno cumple esta métrica,",
                    "&#434343se desencadenará la operación.",
                    " ",
                    "&#434343Parámetro Actual: &#00fbff" + cond,
                    " ",
                    "&#fbd72b▶ Clic para iterar sensor"
            )));
            sensor.setItemMeta(sensorMeta);
        }
        inv.setItem(11, sensor);

        // ⚙️ ACCIÓN (Resultado)
        ItemStack action = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta actionMeta = action.getItemMeta();
        if (actionMeta != null) {
            actionMeta.setDisplayName(serialize("&#ff4b2b<bold>2. Operación (THEN)</bold>"));
            actionMeta.setLore(serializeList(Arrays.asList(
                    "&#434343El comportamiento de la máquina.",
                    " ",
                    "&#434343Rutina Actual: &#ff4b2b" + act,
                    " ",
                    "&#fbd72b▶ Clic para iterar rutina"
            )));
            action.setItemMeta(actionMeta);
        }
        inv.setItem(15, action);

        // 💾 BOTÓN GUARDAR
        ItemStack save = new ItemStack(Material.LIME_DYE);
        ItemMeta saveMeta = save.getItemMeta();
        if (saveMeta != null) {
            saveMeta.setDisplayName(serialize("&#a8ff78<bold>[✔] Compilar e Inyectar Código</bold>"));
            saveMeta.setLore(serializeList(Arrays.asList("&#434343Sobrescribe el Script JSON y", "&#434343reinicia el núcleo operativo.")));
            save.setItemMeta(saveMeta);
        }
        inv.setItem(22, save);

        player.openInventory(inv);
    }

    private String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }

    private List<String> serializeList(List<String> hexList) {
        List<String> out = new ArrayList<>();
        for (String s : hexList) out.add(serialize(s));
        return out;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!plainTitle.equals(TITLE_PLAIN)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        UUID id = player.getUniqueId();
        if (!editingFactory.containsKey(id)) return;

        ActiveFactory factory = editingFactory.get(id);

        if (event.getSlot() == 11) {
            int next = (currentConditionIndex.get(id) + 1) % conditions.size();
            currentConditionIndex.put(id, next);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            renderMenu(player);
        }
        else if (event.getSlot() == 15) {
            int next = (currentActionIndex.get(id) + 1) % actions.size();
            currentActionIndex.put(id, next);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            renderMenu(player);
        }
        else if (event.getSlot() == 22) {
            String cond = conditions.get(currentConditionIndex.get(id));
            String act = actions.get(currentActionIndex.get(id));

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
            player.sendMessage(NexoColor.parse("&#a8ff78<bold>[⚙]</bold> &#00fbffScript compilado e inyectado en el procesador con éxito."));

            editingFactory.remove(id);
            currentConditionIndex.remove(id);
            currentActionIndex.remove(id);
        }
    }
}