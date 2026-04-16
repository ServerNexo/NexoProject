package me.nexo.economy.trade;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 💰 NexoEconomy - Sesión de Intercambio (Arquitectura Enterprise)
 * Nota: No usa Inyección porque se instancia por cada trade activo.
 */
public class TradeSession implements InventoryHolder {

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

        // 🌟 FIX: Título seguro serializado y asignando ESTA CLASE como el InventoryHolder (Inhackeable)
        net.kyori.adventure.text.Component titleComp = CrossplayUtils.parseCrossplay(player1, "&#00f5ff🤝 <bold>INTERCAMBIO SEGURO</bold>");
        this.inventory = Bukkit.createInventory(this, 54, titleComp);

        setupGUI();
    }

    @Override
    public Inventory getInventory() {
        return inventory; // Retorna el inventario para el Holder
    }

    private void setupGUI() {
        setItem(13, Material.GOLD_INGOT, "&#FFAA00[+] <bold>AÑADIR MONEDAS</bold>",
                "&#E6CCFFClic para transferir &#FFAA00+1,000 Monedas&#E6CCFF.");

        setItem(22, Material.EMERALD, "&#55FF55[+] <bold>AÑADIR GEMAS</bold>",
                "&#E6CCFFClic para transferir &#55FF55+100 Gemas&#E6CCFF.");

        setItem(31, Material.AMETHYST_SHARD, "&#ff00ff[+] <bold>AÑADIR MANÁ</bold>",
                "&#E6CCFFClic para transferir &#ff00ff+10 de Maná&#E6CCFF.");

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
            List<net.kyori.adventure.text.Component> loreList = new ArrayList<>();
            for (String l : lore) {
                // 🌟 FIX: Usamos directamente parseCrossplay para no romper colores
                loreList.add(CrossplayUtils.parseCrossplay(player1, l));
            }
            meta.lore(loreList);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    public void updateReadyButtons() {
        // --- Jugador 1 ---
        String p1Color = p1Ready ? "&#55FF55[✓] <bold>AUTORIZADO</bold>" : "&#FF5555[X] <bold>ESPERANDO</bold>";
        List<String> p1LoreRaw = List.of(
                "&#E6CCFFSocio: &#00f5ff" + player1.getName(),
                "",
                "&#E6CCFFOfreciendo:",
                "&#FFAA00" + p1Coins.toString() + " Monedas",
                "&#55FF55" + p1Gems.toString() + " Gemas",
                "&#ff00ff" + p1Mana.toString() + " Maná",
                "",
                "&#E6CCFFClic para " + (p1Ready ? "cancelar." : "aceptar trato.")
        );
        setItem(45, p1Ready ? Material.LIME_DYE : Material.RED_DYE, p1Color, p1LoreRaw.toArray(new String[0]));

        // --- Jugador 2 ---
        String p2Color = p2Ready ? "&#55FF55[✓] <bold>AUTORIZADO</bold>" : "&#FF5555[X] <bold>ESPERANDO</bold>";
        List<String> p2LoreRaw = List.of(
                "&#E6CCFFSocio: &#00f5ff" + player2.getName(),
                "",
                "&#E6CCFFOfreciendo:",
                "&#FFAA00" + p2Coins.toString() + " Monedas",
                "&#55FF55" + p2Gems.toString() + " Gemas",
                "&#ff00ff" + p2Mana.toString() + " Maná",
                "",
                "&#E6CCFFClic para " + (p2Ready ? "cancelar." : "aceptar trato.")
        );
        setItem(53, p2Ready ? Material.LIME_DYE : Material.RED_DYE, p2Color, p2LoreRaw.toArray(new String[0]));
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
        unready(); // Al cambiar los fondos, forzamos a que tengan que aceptar de nuevo
    }

    public void toggleReady(Player player) {
        if (player.equals(player1)) p1Ready = !p1Ready;
        else p2Ready = !p2Ready;

        updateReadyButtons();

        if (p1Ready && p2Ready) iniciarCuentaRegresiva();
        else cancelarCuenta();
    }

    private void iniciarCuentaRegresiva() {
        taskID = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::ejecutarIntercambio, 60L); // 3 Segundos

        // 🌟 FIX: Mensajes limpios y directos
        CrossplayUtils.sendMessage(player1, "&#00f5ff[!] <bold>Ambas partes han aceptado. Intercambio en 3 segundos...</bold>");
        CrossplayUtils.sendMessage(player2, "&#00f5ff[!] <bold>Ambas partes han aceptado. Intercambio en 3 segundos...</bold>");
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

        CrossplayUtils.sendMessage(player1, "&#55FF55[✓] <bold>Intercambio finalizado con éxito.</bold>");
        CrossplayUtils.sendMessage(player2, "&#55FF55[✓] <bold>Intercambio finalizado con éxito.</bold>");
    }

    private void transferCurrency(NexoAccount.Currency currency, BigDecimal amountP1, BigDecimal amountP2) {
        // Enviar fondos de P1 a P2
        if (amountP1.compareTo(BigDecimal.ZERO) > 0) {
            plugin.getEconomyManager().updateBalanceAsync(player1.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amountP1, false);
            plugin.getEconomyManager().updateBalanceAsync(player2.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amountP1, true);
        }
        // Enviar fondos de P2 a P1
        if (amountP2.compareTo(BigDecimal.ZERO) > 0) {
            plugin.getEconomyManager().updateBalanceAsync(player2.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amountP2, false);
            plugin.getEconomyManager().updateBalanceAsync(player1.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amountP2, true);
        }
    }

    private void transferirLado(boolean deP1aP2) {
        Player receptor = deP1aP2 ? player2 : player1;
        for (int i = 0; i < 54; i++) {
            boolean esSlotDeOrigen = deP1aP2 ? (i % 9 < 4) : (i % 9 > 4);
            if (i >= 45) esSlotDeOrigen = false; // Ignorar botones

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

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }

    public void open() {
        player1.openInventory(inventory);
        player2.openInventory(inventory);
    }
}