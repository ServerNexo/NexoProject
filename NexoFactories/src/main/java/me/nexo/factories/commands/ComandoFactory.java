package me.nexo.factories.commands;

import me.nexo.core.utils.NexoColor;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.StructureTemplate;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoFactory implements CommandExecutor {

    private final NexoFactories plugin;

    // 🎨 PALETA HEX
    private static final String ERR_NOT_PLAYER = "&#ff4b2b[!] Acceso denegado: El terminal requiere un operario humano.";
    private static final String MSG_CANCEL = "&#ff4b2b[✓] Proyección holográfica cancelada. Plano borrado de la memoria.";
    private static final String MSG_HELP_TEST = "&#fbd72b/factory test &#434343- Proyecta un plano de prueba (Forja T1).";
    private static final String MSG_HELP_CANCEL = "&#fbd72b/factory cancel &#434343- Borra los hologramas activos.";

    public ComandoFactory(NexoFactories plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
            // 🌟 Creamos una receta de prueba (Una "Forja" de 3 bloques)
            StructureTemplate forjaT1 = new StructureTemplate("FORJA_T1");
            forjaT1.addBlock(0, -1, 0, Material.IRON_BLOCK); // Bloque debajo (Base)
            forjaT1.addBlock(0, 1, 0, Material.FURNACE);     // Bloque arriba (Chimenea)

            // Proyectamos el holograma
            plugin.getBlueprintManager().projectBlueprint(player, player.getLocation().getBlock().getLocation(), forjaT1);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            plugin.getBlueprintManager().clearBlueprint(player);
            player.sendMessage(NexoColor.parse(MSG_CANCEL));
            return true;
        }

        player.sendMessage(NexoColor.parse(MSG_HELP_TEST));
        player.sendMessage(NexoColor.parse(MSG_HELP_CANCEL));
        return true;
    }
}