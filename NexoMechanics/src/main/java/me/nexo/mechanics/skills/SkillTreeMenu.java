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

public class SkillTreeMenu extends NexoMenu {

    private final NexoMechanics plugin;

    // Tu registro original de permisos temporales
    private static final Map<UUID, PermissionAttachment> perms = new HashMap<>();

    public SkillTreeMenu(Player player, NexoMechanics plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "&#00f5ff🌳 Árbol de Conocimiento";
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
        setItem(4, Material.ENCHANTED_BOOK, "&#ff00ffTus Puntos: &#00f5ff" + kp + " KP",
                List.of("&#E6CCFFUsa estos puntos para desbloquear", "&#E6CCFFnuevos nodos tecnológicos."));

        // 🌟 NODO DE EJEMPLO (Plantilla para que agregues el resto)
        ItemStack miningNode = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta miningMeta = miningNode.getItemMeta();
        if (miningMeta != null) {
            miningMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ffNodo: Extracción Profunda"));
            miningMeta.lore(List.of(
                    CrossplayUtils.parseCrossplay(player, "&#E6CCFFPermite minar minerales del vacío."),
                    CrossplayUtils.parseCrossplay(player, " "),
                    CrossplayUtils.parseCrossplay(player, "&#ff00ffCosto: &#00f5ff5 KP")
            ));
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
        NexoUser user = NexoAPI.getInstance().getUserLocal(p.getUniqueId());

        if (user == null) {
            p.sendMessage(NexoColor.parse("&#8b0000[!] Sincronización neuronal incompleta. Aguarda un momento."));
            return;
        }

        if (user.getKnowledgePoints() >= costo) {
            user.removeKnowledgePoints(costo);

            PermissionAttachment attachment = perms.computeIfAbsent(p.getUniqueId(),
                    k -> p.addAttachment(me.nexo.core.NexoCore.getPlugin(me.nexo.core.NexoCore.class)));

            attachment.setPermission(permiso, true);

            p.sendMessage(NexoColor.parse("&#00f5ff[✓] <bold>NODO DESBLOQUEADO:</bold> &#E6CCFFNueva tecnología industrial integrada a tu sistema."));
            p.sendMessage(NexoColor.parse("&#E6CCFFPuntos de Conocimiento restantes: &#00f5ff" + user.getKnowledgePoints()));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        } else {
            p.sendMessage(NexoColor.parse("&#8b0000[!] Conocimiento Insuficiente. &#E6CCFFRequieres más experiencia práctica para procesar este nodo."));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    public static void limpiarCachePermisos(UUID uuid) {
        perms.remove(uuid);
    }
}