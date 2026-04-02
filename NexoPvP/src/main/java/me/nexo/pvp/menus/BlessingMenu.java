package me.nexo.pvp.menus;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import me.nexo.pvp.NexoPvP; // 🌟 IMPORTACIÓN DE TU PLUGIN
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors; // 🌟 IMPORTACIÓN AÑADIDA

public class BlessingMenu extends NexoMenu {

    private final NexoCore core;
    private final NexoPvP plugin; // 🌟 VARIABLE AÑADIDA

    public BlessingMenu(Player player) {
        super(player);
        this.core = NexoCore.getPlugin(NexoCore.class);
        this.plugin = NexoPvP.getPlugin(NexoPvP.class); // Auto-vinculación al plugin local
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
        return getMessage("menus.templo.titulo");
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Aplica automáticamente el fondo Púrpura Profundo

        // 🌟 BENDICIÓN ESTÁNDAR (Economía In-Game)
        ItemStack standardBless = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta metaStd = standardBless.getItemMeta();
        if (metaStd != null) {
            metaStd.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.templo.items.bendicion-menor.nombre")));

            // Leemos el lore de la config y lo parseamos para crossplay/colores
            List<net.kyori.adventure.text.Component> loreStd = getMessageList("menus.templo.items.bendicion-menor.lore").stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line))
                    .collect(Collectors.toList());

            metaStd.lore(loreStd);
            metaStd.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "buy_bless_coins");
            standardBless.setItemMeta(metaStd);
        }

        // 🌟 BENDICIÓN PREMIUM (Monetización)
        ItemStack premiumBless = new ItemStack(Material.NETHER_STAR);
        ItemMeta metaPrem = premiumBless.getItemMeta();
        if (metaPrem != null) {
            metaPrem.displayName(CrossplayUtils.parseCrossplay(player, getMessage("menus.templo.items.bendicion-premium.nombre")));

            List<net.kyori.adventure.text.Component> lorePrem = getMessageList("menus.templo.items.bendicion-premium.lore").stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line))
                    .collect(Collectors.toList());

            metaPrem.lore(lorePrem);
            metaPrem.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "buy_bless_premium");
            premiumBless.setItemMeta(metaPrem);
        }

        inventory.setItem(11, standardBless);
        inventory.setItem(15, premiumBless);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto contra robos de ítems

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        NexoUser user = core.getUserManager().getUserOrNull(player.getUniqueId());
        if (user == null) return;

        // Validar si ya la tiene para no cobrarle doble
        if (user.hasActiveBlessing("VOID_BLESSING")) {
            player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.bendicion-activa")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        player.closeInventory();

        if (action.equals("buy_bless_coins")) {
            // 🌟 COMPRA CON MONEDAS (Asíncrona)
            BigDecimal cost = new BigDecimal("50000");
            NexoEconomy eco = NexoEconomy.getPlugin(NexoEconomy.class);

            eco.getEconomyManager().updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, cost, false).thenAccept(success -> {
                if (success) {
                    aplicarBendicion(player, user, "VOID_BLESSING", getMessage("mensajes.exito.compra-menor"));
                } else {
                    player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.sin-monedas")));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            });

        } else if (action.equals("buy_bless_premium")) {
            // 🌟 COMPRA CON GEMAS (Síncrona en memoria, asíncrona en SQL)
            int gemCost = 150;
            if (user.getGems() >= gemCost) {
                user.removeGems(gemCost);
                aplicarBendicion(player, user, "VOID_BLESSING", getMessage("mensajes.exito.compra-premium"));
            } else {
                player.sendMessage(NexoColor.parse(getMessage("mensajes.errores.sin-gemas")));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    private void aplicarBendicion(Player player, NexoUser user, String blessingId, String successMsg) {
        user.addBlessing(blessingId);
        core.getDatabaseManager().saveUserBlessings(user); // ⚡ Zero-Main-Thread SQL

        player.sendMessage(NexoColor.parse(successMsg));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
    }
}