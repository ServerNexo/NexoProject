package me.nexo.items.mecanicas;

import me.nexo.items.managers.ItemManager;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ArmorDTO;
import me.nexo.items.dtos.EnchantDTO;
import me.nexo.items.dtos.WeaponDTO;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DamageListener implements Listener {

    // 🟢 ARQUITECTURA: Ahora usamos NexoItems
    private final NexoItems plugin;

    public DamageListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alPegar(EntityDamageByEntityEvent event) {

        // ==========================================
        // 🛡️ LÓGICA DE DEFENSA Y ARMADURA (Si la víctima es un Jugador)
        // ==========================================
        if (event.getEntity() instanceof Player victima) {
            double probEvasion = 0.0;
            double reflejoEspinosa = 0.0;
            double defensaExtra = 0.0;

            // Escaneamos la armadura que lleva puesta
            for (ItemStack armor : victima.getInventory().getArmorContents()) {
                if (armor == null || !armor.hasItemMeta()) continue;
                var pdc = armor.getItemMeta().getPersistentDataContainer();

                // 1. Calcular mitigación de daño basada en el Tier/Vida de la armadura
                if (pdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                    ArmorDTO dto = plugin.getFileManager().getArmorDTO(pdc.get(ItemManager.llaveArmaduraId, PersistentDataType.STRING));
                    if (dto != null) {
                        // Por cada 10 puntos de "Vida Extra" que da la armadura, reducimos 1 punto de daño real
                        defensaExtra += (dto.vidaExtra() / 10.0);
                    }
                }

                // 2. Leer Encantamiento: Evasión
                String idEv = "evasion";
                org.bukkit.NamespacedKey keyEv = new org.bukkit.NamespacedKey(plugin, "nexo_enchant_" + idEv);
                if (pdc.has(keyEv, PersistentDataType.INTEGER)) {
                    EnchantDTO ench = plugin.getFileManager().getEnchantDTO(idEv);
                    if (ench != null) probEvasion += ench.getValorPorNivel(pdc.get(keyEv, PersistentDataType.INTEGER));
                }

                // 3. Leer Encantamiento: Coraza Espinosa
                String idEsp = "coraza_espinosa";
                org.bukkit.NamespacedKey keyEsp = new org.bukkit.NamespacedKey(plugin, "nexo_enchant_" + idEsp);
                if (pdc.has(keyEsp, PersistentDataType.INTEGER)) {
                    EnchantDTO ench = plugin.getFileManager().getEnchantDTO(idEsp);
                    if (ench != null) reflejoEspinosa += ench.getValorPorNivel(pdc.get(keyEsp, PersistentDataType.INTEGER));
                }
            }

            // Aplicar Evasión Mágica
            if (probEvasion > 0 && Math.random() * 100 <= probEvasion) {
                event.setCancelled(true);
                victima.sendMessage("§f§l¡EVASIÓN!");
                victima.playSound(victima.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 2f);
                return;
            }

            // Aplicar Mitigación de Daño
            double danioReducido = Math.max(1.0, event.getDamage() - defensaExtra); // Nunca baja de 1 de daño
            event.setDamage(danioReducido);

            // Aplicar Coraza Espinosa
            if (reflejoEspinosa > 0 && event.getDamager() instanceof LivingEntity atacante) {
                double danioDevuelto = danioReducido * (reflejoEspinosa / 100.0);
                atacante.damage(danioDevuelto, victima);
            }
        }

        // ==========================================
        // ⚔️ LÓGICA DE ATAQUE (Si el atacante es un Jugador)
        // ==========================================
        if (event.getDamager() instanceof Player jugador && event.getEntity() instanceof LivingEntity victima) {

            ItemStack arma = jugador.getInventory().getItemInMainHand();
            if (arma == null || !arma.hasItemMeta()) return;

            var pdc = arma.getItemMeta().getPersistentDataContainer();

            if (pdc.has(ItemManager.llaveWeaponId, PersistentDataType.STRING)) {
                String idArma = pdc.get(ItemManager.llaveWeaponId, PersistentDataType.STRING);
                WeaponDTO dto = plugin.getFileManager().getWeaponDTO(idArma);

                if (dto != null) {

                    // 🟢 ARQUITECTURA LIMPIA: Obtenemos el usuario de la caché
                    NexoUser user = NexoAPI.getInstance().getUserLocal(jugador.getUniqueId());

                    String claseJugador = "Ninguna";
                    int nivelCombate = 1;

                    if (user != null) {
                        claseJugador = user.getClaseJugador();
                        nivelCombate = user.getCombateNivel();
                    }

                    // 1. RESTRICCIONES (Clase y Nivel de Combate)
                    if (!dto.claseRequerida().equalsIgnoreCase("Cualquiera") && !dto.claseRequerida().equalsIgnoreCase(claseJugador)) {
                        jugador.sendMessage("§c§l⚠ §cTu clase (" + claseJugador + ") no puede usar esta arma.");
                        jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        event.setDamage(1.0); // Daño de castigo
                        return;
                    }

                    if (nivelCombate < dto.nivelRequerido()) {
                        event.setCancelled(true);
                        jugador.sendMessage("§c§l⚠ ARMA PESADA §8| §7Necesitas Nivel de Combate §e" + dto.nivelRequerido());
                        jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        return;
                    }

                    // 2. DAÑO BASE (Ya viene calculado gracias a la 1.21.11)
                    double dañoFinal = event.getDamage();

                    // 3. MULTIPLICADOR DE PRESTIGIO
                    int prestigio = pdc.getOrDefault(ItemManager.llaveWeaponPrestige, PersistentDataType.INTEGER, 0);
                    if (prestigio > 0 && dto.permitePrestigio()) {
                        dañoFinal += (dañoFinal * (prestigio * dto.multiPrestigio()));
                    }

                    // ==========================================
                    // 🪄 LECTURA DE ENCANTAMIENTOS OFENSIVOS
                    // ==========================================

                    // -- Ejecutor --
                    String idEjecutor = "ejecutor";
                    org.bukkit.NamespacedKey keyEjecutor = new org.bukkit.NamespacedKey(plugin, "nexo_enchant_" + idEjecutor);
                    if (pdc.has(keyEjecutor, PersistentDataType.INTEGER)) {
                        double maxHp = victima.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                        if ((victima.getHealth() / maxHp) <= 0.20) { // Si tiene menos del 20% de vida
                            EnchantDTO ench = plugin.getFileManager().getEnchantDTO(idEjecutor);
                            if (ench != null) {
                                double bono = ench.getValorPorNivel(pdc.get(keyEjecutor, PersistentDataType.INTEGER));
                                dañoFinal += (dañoFinal * (bono / 100.0));
                            }
                        }
                    }

                    // -- Cazador --
                    String idCazador = "cazador";
                    org.bukkit.NamespacedKey keyCazador = new org.bukkit.NamespacedKey(plugin, "nexo_enchant_" + idCazador);
                    if (pdc.has(keyCazador, PersistentDataType.INTEGER) && victima instanceof Monster) {
                        EnchantDTO ench = plugin.getFileManager().getEnchantDTO(idCazador);
                        if (ench != null) {
                            double bono = ench.getValorPorNivel(pdc.get(keyCazador, PersistentDataType.INTEGER));
                            dañoFinal += (dañoFinal * (bono / 100.0));
                        }
                    }

                    // -- Veneno Mortal --
                    String idVeneno = "veneno";
                    org.bukkit.NamespacedKey keyVeneno = new org.bukkit.NamespacedKey(plugin, "nexo_enchant_" + idVeneno);
                    if (pdc.has(keyVeneno, PersistentDataType.INTEGER)) {
                        EnchantDTO ench = plugin.getFileManager().getEnchantDTO(idVeneno);
                        if (ench != null) {
                            int duracionTicks = (int) (ench.getValorPorNivel(pdc.get(keyVeneno, PersistentDataType.INTEGER)) * 20);
                            victima.addPotionEffect(new PotionEffect(PotionEffectType.POISON, duracionTicks, 0, false, false, false));
                        }
                    }

                    // 4. DAÑO Y EFECTOS ELEMENTALES
                    String elementoLimpio = ChatColor.stripColor(dto.elemento()).toUpperCase();
                    String nombreMob = victima.getCustomName() != null ? ChatColor.stripColor(victima.getCustomName()).toUpperCase() : "";
                    double multElemental = 1.0;

                    if (elementoLimpio.contains("FUEGO") || elementoLimpio.contains("MAGMA") || elementoLimpio.contains("SOLAR")) {
                        victima.setFireTicks(60);
                        if (nombreMob.contains("[HIELO]")) multElemental = 2.0;
                    }
                    else if (elementoLimpio.contains("HIELO") || elementoLimpio.contains("AGUA")) {
                        victima.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, false));
                        if (nombreMob.contains("[FUEGO]")) multElemental = 2.0;
                    }
                    else if (elementoLimpio.contains("RAYO") || elementoLimpio.contains("TORMENTA")) {
                        if (Math.random() <= 0.15) victima.getWorld().strikeLightningEffect(victima.getLocation());
                        if (nombreMob.contains("[AGUA]")) multElemental = 2.0;
                    }

                    dañoFinal *= multElemental;

                    if (multElemental > 1.0) {
                        jugador.sendMessage("§a§l¡GOLPE CRÍTICO ELEMENTAL!");
                    }

                    // APLICAR EL DAÑO FINAL
                    event.setDamage(dañoFinal);

                    // 5. VAMPIRISMO (Se calcula con el daño final real)
                    String idVamp = "vampirismo";
                    org.bukkit.NamespacedKey keyVamp = new org.bukkit.NamespacedKey(plugin, "nexo_enchant_" + idVamp);
                    if (pdc.has(keyVamp, PersistentDataType.INTEGER)) {
                        EnchantDTO ench = plugin.getFileManager().getEnchantDTO(idVamp);
                        if (ench != null) {
                            double porcentaje = ench.getValorPorNivel(pdc.get(keyVamp, PersistentDataType.INTEGER));
                            double cura = dañoFinal * (porcentaje / 100.0);
                            double maxVidaJugador = jugador.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

                            jugador.setHealth(Math.min(maxVidaJugador, jugador.getHealth() + cura));
                            jugador.getWorld().spawnParticle(org.bukkit.Particle.HEART, jugador.getLocation().add(0, 1, 0), 1);
                        }
                    }
                }
            }
        }
    }
}