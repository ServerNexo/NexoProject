package me.nexo.protections.commands;

import me.nexo.core.utils.NexoColor;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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

        // 🌟 COMANDO: /nexo reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("nexo.admin")) {
                player.sendMessage(NexoColor.parse("&#FF3366[!] El Vacío rechaza tu petición (Sin Permisos)."));
                return true;
            }
            plugin.reloadSystem();
            player.sendMessage(NexoColor.parse("&#9933FF[✓] <bold>EL ABISMO DESPIERTA:</bold> &#E6CCFFMonolitos y rituales recargados con éxito."));
            return true;
        }

        // 🌟 COMANDO NUEVO: /nexo ver (Revelar Fronteras)
        if (args.length == 1 && args[0].equalsIgnoreCase("ver")) {
            ProtectionStone stone = NexoProtections.getClaimManager().getStoneAt(player.getLocation());
            if (stone == null) {
                player.sendMessage(NexoColor.parse("&#FF3366[!] Error: &#E6CCFFNo te encuentras dentro de las fronteras de ningún Monolito."));
                return true;
            }

            me.nexo.protections.core.ClaimBox box = stone.getBox();
            World world = Bukkit.getWorld(box.world());
            if (world != null) {
                double y = player.getLocation().getY() + 1.0; // Las partículas se dibujan a la altura del pecho del jugador

                // Dibujamos los bordes en el Eje X
                for (int x = box.minX(); x <= box.maxX(); x += 2) { // De 2 en 2 para no saturar los FPS
                    world.spawnParticle(Particle.DRAGON_BREATH, x + 0.5, y, box.minZ() + 0.5, 5, 0, 0.2, 0, 0.02);
                    world.spawnParticle(Particle.DRAGON_BREATH, x + 0.5, y, box.maxZ() + 0.5, 5, 0, 0.2, 0, 0.02);
                }
                // Dibujamos los bordes en el Eje Z
                for (int z = box.minZ(); z <= box.maxZ(); z += 2) {
                    world.spawnParticle(Particle.DRAGON_BREATH, box.minX() + 0.5, y, z + 0.5, 5, 0, 0.2, 0, 0.02);
                    world.spawnParticle(Particle.DRAGON_BREATH, box.maxX() + 0.5, y, z + 0.5, 5, 0, 0.2, 0, 0.02);
                }
            }

            player.sendMessage(NexoColor.parse("&#CC66FF[✓] <bold>VISIÓN DEL VACÍO:</bold> &#E6CCFFLas fronteras de este dominio han sido reveladas a tus ojos."));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
            return true;
        }

        // 🌟 COMANDO: /nexo trust <jugador>
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
            NexoProtections.getClaimManager().saveStoneDataAsync(stone);

            player.sendMessage(NexoColor.parse("&#CC66FF[✓] <bold>PACTO FORJADO:</bold> &#E6CCFF" + target.getName() + " ahora es un Acólito de tu dominio."));
            target.sendMessage(NexoColor.parse("&#9933FF[⟳] Pacto de Sangre: &#E6CCFFHas sido invocado como Acólito en el dominio de " + player.getName() + "."));
            return true;
        }

        // 🌟 COMANDO: /nexo (Dar el Monolito)
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