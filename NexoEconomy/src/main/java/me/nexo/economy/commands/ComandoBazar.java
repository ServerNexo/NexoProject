package me.nexo.economy.commands;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.bazar.BazaarMenu;
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
            new BazaarMenu(player, plugin).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("help")) {
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.ayuda.divisor"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.ayuda.titulo"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.ayuda.vender"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.ayuda.comprar"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.ayuda.reclamar"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.ayuda.divisor"));
            return true;
        }

        if (subCommand.equals("claim")) {
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.reclamar-inicio"));
            plugin.getBazaarManager().reclamarBuzon(player);
            return true;
        }

        if (subCommand.equals("sell")) {
            if (args.length < 2) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.errores.uso-vender"));
                return true;
            }
            ItemStack itemHand = player.getInventory().getItemInMainHand();
            if (itemHand == null || itemHand.getType() == Material.AIR) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.errores.sin-item"));
                return true;
            }
            try {
                BigDecimal precioUnidad = new BigDecimal(args[1]);
                if (precioUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.errores.precio-cero"));
                    return true;
                }
                plugin.getBazaarManager().crearOrdenVenta(player, itemHand.getType().name(), itemHand.getAmount(), precioUnidad);
            } catch (Exception e) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.errores.precio-invalido"));
            }
            return true;
        }

        if (subCommand.equals("buy")) {
            if (args.length < 4) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.errores.uso-comprar"));
                return true;
            }
            try {
                String itemId = args[1].toUpperCase();
                int cantidad = Integer.parseInt(args[2]);
                BigDecimal precioUnidad = new BigDecimal(args[3]);
                if (cantidad <= 0 || precioUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.errores.cantidad-cero"));
                    return true;
                }
                plugin.getBazaarManager().crearOrdenCompra(player, itemId, cantidad, precioUnidad);
            } catch (Exception e) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.bazar.errores.datos-invalidos"));
            }
            return true;
        }

        return true;
    }
}