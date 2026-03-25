package me.nexo.minions.commands;

import com.nexomc.nexo.api.NexoItems;
import me.nexo.core.utils.NexoColor; // 🌟 IMPORT AÑADIDO PARA LA PALETA CIBERPUNK
import me.nexo.minions.data.MinionType;
import org.bukkit.Bukkit;
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
            sender.sendMessage(NexoColor.parse("&#FF5555[!] Acceso Denegado: Autorización requerida para despachar operarios."));
            return true;
        }

        // 2. Verificación de argumentos mínimos
        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(NexoColor.parse("&#FFAA00[!] Sintaxis de Red: /minion give <jugador> <tipo> [nivel]"));
            sender.sendMessage(NexoColor.parse("&#AAAAAATipos de unidad base: COBBLESTONE, WHEAT, OAK_WOOD, etc."));
            return true;
        }

        // 3. Buscar al jugador
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(NexoColor.parse("&#FF5555[!] Error de Enlace: El jugador destino no se encuentra en la red."));
            return true;
        }

        // 4. Leer el tipo de Minion
        MinionType type;
        try {
            type = MinionType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(NexoColor.parse("&#FF5555[!] Clase de operario inválida o no registrada."));
            return true;
        }

        // 5. Leer el nivel (Por defecto 1)
        int tier = 1;
        if (args.length >= 4) {
            try {
                tier = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(NexoColor.parse("&#FF5555[!] Error de Formato: El nivel de firmware debe ser numérico (Ej: 1, 2, 3)."));
                return true;
            }
        }

        // 🌟 6. LA MAGIA: Obtener el ítem de Nexo
        var itemBuilder = NexoItems.itemFromId(type.getNexoModelID());
        if (itemBuilder == null) {
            sender.sendMessage(NexoColor.parse("&#FF5555[!] Error Crítico: ID de modelo '" + type.getNexoModelID() + "' no detectada en la base de datos Nexo."));
            return true;
        }

        ItemStack minionItem = itemBuilder.build();

        // Verificamos si Nexo nos entregó un ítem fantasma (AIR)
        if (minionItem == null || minionItem.getType().isAir()) {
            sender.sendMessage(NexoColor.parse("&#FF5555[!] Error Estructural: El ensamblador Nexo generó una entidad vacía (AIR). Revisa tu configuración."));
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
            sender.sendMessage(NexoColor.parse("&#FF5555[!] Fallo de Escritura NBT: El material base no acepta inyección de datos."));
            return true;
        }

        // 8. Entregar el ítem
        target.getInventory().addItem(minionItem);
        sender.sendMessage(NexoColor.parse("&#55FF55[✓] Suministro Autorizado: &#AAAAAA" + type.getDisplayName() + " (Nv. " + tier + ") despachado a " + target.getName() + "."));
        target.sendMessage(NexoColor.parse("&#00E5FF[📦] PAQUETE RECIBIDO: &#AAAAAAUnidad " + type.getDisplayName() + " (Firmware Nv. " + tier + ") lista para su despliegue en el terreno."));

        return true;
    }
}