package me.nexo.items.artefactos;

// 🟢 Importamos el nuevo NexoItems
import me.nexo.items.NexoItems;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArtefactoManager {

    // 🟢 ARQUITECTURA: Usamos NexoItems
    private final NexoItems plugin;

    // Almacenamiento de Estrategias (Registry)
    private final Map<String, ArtefactoStrategy> estrategias = new HashMap<>();

    // CooldownManager: UUID -> (ArtefactoID -> TiempoExpiracion)
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    // Sets de control de estado (públicos para que el Listener los pueda leer)
    public final Set<UUID> invulnerables = ConcurrentHashMap.newKeySet();
    public final Set<UUID> alasActivas = ConcurrentHashMap.newKeySet();

    public ArtefactoManager(NexoItems plugin) {
        this.plugin = plugin;
        registrarEstrategias();
    }

    // ==========================================
    // ⚡ GESTIÓN DE ENERGÍA CUSTOM Y COOLDOWNS
    // ==========================================
    public boolean procesarUso(Player p, ArtefactoDTO dto) {
        UUID uuid = p.getUniqueId();
        long ahora = System.currentTimeMillis();

        // 1. Validar Cooldown
        cooldowns.putIfAbsent(uuid, new HashMap<>());
        Map<String, Long> playerCds = cooldowns.get(uuid);

        if (playerCds.containsKey(dto.id()) && playerCds.get(dto.id()) > ahora) {
            double restante = (playerCds.get(dto.id()) - ahora) / 1000.0;
            p.sendActionBar(String.format("§e⏳ Cooldown: %.1fs", restante));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return false;
        }

        // 🟢 2. ARQUITECTURA LIMPIA: Validar Energía Custom (Nexo) usando la API
        NexoUser user = NexoAPI.getInstance().getUserLocal(uuid);
        if (user == null) {
            p.sendMessage("§cTus datos aún están cargando...");
            return false;
        }

        int maxEnergia = 100 + ((user.getNexoNivel() - 1) * 20) + user.getEnergiaExtraAccesorios();
        int energiaActual = user.getEnergiaMineria();
        int costoFinal = dto.cost();

        // Lógica especial para costo porcentual (Ej: Orbe de Sobrecarga gasta un % de tu Energía Máxima)
        if (dto.id().equals("orbe_sobrecarga")) {
            costoFinal = (int) (maxEnergia * (dto.cost() / 100.0));
        }

        if (energiaActual < costoFinal) {
            p.sendActionBar(String.format("§c⚡ Energía insuficiente (%d/%d)", energiaActual, costoFinal));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return false;
        }

        // 3. Obtener y Ejecutar la Estrategia
        ArtefactoStrategy estrategia = estrategias.get(dto.id());
        if (estrategia == null) {
            p.sendMessage("§cEsta habilidad aún no está programada.");
            return false;
        }

        if (estrategia.ejecutar(p, dto)) {
            // Si fue exitoso, cobramos ENERGÍA y aplicamos cooldown en el NexoUser
            user.setEnergiaMineria(Math.max(0, energiaActual - costoFinal));

            // Los ítems Toggle manejan su propio cooldown al desactivarse
            if (dto.type() != ArtefactoDTO.HabilidadType.TOGGLE) {
                playerCds.put(dto.id(), ahora + (dto.cooldown() * 1000L));
            }
            return true;
        }
        return false;
    }

    public void limpiarCooldowns(UUID uuid) {
        cooldowns.remove(uuid);
    }

    // ==========================================
    // ⚔️ LAS 11 ESTRATEGIAS DE ARTEFACTOS
    // ==========================================
    private void registrarEstrategias() {

        // 1. Gancho de Cobre
        estrategias.put("gancho_cobre", (p, dto) -> {
            p.setVelocity(p.getLocation().getDirection().multiply(1.8).setY(1.2));
            p.playSound(p.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1f, 1f);
            return true;
        });

        // 2. Tótem de Crecimiento
        estrategias.put("totem_crecimiento", (p, dto) -> {
            Block centro = p.getLocation().getBlock();
            int aplicados = 0;
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Block b = centro.getRelative(x, 0, z);
                    if (b.getBlockData() instanceof Ageable) {
                        b.applyBoneMeal(BlockFace.UP);
                        aplicados++;
                    }
                }
            }
            p.playSound(p.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1f, 0.8f);
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation(), 30, 2, 0.5, 2);
            return aplicados > 0;
        });

        // 3. Imán de Chatarra
        estrategias.put("iman_chatarra", (p, dto) -> {
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
            p.getNearbyEntities(15, 15, 15).stream()
                    .filter(e -> e instanceof Item)
                    .forEach(e -> {
                        Vector pull = p.getLocation().toVector().subtract(e.getLocation().toVector()).normalize().multiply(1.5);
                        e.setVelocity(pull);
                    });
            return true;
        });

        // 4. Hoja del Vacío (AOTE)
        estrategias.put("hoja_vacio", (p, dto) -> {
            RayTraceResult ray = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 8, FluidCollisionMode.NEVER, true);
            Location target = (ray != null && ray.getHitBlock() != null)
                    ? ray.getHitBlock().getLocation()
                    : p.getLocation().add(p.getLocation().getDirection().multiply(8));

            target.setYaw(p.getYaw());
            target.setPitch(p.getPitch());

            p.teleport(target);
            p.playSound(p.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1f, 1f);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0)); // 20% Speed por 3s
            return true;
        });

        // 5. Vara Florbífida
        estrategias.put("vara_florbifida", (p, dto) -> {
            RayTraceResult ray = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 15, FluidCollisionMode.NEVER, true);
            Location impacto = (ray != null && ray.getHitBlock() != null) ? ray.getHitBlock().getLocation() : p.getLocation().add(p.getLocation().getDirection().multiply(15));

            p.getWorld().spawnParticle(Particle.HEART, impacto, 20, 2, 1, 2);
            p.playSound(impacto, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);

            double healAmount = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.05;
            p.setHealth(Math.min(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), p.getHealth() + healAmount));

            impacto.getWorld().getNearbyEntities(impacto, 4, 4, 4).stream()
                    .filter(e -> e instanceof Player && e != p)
                    .map(e -> (Player) e)
                    .forEach(ally -> ally.setHealth(Math.min(ally.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), ally.getHealth() + healAmount)));
            return true;
        });

        // 6. Cetro Glacial
        estrategias.put("cetro_glacial", (p, dto) -> {
            p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
            p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0,1,0), 100, 2, 1, 2, 0.1);

            p.getNearbyEntities(6, 4, 6).stream()
                    .filter(e -> e instanceof Monster && p.hasLineOfSight(e))
                    .map(e -> (Monster) e)
                    .forEach(mob -> {
                        mob.setAware(false); // Stun en la 1.21
                        mob.getWorld().spawnParticle(Particle.SNOWFLAKE, mob.getLocation(), 20);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> mob.setAware(true), 60L); // 3s
                    });
            return true;
        });

        // 7. Pico del Enano Rey
        estrategias.put("pico_enano", (p, dto) -> {
            Block centro = p.getTargetBlockExact(5);
            if (centro == null || !centro.getType().isSolid()) return false;

            p.playSound(centro.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 0.5f);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block b = centro.getRelative(x, y, z);
                        if (b.getType().toString().contains("STONE") || b.getType().toString().contains("ORE")) {
                            b.breakNaturally(p.getInventory().getItemInMainHand());
                        }
                    }
                }
            }
            return true;
        });

        // 8. Orbe de Sobrecarga
        estrategias.put("orbe_sobrecarga", (p, dto) -> {
            Location spawnLoc = p.getLocation().add(0, 2, 0);
            ArmorStand orbe = p.getWorld().spawn(spawnLoc, ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setGravity(false);
                as.setMarker(true);
                as.getEquipment().setHelmet(new ItemStack(Material.BEACON));
            });

            p.playSound(spawnLoc, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f);

            new BukkitRunnable() {
                int tiempo = 30; // 30 segundos
                @Override
                public void run() {
                    if (tiempo <= 0 || orbe.isDead()) {
                        orbe.remove();
                        cancel();
                        return;
                    }
                    orbe.setRotation(orbe.getYaw() + 10, 0); // Gira
                    orbe.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, orbe.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.01);

                    orbe.getNearbyEntities(10, 10, 10).stream()
                            .filter(e -> e instanceof Player)
                            .map(e -> (Player) e)
                            .forEach(ally -> {
                                ally.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false, false));
                            });
                    tiempo--;
                }
            }.runTaskTimer(plugin, 0L, 20L);
            return true;
        });

        // 9. Capa Espectral
        estrategias.put("capa_espectral", (p, dto) -> {
            invulnerables.add(p.getUniqueId());
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0, false, false, true));
            p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1f, 1f);
            p.getWorld().spawnParticle(Particle.LARGE_SMOKE, p.getLocation(), 50, 0.5, 1, 0.5, 0.05);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (invulnerables.remove(p.getUniqueId())) {
                    p.sendMessage("§8La Capa Espectral se ha desvanecido.");
                    p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1f);
                }
            }, 80L); // 4s
            return true;
        });

        // 10. Reloj de Bolsillo Astral
        estrategias.put("reloj_astral", (p, dto) -> {
            limpiarCooldowns(p.getUniqueId());
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 1f, 1.5f);
            p.sendMessage("§e✨ El tiempo se ha reescrito. Tus cooldowns se han reiniciado.");
            return true;
        });

        // 11. Alas del Nexo (TOGGLE - Drenan Energía ⚡)
        estrategias.put("alas_nexo", (p, dto) -> {
            UUID uuid = p.getUniqueId();
            if (alasActivas.contains(uuid)) {
                // Desactivar
                alasActivas.remove(uuid);
                p.setAllowFlight(false);
                p.setFlying(false);
                p.sendMessage("§cAlas del Nexo desactivadas.");
                cooldowns.get(uuid).put(dto.id(), System.currentTimeMillis() + (dto.cooldown() * 1000L)); // Aplica CD al apagar
                return false;
            } else {
                // Activar
                alasActivas.add(uuid);
                p.setAllowFlight(true);
                p.sendMessage("§bAlas del Nexo activadas. Drenando " + dto.cost() + " Energía/s.");

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!p.isOnline() || !alasActivas.contains(uuid)) {
                            cancel();
                            return;
                        }

                        // 🟢 ARQUITECTURA LIMPIA: Lógica de Energía Custom para la tarea asíncrona
                        NexoUser user = NexoAPI.getInstance().getUserLocal(uuid);
                        if (user == null) {
                            cancel();
                            return;
                        }

                        int energiaActual = user.getEnergiaMineria();

                        if (energiaActual < dto.cost()) {
                            alasActivas.remove(uuid);
                            p.setAllowFlight(false);
                            p.setFlying(false);
                            p.sendMessage("§c⚡ Energía agotada. Alas desactivadas.");
                            cooldowns.get(uuid).put(dto.id(), System.currentTimeMillis() + (dto.cooldown() * 1000L));
                            cancel();
                            return;
                        }

                        // Drenamos la energía en el NexoUser
                        user.setEnergiaMineria(energiaActual - dto.cost());
                        p.getWorld().spawnParticle(Particle.WAX_ON, p.getLocation(), 2);
                    }
                }.runTaskTimer(plugin, 20L, 20L); // 1 vez por segundo
                return true;
            }
        });
    }
}