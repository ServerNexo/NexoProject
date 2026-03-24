package me.nexo.pvp.pasivas;

import dev.aurelium.auraskills.api.skill.Skills;

// 🟢 ARQUITECTURA: Importamos tu nuevo plugin principal
import me.nexo.pvp.NexoPvP;

// 🟢 MANTENEMOS ESTO: El Addon se comunica con la API del Core
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;

public class PasivasListener implements Listener {

    // 🟢 ARQUITECTURA: Cambiamos Main por NexoPvP
    private final NexoPvP plugin;
    private final PasivasManager manager;

    // 🟢 ARQUITECTURA: Pedimos NexoPvP en el constructor
    public PasivasListener(NexoPvP plugin, PasivasManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // ==========================================
    // ⚔️ FIGHTING (Combate)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onCombate(EntityDamageByEntityEvent event) {
        // ATACANTE
        if (event.getDamager() instanceof Player atacante) {
            NexoUser user = NexoAPI.getInstance().getUserLocal(atacante.getUniqueId());
            int nivel = user != null ? user.getCombateNivel() : 1;

            // Lvl 25: Ejecutor
            if (nivel >= 25 && event.getEntity() instanceof org.bukkit.entity.LivingEntity victima) {
                double hpPercent = victima.getHealth() / victima.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                if (hpPercent <= 0.20) {
                    event.setDamage(event.getDamage() * 1.20);
                }
            }

            // Lvl 10: Sed de Sangre
            if (nivel >= 10) {
                double cura = event.getFinalDamage() * 0.05;
                double maxHp = atacante.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                atacante.setHealth(Math.min(maxHp, atacante.getHealth() + cura));
            }
        }

        // VÍCTIMA (Última Batalla y Protección de Invulnerabilidad)
        if (event.getEntity() instanceof Player victima) {
            UUID id = victima.getUniqueId();
            if (manager.invulnerablesUltimaBatalla.containsKey(id)) {
                event.setCancelled(true);
                return;
            }

            NexoUser user = NexoAPI.getInstance().getUserLocal(id);
            int nivel = user != null ? user.getCombateNivel() : 1;

            if (nivel >= 50 && event.getFinalDamage() >= victima.getHealth()) {
                long ahora = System.currentTimeMillis();
                long cooldownMilis = 10 * 60 * 1000L; // 10 minutos

                if (!manager.cdUltimaBatalla.containsKey(id) || (ahora - manager.cdUltimaBatalla.get(id)) > cooldownMilis) {
                    event.setCancelled(true);
                    victima.setHealth(1.0);
                    manager.invulnerablesUltimaBatalla.put(id, ahora + 3000L); // 3 segundos
                    manager.cdUltimaBatalla.put(id, ahora);

                    victima.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, victima.getLocation(), 100);
                    victima.playSound(victima.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
                    victima.sendTitle("§c§l¡ÚLTIMA BATALLA!", "§7Has esquivado a la muerte", 5, 40, 5);
                }
            }
        }
    }

    @EventHandler
    public void onDanoGeneral(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p) {
            if (manager.invulnerablesUltimaBatalla.containsKey(p.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            // ⛏️ MINING (Piel de Magma Lvl 25)
            if (event.getCause() == EntityDamageEvent.DamageCause.LAVA || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                NexoUser user = NexoAPI.getInstance().getUserLocal(p.getUniqueId());
                if (user != null && user.getMineriaNivel() >= 25 && p.getInventory().getItemInMainHand().getType().toString().contains("PICKAXE")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // ==========================================
    // ⛏️ MINING & 🪓 WOODCUTTING (Eventos de Bloque)
    // ==========================================
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        Block b = event.getBlock();
        String tipo = b.getType().toString();

        NexoUser user = NexoAPI.getInstance().getUserLocal(p.getUniqueId());
        if (user == null) return;

        // 🪓 WOODCUTTING (Aún lee de AuraSkills)
        int nivelTala = manager.getNivel(p, Skills.FORAGING);
        if (nivelTala >= 10 && tipo.contains("LEAVES")) {
            if (Math.random() <= 0.05) {
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.APPLE));
            }
        }

        if (tipo.contains("LOG")) {
            manager.ultimoTroncoRoto.put(p.getUniqueId(), System.currentTimeMillis());
            if (nivelTala >= 50 && Math.random() <= 0.05) {
                p.playSound(b.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 1.5f);
            }
        }

        // ⛏️ MINING
        int nivelMina = user.getMineriaNivel();
        if (nivelMina >= 50 && tipo.contains("STONE")) {
            if (Math.random() <= 0.01) {
                b.getWorld().spawnParticle(Particle.EXPLOSION, b.getLocation(), 1);
                b.getWorld().playSound(b.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                for(int x = -1; x <= 1; x++){
                    for(int z = -1; z <= 1; z++){
                        Block exp = b.getRelative(x, 0, z);
                        if(exp.getType().toString().contains("STONE")) exp.breakNaturally();
                    }
                }
            }
        }

        // 🌾 FARMING
        int nivelGranja = user.getAgriculturaNivel();
        if (nivelGranja >= 25 && b.getBlockData() instanceof org.bukkit.block.data.Ageable cultivo) {
            if (cultivo.getAge() == cultivo.getMaximumAge() && Math.random() <= 0.10) {
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.GOLDEN_CARROT)); // Placeholder ítem custom
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
            }
        }
    }

    // Raíces Firmes (Woodcutting 25 - Anti Knockback)
    @EventHandler
    public void onKnockback(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player p && event.getDamager() instanceof Monster) {
            if (manager.getNivel(p, Skills.FORAGING) >= 25) {
                Long ultimoTala = manager.ultimoTroncoRoto.get(p.getUniqueId());
                if (ultimoTala != null && (System.currentTimeMillis() - ultimoTala) <= 2000) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> p.setVelocity(new Vector(0, p.getVelocity().getY(), 0)), 1L);
                }
            }
        }
    }

    // ==========================================
    // 🌾 FARMING (Manos de Seda)
    // ==========================================
    @EventHandler
    public void onPisadas(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock() != null) {
            NexoUser user = NexoAPI.getInstance().getUserLocal(event.getPlayer().getUniqueId());
            if (event.getClickedBlock().getType() == Material.FARMLAND && user != null && user.getAgriculturaNivel() >= 10) {
                event.setCancelled(true);
            }
        }
    }

    // ==========================================
    // 🎣 FISHING (Pesca)
    // ==========================================
    @EventHandler
    public void onPescado(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player p = event.getPlayer();
            int nivel = manager.getNivel(p, Skills.FISHING);

            if (nivel >= 10) {
                NexoUser user = NexoAPI.getInstance().getUserLocal(p.getUniqueId());
                if (user != null) {
                    int energiaAct = user.getEnergiaMineria();
                    int maxEnergia = 100 + ((user.getNexoNivel() - 1) * 20) + user.getEnergiaExtraAccesorios();
                    user.setEnergiaMineria(Math.min(energiaAct + 5, maxEnergia));
                }
            }

            if (nivel >= 25 && event.getCaught() instanceof Item itemEntity && Math.random() <= 0.10) {
                ItemStack caught = itemEntity.getItemStack();
                caught.setAmount(caught.getAmount() * 2);
                itemEntity.setItemStack(caught);
                p.sendMessage("§b🎣 ¡Botín Gemelo activado!");
            }
        }
    }

    @EventHandler
    public void onAire(EntityAirChangeEvent event) {
        if (event.getEntity() instanceof Player p && manager.getNivel(p, Skills.FISHING) >= 50) {
            if (event.getAmount() < p.getRemainingAir()) {
                event.setCancelled(true);
                p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 100, 0, false, false, false));
            }
        }
    }

    // ==========================================
    // 🧪 ALQUIMIA
    // ==========================================
    @EventHandler
    public void onBeber(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.POTION) {
            Player p = event.getPlayer();
            int nivel = manager.getNivel(p, Skills.ALCHEMY);

            if (nivel >= 10) {
                // Multiplicador gestionado modificando la duración de los efectos actuales un tick después
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (PotionEffect effect : p.getActivePotionEffects()) {
                        p.addPotionEffect(new PotionEffect(effect.getType(), (int) (effect.getDuration() * 1.2), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
                    }
                }, 1L);
            }
            if (nivel >= 25) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1)); // Speed II
            }
        }
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        // Asumiendo que podemos verificar si un jugador online inició el stand
        if (Math.random() <= 0.10) {
            for (ItemStack item : event.getContents().getContents()) {
                if (item != null && item.getType() == Material.POTION) {
                    item.setAmount(item.getAmount() * 2);
                }
            }
        }
    }

    // ==========================================
    // 🔮 ENCANTAMIENTOS
    // ==========================================
    @EventHandler
    public void onXpGain(PlayerExpChangeEvent event) {
        if (manager.getNivel(event.getPlayer(), Skills.ENCHANTING) >= 10) {
            event.setAmount((int) (event.getAmount() * 1.10));
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        Player p = event.getEnchanter();
        if (manager.getNivel(p, Skills.ENCHANTING) >= 25 && Math.random() <= 0.15) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.setLevel(p.getLevel() + event.getExpLevelCost());
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
                p.sendMessage("§d✨ ¡Retención Mágica! Niveles devueltos.");
            }, 1L);
        }
    }
}