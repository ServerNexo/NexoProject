package me.nexo.mechanics.minigames;

import me.nexo.core.utils.NexoColor;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WoodcuttingMinigameManager implements Listener {

    private final NexoMechanics plugin;
    private final Map<UUID, NucleoActivo> nucleos = new ConcurrentHashMap<>();

    private record NucleoActivo(Block bloque, long expiracion, Material tipoOriginal) {}

    public WoodcuttingMinigameManager(NexoMechanics plugin) {
        this.plugin = plugin;
        iniciarLimpiador();
    }

    @EventHandler
    public void alGolpearMadera(BlockDamageEvent event) {
        Player p = event.getPlayer();
        Block b = event.getBlock();
        UUID id = p.getUniqueId();

        if (Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) {
            me.nexo.protections.core.ProtectionStone stone = me.nexo.protections.NexoProtections.getClaimManager().getStoneAt(b.getLocation());
            if (stone != null && !stone.hasPermission(id, me.nexo.protections.core.ClaimAction.BREAK)) {
                return;
            }
        }

        if (nucleos.containsKey(id)) {
            NucleoActivo nucleo = nucleos.get(id);
            if (b.getLocation().equals(nucleo.bloque().getLocation())) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 20);

                talarArbol(b);

                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.HONEYCOMB, 2));

                NexoUser user = NexoAPI.getInstance().getUserLocal(id);
                if (user != null) {
                    int maxEnergia = 100 + ((user.getNexoNivel() - 1) * 20) + user.getEnergiaExtraAccesorios();
                    int nuevaEnergia = Math.min(user.getEnergiaMineria() + 10, maxEnergia);
                    user.setEnergiaMineria(nuevaEnergia);
                    p.sendActionBar(NexoColor.parse("&#ff00ff[✓] <bold>NÚCLEO ORGÁNICO DESTRUIDO:</bold> &#1c0f2aRecarga del traje &#00f5ff(+10⚡)"));
                } else {
                    p.sendActionBar(NexoColor.parse("&#ff00ff[✓] <bold>NÚCLEO ORGÁNICO DESTRUIDO</bold>"));
                }

                p.sendBlockChange(b.getLocation(), Bukkit.createBlockData(Material.AIR));
                nucleos.remove(id);
                return;
            }
        }

        if (b.getType().toString().contains("LOG") && !nucleos.containsKey(id)) {
            if (Math.random() <= 0.05) {
                activarNucleo(p, b);
            }
        }
    }

    private void activarNucleo(Player p, Block origen) {
        Block objetivo = origen.getRelative(BlockFace.UP);
        if (!objetivo.getType().toString().contains("LOG")) {
            objetivo = origen.getRelative(BlockFace.DOWN);
        }

        if (objetivo.getType().toString().contains("LOG")) {
            p.sendBlockChange(objetivo.getLocation(), Bukkit.createBlockData(Material.CRIMSON_STEM));
            p.playSound(objetivo.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            p.getWorld().spawnParticle(Particle.WAX_ON, objetivo.getLocation().add(0.5, 0.5, 0.5), 15);
            p.sendActionBar(NexoColor.parse("&#8b0000✨ <bold>¡ANOMALÍA BOTÁNICA!</bold> &#1c0f2aGolpea el núcleo inestable rápido."));
            nucleos.put(p.getUniqueId(), new NucleoActivo(objetivo, System.currentTimeMillis() + 3000L, objetivo.getType()));
        }
    }

    private void talarArbol(Block inicio) {
        Block actual = inicio;
        while (actual.getType().toString().contains("LOG") || actual.getType().toString().contains("LEAVES")) {
            actual.breakNaturally();
            actual = actual.getRelative(BlockFace.UP);
        }
    }

    private void iniciarLimpiador() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long ahora = System.currentTimeMillis();
            for (Map.Entry<UUID, NucleoActivo> entry : nucleos.entrySet()) {
                if (ahora > entry.getValue().expiracion()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null) {
                        p.sendBlockChange(entry.getValue().bloque().getLocation(), Bukkit.createBlockData(entry.getValue().tipoOriginal()));
                        p.playSound(p.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 0.5f, 1f);
                        p.sendActionBar(NexoColor.parse("&#1c0f2a[!] La biomasa se ha endurecido. Oportunidad perdida."));
                    }
                    nucleos.remove(entry.getKey());
                }
            }
        }, 10L, 10L);
    }
}