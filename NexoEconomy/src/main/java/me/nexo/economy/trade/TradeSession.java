package me.nexo.economy.trade;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class TradeSession {

    private final NexoEconomy plugin;
    private final Player player1;
    private final Player player2;
    private final Inventory inventory;

    private boolean p1Ready = false;
    private boolean p2Ready = false;

    private BigDecimal p1Coins = BigDecimal.ZERO;
    private BigDecimal p2Coins = BigDecimal.ZERO;
    private BigDecimal p1Gems = BigDecimal.ZERO;
    private BigDecimal p2Gems = BigDecimal.ZERO;
    private BigDecimal p1Mana = BigDecimal.ZERO;
    private BigDecimal p2Mana = BigDecimal.ZERO;

    private int taskID = -1;

    public TradeSession(NexoEconomy plugin, Player player1, Player player2) {
        this.plugin = plugin;
        this.player1 = player1;
        this.player2 = player2;
        this.inventory = Bukkit.createInventory(null, 54, CrossplayUtils.parseCrossplay(player1, plugin.getConfigManager().getMessage("menus.trade.titulo")));
        setupGUI();
    }

    private void setupGUI() {
        setItem(13, Material.GOLD_INGOT, plugin.getConfigManager().getMessage("menus.trade.transferir-monedas"), plugin.getConfigManager().getMessage("menus.trade.depositar-lore"));
        setItem(22, Material.EMERALD, plugin.getConfigManager().getMessage("menus.trade.transferir-gemas"), plugin.getConfigManager().getMessage("menus.trade.depositar-lore"));
        setItem(31, Material.AMETHYST_SHARD, plugin.getConfigManager().getMessage("menus.trade.transferir-mana"), plugin.getConfigManager().getMessage("menus.trade.depositar-lore"));

        ItemStack separator = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta sepMeta = separator.getItemMeta();
        if (sepMeta != null) {
            sepMeta.displayName(CrossplayUtils.parseCrossplay(player1, " "));
            separator.setItemMeta(sepMeta);
        }

        inventory.setItem(4, separator);
        inventory.setItem(40, separator);
        inventory.setItem(49, separator);

        updateReadyButtons();
    }

    private void setItem(int slot, Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(CrossplayUtils.parseCrossplay(player1, name));
            List<net.kyori.adventure.text.Component> loreList = new java.util.ArrayList<>();
            for (String l : lore) {
                loreList.add(CrossplayUtils.parseCrossplay(player1, l));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    public void updateReadyButtons() {
        String p1Color = p1Ready ? plugin.getConfigManager().getMessage("menus.trade.estado.autorizado") : plugin.getConfigManager().getMessage("menus.trade.estado.esperando");
        List<String> p1LoreConfig = plugin.getConfigManager().getMessages().getStringList("menus.trade.estado.lore");
        List<net.kyori.adventure.text.Component> p1Lore = p1LoreConfig.stream()
                .map(line -> CrossplayUtils.parseCrossplay(player1, line
                        .replace("%player%", player1.getName())
                        .replace("%coins%", p1Coins.toString())
                        .replace("%gems%", p1Gems.toString())
                        .replace("%mana%", p1Mana.toString())))
                .collect(Collectors.toList());
        setItem(45, p1Ready ? Material.LIME_DYE : Material.RED_DYE, p1Color, p1Lore.stream().map(c -> "").toArray(String[]::new));

        String p2Color = p2Ready ? plugin.getConfigManager().getMessage("menus.trade.estado.autorizado") : plugin.getConfigManager().getMessage("menus.trade.estado.esperando");
        List<String> p2LoreConfig = plugin.getConfigManager().getMessages().getStringList("menus.trade.estado.lore");
        List<net.kyori.adventure.text.Component> p2Lore = p2LoreConfig.stream()
                .map(line -> CrossplayUtils.parseCrossplay(player2, line
                        .replace("%player%", player2.getName())
                        .replace("%coins%", p2Coins.toString())
                        .replace("%gems%", p2Gems.toString())
                        .replace("%mana%", p2Mana.toString())))
                .collect(Collectors.toList());
        setItem(53, p2Ready ? Material.LIME_DYE : Material.RED_DYE, p2Color, p2Lore.stream().map(c -> "").toArray(String[]::new));
    }

    public void addCurrency(Player p, NexoAccount.Currency currency, BigDecimal amount) {
        if (p.equals(player1)) {
            if (currency == NexoAccount.Currency.COINS) p1Coins = p1Coins.add(amount);
            else if (currency == NexoAccount.Currency.GEMS) p1Gems = p1Gems.add(amount);
            else if (currency == NexoAccount.Currency.MANA) p1Mana = p1Mana.add(amount);
        } else {
            if (currency == NexoAccount.Currency.COINS) p2Coins = p2Coins.add(amount);
            else if (currency == NexoAccount.Currency.GEMS) p2Gems = p2Gems.add(amount);
            else if (currency == NexoAccount.Currency.MANA) p2Mana = p2Mana.add(amount);
        }
        unready();
        updateReadyButtons();
    }

    public void toggleReady(Player player) {
        if (player.equals(player1)) p1Ready = !p1Ready;
        else p2Ready = !p2Ready;

        updateReadyButtons();

        if (p1Ready && p2Ready) iniciarCuentaRegresiva();
        else cancelarCuenta();
    }

    private void iniciarCuentaRegresiva() {
        taskID = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::ejecutarIntercambio, 60L);
        CrossplayUtils.sendMessage(player1, plugin.getConfigManager().getMessage("eventos.trade.session.cuenta-regresiva"));
        CrossplayUtils.sendMessage(player2, plugin.getConfigManager().getMessage("eventos.trade.session.cuenta-regresiva"));
    }

    public void cancelarCuenta() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }

    private void ejecutarIntercambio() {
        transferirLado(true);
        transferirLado(false);

        transferCurrency(NexoAccount.Currency.COINS, p1Coins, p2Coins);
        transferCurrency(NexoAccount.Currency.GEMS, p1Gems, p2Gems);
        transferCurrency(NexoAccount.Currency.MANA, p1Mana, p2Mana);

        player1.closeInventory();
        player2.closeInventory();
        CrossplayUtils.sendMessage(player1, plugin.getConfigManager().getMessage("eventos.trade.session.intercambio-exitoso"));
        CrossplayUtils.sendMessage(player2, plugin.getConfigManager().getMessage("eventos.trade.session.intercambio-exitoso"));
    }

    private void transferCurrency(NexoAccount.Currency currency, BigDecimal amountP1, BigDecimal amountP2) {
        if (amountP1.compareTo(BigDecimal.ZERO) > 0) {
            plugin.getEconomyManager().updateBalanceAsync(player1.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amountP1, false);
            plugin.getEconomyManager().updateBalanceAsync(player2.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amountP1, true);
        }
        if (amountP2.compareTo(BigDecimal.ZERO) > 0) {
            plugin.getEconomyManager().updateBalanceAsync(player2.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amountP2, false);
            plugin.getEconomyManager().updateBalanceAsync(player1.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amountP2, true);
        }
    }

    private void transferirLado(boolean deP1aP2) {
        Player receptor = deP1aP2 ? player2 : player1;
        for (int i = 0; i < 54; i++) {
            boolean esSlotDeOrigen = deP1aP2 ? (i % 9 < 4) : (i % 9 > 4);
            if (i >= 45) esSlotDeOrigen = false;

            if (esSlotDeOrigen) {
                ItemStack item = inventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    if (receptor.getInventory().firstEmpty() == -1)
                        receptor.getWorld().dropItemNaturally(receptor.getLocation(), item);
                    else receptor.getInventory().addItem(item);
                    inventory.setItem(i, null);
                }
            }
        }
    }

    public void unready() {
        p1Ready = false;
        p2Ready = false;
        updateReadyButtons();
        cancelarCuenta();
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void open() {
        player1.openInventory(inventory);
        player2.openInventory(inventory);
    }
}