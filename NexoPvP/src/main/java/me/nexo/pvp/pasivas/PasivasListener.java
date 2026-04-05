package me.nexo.pvp.pasivas;

import com.google.inject.Inject;
import dev.aurelium.auraskills.api.skill.Skills;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.config.ConfigManager;
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

/**
 * 🏛️ NexoPvP - Listener de Pasivas (Arquitectura Enterprise)
 * Cero static API calls. Type-Safe Configs.
 */
public class PasivasListener implements Listener {

    // 💉 PILAR 3: Dependencias Inyectadas
    private final NexoPvP plugin;
    private final PasivasManager manager;
    private final UserManager userManager;
    private final ConfigManager configManager;

    @Inject
    public PasivasListener(NexoPvP plugin, PasivasManager manager, UserManager userManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.manager = manager;
        this.userManager = userManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCombate(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player atacante) {
            NexoUser user = userManager.getUserOrNull(atacante.getUniqueId());
            int nivel = user != null ? user.getCombateNivel() : 1;

            if (nivel >= 25 && event.getEntity() instanceof org.bukkit.entity.LivingEntity victima) {
                double hpPercent = victima.getHealth() / victima.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                if (hpPercent <= 0.20) {
                    event.setDamage(event.getDamage() * 1.20);
                }
            }

            if (nivel >= 10) {
                double cura = event.getFinalDamage() * 0.05;
                double maxHp = atacante.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                atacante.setHealth(Math.min(maxHp, atacante.getHealth() + cura));
            }
        }

        if (event.getEntity() instanceof Player victima) {
            UUID id = victima.getUniqueId();
            if (manager.invulnerablesUltimaBatalla.containsKey(id)) {
                event.setCancelled(true);
                return;
            }

            NexoUser user = userManager.getUserOrNull(id);
            int nivel = user != null ? user.getCombateNivel() : 1;

            if (nivel >= 50 && event.getFinalDamage() >= victima.getHealth()) {
                long ahora = System.currentTimeMillis();
                long cooldownMilis = 10 * 60 * 1000L;

                if (!manager.cdUltimaBatalla.containsKey(id) || (ahora - manager.cdUltimaBatalla.get(id)) > cooldownMilis) {
                    event.setCancelled(true);
                    victima.setHealth(1.0);
                    manager.invulnerablesUltimaBatalla.put(id, ahora + 3000L);
                    manager.cdUltimaBatalla.put(id, ahora);

                    victima.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, victima.getLocation(), 100);
                    victima.playSound(victima.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);

                    // 💡 PILAR 2 & 6: Título nativo usando Type-Safe y Crossplay
                    victima.sendTitle(
                            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(
                                    CrossplayUtils.parseCrossplay(victima, configManager.getMessages().mensajes().pvp().escudoEmergenciaTitulo())
                            ),
                            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(
                                    CrossplayUtils.parseCrossplay(victima, configManager.getMessages().mensajes().pvp().escudoEmergenciaSub())
                            ),
                            5, 40, 5
                    );
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

            if (event.getCause() == EntityDamageEvent.DamageCause.LAVA || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                NexoUser user = userManager.getUserOrNull(p.getUniqueId());
                if (user != null && user.getMineriaNivel() >= 25 && p.getInventory().getItemInMainHand().getType().toString().contains("PICKAXE")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        Block b = event.getBlock();
        String tipo = b.getType().toString();

        NexoUser user = userManager.getUserOrNull(p.getUniqueId());
        if (user == null) return;

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

        int nivelGranja = user.getAgriculturaNivel();
        if (nivelGranja >= 25 && b.getBlockData() instanceof org.bukkit.block.data.Ageable cultivo) {
            if (cultivo.getAge() == cultivo.getMaximumAge() && Math.random() <= 0.10) {
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.GOLDEN_CARROT));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
            }
        }
    }

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

    @EventHandler
    public void onPisadas(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock() != null) {
            NexoUser user = userManager.getUserOrNull(event.getPlayer().getUniqueId());
            if (event.getClickedBlock().getType() == Material.FARMLAND && user != null && user.getAgriculturaNivel() >= 10) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPescado(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player p = event.getPlayer();
            int nivel = manager.getNivel(p, Skills.FISHING);

            if (nivel >= 10) {
                NexoUser user = userManager.getUserOrNull(p.getUniqueId());
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

                // 💡 PILAR 2 & 6: ActionBar segura
                CrossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().pvp().pescaCuantica());
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

    @EventHandler
    public void onBeber(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.POTION) {
            Player p = event.getPlayer();
            int nivel = manager.getNivel(p, Skills.ALCHEMY);

            if (nivel >= 10) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (PotionEffect effect : p.getActivePotionEffects()) {
                        p.addPotionEffect(new PotionEffect(effect.getType(), (int) (effect.getDuration() * 1.2), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
                    }
                }, 1L);
            }
            if (nivel >= 25) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
            }
        }
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (Math.random() <= 0.10) {
            for (ItemStack item : event.getContents().getContents()) {
                if (item != null && item.getType() == Material.POTION) {
                    item.setAmount(item.getAmount() * 2);
                }
            }
        }
    }

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
                CrossplayUtils.sendMessage(p, configManager.getMessages().mensajes().pvp().retencionEnergia());
            }, 1L);
        }
    }
}