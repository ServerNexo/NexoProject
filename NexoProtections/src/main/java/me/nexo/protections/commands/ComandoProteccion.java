package me.nexo.protections.commands;

import me.nexo.core.utils.NexoColor;
import me.nexo.protections.NexoProtections;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ComandoProteccion implements CommandExecutor {

    private final NexoProtections plugin;

    public ComandoProteccion(NexoProtections plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("nexo.admin")) {
            player.sendMessage(NexoColor.parse("<red>No tienes permiso para generar un Nexo."));
            return true;
        }

        // 🌟 GENERAMOS LA PIEDRA DE PROTECCIÓN
        ItemStack stone = new ItemStack(Material.BEACON);
        ItemMeta meta = stone.getItemMeta();

        // ¡Usamos el nuevo Motor de Color!
        meta.displayName(NexoColor.parse("<gradient:#00FFAA:#0055FF><bold>Nexo de Protección</bold></gradient>"));
        meta.lore(List.of(
                NexoColor.parse("&7Coloca este bloque para reclamar"),
                NexoColor.parse("&7un territorio y habilitar la"),
                NexoColor.parse("&7construcción de Factorías."),
                NexoColor.parse(" "),
                NexoColor.parse("&#00ff00[!] Clic para colocar")
        ));

        stone.setItemMeta(meta);
        player.getInventory().addItem(stone);
        player.sendMessage(NexoColor.parse("&#00ff00¡Has recibido un Nexo de Protección!"));

        return true;
    }
}