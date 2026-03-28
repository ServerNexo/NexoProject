package me.nexo.pvp.menus;

import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;

public class BlessingMenuListener implements Listener {

    private final NexoCore core;

    public BlessingMenuListener() {
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlessingMenu)) return;
        event.setCancelled(true); // Evitamos que roben los ítems

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
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
                // Aquí podrías guardar las gemas en la DB si lo deseas
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