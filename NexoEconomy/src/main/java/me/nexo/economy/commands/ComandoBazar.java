package me.nexo.economy.commands;

import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

public class ComandoBazar implements CommandExecutor {

    private final NexoEconomy plugin;

    // 🎨 PALETA HEX
    private static final String BC_DIVIDER = "&#434343========================================";
    private static final String MSG_TITLE = "&#fbd72b<bold>📈 BAZAR GLOBAL DE NEXO</bold>";
    private static final String MSG_HELP_SELL = "&#fbd72b/bazar sell <precio_c/u> &#434343- Vende el ítem de tu mano.";
    private static final String MSG_HELP_BUY = "&#fbd72b/bazar buy <item> <cantidad> <precio_c/u> &#434343- Crea orden de compra.";
    private static final String MSG_HELP_CLAIM = "&#fbd72b/bazar claim &#434343- Recoge tus extracciones pendientes.";

    private static final String MSG_CLAIM_START = "&#00fbff[NEXO] Conectando con tu buzón en Wall Street...";
    private static final String ERR_USAGE_SELL = "&#ff4b2b[!] Error de sintaxis. Uso: &#fbd72b/bazar sell <precio_c/u>";
    private static final String ERR_NO_ITEM = "&#ff4b2b[!] Manos vacías. Debes sostener un activo para venderlo.";
    private static final String ERR_PRICE_ZERO = "&#ff4b2b[!] El valor de cotización debe ser mayor a 0.";
    private static final String ERR_INVALID_PRICE = "&#ff4b2b[!] Formato de precio inválido.";
    private static final String ERR_USAGE_BUY = "&#ff4b2b[!] Uso: &#fbd72b/bazar buy <item_id> <cantidad> <precio_c/u>";
    private static final String ERR_AMOUNT_ZERO = "&#ff4b2b[!] La cantidad y el precio de cotización deben ser mayores a 0.";
    private static final String ERR_INVALID_DATA = "&#ff4b2b[!] Datos financieros inválidos. Verifica los parámetros.";

    public ComandoBazar(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            player.sendMessage(NexoColor.parse(BC_DIVIDER));
            player.sendMessage(NexoColor.parse(MSG_TITLE));
            player.sendMessage(NexoColor.parse(MSG_HELP_SELL));
            player.sendMessage(NexoColor.parse(MSG_HELP_BUY));
            player.sendMessage(NexoColor.parse(MSG_HELP_CLAIM));
            player.sendMessage(NexoColor.parse(BC_DIVIDER));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 📦 /bazar claim
        if (subCommand.equals("claim")) {
            player.sendMessage(NexoColor.parse(MSG_CLAIM_START));
            plugin.getBazaarManager().reclamarBuzon(player);
            return true;
        }

        // 📉 /bazar sell <precio_unidad>
        if (subCommand.equals("sell")) {
            if (args.length < 2) {
                player.sendMessage(NexoColor.parse(ERR_USAGE_SELL));
                return true;
            }

            ItemStack itemHand = player.getInventory().getItemInMainHand();
            if (itemHand == null || itemHand.getType() == Material.AIR) {
                player.sendMessage(NexoColor.parse(ERR_NO_ITEM));
                return true;
            }

            try {
                BigDecimal precioUnidad = new BigDecimal(args[1]);
                if (precioUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    player.sendMessage(NexoColor.parse(ERR_PRICE_ZERO));
                    return true;
                }
                plugin.getBazaarManager().crearOrdenVenta(player, itemHand.getType().name(), itemHand.getAmount(), precioUnidad);
            } catch (Exception e) {
                player.sendMessage(NexoColor.parse(ERR_INVALID_PRICE));
            }
            return true;
        }

        // 📈 /bazar buy <item> <cantidad> <precio_unidad>
        if (subCommand.equals("buy")) {
            if (args.length < 4) {
                player.sendMessage(NexoColor.parse(ERR_USAGE_BUY));
                return true;
            }

            try {
                String itemId = args[1].toUpperCase();
                int cantidad = Integer.parseInt(args[2]);
                BigDecimal precioUnidad = new BigDecimal(args[3]);

                if (cantidad <= 0 || precioUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    player.sendMessage(NexoColor.parse(ERR_AMOUNT_ZERO));
                    return true;
                }
                plugin.getBazaarManager().crearOrdenCompra(player, itemId, cantidad, precioUnidad);
            } catch (Exception e) {
                player.sendMessage(NexoColor.parse(ERR_INVALID_DATA));
            }
            return true;
        }

        return true;
    }
}