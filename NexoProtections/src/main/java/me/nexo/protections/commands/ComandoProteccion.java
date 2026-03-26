package me.nexo.protections.commands;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.utils.NexoColor;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ComandoProteccion implements CommandExecutor {

    private final NexoProtections plugin;

    public ComandoProteccion(NexoProtections plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("nexo.admin")) {
                player.sendMessage(NexoColor.parse("&#FF3366[!] El Vacío rechaza tu petición (Sin Permisos)."));
                return true;
            }
            plugin.reloadSystem();
            player.sendMessage(NexoColor.parse("&#9933FF[✓] <bold>EL ABISMO DESPIERTA:</bold> &#E6CCFFMonolitos y rituales recargados con éxito."));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("trust")) {
            ProtectionStone stone = NexoProtections.getClaimManager().getStoneAt(player.getLocation());
            if (stone == null || !stone.getOwnerId().equals(player.getUniqueId())) {
                player.sendMessage(NexoColor.parse("&#FF3366[!] Herejía: &#E6CCFFDebes estar dentro de tu Monolito para forjar un Pacto de Sangre."));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(NexoColor.parse("&#FF3366[!] Error: &#E6CCFFEsa alma no se encuentra en este reino (Offline)."));
                return true;
            }

            stone.addFriend(target.getUniqueId());
            player.sendMessage(NexoColor.parse("&#CC66FF[✓] <bold>PACTO FORJADO:</bold> &#E6CCFF" + target.getName() + " ahora es un Acólito de tu dominio."));
            target.sendMessage(NexoColor.parse("&#9933FF[⟳] Pacto de Sangre: &#E6CCFFHas sido invocado como Acólito en el dominio de " + player.getName() + "."));
            return true;
        }

        if (!player.hasPermission("nexo.admin")) {
            player.sendMessage(NexoColor.parse("&#FF3366[!] Acceso Denegado."));
            return true;
        }

        ItemStack stone = new ItemStack(Material.LODESTONE);
        ItemMeta meta = stone.getItemMeta();
        if (meta != null) {
            meta.displayName(NexoColor.parse("&#9933FF<bold>SELLO DEL ABISMO</bold>"));
            meta.lore(List.of(
                    NexoColor.parse("&#E6CCFFColoca este altar antiguo para reclamar"),
                    NexoColor.parse("&#E6CCFFun fragmento del mundo y sellarlo"),
                    NexoColor.parse("&#E6CCFFcon el poder del Vacío."),
                    NexoColor.parse(" "),
                    NexoColor.parse("&#CC66FF► Clic derecho para invocar el dominio")
            ));
            NamespacedKey key = new NamespacedKey(plugin, "is_protection_stone");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            stone.setItemMeta(meta);
        }

        player.getInventory().addItem(stone);
        player.sendMessage(NexoColor.parse("&#9933FF[✓] <bold>RITUAL CONCEDIDO:</bold> &#E6CCFFSello del Abismo entregado a tu inventario."));
        return true;
    }
}