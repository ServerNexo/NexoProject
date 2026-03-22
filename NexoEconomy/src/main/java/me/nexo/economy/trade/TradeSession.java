package me.nexo.economy.trade;

import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.Arrays;

public class TradeSession {

    private final Player player1;
    private final Player player2;
    private final Inventory inventory;

    private boolean p1Ready = false;
    private boolean p2Ready = false;

    // 🌟 DINERO EN EL TRADE (Multidivisa)
    private BigDecimal p1Coins = BigDecimal.ZERO;
    private BigDecimal p2Coins = BigDecimal.ZERO;
    private BigDecimal p1Gems = BigDecimal.ZERO;
    private BigDecimal p2Gems = BigDecimal.ZERO;
    private BigDecimal p1Mana = BigDecimal.ZERO;
    private BigDecimal p2Mana = BigDecimal.ZERO;

    private int taskID = -1;

    public TradeSession(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.inventory = Bukkit.createInventory(null, 54, "§8Intercambio Seguro");
        setupGUI();
    }

    private void setupGUI() {
        // 💎 Botones Multidivisa en el centro (Columna 4)
        setItem(13, Material.GOLD_INGOT, "§e🪙 Añadir 1,000 Monedas", "§7Haz clic para poner monedas.");
        setItem(22, Material.EMERALD, "§a💎 Añadir 100 Gemas", "§7Haz clic para poner gemas.");
        setItem(31, Material.AMETHYST_SHARD, "§b💧 Añadir 10 Maná", "§7Haz clic para poner maná.");

        ItemStack separator = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        inventory.setItem(4, separator);
        inventory.setItem(40, separator);
        inventory.setItem(49, separator);

        updateReadyButtons();
    }

    // 🌟 Utilidad mejorada para soportar múltiples líneas de Lore
    private void setItem(int slot, Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    public void updateReadyButtons() {
        // Lado P1: Mostramos las 3 divisas
        setItem(45, p1Ready ? Material.LIME_DYE : Material.RED_DYE,
                p1Ready ? "§a§lLISTO" : "§c§lCONFIRMAR",
                "§7Aportes de " + player1.getName() + ":",
                "§e🪙 " + p1Coins + " Monedas",
                "§a💎 " + p1Gems + " Gemas",
                "§b💧 " + p1Mana + " Maná");

        // Lado P2: Mostramos las 3 divisas
        setItem(53, p2Ready ? Material.LIME_DYE : Material.RED_DYE,
                p2Ready ? "§a§lLISTO" : "§c§lCONFIRMAR",
                "§7Aportes de " + player2.getName() + ":",
                "§e🪙 " + p2Coins + " Monedas",
                "§a💎 " + p2Gems + " Gemas",
                "§b💧 " + p2Mana + " Maná");
    }

    // 🌟 NUEVO MOTOR DE INYECCIÓN DE DIVISAS
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
        unready(); // Quitamos el "Listo" por seguridad
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
        taskID = Bukkit.getScheduler().scheduleSyncDelayedTask(NexoEconomy.getPlugin(NexoEconomy.class), this::ejecutarIntercambio, 60L); // 3 segundos
        player1.sendMessage("§eEl intercambio se completará en 3 segundos...");
        player2.sendMessage("§eEl intercambio se completará en 3 segundos...");
    }

    public void cancelarCuenta() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }

    private void ejecutarIntercambio() {
        // 1. Transferir ítems de inventario
        transferirLado(true);
        transferirLado(false);

        // 2. Transferir TODAS las Divisas Atómicamente
        NexoEconomy eco = NexoEconomy.getPlugin(NexoEconomy.class);
        transferCurrency(eco, NexoAccount.Currency.COINS, p1Coins, p2Coins);
        transferCurrency(eco, NexoAccount.Currency.GEMS, p1Gems, p2Gems);
        transferCurrency(eco, NexoAccount.Currency.MANA, p1Mana, p2Mana);

        player1.closeInventory();
        player2.closeInventory();
        player1.sendMessage("§a§l¡Intercambio realizado con éxito!");
        player2.sendMessage("§a§l¡Intercambio realizado con éxito!");
    }

    // 🌟 MOTOR AUXILIAR PARA TRANSFERENCIAS BANCARIAS
    private void transferCurrency(NexoEconomy eco, NexoAccount.Currency currency, BigDecimal amountP1, BigDecimal amountP2) {
        if (amountP1.compareTo(BigDecimal.ZERO) > 0) {
            eco.getEconomyManager().updateBalanceAsync(player1.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amountP1, false);
            eco.getEconomyManager().updateBalanceAsync(player2.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amountP1, true);
        }
        if (amountP2.compareTo(BigDecimal.ZERO) > 0) {
            eco.getEconomyManager().updateBalanceAsync(player2.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amountP2, false);
            eco.getEconomyManager().updateBalanceAsync(player1.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amountP2, true);
        }
    }

    private void transferirLado(boolean deP1aP2) {
        Player receptor = deP1aP2 ? player2 : player1;
        for (int i = 0; i < 54; i++) {
            boolean esSlotDeOrigen = deP1aP2 ? (i % 9 < 4) : (i % 9 > 4);
            if (i >= 45) esSlotDeOrigen = false; // Ignoramos botones

            if (esSlotDeOrigen) {
                ItemStack item = inventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    if (receptor.getInventory().firstEmpty() == -1) {
                        receptor.getWorld().dropItemNaturally(receptor.getLocation(), item);
                    } else {
                        receptor.getInventory().addItem(item);
                    }
                    inventory.setItem(i, null);
                }
            }
        }
    }

    public void unready() { p1Ready = false; p2Ready = false; updateReadyButtons(); cancelarCuenta(); }
    public Inventory getInventory() { return inventory; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }

    // 🌟 MÉTODO PARA ABRIR LA INTERFAZ
    public void open() {
        player1.openInventory(inventory);
        player2.openInventory(inventory);
    }
}