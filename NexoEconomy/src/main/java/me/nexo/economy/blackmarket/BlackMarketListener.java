package me.nexo.economy.blackmarket;

import me.nexo.core.utils.NexoColor;
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
        if (!plainTitle.equals(BlackMarketMenu.TITLE_PLAIN)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!plugin.getBlackMarketManager().isMarketOpen()) {
            player.closeInventory();
            player.sendMessage(NexoColor.parse("&#ff4b2b[!] El Mercader ha huido repentinamente entre las sombras..."));
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

            player.sendMessage(NexoColor.parse("&#8b008b[MERCADER] &#434343Procesando el pago con el inframundo..."));

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
                    player.sendMessage(NexoColor.parse("&#8b008b[MERCADER] &#00fbffHa sido un placer hacer negocios oscuros contigo..."));
                    player.closeInventory();
                } else {
                    String divisa = bmItem.currency() == NexoAccount.Currency.GEMS ? "Gemas" : "Maná";
                    player.sendMessage(NexoColor.parse("&#ff4b2b[!] Fondos insuficientes de " + divisa + " para completar la transacción."));
                }
            });
        }
    }
}