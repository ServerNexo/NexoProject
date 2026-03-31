package me.nexo.economy.blackmarket;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
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

import java.util.List;
import java.util.stream.Collectors;

public class BlackMarketMenu extends NexoMenu {

    private final NexoEconomy plugin;

    public BlackMarketMenu(Player player, NexoEconomy plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return plugin.getConfigManager().getMessage("menus.blackmarket.titulo");
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Rellena el fondo morado automáticamente

        List<BlackMarketItem> stock = plugin.getBlackMarketManager().getCurrentStock();
        int[] slots = {11, 13, 15};

        for (int i = 0; i < stock.size() && i < slots.length; i++) {
            BlackMarketItem bmItem = stock.get(i);

            ItemStack display = bmItem.displayItem().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.blackmarket.item-lore");
                String color = bmItem.currency() == NexoAccount.Currency.GEMS ? "&#00f5ff" : "&#ff00ff";
                String divisaNombre = bmItem.currency() == NexoAccount.Currency.GEMS ? "💎 Gemas" : "💧 Maná";

                List<net.kyori.adventure.text.Component> lore = loreConfig.stream()
                        .map(line -> CrossplayUtils.parseCrossplay(player, line
                                .replace("%color%", color)
                                .replace("%price%", bmItem.price().toString())
                                .replace("%currency%", divisaNombre)))
                        .collect(Collectors.toList());

                if (meta.hasLore()) {
                    List<net.kyori.adventure.text.Component> originalLore = meta.lore();
                    if (originalLore != null) {
                        originalLore.addAll(lore);
                        meta.lore(originalLore);
                    } else {
                        meta.lore(lore);
                    }
                } else {
                    meta.lore(lore);
                }

                // 🌟 MAGIA PDC: Guardamos el índice del ítem para identificarlo en el clic
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "bm_index"), PersistentDataType.INTEGER, i);
                display.setItemMeta(meta);
            }
            inventory.setItem(slots[i], display);
        }
    }

    // 🌟 LÓGICA DE COMPRA INCORPORADA (Adiós BlackMarketListener)
    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto contra robos

        if (!plugin.getBlackMarketManager().isMarketOpen()) {
            player.closeInventory();
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blackmarket.mercader-huido"));
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey indexKey = new NamespacedKey(plugin, "bm_index");

        // Si el ítem que clickeó tiene nuestra llave del Mercado Negro
        if (meta.getPersistentDataContainer().has(indexKey, PersistentDataType.INTEGER)) {
            int index = meta.getPersistentDataContainer().get(indexKey, PersistentDataType.INTEGER);
            List<BlackMarketItem> stock = plugin.getBlackMarketManager().getCurrentStock();

            if (index >= 0 && index < stock.size()) {
                BlackMarketItem bmItem = stock.get(index);

                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blackmarket.procesando-pago"));

                // 🚀 Compra asíncrona segura contra la base de datos
                plugin.getEconomyManager().updateBalanceAsync(
                        player.getUniqueId(),
                        NexoAccount.AccountType.PLAYER,
                        bmItem.currency(),
                        bmItem.price(),
                        false
                ).thenAccept(success -> {
                    if (success) {
                        ItemStack buyItem = bmItem.displayItem().clone();
                        // Entregamos el ítem al inventario o lo tiramos al suelo si está lleno
                        if (player.getInventory().firstEmpty() == -1) {
                            player.getWorld().dropItemNaturally(player.getLocation(), buyItem);
                        } else {
                            player.getInventory().addItem(buyItem);
                        }
                        CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blackmarket.negocios-exitosos"));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
                        player.closeInventory();
                    } else {
                        String divisa = bmItem.currency() == NexoAccount.Currency.GEMS ? "Gemas" : "Maná";
                        CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blackmarket.fondos-insuficientes").replace("%divisa%", divisa));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                });
            }
        }
    }
}