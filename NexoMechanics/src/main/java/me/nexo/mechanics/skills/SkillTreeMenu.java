package me.nexo.mechanics.skills;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import me.nexo.mechanics.NexoMechanics;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SkillTreeMenu extends NexoMenu {

    private final NexoMechanics plugin;

    // Tu registro original de permisos temporales
    private static final Map<UUID, PermissionAttachment> perms = new HashMap<>();

    public SkillTreeMenu(Player player, NexoMechanics plugin) {
        super(player);
        this.plugin = plugin;
    }

    // 🌟 MÉTODOS MÁGICOS DE LECTURA
    private String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    private List<String> getMessageList(String path) {
        return plugin.getConfigManager().getMessages().getStringList(path);
    }

    @Override
    public String getMenuName() {
        return getMessage("menus.skills.titulo");
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Fondo de la Arquitectura Omega

        NexoUser user = NexoAPI.getInstance().getUserLocal(player.getUniqueId());
        int kp = user != null ? user.getKnowledgePoints() : 0;

        // 🌟 ÍTEM DE INFORMACIÓN DEL JUGADOR
        String infoName = getMessage("menus.skills.items.info.nombre").replace("%kp%", String.valueOf(kp));
        List<String> infoLore = getMessageList("menus.skills.items.info.lore");
        setItem(4, Material.ENCHANTED_BOOK, infoName, infoLore);

        // 🌟 NODO DE EJEMPLO (Minería)
        ItemStack miningNode = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta miningMeta = miningNode.getItemMeta();
        if (miningMeta != null) {
            String nodeName = getMessage("menus.skills.nodos.mineria.nombre");
            miningMeta.displayName(CrossplayUtils.parseCrossplay(player, nodeName));

            List<net.kyori.adventure.text.Component> lore = getMessageList("menus.skills.nodos.mineria.lore").stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%cost%", "5")))
                    .collect(Collectors.toList());

            miningMeta.lore(lore);

            // MAGIA PDC: Guardamos el permiso a dar y cuánto cuesta
            miningMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "unlock_node");
            miningMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "node_perm"), PersistentDataType.STRING, "nexo.skills.mining.1");
            miningMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "node_cost"), PersistentDataType.INTEGER, 5);
            miningNode.setItemMeta(miningMeta);
        }
        inventory.setItem(22, miningNode);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto anti-robos

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        if (meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

            if (action.equals("unlock_node")) {
                String perm = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "node_perm"), PersistentDataType.STRING);
                Integer cost = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "node_cost"), PersistentDataType.INTEGER);

                if (perm != null && cost != null) {
                    desbloquearNodo(player, perm, cost);
                    // Actualizamos la interfaz al instante para que vea cómo se restaron sus KP
                    setMenuItems();
                }
            }
        }
    }

    // ==========================================
    // 🧠 TU LÓGICA ORIGINAL INTACTA
    // ==========================================
    public static void desbloquearNodo(Player p, String permiso, int costo) {
        NexoMechanics mechPlugin = NexoMechanics.getPlugin(NexoMechanics.class);
        NexoUser user = NexoAPI.getInstance().getUserLocal(p.getUniqueId());

        if (user == null) {
            p.sendMessage(NexoColor.parse(mechPlugin.getConfigManager().getMessage("mensajes.errores.sincronizacion-incompleta")));
            return;
        }

        if (user.getKnowledgePoints() >= costo) {
            user.removeKnowledgePoints(costo);

            PermissionAttachment attachment = perms.computeIfAbsent(p.getUniqueId(),
                    k -> p.addAttachment(me.nexo.core.NexoCore.getPlugin(me.nexo.core.NexoCore.class)));

            attachment.setPermission(permiso, true);

            p.sendMessage(NexoColor.parse(mechPlugin.getConfigManager().getMessage("mensajes.exito.nodo-desbloqueado")));
            p.sendMessage(NexoColor.parse(mechPlugin.getConfigManager().getMessage("mensajes.exito.puntos-restantes").replace("%kp%", String.valueOf(user.getKnowledgePoints()))));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        } else {
            p.sendMessage(NexoColor.parse(mechPlugin.getConfigManager().getMessage("mensajes.errores.conocimiento-insuficiente")));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    public static void limpiarCachePermisos(UUID uuid) {
        perms.remove(uuid);
    }
}