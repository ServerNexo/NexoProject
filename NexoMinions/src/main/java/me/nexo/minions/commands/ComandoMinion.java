package me.nexo.minions.commands;

import com.nexomc.nexo.api.NexoItems;
import me.nexo.minions.data.MinionType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ComandoMinion implements CommandExecutor {
    private final JavaPlugin plugin;

    public ComandoMinion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Verificación de permisos
        if (!sender.hasPermission("nexominions.admin")) {
            sender.sendMessage(ChatColor.RED + "¡No tienes permiso para invocar abejas!");
            return true;
        }

        // 2. Verificación de argumentos mínimos
        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.RED + "Uso correcto: /minion give <jugador> <tipo> [nivel]");
            sender.sendMessage(ChatColor.GRAY + "Tipos disponibles: COBBLESTONE, WHEAT, OAK_WOOD");
            return true;
        }

        // 3. Buscar al jugador
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado o desconectado.");
            return true;
        }

        // 4. Leer el tipo de Minion
        MinionType type;
        try {
            type = MinionType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Tipo inválido. Usa: COBBLESTONE, WHEAT, u OAK_WOOD");
            return true;
        }

        // 5. Leer el nivel (Por defecto 1)
        int tier = 1;
        if (args.length >= 4) {
            try {
                tier = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "El nivel debe ser un número entero (Ej: 1, 2, 3).");
                return true;
            }
        }

        // 🌟 6. LA MAGIA: Obtener el ítem de Nexo
        var itemBuilder = NexoItems.itemFromId(type.getNexoModelID());
        if (itemBuilder == null) {
            sender.sendMessage(ChatColor.DARK_RED + "Error: No existe la ID '" + type.getNexoModelID() + "' en Nexo.");
            return true;
        }

        ItemStack minionItem = itemBuilder.build();

        // Verificamos si Nexo nos entregó un ítem fantasma (AIR)
        if (minionItem == null || minionItem.getType().isAir()) {
            sender.sendMessage(ChatColor.DARK_RED + "Error: Nexo encontró la ID, pero el material está mal configurado en tu minions.yml (Generó AIR).");
            return true;
        }

        ItemMeta meta = minionItem.getItemMeta();

        // 7. Inyectar datos invisibles (NBT) al ítem
        if (meta != null) {
            NamespacedKey keyType = new NamespacedKey(plugin, "minion_type");
            NamespacedKey keyTier = new NamespacedKey(plugin, "minion_tier");

            meta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, type.name());
            meta.getPersistentDataContainer().set(keyTier, PersistentDataType.INTEGER, tier);

            minionItem.setItemMeta(meta);
        } else {
            sender.sendMessage(ChatColor.RED + "Error: El ítem de Nexo no acepta datos NBT. Revisa su material.");
            return true;
        }

        // 8. Entregar el ítem
        target.getInventory().addItem(minionItem);
        sender.sendMessage(ChatColor.GREEN + "Le diste un " + type.getDisplayName() + " (Nivel " + tier + ") a " + target.getName() + ".");
        target.sendMessage(ChatColor.AQUA + "¡Recibiste un " + type.getDisplayName() + " (Nivel " + tier + ")! Colócalo en el suelo para activarlo.");

        return true;
    }
}