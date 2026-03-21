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
import java.util.ArrayList;
import java.util.List;

public class TradeSession {

    private final Player player1;
    private final Player player2;
    private final Inventory inventory;

    private boolean p1Ready = false;
    private boolean p2Ready = false;

    // Dinero en el trade
    private BigDecimal p1Coins = BigDecimal.ZERO;
    private BigDecimal p2Coins = BigDecimal.ZERO;

    private int taskID = -1;

    public TradeSession(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.inventory = Bukkit.createInventory(null, 54, "§8Intercambio Seguro");
        setupGUI();
    }

    private void setupGUI() {
        // Cristal separador y botones de dinero (Columna 4 - Index 4, 13, 22, 31, 40, 49)
        setItem(13, Material.GOLD_INGOT, "§e🪙 Añadir 1,000 Monedas", "§7Haz clic para poner dinero.");
        setItem(22, Material.DIAMOND, "§a💎 Añadir 100 Gemas", "§7Haz clic para poner gemas.");
        setItem(31, Material.LAPIS_LAZULI, "§b💧 Añadir 10 Maná", "§7Haz clic para poner maná.");

        ItemStack separator = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        inventory.setItem(4, separator);
        inventory.setItem(40, separator);
        inventory.setItem(49, separator);

        updateReadyButtons();
    }

    private void setItem(int slot, Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> l = new ArrayList<>();
        l.add(lore);
        meta.setLore(l);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    public void updateReadyButtons() {
        // Lado P1
        setItem(45, p1Ready ? Material.LIME_DYE : Material.RED_DYE,
                p1Ready ? "§a§lLISTO" : "§c§lCONFIRMAR", "§7Dinero: §e" + p1Coins + " Monedas");

        // Lado P2
        setItem(53, p2Ready ? Material.LIME_DYE : Material.RED_DYE,
                p2Ready ? "§a§lLISTO" : "§c§lCONFIRMAR", "§7Dinero: §e" + p2Coins + " Monedas");
    }

    public void addMoney(Player p, BigDecimal amount) {
        if (p.equals(player1)) p1Coins = p1Coins.add(amount);
        else p2Coins = p2Coins.add(amount);
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
        // 1. Transferir ítems de P1 a P2
        transferirLado(true);
        // 2. Transferir ítems de P2 a P1
        transferirLado(false);

        // 3. Transferir Dinero (Llamamos a tu EconomyManager)
        NexoEconomy eco = NexoEconomy.getPlugin(NexoEconomy.class);
        if (p1Coins.compareTo(BigDecimal.ZERO) > 0) {
            eco.getEconomyManager().updateBalanceAsync(player1.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, p1Coins, false);
            eco.getEconomyManager().updateBalanceAsync(player2.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, p1Coins, true);
        }
        if (p2Coins.compareTo(BigDecimal.ZERO) > 0) {
            eco.getEconomyManager().updateBalanceAsync(player2.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, p2Coins, false);
            eco.getEconomyManager().updateBalanceAsync(player1.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, p2Coins, true);
        }

        player1.closeInventory();
        player2.closeInventory();
        player1.sendMessage("§a§l¡Intercambio realizado con éxito!");
        player2.sendMessage("§a§l¡Intercambio realizado con éxito!");
    }

    private void transferirLado(boolean deP1aP2) {
        Player receptor = deP1aP2 ? player2 : player1;
        for (int i = 0; i < 54; i++) {
            boolean esSlotDeOrigen = deP1aP2 ? (i % 9 < 4) : (i % 9 > 4);
            // Ignoramos filas de botones
            if (i >= 45) esSlotDeOrigen = false;

            if (esSlotDeOrigen) {
                ItemStack item = inventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    // Si el receptor tiene el inventario lleno, soltamos el ítem al suelo
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

    // Getters y métodos de limpieza
    public void unready() { p1Ready = false; p2Ready = false; updateReadyButtons(); cancelarCuenta(); }
    public Inventory getInventory() { return inventory; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
}