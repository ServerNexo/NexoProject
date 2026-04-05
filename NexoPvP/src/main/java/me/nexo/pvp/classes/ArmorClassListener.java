package me.nexo.pvp.classes;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.google.inject.Inject;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.config.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * 🏛️ NexoPvP - Listener de Clases de Armadura (Arquitectura Enterprise)
 */
public class ArmorClassListener implements Listener {

    // 💉 PILAR 3: Inyección
    private final NexoPvP plugin;
    private final ConfigManager configManager;

    private final NamespacedKey classKey;
    private final NamespacedKey healthModKey;
    private final NamespacedKey speedModKey;

    @Inject
    public ArmorClassListener(NexoPvP plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        // La llave maestra que leeremos del NBT de NexoItems
        this.classKey = new NamespacedKey("nexoitems", "nexo_class");
        this.healthModKey = new NamespacedKey(plugin, "class_health_modifier");
        this.speedModKey = new NamespacedKey(plugin, "class_speed_modifier");
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        // Evaluamos en el siguiente tick para asegurar que el inventario se haya actualizado
        plugin.getServer().getScheduler().runTask(plugin, () -> evaluateClassSet(event.getPlayer()));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        evaluateClassSet(event.getPlayer());
    }

    private void evaluateClassSet(Player player) {
        PlayerInventory inv = player.getInventory();
        String helmetClass = getClassTag(inv.getHelmet());
        String chestClass = getClassTag(inv.getChestplate());
        String legsClass = getClassTag(inv.getLeggings());
        String bootsClass = getClassTag(inv.getBoots());

        // Limpiamos atributos anteriores para evitar bugs de duplicación
        clearClassModifiers(player);

        // Si no tiene casco, o los tags no son idénticos en las 4 piezas, no hay buff
        if (helmetClass == null || !helmetClass.equals(chestClass) || !helmetClass.equals(legsClass) || !helmetClass.equals(bootsClass)) {
            return;
        }

        // 🌟 APLICACIÓN DE PASIVAS DE CLASE
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);

        switch (helmetClass.toUpperCase()) {
            case "ASSASSIN":
                // Asesino: Mitad de vida (-50%), +40% Velocidad
                if (healthAttr != null) healthAttr.addModifier(new AttributeModifier(healthModKey, -0.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
                if (speedAttr != null) speedAttr.addModifier(new AttributeModifier(speedModKey, 0.4, AttributeModifier.Operation.MULTIPLY_SCALAR_1));

                // 💡 PILAR 2: Action Bar Type-Safe
                CrossplayUtils.sendActionBar(player, configManager.getMessages().mensajes().pvp().setAsesinoActivo());
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.3f, 2.0f);
                break;

            case "INQUISITOR":
                // Inquisidor: -30% Vida
                if (healthAttr != null) healthAttr.addModifier(new AttributeModifier(healthModKey, -0.3, AttributeModifier.Operation.MULTIPLY_SCALAR_1));

                // 💡 PILAR 2: Action Bar Type-Safe
                CrossplayUtils.sendActionBar(player, configManager.getMessages().mensajes().pvp().setInquisidorActivo());
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
                break;
        }
    }

    private String getClassTag(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(classKey, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(classKey, PersistentDataType.STRING);
        }
        return null;
    }

    private void clearClassModifiers(Player player) {
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            for (AttributeModifier mod : healthAttr.getModifiers()) {
                if (mod.getKey().equals(healthModKey)) healthAttr.removeModifier(mod);
            }
        }

        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            for (AttributeModifier mod : speedAttr.getModifiers()) {
                if (mod.getKey().equals(speedModKey)) speedAttr.removeModifier(mod);
            }
        }
    }
}