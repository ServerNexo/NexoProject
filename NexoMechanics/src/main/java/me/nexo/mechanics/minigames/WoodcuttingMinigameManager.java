package me.nexo.mechanics.minigames;

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

    // 🟢 ARQUITECTURA: Usamos el plugin de Mechanics
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

        // 1. Verificar si está golpeando el Núcleo de Ámbar activo
        if (nucleos.containsKey(id)) {
            NucleoActivo nucleo = nucleos.get(id);
            if (b.getLocation().equals(nucleo.bloque().getLocation())) {
                // ¡Éxito!
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 20);

                // Efecto Treecapitator (Talar el árbol hacia arriba)
                talarArbol(b);

                // Recompensa especial
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.HONEYCOMB, 2)); // Savia Mágica

                // 🟢 ARQUITECTURA LIMPIA: ¡Premio de Energía usando la NexoAPI!
                NexoUser user = NexoAPI.getInstance().getUserLocal(id);
                if (user != null) {
                    int maxEnergia = 100 + ((user.getNexoNivel() - 1) * 20) + user.getEnergiaExtraAccesorios();
                    int nuevaEnergia = Math.min(user.getEnergiaMineria() + 10, maxEnergia);
                    user.setEnergiaMineria(nuevaEnergia);
                    p.sendActionBar("§6§l¡Núcleo de Ámbar destruido! §e+10⚡");
                } else {
                    p.sendActionBar("§6§l¡Núcleo de Ámbar destruido!");
                }

                // Limpieza visual - CORREGIDO PARA 1.21
                p.sendBlockChange(b.getLocation(), Bukkit.createBlockData(Material.AIR));
                nucleos.remove(id);
                return;
            }
        }

        // 2. Probabilidad de activar el minijuego (5%) si golpea un tronco normal
        if (b.getType().toString().contains("LOG") && !nucleos.containsKey(id)) {
            if (Math.random() <= 0.05) {
                activarNucleo(p, b);
            }
        }
    }

    private void activarNucleo(Player p, Block origen) {
        // Busca un bloque arriba o abajo que también sea tronco
        Block objetivo = origen.getRelative(BlockFace.UP);
        if (!objetivo.getType().toString().contains("LOG")) {
            objetivo = origen.getRelative(BlockFace.DOWN);
        }

        if (objetivo.getType().toString().contains("LOG")) {
            // Cambiamos visualmente el bloque a "Madera Resonante" - CORREGIDO PARA 1.21
            p.sendBlockChange(objetivo.getLocation(), Bukkit.createBlockData(Material.CRIMSON_STEM));
            p.playSound(objetivo.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);

            // Efecto visual para llamar la atención
            p.getWorld().spawnParticle(Particle.WAX_ON, objetivo.getLocation().add(0.5, 0.5, 0.5), 15);

            // Tiene 3 segundos para reaccionar
            nucleos.put(p.getUniqueId(), new NucleoActivo(objetivo, System.currentTimeMillis() + 3000L, objetivo.getType()));
        }
    }

    private void talarArbol(Block inicio) {
        // Lógica simple de Treecapitator (rompe hacia arriba hasta que no haya madera)
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
                        // El jugador falló, el bloque vuelve a la normalidad - CORREGIDO PARA 1.21
                        p.sendBlockChange(entry.getValue().bloque().getLocation(), Bukkit.createBlockData(entry.getValue().tipoOriginal()));
                        p.playSound(p.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 0.5f, 1f);
                    }
                    nucleos.remove(entry.getKey());
                }
            }
        }, 10L, 10L);
    }
}