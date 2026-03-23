package me.nexo.factories.commands;

import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.StructureTemplate;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoFactory implements CommandExecutor {

    private final NexoFactories plugin;

    public ComandoFactory(NexoFactories plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
            // 🌟 Creamos una receta de prueba (Una "Forja" de 3 bloques)
            StructureTemplate forjaT1 = new StructureTemplate("FORJA_T1");
            forjaT1.addBlock(0, -1, 0, Material.IRON_BLOCK); // Bloque debajo (Base)
            forjaT1.addBlock(0, 1, 0, Material.FURNACE);     // Bloque arriba (Chimenea)

            // Proyectamos el holograma teniendo como núcleo (Centro) el bloque donde está parado el jugador
            plugin.getBlueprintManager().projectBlueprint(player, player.getLocation().getBlock().getLocation(), forjaT1);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            plugin.getBlueprintManager().clearBlueprint(player);
            player.sendMessage("§cPlano cancelado.");
            return true;
        }

        player.sendMessage("§e/factory test §7- Proyecta un plano de prueba");
        player.sendMessage("§e/factory cancel §7- Borra los hologramas");
        return true;
    }
}