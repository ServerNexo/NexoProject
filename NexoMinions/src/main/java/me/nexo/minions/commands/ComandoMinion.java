package me.nexo.minions.commands;

import com.nexomc.nexo.api.NexoItems;
import me.nexo.core.utils.NexoColor;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ComandoMinion implements CommandExecutor {
    private final NexoMinions plugin;

    public ComandoMinion(NexoMinions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Verificación de permisos
        if (!sender.hasPermission("nexominions.admin")) {
            sender.sendMessage(NexoColor.parse("&#FF3366[!] Herejía: &#E6CCFFNo posees el poder para invocar esclavos del vacío."));
            return true;
        }

        // 🌟 COMANDO RELOAD
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.getTiersConfig().cargarConfig();
            plugin.getUpgradesConfig().cargarConfig();
            sender.sendMessage(NexoColor.parse("&#9933FF[✓] <bold>TEXTOS SAGRADOS RENOVADOS:</bold> &#E6CCFFConfiguraciones de esclavos recargadas con éxito."));
            return true;
        }

        // 2. Verificación de argumentos mínimos
        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(NexoColor.parse("&#CC66FFUso correcto: &#E6CCFF/minion give <Jugador> <Tipo> [Nivel]"));
            return true;
        }

        // 3. Buscar al jugador
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(NexoColor.parse("&#FF3366[!] Error: &#E6CCFFEsa alma no se encuentra en este reino."));
            return true;
        }

        // 4. Leer el tipo de Minion
        MinionType type;
        try {
            type = MinionType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(NexoColor.parse("&#FF3366[!] Identidad de Esclavo inválida. &#E6CCFF(Ej: WHEAT, ORE_DIAMOND)"));
            return true;
        }

        // 5. Leer el nivel (Por defecto 1)
        int tier = 1;
        if (args.length >= 4) {
            try {
                tier = Integer.parseInt(args[3]);
                if (tier < 1 || tier > 12) {
                    sender.sendMessage(NexoColor.parse("&#FF3366[!] El Nivel de Vínculo debe estar entre 1 y 12."));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(NexoColor.parse("&#FF3366[!] El nivel debe ser un número entero válido."));
                return true;
            }
        }

        // 🌟 6. LA MAGIA: Obtener el ítem de Nexo
        var itemFactory = NexoItems.itemFromId(type.getNexoModelID()); // Asegúrate que getNexoModelID() devuelva el ID exacto (Ej: "minion_wheat")
        if (itemFactory == null) {
            sender.sendMessage(NexoColor.parse("&#FF3366[!] Error Arcano: &#E6CCFFEl sello '" + type.getNexoModelID() + "' no existe en los registros de Nexo."));
            return true;
        }

        ItemStack minionItem = itemFactory.build();

        // Verificamos si Nexo nos entregó un ítem fantasma (AIR)
        if (minionItem == null || minionItem.getType().isAir()) {
            sender.sendMessage(NexoColor.parse("&#FF3366[!] Fallo de Invocación: &#E6CCFFEl ensamblador generó materia vacía (AIR)."));
            return true;
        }

        ItemMeta meta = minionItem.getItemMeta();

        // 7. Inyectar Nombre, Lore y datos invisibles (NBT) al ítem
        if (meta != null) {
            meta.displayName(NexoColor.parse("&#9933FF⭐ <bold>" + type.getDisplayName() + "</bold> &#E6CCFF(Nv. " + tier + ")"));

            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(NexoColor.parse("&#E6CCFFUn alma encadenada a este sello,"));
            lore.add(NexoColor.parse("&#E6CCFFlista para servir en tu dominio."));
            lore.add(NexoColor.parse(" "));
            lore.add(NexoColor.parse("&#CC66FF► Clic derecho en el suelo para invocar al Esclavo"));
            meta.lore(lore);

            // ⚠️ VITAL: Usar las llaves oficiales del plugin para que el Listener las reconozca
            meta.getPersistentDataContainer().set(MinionKeys.TYPE, PersistentDataType.STRING, type.name());
            meta.getPersistentDataContainer().set(MinionKeys.TIER, PersistentDataType.INTEGER, tier);

            minionItem.setItemMeta(meta);
        } else {
            sender.sendMessage(NexoColor.parse("&#FF3366[!] Fallo de Escritura NBT: El material base no acepta inyección de datos."));
            return true;
        }

        // 8. Entregar el ítem
        target.getInventory().addItem(minionItem);
        sender.sendMessage(NexoColor.parse("&#9933FF[📦] Invocación Aprobada: &#E6CCFFHas conjurado un " + type.getDisplayName() + " Nivel " + tier + " para " + target.getName() + "."));
        target.sendMessage(NexoColor.parse("&#CC66FF[✓] <bold>PACTO FORJADO:</bold> &#E6CCFFUn nuevo esclavo del vacío ha sido encadenado a tu inventario."));

        return true;
    }
}