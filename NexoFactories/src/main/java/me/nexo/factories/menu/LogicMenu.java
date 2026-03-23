package me.nexo.factories.menu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LogicMenu implements Listener {

    private final NexoFactories plugin;
    public static final String MENU_TITLE = "§8💻 §lScripting Visual";

    // Opciones disponibles para la programación
    private final List<String> conditions = Arrays.asList("NONE", "ENERGY_>_50", "ENERGY_>_20", "STORAGE_<_100", "STORAGE_<_500");
    private final List<String> actions = Arrays.asList("NONE", "START_MACHINE", "PAUSE_MACHINE");

    // Memoria temporal mientras el jugador edita
    private final Map<UUID, ActiveFactory> editingFactory = new HashMap<>();
    private final Map<UUID, Integer> currentConditionIndex = new HashMap<>();
    private final Map<UUID, Integer> currentActionIndex = new HashMap<>();

    public LogicMenu(NexoFactories plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, ActiveFactory factory) {
        editingFactory.put(player.getUniqueId(), factory);

        // Intentamos leer el JSON actual para mostrarlo en el menú
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
        Inventory inv = Bukkit.createInventory(null, 27, MENU_TITLE);
        UUID id = player.getUniqueId();

        String cond = conditions.get(currentConditionIndex.get(id));
        String act = actions.get(currentActionIndex.get(id));

        // 📡 SENSOR (Condición)
        ItemStack sensor = new ItemStack(Material.COMPARATOR);
        ItemMeta sensorMeta = sensor.getItemMeta();
        sensorMeta.setDisplayName("§e§l1. Sensor (Condición IF)");
        sensorMeta.setLore(Arrays.asList(
                "§7Si se cumple esta regla, la",
                "§7máquina ejecutará la acción.",
                " ",
                "§fActual: §b" + cond,
                " ",
                "§e¡Clic para cambiar!"
        ));
        sensor.setItemMeta(sensorMeta);
        inv.setItem(11, sensor);

        // ⚙️ ACCIÓN (Resultado)
        ItemStack action = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta actionMeta = action.getItemMeta();
        actionMeta.setDisplayName("§c§l2. Operación (Acción THEN)");
        actionMeta.setLore(Arrays.asList(
                "§7Lo que hará la máquina.",
                " ",
                "§fActual: §c" + act,
                " ",
                "§e¡Clic para cambiar!"
        ));
        action.setItemMeta(actionMeta);
        inv.setItem(15, action);

        // 💾 BOTÓN GUARDAR
        ItemStack save = new ItemStack(Material.LIME_DYE);
        ItemMeta saveMeta = save.getItemMeta();
        saveMeta.setDisplayName("§a§l[✔] Compilar y Guardar");
        saveMeta.setLore(Arrays.asList("§7Escribe el código JSON y", "§7reinicia la máquina."));
        save.setItemMeta(saveMeta);
        inv.setItem(22, save);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(MENU_TITLE)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        UUID id = player.getUniqueId();
        if (!editingFactory.containsKey(id)) return;

        ActiveFactory factory = editingFactory.get(id);

        if (event.getSlot() == 11) { // Cambiar Condición
            int next = (currentConditionIndex.get(id) + 1) % conditions.size();
            currentConditionIndex.put(id, next);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            renderMenu(player);
        }
        else if (event.getSlot() == 15) { // Cambiar Acción
            int next = (currentActionIndex.get(id) + 1) % actions.size();
            currentActionIndex.put(id, next);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            renderMenu(player);
        }
        else if (event.getSlot() == 22) { // Guardar
            String cond = conditions.get(currentConditionIndex.get(id));
            String act = actions.get(currentActionIndex.get(id));

            // 🌟 ENSAMBLAMOS EL JSON
            if (cond.equals("NONE") || act.equals("NONE")) {
                factory.setJsonLogic("NONE");
            } else {
                JsonObject json = new JsonObject();
                json.addProperty("condition", cond);
                json.addProperty("action", act);
                factory.setJsonLogic(json.toString());
            }

            // Guardamos en la base de datos de manera asíncrona
            plugin.getFactoryManager().saveFactoryStatusAsync(factory);

            // Si quieres también puedes guardar en la nueva tabla `nexo_factory_scripts` aquí

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            player.sendMessage("§a§l[⚙] §fScript compilado y cargado en la máquina con éxito.");

            editingFactory.remove(id);
            currentConditionIndex.remove(id);
            currentActionIndex.remove(id);
        }
    }
}