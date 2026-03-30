package me.nexo.pvp.menus;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BlessingMenu extends NexoMenu {

    private final NexoCore core;

    public BlessingMenu(Player player) {
        super(player);
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    @Override
    public String getMenuName() {
        return "&#ff00ff🏛 Templo del Vacío";
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
            metaStd.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ff<bold>Bendición Menor</bold>"));
            List<net.kyori.adventure.text.Component> loreStd = new ArrayList<>();
            loreStd.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aProtege tu experiencia y equipo de 1 muerte."));
            loreStd.add(CrossplayUtils.parseCrossplay(player, " "));
            loreStd.add(CrossplayUtils.parseCrossplay(player, "&#ff00ffPrecio: 50,000 Monedas"));
            metaStd.lore(loreStd);
            metaStd.getPersistentDataContainer().set(new NamespacedKey(core, "action"), PersistentDataType.STRING, "buy_bless_coins");
            standardBless.setItemMeta(metaStd);
        }

        // 🌟 BENDICIÓN PREMIUM (Monetización)
        ItemStack premiumBless = new ItemStack(Material.NETHER_STAR);
        ItemMeta metaPrem = premiumBless.getItemMeta();
        if (metaPrem != null) {
            metaPrem.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>Bendición del Vacío Absoluto</bold>"));
            List<net.kyori.adventure.text.Component> lorePrem = new ArrayList<>();
            lorePrem.add(CrossplayUtils.parseCrossplay(player, "&#1c0f2aProtección total garantizada por los dioses."));
            lorePrem.add(CrossplayUtils.parseCrossplay(player, " "));
            lorePrem.add(CrossplayUtils.parseCrossplay(player, "&#00f5ffPrecio: 150 Gemas"));
            metaPrem.lore(lorePrem);
            metaPrem.getPersistentDataContainer().set(new NamespacedKey(core, "action"), PersistentDataType.STRING, "buy_bless_premium");
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
        NamespacedKey actionKey = new NamespacedKey(core, "action");

        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        NexoUser user = core.getUserManager().getUserOrNull(player.getUniqueId());
        if (user == null) return;

        // Validar si ya la tiene para no cobrarle doble
        if (user.hasActiveBlessing("VOID_BLESSING")) {
            player.sendMessage(NexoColor.parse("&#ff4b2b[!] Ya posees una Bendición activa protegiendo tu alma."));
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
                    aplicarBendicion(player, user, "VOID_BLESSING", "&#00f5ff[⚡] Has adquirido la Bendición Menor. Tu próxima muerte será perdonada.");
                } else {
                    player.sendMessage(NexoColor.parse("&#8b0000[!] No tienes suficientes monedas para este rito."));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            });

        } else if (action.equals("buy_bless_premium")) {
            // 🌟 COMPRA CON GEMAS (Síncrona en memoria, asíncrona en SQL)
            int gemCost = 150;
            if (user.getGems() >= gemCost) {
                user.removeGems(gemCost);
                aplicarBendicion(player, user, "VOID_BLESSING", "&#ff00ff[⚡] Has adquirido la Bendición del Vacío Absoluto mediante ofrendas premium.");
            } else {
                player.sendMessage(NexoColor.parse("&#8b0000[!] No tienes suficientes Gemas. Adquiérelas en la tienda corporativa."));
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