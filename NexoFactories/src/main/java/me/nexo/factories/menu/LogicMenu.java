package me.nexo.factories.menu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class LogicMenu implements Listener {

    private final NexoFactories plugin;
    private final NexoCore core;
    private final List<String> conditions = Arrays.asList("NONE", "ENERGY_>_50", "ENERGY_>_20", "STORAGE_<_100", "STORAGE_<_500");
    private final List<String> actions = Arrays.asList("NONE", "START_MACHINE", "PAUSE_MACHINE");

    private final Map<UUID, ActiveFactory> editingFactory = new HashMap<>();
    private final Map<UUID, Integer> currentConditionIndex = new HashMap<>();
    private final Map<UUID, Integer> currentActionIndex = new HashMap<>();

    public LogicMenu(NexoFactories plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    private String getMessage(String path) {
        return core.getConfigManager().getMessage("factories_messages.yml", path);
    }

    // 🌟 CORRECCIÓN 1: Método ayudante para obtener listas correctamente
    private List<String> getMessageList(String path) {
        return core.getConfigManager().getConfig("factories_messages.yml").getStringList(path);
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
        Inventory inv = Bukkit.createInventory(null, 27, CrossplayUtils.parseCrossplay(player, getMessage("menus.logic.titulo")));
        UUID id = player.getUniqueId();
        String cond = conditions.get(currentConditionIndex.get(id));
        String act = actions.get(currentActionIndex.get(id));

        ItemStack sensor = new ItemStack(Material.COMPARATOR);
        ItemMeta sensorMeta = sensor.getItemMeta();
        if (sensorMeta != null) {
            sensorMeta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.logic.sensor.titulo")));
            List<String> loreConfig = getMessageList("menus.logic.sensor.lore");
            sensorMeta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%condition%", cond))).collect(Collectors.toList()));
            sensor.setItemMeta(sensorMeta);
        }
        inv.setItem(11, sensor);

        ItemStack action = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta actionMeta = action.getItemMeta();
        if (actionMeta != null) {
            actionMeta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.logic.operacion.titulo")));
            List<String> loreConfig = getMessageList("menus.logic.operacion.lore");
            actionMeta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%action%", act))).collect(Collectors.toList()));
            action.setItemMeta(actionMeta);
        }
        inv.setItem(15, action);

        ItemStack save = new ItemStack(Material.LIME_DYE);
        ItemMeta saveMeta = save.getItemMeta();
        if (saveMeta != null) {
            saveMeta.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.logic.guardar.titulo")));
            List<String> loreConfig = getMessageList("menus.logic.guardar.lore");
            saveMeta.lore(loreConfig.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));
            save.setItemMeta(saveMeta);
        }
        inv.setItem(22, save);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 🌟 CORRECCIÓN 2: Validación de título moderna usando componentes Kyori
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String expectedTitle = PlainTextComponentSerializer.plainText().serialize(
                CrossplayUtils.parseCrossplay(player, getMessage("menus.logic.titulo"))
        );

        if (!plainTitle.equals(expectedTitle)) return;

        event.setCancelled(true);
        UUID id = player.getUniqueId();
        if (!editingFactory.containsKey(id)) return;

        ActiveFactory factory = editingFactory.get(id);

        if (event.getSlot() == 11) {
            int next = (currentConditionIndex.get(id) + 1) % conditions.size();
            currentConditionIndex.put(id, next);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            renderMenu(player);
        } else if (event.getSlot() == 15) {
            int next = (currentActionIndex.get(id) + 1) % actions.size();
            currentActionIndex.put(id, next);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            renderMenu(player);
        } else if (event.getSlot() == 22) {
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
            CrossplayUtils.sendMessage(player, getMessage("eventos.script-compilado"));

            editingFactory.remove(id);
            currentConditionIndex.remove(id);
            currentActionIndex.remove(id);
        }
    }
}