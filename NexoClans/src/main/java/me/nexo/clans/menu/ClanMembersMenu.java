package me.nexo.clans.menu;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.ClanMember;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ClanMembersMenu extends NexoMenu {

    private final NexoClans plugin;
    private final NexoCore core;
    private final NexoClan clan;
    private final NexoUser user;

    public ClanMembersMenu(Player player, NexoClans plugin, NexoClan clan, NexoUser user) {
        super(player);
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
        this.clan = clan;
        this.user = user;
    }

    @Override
    public String getMenuName() {
        return core.getConfigManager().getMessage("clans_messages.yml", "menus.miembros.titulo");
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        CrossplayUtils.sendMessage(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.miembros.buscando"));

        // Ítem temporal mientras carga
        ItemStack loading = new ItemStack(Material.CLOCK);
        ItemMeta lMeta = loading.getItemMeta();
        lMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>Buscando miembros...</bold>"));
        loading.setItemMeta(lMeta);
        inventory.setItem(22, loading);

        // 🚀 Carga asíncrona segura a la base de datos
        plugin.getClanManager().getMiembrosAsync(clan.getId(), miembros -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Protección Anti-Crash: Verificamos si el jugador cerró el menú mientras cargaba
                if (player.getOpenInventory().getTopInventory() != inventory) return;

                inventory.clear(); // Limpiamos el ítem de carga

                for (int i = 0; i < miembros.size() && i < 53; i++) {
                    ClanMember m = miembros.get(i);
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();

                    if (meta != null) {
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(m.uuid()));
                        meta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff" + m.name()));

                        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                        lore.add(CrossplayUtils.parseCrossplay(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.miembros.lore-rango").replace("%role%", m.role())));
                        meta.lore(lore);

                        // 🌟 Magia PDC: Guardamos el nombre del jugador dentro del ítem de forma segura
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "member_name"), PersistentDataType.STRING, m.name());

                        head.setItemMeta(meta);
                    }
                    inventory.setItem(i, head);
                }

                // Botón de regresar
                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta backMeta = back.getItemMeta();
                if (backMeta != null) {
                    backMeta.displayName(CrossplayUtils.parseCrossplay(player, core.getConfigManager().getMessage("clans_messages.yml", "menus.miembros.boton-regresar")));
                    back.setItemMeta(backMeta);
                }
                inventory.setItem(49, back);
            });
        });
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Botón de Regresar
        if (clicked.getType() == Material.ARROW && event.getRawSlot() == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            new ClanMenu(player, plugin, clan, user).open();
            return;
        }

        // Sistema de Expulsión Seguro (Right Click a una cabeza)
        if (clicked.getType() == Material.PLAYER_HEAD && event.getClick().isRightClick()) {
            NamespacedKey memberKey = new NamespacedKey(plugin, "member_name");
            if (clicked.getItemMeta().getPersistentDataContainer().has(memberKey, PersistentDataType.STRING)) {
                String targetName = clicked.getItemMeta().getPersistentDataContainer().get(memberKey, PersistentDataType.STRING);

                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
                player.closeInventory();

                // Ejecución instantánea del comando
                player.performCommand("clan kick " + targetName);
            }
        }
    }
}