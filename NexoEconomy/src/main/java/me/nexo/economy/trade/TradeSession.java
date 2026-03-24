package me.nexo.economy.trade;

import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    public static final String TITLE_PLAIN = "» Canal de Intercambio";
    public static final String TITLE_MENU = "&#434343<bold>»</bold> &#00fbffCanal de Intercambio";

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

    public TradeSession(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.inventory = Bukkit.createInventory(null, 54, NexoColor.parse(TITLE_MENU));
        setupGUI();
    }

    private void setupGUI() {
        setItem(13, Material.GOLD_INGOT, "&#fbd72b🪙 Transferir 1,000 Monedas", "&#434343Clic para depositar en el fondo.");
        setItem(22, Material.EMERALD, "&#a8ff78💎 Transferir 100 Gemas", "&#434343Clic para depositar en el fondo.");
        setItem(31, Material.AMETHYST_SHARD, "&#00fbff💧 Transferir 10 Maná", "&#434343Clic para depositar en el fondo.");

        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta sepMeta = separator.getItemMeta();
        if (sepMeta != null) {
            sepMeta.setDisplayName(serialize(" "));
            separator.setItemMeta(sepMeta);
        }

        inventory.setItem(4, separator);
        inventory.setItem(40, separator);
        inventory.setItem(49, separator);

        updateReadyButtons();
    }

    private void setItem(int slot, Material mat, String hexName, String... hexLore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(serialize(hexName));
            List<String> loreList = new ArrayList<>();
            for (String l : hexLore) loreList.add(serialize(l));
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }

    public void updateReadyButtons() {
        String p1Color = p1Ready ? "&#a8ff78<bold>AUTORIZADO</bold>" : "&#ff4b2b<bold>ESPERANDO...</bold>";
        setItem(45, p1Ready ? Material.LIME_DYE : Material.RED_DYE, p1Color,
                "&#434343Fondo total de " + player1.getName() + ":",
                "&#fbd72b🪙 " + p1Coins + " Monedas",
                "&#a8ff78💎 " + p1Gems + " Gemas",
                "&#00fbff💧 " + p1Mana + " Maná");

        String p2Color = p2Ready ? "&#a8ff78<bold>AUTORIZADO</bold>" : "&#ff4b2b<bold>ESPERANDO...</bold>";
        setItem(53, p2Ready ? Material.LIME_DYE : Material.RED_DYE, p2Color,
                "&#434343Fondo total de " + player2.getName() + ":",
                "&#fbd72b🪙 " + p2Coins + " Monedas",
                "&#a8ff78💎 " + p2Gems + " Gemas",
                "&#00fbff💧 " + p2Mana + " Maná");
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
        taskID = Bukkit.getScheduler().scheduleSyncDelayedTask(NexoEconomy.getPlugin(NexoEconomy.class), this::ejecutarIntercambio, 60L);
        player1.sendMessage(NexoColor.parse("&#fbd72b[INFO] La transacción comercial se ejecutará en 3 segundos..."));
        player2.sendMessage(NexoColor.parse("&#fbd72b[INFO] La transacción comercial se ejecutará en 3 segundos..."));
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

        NexoEconomy eco = NexoEconomy.getPlugin(NexoEconomy.class);
        transferCurrency(eco, NexoAccount.Currency.COINS, p1Coins, p2Coins);
        transferCurrency(eco, NexoAccount.Currency.GEMS, p1Gems, p2Gems);
        transferCurrency(eco, NexoAccount.Currency.MANA, p1Mana, p2Mana);

        player1.closeInventory();
        player2.closeInventory();
        player1.sendMessage(NexoColor.parse("&#a8ff78[✓] Intercambio corporativo realizado con éxito."));
        player2.sendMessage(NexoColor.parse("&#a8ff78[✓] Intercambio corporativo realizado con éxito."));
    }

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
            if (i >= 45) esSlotDeOrigen = false;

            if (esSlotDeOrigen) {
                ItemStack item = inventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    if (receptor.getInventory().firstEmpty() == -1) receptor.getWorld().dropItemNaturally(receptor.getLocation(), item);
                    else receptor.getInventory().addItem(item);
                    inventory.setItem(i, null);
                }
            }
        }
    }

    public void unready() { p1Ready = false; p2Ready = false; updateReadyButtons(); cancelarCuenta(); }
    public Inventory getInventory() { return inventory; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }

    public void open() {
        player1.openInventory(inventory);
        player2.openInventory(inventory);
    }
}