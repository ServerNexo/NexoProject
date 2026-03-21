package me.nexo.economy.blackmarket;

import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
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
        if (!event.getView().getTitle().equals("§5🌑 Mercado Negro")) return;
        event.setCancelled(true); // Evitar que roben los ítems del menú

        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Si el admin cierra el mercado mientras alguien lo ve
        if (!plugin.getBlackMarketManager().isMarketOpen()) {
            player.closeInventory();
            player.sendMessage("§cEl Mercader ha huido repentinamente...");
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

            player.sendMessage("§eProcesando pago con el inframundo...");

            // ⚡ TRANSACCIÓN ATÓMICA: Cobramos Gemas o Maná
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
                    player.sendMessage("§5§l[MERCADER] §dUn placer hacer negocios oscuros contigo...");
                    player.closeInventory();
                } else {
                    String divisa = bmItem.currency() == NexoAccount.Currency.GEMS ? "Gemas" : "Maná";
                    player.sendMessage("§cNo tienes suficiente " + divisa + " para pagar esto.");
                }
            });
        }
    }
}