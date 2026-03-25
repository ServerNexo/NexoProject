package me.nexo.protections.commands;

import me.nexo.core.utils.NexoColor;
import me.nexo.protections.NexoProtections;
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

        if (!player.hasPermission("nexo.admin")) {
            player.sendMessage(NexoColor.parse("&#FF5555[!] Acceso Denegado: &#AAAAAACredenciales insuficientes para solicitar hardware de nivel administrador."));
            return true;
        }

        ItemStack stone = new ItemStack(Material.BEACON);
        ItemMeta meta = stone.getItemMeta();

        if (meta != null) {
            meta.displayName(NexoColor.parse("&#00E5FF<bold>NEXO DE PROTECCIÓN TÁCTICA</bold>"));
            meta.lore(List.of(
                    NexoColor.parse("&#AAAAAADespliega este hardware para asegurar"),
                    NexoColor.parse("&#AAAAAAun perímetro corporativo y autorizar"),
                    NexoColor.parse("&#AAAAAAla construcción de instalaciones seguras."),
                    NexoColor.parse(" "),
                    NexoColor.parse("&#55FF55► Clic derecho para inicializar despliegue")
            ));

            // 🌟 CHIP DE IDENTIFICACIÓN: Marca este faro matemáticamente como un Escudo Nexo
            NamespacedKey key = new NamespacedKey(plugin, "is_protection_stone");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            stone.setItemMeta(meta);
        }

        player.getInventory().addItem(stone);
        player.sendMessage(NexoColor.parse("&#55FF55[✓] <bold>SUMINISTRO AUTORIZADO:</bold> &#AAAAAANexo de Protección Táctica transferido a tu inventario."));

        return true;
    }
}