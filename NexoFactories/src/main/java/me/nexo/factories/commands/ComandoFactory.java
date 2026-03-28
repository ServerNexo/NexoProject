package me.nexo.factories.commands;

import me.nexo.core.crossplay.CrossplayUtils;
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
        if (!(sender instanceof Player player)) {
            CrossplayUtils.sendMessage(null, plugin.getConfigManager().getMessage("comandos.factory.no-jugador"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
            StructureTemplate forjaT1 = new StructureTemplate("FORJA_T1");
            forjaT1.addBlock(0, -1, 0, Material.IRON_BLOCK);
            forjaT1.addBlock(0, 1, 0, Material.FURNACE);
            plugin.getBlueprintManager().projectBlueprint(player, player.getLocation().getBlock().getLocation(), forjaT1);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            plugin.getBlueprintManager().clearBlueprint(player);
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.factory.cancelar"));
            return true;
        }

        CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.factory.ayuda-test"));
        CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.factory.ayuda-cancelar"));
        return true;
    }
}