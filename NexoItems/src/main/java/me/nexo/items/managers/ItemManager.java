package me.nexo.items.managers;

import me.nexo.items.dtos.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {

    // Llaves Universales y Materiales
    public static NamespacedKey llaveNivelMejora, llaveMaterialMejora, llaveVidaExtra, llaveElemento, llaveSoulbound;

    // Llaves Antiguas (Profesiones/Stats)
    public static NamespacedKey llaveSuerteMinera, llaveVelocidadMineria, llaveSuerteAgricola, llaveVelocidadMovimiento, llaveSuerteTala, llaveFuerzaHacha, llaveVelocidadPesca, llaveCriaturaMarina;

    // 🚀 NUEVAS LLAVES DTO OPTIMIZADAS
    public static NamespacedKey llaveArmaduraId;
    public static NamespacedKey llaveWeaponId;
    public static NamespacedKey llaveWeaponPrestige;
    public static NamespacedKey llaveHerramientaId;
    public static NamespacedKey llaveBloquesRotos;
    public static NamespacedKey llaveReforja;
    public static NamespacedKey llaveEnchantId;
    public static NamespacedKey llaveEnchantNivel;

    // Llaves Antiguas de Armas (Las mantenemos por compatibilidad)
    public static NamespacedKey llaveArmaClase, llaveArmaReqCombate, llaveArmaDanioBase, llaveArmaMitica;

    // 🟢 ARQUITECTURA: Usamos nuestra clase principal especificando el paquete para evitar choque con Nexo API
    public static me.nexo.items.NexoItems pluginMemoria;

    public static void init(me.nexo.items.NexoItems plugin) {
        pluginMemoria = plugin;
        llaveNivelMejora = new NamespacedKey(plugin, "nexo_upgrade");
        llaveMaterialMejora = new NamespacedKey(plugin, "nexo_material_polvo");
        llaveVidaExtra = new NamespacedKey(plugin, "nexo_vida_extra");
        llaveElemento = new NamespacedKey(plugin, "nexo_elemento");
        llaveSoulbound = new NamespacedKey(plugin, "nexo_soulbound");

        llaveSuerteMinera = new NamespacedKey(plugin, "nexo_suerte_minera");
        llaveVelocidadMineria = new NamespacedKey(plugin, "nexo_velocidad_mineria");
        llaveSuerteAgricola = new NamespacedKey(plugin, "nexo_suerte_agricola");
        llaveVelocidadMovimiento = new NamespacedKey(plugin, "nexo_velocidad_movimiento");
        llaveSuerteTala = new NamespacedKey(plugin, "nexo_suerte_tala");
        llaveFuerzaHacha = new NamespacedKey(plugin, "nexo_fuerza_hacha");
        llaveVelocidadPesca = new NamespacedKey(plugin, "nexo_velocidad_pesca");
        llaveCriaturaMarina = new NamespacedKey(plugin, "nexo_criatura_marina");

        // DTOs
        llaveArmaduraId = new NamespacedKey(plugin, "nexo_armadura_id");
        llaveWeaponId = new NamespacedKey(plugin, "nexo_weapon_id");
        llaveWeaponPrestige = new NamespacedKey(plugin, "nexo_weapon_prestige");
        llaveHerramientaId = new NamespacedKey(plugin, "nexo_herramienta_id");
        llaveBloquesRotos = new NamespacedKey(plugin, "nexo_bloques_rotos");
        llaveReforja = new NamespacedKey(plugin, "nexo_reforja");

        // ⬇️ NUEVAS LLAVES DE ENCANTAMIENTOS ⬇️
        llaveEnchantId = new NamespacedKey(plugin, "nexo_enchant_id");
        llaveEnchantNivel = new NamespacedKey(plugin, "nexo_enchant_nivel");

        // Compatibilidad armas viejas
        llaveArmaClase = new NamespacedKey(plugin, "nexo_arma_clase");
        llaveArmaReqCombate = new NamespacedKey(plugin, "nexo_arma_req_combate");
        llaveArmaDanioBase = new NamespacedKey(plugin, "nexo_arma_danio_base");
        llaveArmaMitica = new NamespacedKey(plugin, "nexo_arma_mitica");
    }

    // ==========================================
    // ⛏️ FÁBRICA DE HERRAMIENTAS (NexoCore + Nexo RP)
    // ==========================================
    public static ItemStack generarHerramientaProfesion(String id_yml) {
        ToolDTO dto = pluginMemoria.getFileManager().getToolDTO(id_yml);
        if (dto == null) {
            org.bukkit.Bukkit.getLogger().warning("¡No se encontró la herramienta " + id_yml + " en caché!");
            return new ItemStack(Material.WOODEN_PICKAXE);
        }

        String nexoId = pluginMemoria.getFileManager().getHerramientas().getString("herramientas." + id_yml + ".nexo_id");
        ItemStack item;

        try {
            // 🟢 FIX CHOQUE DE NOMBRES: Usamos la ruta completa para referirnos al plugin de texturas
            if (nexoId != null && com.nexomc.nexo.api.NexoItems.itemFromId(nexoId) != null) {
                item = com.nexomc.nexo.api.NexoItems.itemFromId(nexoId).build();
            } else {
                String matString = pluginMemoria.getFileManager().getHerramientas().getString("herramientas." + id_yml + ".material", "IRON_PICKAXE");
                Material mat = Material.matchMaterial(matString);
                item = new ItemStack(mat != null ? mat : Material.IRON_PICKAXE);
            }
        } catch (NoClassDefFoundError e) {
            String matString = pluginMemoria.getFileManager().getHerramientas().getString("herramientas." + id_yml + ".material", "IRON_PICKAXE");
            Material mat = Material.matchMaterial(matString);
            item = new ItemStack(mat != null ? mat : Material.IRON_PICKAXE);
        }

        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', dto.nombre()));

        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', dto.rareza()));
        lore.add("§7Profesión: " + dto.profesion());
        lore.add("§7Tier: " + dto.tier());
        lore.add("");
        lore.add("§7Velocidad Base: §e+" + dto.velocidadBase());
        lore.add("§7Bonus Drops: §b+" + dto.multiplicadorFortuna() + "%");
        lore.add("");
        lore.add("§7Bloques Rotos: §e0");
        lore.add("");
        lore.add("§fRequisito de " + dto.profesion() + ": Nivel " + dto.nivelRequerido());

        meta.setLore(lore);
        meta.setUnbreakable(true);

        meta.getPersistentDataContainer().set(llaveHerramientaId, PersistentDataType.STRING, dto.id());
        meta.getPersistentDataContainer().set(llaveBloquesRotos, PersistentDataType.INTEGER, 0);
        item.setItemMeta(meta);

        if (dto.esTaladro()) {
            org.bukkit.inventory.meta.components.ToolComponent tool = meta.getTool();
            tool.addRule(org.bukkit.Tag.MINEABLE_SHOVEL, (float) dto.velocidadBase(), true);
            tool.addRule(org.bukkit.Tag.MINEABLE_PICKAXE, (float) dto.velocidadBase(), true);
            meta.setTool(tool);
        }

        return item;
    }

    // ==========================================
    // ⚔️ FÁBRICA DATA-DRIVEN (ARMAS RPG 1.21)
    // ==========================================
    public static ItemStack generarArmaRPG(String id_yml) {
        WeaponDTO dto = pluginMemoria.getFileManager().getWeaponDTO(id_yml);
        if (dto == null) {
            org.bukkit.Bukkit.getLogger().warning("¡No se encontró el arma " + id_yml + " en caché!");
            return new ItemStack(Material.WOODEN_SWORD);
        }

        String matString = pluginMemoria.getFileManager().getArmas().getString("armas_rpg." + id_yml + ".material", "IRON_SWORD");
        Material mat = Material.matchMaterial(matString);
        if (mat == null) mat = Material.IRON_SWORD;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', dto.nombre()) + " §8[§e+0§8]");

        lore.add("§7Clase: " + dto.claseRequerida());
        lore.add("§7Elemento: " + org.bukkit.ChatColor.translateAlternateColorCodes('&', dto.elemento()));
        lore.add("");
        lore.add("§7Daño Base: §c" + dto.danioBase() + " ⚔");
        lore.add("§7Velocidad: §e" + dto.velocidadAtaque() + " ⚡");
        lore.add("");

        if (!dto.habilidadId().equalsIgnoreCase("ninguna")) {
            lore.add("§6✦ Habilidad: §f" + dto.habilidadId().toUpperCase() + " §e§l(CLIC DERECHO)");
            lore.add("");
        }

        lore.add("§fRequisito de Combate: Nivel " + dto.nivelRequerido());
        meta.setLore(lore);
        meta.setUnbreakable(true);

        meta.getPersistentDataContainer().set(llaveWeaponId, PersistentDataType.STRING, dto.id());
        meta.getPersistentDataContainer().set(llaveWeaponPrestige, PersistentDataType.INTEGER, 0);

        NamespacedKey dmgKey = new NamespacedKey(pluginMemoria, "nexo_dmg_" + dto.id());
        org.bukkit.attribute.AttributeModifier dmgMod = new org.bukkit.attribute.AttributeModifier(
                dmgKey, dto.danioBase(), Operation.ADD_NUMBER, org.bukkit.inventory.EquipmentSlotGroup.MAINHAND);
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, dmgMod);

        NamespacedKey spdKey = new NamespacedKey(pluginMemoria, "nexo_spd_" + dto.id());
        double speedOffset = dto.velocidadAtaque() - 4.0;
        org.bukkit.attribute.AttributeModifier spdMod = new org.bukkit.attribute.AttributeModifier(
                spdKey, speedOffset, Operation.ADD_NUMBER, org.bukkit.inventory.EquipmentSlotGroup.MAINHAND);
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, spdMod);

        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    // ==========================================
    // ⚙️ FÁBRICA DATA-DRIVEN (ARMADURAS PROFESIONES)
    // ==========================================
    public static ItemStack generarArmaduraProfesion(String id_yml, String tipoPieza) {
        ArmorDTO dto = pluginMemoria.getFileManager().getArmorDTO(id_yml);
        if (dto == null) {
            org.bukkit.Bukkit.getLogger().warning("¡No se encontró la armadura " + id_yml + " en caché!");
            return new ItemStack(Material.STONE);
        }

        String matString = pluginMemoria.getFileManager().getArmaduras().getString("armaduras_profesion." + id_yml + ".material", "LEATHER_CHESTPLATE");
        String prefijoMat = matString.contains("_") ? matString.split("_")[0] : matString;
        Material mat;
        try {
            mat = Material.valueOf(prefijoMat + "_" + tipoPieza.toUpperCase());
        } catch (Exception e) {
            mat = Material.LEATHER_CHESTPLATE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        String etiquetaPieza = "";
        switch (tipoPieza.toUpperCase()) {
            case "HELMET": etiquetaPieza = " §8(Casco)"; break;
            case "CHESTPLATE": etiquetaPieza = " §8(Peto)"; break;
            case "LEGGINGS": etiquetaPieza = " §8(Pantalones)"; break;
            case "BOOTS": etiquetaPieza = " §8(Botas)"; break;
        }
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', dto.nombre()) + etiquetaPieza);

        lore.add("§7Clase: " + dto.claseRequerida());
        lore.add("");

        if (dto.vidaExtra() > 0) lore.add("§7Vida Extra: §c+" + dto.vidaExtra() + " ❤");
        if (dto.velocidadMovimiento() > 0) lore.add("§7Velocidad: §f+" + dto.velocidadMovimiento() + " 🍃");
        if (dto.suerteMinera() > 0) lore.add("§7Fortuna Minera: §b+" + dto.suerteMinera() + "% ✨");
        if (dto.velocidadMineria() > 0) lore.add("§7Prisa Minera: §e+" + dto.velocidadMineria() + " ⚡");
        if (dto.suerteAgricola() > 0) lore.add("§7Fortuna Agrícola: §a+" + dto.suerteAgricola() + "% 🌾");
        if (dto.suerteTala() > 0) lore.add("§7Doble Caída (Tala): §2+" + dto.suerteTala() + "% 🪓");
        if (dto.criaturaMarina() > 0) lore.add("§7Prob. Criatura Marina: §3+" + dto.criaturaMarina() + "% 🦑");
        if (dto.velocidadPesca() > 0) lore.add("§7Velocidad Pesca: §9+" + dto.velocidadPesca() + "% 🎣");

        List<String> loreCustom = pluginMemoria.getFileManager().getArmaduras().getStringList("armaduras_profesion." + id_yml + ".lore_custom");
        if (loreCustom != null && !loreCustom.isEmpty()) {
            lore.add("");
            for (String linea : loreCustom) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', linea));
            }
        }

        lore.add("");
        lore.add("§fRequisito de " + dto.skillRequerida() + ": Nivel " + dto.nivelRequerido());

        meta.setLore(lore);
        meta.setUnbreakable(true);

        meta.getPersistentDataContainer().set(llaveArmaduraId, PersistentDataType.STRING, dto.id());
        if (dto.vidaExtra() > 0) meta.getPersistentDataContainer().set(llaveVidaExtra, PersistentDataType.DOUBLE, dto.vidaExtra());

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack crearPolvoEstelar() {
        ItemStack item = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e✨ Polvo Estelar");
        meta.getPersistentDataContainer().set(llaveMaterialMejora, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack crearHojaVacio() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d🌌 Hoja del Vacío");
        List<String> lore = new ArrayList<>();
        lore.add("§7Artefacto de Utilidad");
        lore.add("");
        lore.add("§eHabilidad: Transmisión Instantánea §e§l(CLIC DERECHO)");
        lore.add("§8Costo: §e40 Energía ⚡");
        lore.add("§c🔒 Ligado al Alma");
        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(llaveSoulbound, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    // ==========================================
    // 🔨 SISTEMA DE REFORJAS INTELIGENTE (Armas y Herramientas)
    // ==========================================
    public static ItemStack aplicarReforja(ItemStack item, String idReforja) {
        if (item == null || !item.hasItemMeta()) return item;

        ReforgeDTO reforge = pluginMemoria.getFileManager().getReforgeDTO(idReforja);
        if (reforge == null) return item;

        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();

        // 1. Detección automática: ¿Es arma o es herramienta?
        boolean esArma = pdc.has(llaveWeaponId, PersistentDataType.STRING);
        boolean esHerramienta = pdc.has(llaveHerramientaId, PersistentDataType.STRING);

        if (!esArma && !esHerramienta) return item; // Si no es ninguna, abortamos.

        String claseOriginal = "Cualquiera";
        String nombreOriginal = "Ítem";
        double danioOriginal = 1.0;
        double velOriginal = 1.0;
        double fortunaOriginal = 0.0;
        String idBase = "";

        if (esArma) {
            idBase = pdc.get(llaveWeaponId, PersistentDataType.STRING);
            WeaponDTO arma = pluginMemoria.getFileManager().getWeaponDTO(idBase);
            if (arma == null) return item;
            claseOriginal = arma.claseRequerida();
            nombreOriginal = arma.nombre();
            danioOriginal = arma.danioBase();
            velOriginal = arma.velocidadAtaque();
        } else {
            idBase = pdc.get(llaveHerramientaId, PersistentDataType.STRING);
            ToolDTO tool = pluginMemoria.getFileManager().getToolDTO(idBase);
            if (tool == null) return item;
            claseOriginal = tool.profesion(); // Ej: "Minería", "Tala"
            nombreOriginal = tool.nombre();
            fortunaOriginal = tool.multiplicadorFortuna();
        }

        // 2. Comprobamos compatibilidad (Ej: ¿Es Magnético para Minería?)
        if (!reforge.aplicaAClase(claseOriginal) && !reforge.aplicaAClase("Cualquiera")) {
            return item;
        }

        // Guardamos la reforja
        pdc.set(llaveReforja, PersistentDataType.STRING, reforge.id());

        // 3. CAMBIAR EL NOMBRE (Con su nivel de mejora si es que tiene)
        String nombreBase = org.bukkit.ChatColor.translateAlternateColorCodes('&', nombreOriginal);
        int nivelMejora = pdc.getOrDefault(llaveNivelMejora, PersistentDataType.INTEGER, 0);
        String sufijoMejora = (esArma || nivelMejora > 0) ? " §8[§e+" + nivelMejora + "§8]" : "";

        String nombreReforjado = org.bukkit.ChatColor.translateAlternateColorCodes('&', reforge.prefijoColor()) + reforge.nombre() + " " + nombreBase + sufijoMejora;
        meta.setDisplayName(nombreReforjado);

        // 4. APLICAR ATRIBUTOS DE COMBATE (Si es arma o da daño)
        if (esArma || reforge.danioExtra() > 0) {
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);

            double danioTotal = danioOriginal + reforge.danioExtra();
            NamespacedKey dmgKey = new NamespacedKey(pluginMemoria, "nexo_dmg_" + idBase);
            org.bukkit.attribute.AttributeModifier dmgMod = new org.bukkit.attribute.AttributeModifier(
                    dmgKey, danioTotal, Operation.ADD_NUMBER, org.bukkit.inventory.EquipmentSlotGroup.MAINHAND);
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, dmgMod);

            double velocidadTotal = (velOriginal - 4.0) + reforge.velocidadAtaqueExtra();
            NamespacedKey spdKey = new NamespacedKey(pluginMemoria, "nexo_spd_" + idBase);
            org.bukkit.attribute.AttributeModifier spdMod = new org.bukkit.attribute.AttributeModifier(
                    spdKey, velocidadTotal, Operation.ADD_NUMBER, org.bukkit.inventory.EquipmentSlotGroup.MAINHAND);
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, spdMod);
        }

        // 5. APLICAR BONOS DE PROFESIÓN (FORTUNA)
        if (reforge.fortunaExtra() > 0) {
            if (claseOriginal.equalsIgnoreCase("Minería")) {
                pdc.set(llaveSuerteMinera, PersistentDataType.DOUBLE, reforge.fortunaExtra());
            } else if (claseOriginal.equalsIgnoreCase("Agricultura")) {
                pdc.set(llaveSuerteAgricola, PersistentDataType.DOUBLE, reforge.fortunaExtra());
            } else if (claseOriginal.equalsIgnoreCase("Tala")) {
                pdc.set(llaveSuerteTala, PersistentDataType.DOUBLE, reforge.fortunaExtra());
            } else if (claseOriginal.equalsIgnoreCase("Pesca")) {
                pdc.set(llaveCriaturaMarina, PersistentDataType.DOUBLE, reforge.fortunaExtra());
            }
        }

        // 6. ACTUALIZAR EL LORE (VISUAL)
        List<String> lore = meta.getLore();
        if (lore != null) {
            for (int i = 0; i < lore.size(); i++) {
                if (esArma) {
                    if (lore.get(i).contains("Daño Base:")) {
                        double danioTotal = danioOriginal + reforge.danioExtra();
                        lore.set(i, "§7Daño Base: §c" + danioTotal + " ⚔ " + org.bukkit.ChatColor.translateAlternateColorCodes('&', reforge.prefijoColor() + "(+" + reforge.danioExtra() + ")"));
                    }
                    if (lore.get(i).contains("Velocidad:")) {
                        double velVisual = velOriginal + reforge.velocidadAtaqueExtra();
                        lore.set(i, "§7Velocidad: §e" + velVisual + " ⚡ " + org.bukkit.ChatColor.translateAlternateColorCodes('&', reforge.prefijoColor() + "(" + (reforge.velocidadAtaqueExtra() >= 0 ? "+" : "") + reforge.velocidadAtaqueExtra() + ")"));
                    }
                } else if (esHerramienta) {
                    if (lore.get(i).contains("Bonus Drops:")) {
                        double fortunaTotal = fortunaOriginal + reforge.fortunaExtra();
                        lore.set(i, "§7Bonus Drops: §b+" + fortunaTotal + "% " + org.bukkit.ChatColor.translateAlternateColorCodes('&', reforge.prefijoColor() + "(+" + reforge.fortunaExtra() + "%)"));
                    }
                }
            }
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    // ==========================================
    // 📖 FÁBRICA DE LIBROS DE ENCANTAMIENTO CUSTOM
    // ==========================================
    public static ItemStack generarLibroEncantamiento(String idEnchant, int nivel) {
        EnchantDTO dto = pluginMemoria.getFileManager().getEnchantDTO(idEnchant);

        // Si el encantamiento no existe, devolvemos un libro normal feo para avisarte
        if (dto == null) {
            org.bukkit.Bukkit.getLogger().warning("¡No se encontró el encantamiento " + idEnchant + " en la caché!");
            return new ItemStack(Material.BOOK);
        }

        // Limitamos el nivel al máximo permitido por el DTO
        int nivelReal = Math.min(nivel, dto.nivelMaximo());

        ItemStack libro = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = libro.getItemMeta();

        // Nombre: Ej. "Vampirismo III"
        String nombreRomanos = "I";
        switch (nivelReal) {
            case 2: nombreRomanos = "II"; break;
            case 3: nombreRomanos = "III"; break;
            case 4: nombreRomanos = "IV"; break;
            case 5: nombreRomanos = "V"; break;
        }

        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', dto.nombre() + " " + nombreRomanos));

        // Construimos el Lore
        List<String> lore = new ArrayList<>();
        lore.add("§7Libro de Encantamiento Mágico");
        lore.add("");

        // Reemplazamos {val} por el valor real que da ese nivel
        double valorActual = dto.getValorPorNivel(nivelReal);
        String descReemplazada = dto.descripcion().replace("{val}", String.valueOf(valorActual));
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', descReemplazada));

        lore.add("");
        lore.add("§8Aplica a: " + String.join(", ", dto.aplicaA()));
        lore.add("§eLlévalo a un Yunque Mágico para aplicarlo.");

        meta.setLore(lore);

        // Inyectamos la información en el PDC para que el Yunque la lea luego
        meta.getPersistentDataContainer().set(llaveEnchantId, PersistentDataType.STRING, dto.id());
        meta.getPersistentDataContainer().set(llaveEnchantNivel, PersistentDataType.INTEGER, nivelReal);

        libro.setItemMeta(meta);
        return libro;
    }

    // ==========================================
    // ✨ SISTEMA DE ENCANTAMIENTOS CUSTOM
    // ==========================================
    public static ItemStack aplicarEncantamiento(ItemStack item, String idEnchant, int nivel) {
        if (item == null || !item.hasItemMeta()) return item;

        EnchantDTO enchant = pluginMemoria.getFileManager().getEnchantDTO(idEnchant);
        if (enchant == null) return item;

        ItemMeta meta = item.getItemMeta();

        // 1. Guardamos el encantamiento en el PDC del arma usando una llave dinámica
        NamespacedKey keyEnchant = new NamespacedKey(pluginMemoria, "nexo_enchant_" + idEnchant);
        meta.getPersistentDataContainer().set(keyEnchant, PersistentDataType.INTEGER, nivel);

        // 2. Traducimos el nivel a números romanos
        String nombreRomanos = "I";
        switch (nivel) {
            case 2: nombreRomanos = "II"; break;
            case 3: nombreRomanos = "III"; break;
            case 4: nombreRomanos = "IV"; break;
            case 5: nombreRomanos = "V"; break;
        }

        // 3. Preparamos el texto visual para el Lore (Ej: "&cVampirismo III")
        String nombrePuro = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', enchant.nombre()));
        String lineaEncantamiento = org.bukkit.ChatColor.translateAlternateColorCodes('&', enchant.nombre() + " " + nombreRomanos);

        // 4. Actualizamos el Lore
        List<String> lore = meta.getLore();
        if (lore != null) {
            boolean encontrado = false;
            // Buscamos si ya tiene este encantamiento para actualizar el nivel
            for (int i = 0; i < lore.size(); i++) {
                if (org.bukkit.ChatColor.stripColor(lore.get(i)).startsWith(nombrePuro)) {
                    lore.set(i, lineaEncantamiento);
                    encontrado = true;
                    break;
                }
            }
            // Si es un encantamiento nuevo, lo añadimos al final
            if (!encontrado) {
                lore.add(lineaEncantamiento);
            }
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

}