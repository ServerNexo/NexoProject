package me.nexo.core.crossplay;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public class ParticleOptimizer {

    /**
     * Motor principal para el lanzamiento de partículas en todo el ecosistema (Dungeons, Combate, Habilidades).
     * Se ejecuta usando Virtual Threads de Java 21 para no bloquear el Main Thread calculando matemáticas.
     */
    public static void spawnOptimized(Location loc, Particle particle, int count, double oX, double oY, double oZ, double speed) {
        if (loc.getWorld() == null) return;

        // Desplegamos un Hilo Virtual de Java 21 (Ultraligero)
        Thread.startVirtualThread(() -> {
            // Recolectamos jugadores en un radio de 48 bloques (Visibilidad máxima estándar)
            loc.getWorld().getNearbyPlayers(loc, 48).forEach(player -> {

                // Si el jugador está conectado desde un móvil/consola (Bedrock)
                if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                    enviarParticulaBedrock(player, loc, particle, count, oX, oY, oZ, speed);
                }
                // Si el jugador está en PC (Java)
                else {
                    enviarParticulaJava(player, loc, particle, count, oX, oY, oZ, speed);
                }

            });
        });
    }

    private static void enviarParticulaJava(Player player, Location loc, Particle particle, int count, double oX, double oY, double oZ, double speed) {
        // En PC (Java), lanzamos todo el poder visual sin piedad
        player.spawnParticle(particle, loc, count, oX, oY, oZ, speed);
    }

    private static void enviarParticulaBedrock(Player player, Location loc, Particle particle, int count, double oX, double oY, double oZ, double speed) {
        // 1. Particle Replacement (Sustituimos partículas pesadas de Renderizado de GPU)
        Particle bedrockSafeParticle = getLighterParticle(particle);

        // 2. Throttling Law (Reducción estricta del 70% de densidad visual)
        int bedrockCount = Math.max(1, (int) (count * 0.3));

        player.spawnParticle(bedrockSafeParticle, loc, bedrockCount, oX, oY, oZ, speed);
    }

    /**
     * Convierte partículas masivas de humo o fuego expansivo (Causantes de Lag Spikes en móviles)
     * en indicadores de daño críticos o mágicos de muy bajo consumo.
     */
    private static Particle getLighterParticle(Particle p) {
        return switch (p) {
            case FLAME, LAVA, CAMPFIRE_COSY_SMOKE, SMOKE -> Particle.CRIT;
            case EXPLOSION_EMITTER, EXPLOSION -> Particle.FIREWORK;
            case DRAGON_BREATH -> Particle.WITCH;
            case SWEEP_ATTACK -> Particle.DAMAGE_INDICATOR;
            default -> p; // Si es segura, la dejamos intacta
        };
    }
}