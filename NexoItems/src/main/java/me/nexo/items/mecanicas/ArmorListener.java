package me.nexo.items.mecanicas;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ArmorDTO;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ArmorListener implements Listener {

    private final NexoItems plugin;

    public ArmorListener(NexoItems plugin) {
        this.plugin = plugin;

        // 🌟 EL HEARTBEAT (Latido): Escanea silenciosamente a todos cada 2 segundos.
        // Esto evita que un jugador retenga armaduras si le bajan el nivel o le cambian de clase.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                evaluarArmadura(p);
            }
        }, 40L, 40L);
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> evaluarArmadura(event.getPlayer()));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        evaluarArmadura(event.getPlayer());
    }

    public void evaluarArmadura(Player p) {
        ItemStack[] armor = p.getInventory().getArmorContents();
        double extraVida = 0;
        double velMineria = 0;
        double velMovimiento = 0;

        String claseDominante = "Cualquiera";

        // ==========================================
        // 1. DETECTAR CLASE DOMINANTE
        // ==========================================
        for (ItemStack item : armor) {
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) continue;
            var pdc = item.getItemMeta().getPersistentDataContainer();
            if (pdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                ArmorDTO dto = plugin.getFileManager().getArmorDTO(pdc.get(ItemManager.llaveArmaduraId, PersistentDataType.STRING));
                if (dto != null && !dto.claseRequerida().equalsIgnoreCase("Cualquiera") && !dto.claseRequerida().equalsIgnoreCase("Ninguna")) {
                    claseDominante = dto.claseRequerida();
                    break;
                }
            }
        }

        NexoUser user = NexoAPI.getInstance().getUserLocal(p.getUniqueId());
        if (user != null) {
            user.setClaseJugador(claseDominante);
        }

        // ==========================================
        // 2. APLICAR STATS Y RESTRICCIONES
        // ==========================================
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) continue;
            var pdc = item.getItemMeta().getPersistentDataContainer();

            if (pdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                String id = pdc.get(ItemManager.llaveArmaduraId, PersistentDataType.STRING);
                ArmorDTO dto = plugin.getFileManager().getArmorDTO(id);

                if (dto != null) {
                    boolean cumpleRequisitos = true;
                    String razonFallo = "";

                    if (!dto.claseRequerida().equalsIgnoreCase("Cualquiera") &&
                            !dto.claseRequerida().equalsIgnoreCase("Ninguna") &&
                            !dto.claseRequerida().equalsIgnoreCase(claseDominante)) {
                        razonFallo = "Choque de Clases (" + dto.claseRequerida() + ")";
                        cumpleRequisitos = false;
                    }

                    if (cumpleRequisitos) {
                        int nivelJugador = 1;
                        String skill = dto.skillRequerida();

                        if (user != null) {
                            if (skill.equalsIgnoreCase("Combate")) nivelJugador = Math.max(1, user.getCombateNivel());
                            else if (skill.equalsIgnoreCase("Minería")) nivelJugador = Math.max(1, user.getMineriaNivel());
                            else if (skill.equalsIgnoreCase("Agricultura")) nivelJugador = Math.max(1, user.getAgriculturaNivel());
                        }

                        try {
                            if (skill.equalsIgnoreCase("Pesca")) nivelJugador = Math.max(1, (int) AuraSkillsApi.get().getUser(p.getUniqueId()).getSkillLevel(Skills.FISHING));
                            else if (skill.equalsIgnoreCase("Tala")) nivelJugador = Math.max(1, (int) AuraSkillsApi.get().getUser(p.getUniqueId()).getSkillLevel(Skills.FORAGING));
                        } catch (Exception ignored) {}

                        if (nivelJugador < dto.nivelRequerido()) {
                            razonFallo = skill + " Nv." + dto.nivelRequerido();
                            cumpleRequisitos = false;
                        }
                    }

                    if (cumpleRequisitos) {
                        extraVida += dto.vidaExtra();
                        velMineria += dto.velocidadMineria();
                        velMovimiento += dto.velocidadMovimiento();
                    } else {
                        quitarArmadura(p, item, i, razonFallo);
                    }
                }
            }
            else if (pdc.has(ItemManager.llaveVidaExtra, PersistentDataType.DOUBLE)) {
                extraVida += pdc.get(ItemManager.llaveVidaExtra, PersistentDataType.DOUBLE);
            }
        }

        // ==========================================
        // 3. APLICAR VIDA Y EFECTOS FINALES
        // ==========================================
        double total = 20.0 + extraVida;
        var healthAttr = p.getAttribute(Attribute.MAX_HEALTH);

        if (healthAttr != null && healthAttr.getBaseValue() != total) {
            if (p.getHealth() > total) p.setHealth(total);
            healthAttr.setBaseValue(total);
            p.setHealthScaled(true);
            p.setHealthScale(20.0);
        }

        // 🌟 Usamos el gestor inteligente de efectos para no crashear la cámara del jugador
        gestionarEfecto(p, PotionEffectType.HASTE, (int) (velMineria / 20), velMineria > 0);
        gestionarEfecto(p, PotionEffectType.SPEED, (int) (velMovimiento / 20), velMovimiento > 0);
    }

    // 🌟 HELPER: Actualiza pociones sin parpadeos visuales
    private void gestionarEfecto(Player p, PotionEffectType tipo, int nivel, boolean aplicar) {
        if (aplicar) {
            boolean necesitaActualizar = true;
            PotionEffect efectoActual = p.getPotionEffect(tipo);

            // Si ya tiene el efecto al mismo nivel y le queda mucho tiempo, no lo sobrescribimos
            if (efectoActual != null && efectoActual.getAmplifier() == nivel && efectoActual.getDuration() > 100) {
                necesitaActualizar = false;
            }

            if (necesitaActualizar) {
                p.addPotionEffect(new PotionEffect(tipo, PotionEffect.INFINITE_DURATION, nivel, false, false, false));
            }
        } else {
            p.removePotionEffect(tipo);
        }
    }

    private void quitarArmadura(Player p, ItemStack item, int slotIndex, String razon) {
        ItemStack clon = item.clone();

        if(slotIndex == 0) p.getInventory().setBoots(null);
        if(slotIndex == 1) p.getInventory().setLeggings(null);
        if(slotIndex == 2) p.getInventory().setChestplate(null);
        if(slotIndex == 3) p.getInventory().setHelmet(null);

        if (!p.getInventory().addItem(clon).isEmpty()) {
            p.getWorld().dropItem(p.getLocation(), clon);
        }

        p.sendMessage("§c§l⚠ NO ERES DIGNO §7| Requisito: §e" + razon);
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
    }
}