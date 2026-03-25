package me.nexo.mechanics.minigames;

import me.nexo.core.utils.NexoColor; // 🌟 IMPORT AÑADIDO PARA LA PALETA CIBERPUNK
import me.nexo.mechanics.NexoMechanics;
import me.nexo.economy.NexoEconomy; // 🌟 IMPORT ECONOMÍA
import me.nexo.economy.core.NexoAccount;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.core.ClaimAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal; // 🌟 IMPORT BIGDECIMAL
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MiningMinigameManager implements Listener {

    private final NexoMechanics plugin;

    // Guarda qué jugador tiene una veta activa, dónde está, y cuándo expira
    private final Map<UUID, VetaActiva> vetasActivas = new ConcurrentHashMap<>();

    private record VetaActiva(Location loc, long expiracion, Material tipoOriginal) {}

    public MiningMinigameManager(NexoMechanics plugin) {
        this.plugin = plugin;
        iniciarLimpiador();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alPicar(BlockBreakEvent event) {
        Player p = event.getPlayer();
        Block b = event.getBlock();
        UUID id = p.getUniqueId();

        // 🌟 INTEGRACIÓN: Prevenir grifeo mágico en bases enemigas
        if (Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) {
            me.nexo.protections.core.ProtectionStone stone = me.nexo.protections.NexoProtections.getClaimManager().getStoneAt(b.getLocation());
            if (stone != null && !stone.hasPermission(id, me.nexo.protections.core.ClaimAction.BREAK)) {
                return; // Cortamos el flujo: No se activan minijuegos en territorio ajeno
            }
        }

        // 1. ¿Está rompiendo su bloque de minijuego activo?
        if (vetasActivas.containsKey(id)) {
            VetaActiva veta = vetasActivas.get(id);
            if (b.getLocation().equals(veta.loc())) {
                event.setCancelled(true); // Cancelamos el rompimiento real

                // Le damos los drops multiplicados x3 basados en el original
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(veta.tipoOriginal(), 3));
                b.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, b.getLocation().add(0.5, 0.5, 0.5), 20);
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 2f);

                // Simulamos que el bloque desapareció - CORREGIDO PARA 1.21
                p.sendBlockChange(b.getLocation(), Bukkit.createBlockData(Material.AIR));
                vetasActivas.remove(id);

                // 🪙 INYECCIÓN DE ECONOMÍA: Recompensar con Monedas
                int monedasRandom = (int) (Math.random() * 40) + 10; // Aleatorio entre 10 y 50
                BigDecimal recompensaMonedas = new BigDecimal(monedasRandom);

                NexoEconomy.getPlugin(NexoEconomy.class).getEconomyManager()
                        .updateBalanceAsync(p.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, recompensaMonedas, true);

                p.sendActionBar(NexoColor.parse("&#FFAA00[✓] Extracción Rentable: +" + monedasRandom + " Créditos 🪙"));

                // Reacción en cadena: Volvemos a invocar otra veta cercana
                generarVetaContigua(p, b);
                return;
            }
        }

        // 2. Si no es un minijuego, verificamos si es piedra/mineral para intentar activarlo (2% Prob)
        if (b.getType().toString().contains("STONE") || b.getType().toString().contains("ORE")) {
            if (!vetasActivas.containsKey(id) && Math.random() <= 0.02) {
                generarVetaContigua(p, b);
            }
        }
    }

    private void generarVetaContigua(Player p, Block origen) {
        // Busca un bloque sólido adyacente
        BlockFace[] caras = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace cara : caras) {
            Block contiguo = origen.getRelative(cara);
            if (contiguo.getType().toString().contains("STONE") || contiguo.getType().toString().contains("ORE")) {

                // ¡Enviamos el paquete visual (Fake Block) al jugador! - CORREGIDO PARA 1.21
                p.sendBlockChange(contiguo.getLocation(), Bukkit.createBlockData(Material.RAW_GOLD_BLOCK));
                p.playSound(contiguo.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.5f);
                p.getWorld().spawnParticle(Particle.WAX_ON, contiguo.getLocation().add(0.5, 0.5, 0.5), 10);

                // Alerta Ciberpunk para el jugador
                p.sendActionBar(NexoColor.parse("&#00E5FF✨ <bold>¡ANOMALÍA GEOLÓGICA!</bold> &#AAAAAAExtrae el mineral brillante rápido."));

                // Registramos el minijuego por 4 segundos
                vetasActivas.put(p.getUniqueId(), new VetaActiva(contiguo.getLocation(), System.currentTimeMillis() + 4000L, contiguo.getType()));
                break;
            }
        }
    }

    private void iniciarLimpiador() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long ahora = System.currentTimeMillis();
            for (Map.Entry<UUID, VetaActiva> entry : vetasActivas.entrySet()) {
                if (ahora > entry.getValue().expiracion()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null) {
                        // Le devolvemos la textura original porque falló - CORREGIDO PARA 1.21
                        p.sendBlockChange(entry.getValue().loc(), Bukkit.createBlockData(entry.getValue().tipoOriginal()));
                        p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1f);
                        p.sendActionBar(NexoColor.parse("&#FF5555[!] La anomalía se ha estabilizado. Oportunidad perdida."));
                    }
                    vetasActivas.remove(entry.getKey());
                }
            }
        }, 10L, 10L);
    }
}