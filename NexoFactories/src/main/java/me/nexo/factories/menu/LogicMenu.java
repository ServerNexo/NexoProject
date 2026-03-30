package me.nexo.factories.menu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// 🌟 Heredamos del sistema base de menús
public class LogicMenu extends NexoMenu {

    private final NexoFactories plugin;
    private final NexoCore core;
    private final ActiveFactory factory;

    private final List<String> conditions = Arrays.asList("NONE", "ENERGY_>_50", "ENERGY_>_20", "STORAGE_<_100", "STORAGE_<_500");
    private final List<String> actions = Arrays.asList("NONE", "START_MACHINE", "PAUSE_MACHINE");

    private int currentConditionIndex = 0;
    private int currentActionIndex = 0;

    public LogicMenu(Player player, NexoFactories plugin, ActiveFactory factory) {
        super(player);
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
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

    private String getMessage(String path) {
        return core.getConfigManager().getMessage("factories_messages.yml", path);
    }

    private List<String> getMessageList(String path) {
        return core.getConfigManager().getConfig("factories_messages.yml").getStringList(path);
    }

    @Override
    public String getMenuName() {
        return getMessage("menus.logic.titulo");
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

        List<String> sensorLore = getMessageList("menus.logic.sensor.lore").stream()
                .map(line -> line.replace("%condition%", cond)).collect(Collectors.toList());
        setItem(11, Material.COMPARATOR, getMessage("menus.logic.sensor.titulo"), sensorLore);

        List<String> actionLore = getMessageList("menus.logic.operacion.lore").stream()
                .map(line -> line.replace("%action%", act)).collect(Collectors.toList());
        setItem(15, Material.REDSTONE_TORCH, getMessage("menus.logic.operacion.titulo"), actionLore);

        setItem(22, Material.LIME_DYE, getMessage("menus.logic.guardar.titulo"), getMessageList("menus.logic.guardar.lore"));
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
            CrossplayUtils.sendMessage(player, getMessage("eventos.script-compilado"));
        }
    }
}