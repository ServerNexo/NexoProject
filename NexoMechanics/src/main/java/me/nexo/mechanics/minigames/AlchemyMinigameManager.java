package me.nexo.mechanics.minigames;

import me.nexo.mechanics.NexoMechanics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AlchemyMinigameManager implements Listener {

    // 🟢 ARQUITECTURA: Ahora usamos el plugin de Mechanics en lugar del Core
    private final NexoMechanics plugin;

    // Guardamos el soporte inestable: Ubicación -> Datos de Estabilización
    private final Map<Location, MezclaVolatil> mezclas = new ConcurrentHashMap<>();

    private static class MezclaVolatil {
        int bombeos = 0;
        int tiempoRestante = 5; // 5 segundos
        ItemStack[] pocionesOriginales;

        public MezclaVolatil(ItemStack[] originales) {
            this.pocionesOriginales = originales;
        }
    }

    public AlchemyMinigameManager(NexoMechanics plugin) {
        this.plugin = plugin;
        iniciarReloj();
    }

    @EventHandler
    public void alDestilar(BrewEvent event) {
        Block b = event.getBlock();

        // 10% de probabilidad de volverse inestable
        if (Math.random() <= 0.10) {
            if (!(b.getState() instanceof BrewingStand stand)) return;

            // Guardamos las pociones que se acaban de crear
            ItemStack[] resultados = new ItemStack[3];
            for (int i = 0; i < 3; i++) {
                ItemStack item = stand.getInventory().getItem(i);
                if (item != null) resultados[i] = item.clone();
            }

            mezclas.put(b.getLocation(), new MezclaVolatil(resultados));

            // Efectos de peligro
            b.getWorld().playSound(b.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.5f);

            // Avisamos a los jugadores cercanos
            for (Player p : b.getWorld().getPlayers()) {
                if (p.getLocation().distance(b.getLocation()) <= 5) {
                    p.sendTitle("§8§l[ §c§l! §8§l]", "§7¡La mezcla es inestable! (Shift x3)", 5, 40, 5);
                }
            }
        }
    }

    @EventHandler
    public void alAgacharse(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return; // Solo cuenta cuando presiona el shift, no al soltarlo

        Player p = event.getPlayer();
        Location pLoc = p.getLocation();

        // Buscar si hay un soporte inestable a menos de 3 bloques
        for (Map.Entry<Location, MezclaVolatil> entry : mezclas.entrySet()) {
            Location locSoporte = entry.getKey();
            if (locSoporte.getWorld().equals(pLoc.getWorld()) && locSoporte.distance(pLoc) <= 3) {

                MezclaVolatil mezcla = entry.getValue();
                mezcla.bombeos++;

                // Efecto de "bombeo"
                p.playSound(locSoporte, Sound.ITEM_BUCKET_FILL, 0.5f, 1f + (mezcla.bombeos * 0.2f));
                locSoporte.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, locSoporte.add(0.5, 1, 0.5), 5);

                if (mezcla.bombeos >= 3) {
                    // ¡Éxito!
                    p.playSound(locSoporte, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                    p.sendMessage("§a🧪 ¡Mezcla estabilizada! Las pociones son ahora más puras.");

                    aplicarPremio(locSoporte, mezcla.pocionesOriginales);
                    mezclas.remove(locSoporte);
                }
                break;
            }
        }
    }

    private void aplicarPremio(Location loc, ItemStack[] originales) {
        if (loc.getBlock().getState() instanceof BrewingStand stand) {
            for (int i = 0; i < 3; i++) {
                ItemStack item = originales[i];
                if (item != null && item.getType() == Material.POTION && item.hasItemMeta()) {
                    PotionMeta meta = (PotionMeta) item.getItemMeta();
                    if (meta.hasCustomEffects()) {
                        // Duplicar duración de los efectos custom
                        for (PotionEffect effect : meta.getCustomEffects()) {
                            meta.removeCustomEffect(effect.getType());
                            meta.addCustomEffect(new PotionEffect(effect.getType(), effect.getDuration() * 2, effect.getAmplifier()), true);
                        }
                    }
                    item.setItemMeta(meta);
                    stand.getInventory().setItem(i, item);
                }
            }
        }
    }

    private void aplicarCastigo(Location loc) {
        if (loc.getBlock().getState() instanceof BrewingStand stand) {
            for (int i = 0; i < 3; i++) {
                ItemStack item = stand.getInventory().getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    // Convierte en botella de agua básica
                    stand.getInventory().setItem(i, new ItemStack(Material.GLASS_BOTTLE)); // O Potion_Water
                }
            }
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
            loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0.5, 0.5, 0.5), 20, 0.2, 0.2, 0.2, 0.05); // 1.21 Fix Particle name
        }
    }

    private void iniciarReloj() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<Location, MezclaVolatil> entry : mezclas.entrySet()) {
                MezclaVolatil mezcla = entry.getValue();
                Location loc = entry.getKey();

                // Efecto visual constante mientras está inestable
                loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0.5, 1, 0.5), 2, 0.1, 0.1, 0.1, 0.01);

                mezcla.tiempoRestante--;
                if (mezcla.tiempoRestante <= 0) {
                    aplicarCastigo(loc);
                    mezclas.remove(loc);
                }
            }
        }, 20L, 20L); // 1 vez por segundo
    }
}