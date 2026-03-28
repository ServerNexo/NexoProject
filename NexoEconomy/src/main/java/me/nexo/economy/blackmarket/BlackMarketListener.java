package me.nexo.economy.blackmarket;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BlackMarketListener implements Listener {

    private final NexoEconomy plugin;

    public BlackMarketListener(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!plainTitle.equals(plugin.getConfigManager().getMessage("menus.blackmarket.titulo").replaceAll("<[^>]*>", ""))) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!plugin.getBlackMarketManager().isMarketOpen()) {
            player.closeInventory();
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blackmarket.mercader-huido"));
            return;
        }

        int slot = event.getSlot();
        List<BlackMarketItem> stock = plugin.getBlackMarketManager().getCurrentStock();

        int index = -1;
        if (slot == 11) index = 0;
        else if (slot == 13) index = 1;
        else if (slot == 15) index = 2;

        if (index != -1 && index < stock.size()) {
            BlackMarketItem bmItem = stock.get(index);

            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blackmarket.procesando-pago"));

            plugin.getEconomyManager().updateBalanceAsync(
                    player.getUniqueId(),
                    NexoAccount.AccountType.PLAYER,
                    bmItem.currency(),
                    bmItem.price(),
                    false
            ).thenAccept(success -> {
                if (success) {
                    ItemStack item = bmItem.displayItem().clone();
                    if (player.getInventory().firstEmpty() == -1) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    } else {
                        player.getInventory().addItem(item);
                    }
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blackmarket.negocios-exitosos"));
                    player.closeInventory();
                } else {
                    String divisa = bmItem.currency() == NexoAccount.Currency.GEMS ? "Gemas" : "Maná";
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blackmarket.fondos-insuficientes").replace("%divisa%", divisa));
                }
            });
        }
    }
}