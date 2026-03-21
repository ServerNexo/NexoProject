package me.nexo.economy.commands;

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

    public ComandoBazar(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            player.sendMessage("§8========================================");
            player.sendMessage("§6§l📈 BAZAR GLOBAL DE NEXO");
            player.sendMessage("§e/bazar sell <precio_c/u> §7- Vende el ítem de tu mano.");
            player.sendMessage("§e/bazar buy <item> <cantidad> <precio_c/u> §7- Crea orden de compra.");
            player.sendMessage("§e/bazar claim §7- Recoge tus compras completadas.");
            player.sendMessage("§8========================================");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 📦 /bazar claim
        if (subCommand.equals("claim")) {
            player.sendMessage("§eRevisando tu buzón de Wall Street...");
            plugin.getBazaarManager().reclamarBuzon(player);
            return true;
        }

        // 📉 /bazar sell <precio_unidad>
        if (subCommand.equals("sell")) {
            if (args.length < 2) {
                player.sendMessage("§cUso: /bazar sell <precio_por_unidad>");
                return true;
            }

            ItemStack itemHand = player.getInventory().getItemInMainHand();
            if (itemHand == null || itemHand.getType() == Material.AIR) {
                player.sendMessage("§cDebes tener un ítem en la mano para venderlo.");
                return true;
            }

            try {
                BigDecimal precioUnidad = new BigDecimal(args[1]);
                if (precioUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    player.sendMessage("§cEl precio debe ser mayor a 0.");
                    return true;
                }

                plugin.getBazaarManager().crearOrdenVenta(player, itemHand.getType().name(), itemHand.getAmount(), precioUnidad);
            } catch (Exception e) {
                player.sendMessage("§cPrecio inválido.");
            }
            return true;
        }

        // 📈 /bazar buy <item> <cantidad> <precio_unidad>
        if (subCommand.equals("buy")) {
            if (args.length < 4) {
                player.sendMessage("§cUso: /bazar buy <item_id> <cantidad> <precio_por_unidad>");
                return true;
            }

            try {
                String itemId = args[1].toUpperCase();
                int cantidad = Integer.parseInt(args[2]);
                BigDecimal precioUnidad = new BigDecimal(args[3]);

                if (cantidad <= 0 || precioUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    player.sendMessage("§cLa cantidad y el precio deben ser mayores a 0.");
                    return true;
                }

                plugin.getBazaarManager().crearOrdenCompra(player, itemId, cantidad, precioUnidad);
            } catch (Exception e) {
                player.sendMessage("§cDatos inválidos. Verifica la cantidad y el precio.");
            }
            return true;
        }

        return true;
    }
}