package me.nexo.items.managers;

import me.nexo.core.utils.NexoColor;
import me.nexo.items.dtos.*;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class ItemManager {

    // Llaves Universales y Materiales
    public static NamespacedKey llaveNivelMejora, llaveMaterialMejora, llaveVidaExtra, llaveElemento, llaveSoulbound;
    public static NamespacedKey llaveSuerteMinera, llaveVelocidadMineria, llaveSuerteAgricola, llaveVelocidadMovimiento, llaveSuerteTala, llaveFuerzaHacha, llaveVelocidadPesca, llaveCriaturaMarina;

    // 🚀 NUEVAS LLAVES DTO OPTIMIZADAS
    public static NamespacedKey llaveArmaduraId, llaveWeaponId, llaveWeaponPrestige, llaveHerramientaId, llaveBloquesRotos, llaveReforja, llaveEnchantId, llaveEnchantNivel;

    // 🚀 LLAVES EVOLUCIÓN CÉNIT (FASE 5)
    public static NamespacedKey llaveNivelEvolucion, llaveEsencia, llaveFragmento;

    // Llaves Antiguas de Armas
    public static NamespacedKey llaveArmaClase, llaveArmaReqCombate, llaveArmaDanioBase, llaveArmaMitica;

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

        llaveArmaduraId = new NamespacedKey(plugin, "nexo_armadura_id");
        llaveWeaponId = new NamespacedKey(plugin, "nexo_weapon_id");
        llaveWeaponPrestige = new NamespacedKey(plugin, "nexo_weapon_prestige");
        llaveHerramientaId = new NamespacedKey(plugin, "nexo_herramienta_id");
        llaveBloquesRotos = new NamespacedKey(plugin, "nexo_bloques_rotos");
        llaveReforja = new NamespacedKey(plugin, "nexo_reforja");

        llaveEnchantId = new NamespacedKey(plugin, "nexo_enchant_id");
        llaveEnchantNivel = new NamespacedKey(plugin, "nexo_enchant_nivel");

        llaveNivelEvolucion = new NamespacedKey(plugin, "nexo_nivel_evolucion");
        llaveEsencia = new NamespacedKey(plugin, "nexo_esencia");
        llaveFragmento = new NamespacedKey(plugin, "nexo_fragmento");

        llaveArmaClase = new NamespacedKey(plugin, "nexo_arma_clase");
        llaveArmaReqCombate = new NamespacedKey(plugin, "nexo_arma_req_combate");
        llaveArmaDanioBase = new NamespacedKey(plugin, "nexo_arma_danio_base");
        llaveArmaMitica = new NamespacedKey(plugin, "nexo_arma_mitica");
    }

    private static String serialize(String hexString) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hexString));
    }

    // ====================================================
    // 🔄 MÓDULO 1: SINCRONIZACIÓN Y LEGADO CÉNIT (VIRTUAL THREADS)
    // ====================================================
    public static void sincronizarItemAsync(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;

        CompletableFuture.runAsync(() -> {
            ItemMeta meta = item.getItemMeta();
            String weaponId = meta.getPersistentDataContainer().get(llaveWeaponId, PersistentDataType.STRING);
            String toolId = meta.getPersistentDataContainer().get(llaveHerramientaId, PersistentDataType.STRING);

            int nivelEvolucion = meta.getPersistentDataContainer().getOrDefault(llaveNivelEvolucion, PersistentDataType.INTEGER, 1);
            String reforja = meta.getPersistentDataContainer().getOrDefault(llaveReforja, PersistentDataType.STRING, "");

            org.bukkit.Bukkit.getScheduler().runTask(pluginMemoria, () -> {
                if (weaponId != null) {
                    // 🌟 CORRECCIÓN: Cambiamos getWeapon por getWeaponDTO
                    WeaponDTO dto = pluginMemoria.getFileManager().getWeaponDTO(weaponId);
                    if (dto != null) aplicarEvolucionVisual(item, dto.nombre(), dto.danioBase(), nivelEvolucion, reforja);
                } else if (toolId != null) {
                    ToolDTO dto = pluginMemoria.getFileManager().getToolDTO(toolId);
                    if (dto != null) aplicarEvolucionVisual(item, dto.nombre(), 0, nivelEvolucion, reforja);
                }
            });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    private static void aplicarEvolucionVisual(ItemStack item, String nombreBase, double danioBase, int nivel, String reforja) {
        ItemMeta meta = item.getItemMeta();

        String prefijoReforja = reforja.isEmpty() ? "" : "&#FFAA00" + reforja + " ";
        String nombreFinal = prefijoReforja + nombreBase + " &#AAAAAA[Nv. " + nivel + "]";
        meta.setDisplayName(serialize(nombreFinal));

        if (danioBase > 0) {
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
            org.bukkit.attribute.AttributeModifier mod = new org.bukkit.attribute.AttributeModifier(llaveWeaponId, danioBase, Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, mod);
        }

        List<String> nuevoLore = new ArrayList<>();
        nuevoLore.add(serialize("&#555555Estadísticas de Evolución:"));
        nuevoLore.add(serialize("&#AAAAAANivel Cénit: &#FFAA00" + nivel + "&#AAAAAA/60"));

        if (meta.hasLore()) {
            for (String linea : meta.getLore()) {
                if (linea.contains("✦") || linea.contains("Nivel Cénit")) continue;
                nuevoLore.add(linea);
            }
        }

        meta.setLore(nuevoLore);
        item.setItemMeta(meta);

        if (nivel >= 10 && nivel < 20) {
            // Ejemplo para cambiar de skin cuando sube a nivel 10 usando la API de Nexo/Oraxen
        }
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

        meta.setDisplayName(serialize(dto.nombre()));

        lore.add(serialize(dto.rareza()));
        lore.add(serialize("&#AAAAAAProfesión: &#FFAA00" + dto.profesion()));
        lore.add(serialize("&#AAAAAATier: &#FFAA00" + dto.tier()));
        lore.add(" ");
        lore.add(serialize("&#AAAAAAVelocidad Base: &#55FF55+" + dto.velocidadBase()));
        lore.add(serialize("&#AAAAAABonus Drops: &#00E5FF+" + dto.multiplicadorFortuna() + "%"));
        lore.add(" ");
        lore.add(serialize("&#AAAAAABloques Rotos: &#FFAA000"));
        lore.add(" ");
        lore.add(serialize("&#FFFFFFRequisito de " + dto.profesion() + ": Nivel " + dto.nivelRequerido()));

        meta.setLore(lore);
        meta.setUnbreakable(true);

        meta.getPersistentDataContainer().set(llaveHerramientaId, PersistentDataType.STRING, dto.id());
        meta.getPersistentDataContainer().set(llaveBloquesRotos, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(llaveNivelEvolucion, PersistentDataType.INTEGER, 1);

        item.setItemMeta(meta);

        if (dto.esTaladro()) {
            org.bukkit.inventory.meta.components.ToolComponent tool = meta.getTool();
            tool.addRule(org.bukkit.Tag.MINEABLE_SHOVEL, (float) dto.velocidadBase(), true);
            tool.addRule(org.bukkit.Tag.MINEABLE_PICKAXE, (float) dto.velocidadBase(), true);
            meta.setTool(tool);
        }

        sincronizarItemAsync(item);
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

        meta.setDisplayName(serialize(dto.nombre()) + serialize(" &#555555[&#FFAA00+0&#555555]"));

        lore.add(serialize("&#AAAAAAClase: &#FFAA00" + dto.claseRequerida()));
        lore.add(serialize("&#AAAAAAElemento: " + dto.elemento()));
        lore.add(" ");
        lore.add(serialize("&#AAAAAADaño Base: &#FF5555" + dto.danioBase() + " ⚔"));
        lore.add(serialize("&#AAAAAAVelocidad: &#FFAA00" + dto.velocidadAtaque() + " ⚡"));
        lore.add(" ");

        if (!dto.habilidadId().equalsIgnoreCase("ninguna")) {
            lore.add(serialize("&#FFAA00✦ Habilidad: &#FFFFFF" + dto.habilidadId().toUpperCase() + " &#FFAA00<bold>(CLIC DERECHO)</bold>"));
            lore.add(" ");
        }

        lore.add(serialize("&#FFFFFFRequisito de Combate: Nivel " + dto.nivelRequerido()));
        meta.setLore(lore);
        meta.setUnbreakable(true);

        meta.getPersistentDataContainer().set(llaveWeaponId, PersistentDataType.STRING, dto.id());
        meta.getPersistentDataContainer().set(llaveWeaponPrestige, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(llaveNivelEvolucion, PersistentDataType.INTEGER, 1);

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

        sincronizarItemAsync(item);
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
            case "HELMET": etiquetaPieza = " &#555555(Casco)"; break;
            case "CHESTPLATE": etiquetaPieza = " &#555555(Peto)"; break;
            case "LEGGINGS": etiquetaPieza = " &#555555(Pantalones)"; break;
            case "BOOTS": etiquetaPieza = " &#555555(Botas)"; break;
        }
        meta.setDisplayName(serialize(dto.nombre() + etiquetaPieza));

        lore.add(serialize("&#AAAAAAClase: &#FFAA00" + dto.claseRequerida()));
        lore.add(" ");

        if (dto.vidaExtra() > 0) lore.add(serialize("&#AAAAAAVida Extra: &#FF5555+" + dto.vidaExtra() + " ❤"));
        if (dto.velocidadMovimiento() > 0) lore.add(serialize("&#AAAAAAVelocidad: &#FFFFFF+" + dto.velocidadMovimiento() + " 🍃"));
        if (dto.suerteMinera() > 0) lore.add(serialize("&#AAAAAAFortuna Minera: &#00E5FF+" + dto.suerteMinera() + "% ✨"));
        if (dto.velocidadMineria() > 0) lore.add(serialize("&#AAAAAAPrisa Minera: &#FFAA00+" + dto.velocidadMineria() + " ⚡"));
        if (dto.suerteAgricola() > 0) lore.add(serialize("&#AAAAAAFortuna Agrícola: &#55FF55+" + dto.suerteAgricola() + "% 🌾"));
        if (dto.suerteTala() > 0) lore.add(serialize("&#AAAAAADoble Caída (Tala): &#55FF55+" + dto.suerteTala() + "% 🪓"));
        if (dto.criaturaMarina() > 0) lore.add(serialize("&#AAAAAAProb. Criatura Marina: &#00E5FF+" + dto.criaturaMarina() + "% 🦑"));
        if (dto.velocidadPesca() > 0) lore.add(serialize("&#AAAAAAVelocidad Pesca: &#00E5FF+" + dto.velocidadPesca() + "% 🎣"));

        List<String> loreCustom = pluginMemoria.getFileManager().getArmaduras().getStringList("armaduras_profesion." + id_yml + ".lore_custom");
        if (loreCustom != null && !loreCustom.isEmpty()) {
            lore.add(" ");
            for (String linea : loreCustom) {
                lore.add(serialize(linea));
            }
        }

        lore.add(" ");
        lore.add(serialize("&#FFFFFFRequisito de " + dto.skillRequerida() + ": Nivel " + dto.nivelRequerido()));

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
        meta.setDisplayName(serialize("&#FFAA00✨ Polvo Estelar"));
        meta.getPersistentDataContainer().set(llaveMaterialMejora, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack crearHojaVacio() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(serialize("&#FF55FF🌌 Hoja del Vacío"));
        List<String> lore = new ArrayList<>();
        lore.add(serialize("&#AAAAAAArtefacto de Utilidad"));
        lore.add(" ");
        lore.add(serialize("&#FFAA00Habilidad: Transmisión Instantánea <bold>(CLIC DERECHO)</bold>"));
        lore.add(serialize("&#555555Costo: &#FFAA0040 Energía ⚡"));
        lore.add(serialize("&#FF5555🔒 Ligado al Alma"));
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

        boolean esArma = pdc.has(llaveWeaponId, PersistentDataType.STRING);
        boolean esHerramienta = pdc.has(llaveHerramientaId, PersistentDataType.STRING);

        if (!esArma && !esHerramienta) return item;

        String claseOriginal = "Cualquiera";
        String idBase = "";

        if (esArma) {
            idBase = pdc.get(llaveWeaponId, PersistentDataType.STRING);
            WeaponDTO arma = pluginMemoria.getFileManager().getWeaponDTO(idBase);
            if (arma == null) return item;
            claseOriginal = arma.claseRequerida();
        } else {
            idBase = pdc.get(llaveHerramientaId, PersistentDataType.STRING);
            ToolDTO tool = pluginMemoria.getFileManager().getToolDTO(idBase);
            if (tool == null) return item;
            claseOriginal = tool.profesion();
        }

        if (!reforge.aplicaAClase(claseOriginal) && !reforge.aplicaAClase("Cualquiera")) {
            return item;
        }

        pdc.set(llaveReforja, PersistentDataType.STRING, reforge.id());

        item.setItemMeta(meta);
        sincronizarItemAsync(item);

        return item;
    }

    // ==========================================
    // 📖 FÁBRICA DE LIBROS DE ENCANTAMIENTO CUSTOM
    // ==========================================
    public static ItemStack generarLibroEncantamiento(String idEnchant, int nivel) {
        EnchantDTO dto = pluginMemoria.getFileManager().getEnchantDTO(idEnchant);

        if (dto == null) {
            org.bukkit.Bukkit.getLogger().warning("¡No se encontró el encantamiento " + idEnchant + " en la caché!");
            return new ItemStack(Material.BOOK);
        }

        int nivelReal = Math.min(nivel, dto.nivelMaximo());

        ItemStack libro = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = libro.getItemMeta();

        String nombreRomanos = "I";
        switch (nivelReal) {
            case 2: nombreRomanos = "II"; break;
            case 3: nombreRomanos = "III"; break;
            case 4: nombreRomanos = "IV"; break;
            case 5: nombreRomanos = "V"; break;
        }

        meta.setDisplayName(serialize(dto.nombre() + " " + nombreRomanos));

        List<String> lore = new ArrayList<>();
        lore.add(serialize("&#AAAAAALibro de Encantamiento Mágico"));
        lore.add(" ");

        double valorActual = dto.getValorPorNivel(nivelReal);
        String descReemplazada = dto.descripcion().replace("{val}", String.valueOf(valorActual));
        lore.add(serialize(descReemplazada));

        lore.add(" ");
        lore.add(serialize("&#555555Aplica a: " + String.join(", ", dto.aplicaA())));
        lore.add(serialize("&#FFAA00Llévalo a un Yunque Mágico para aplicarlo."));

        meta.setLore(lore);

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

        NamespacedKey keyEnchant = new NamespacedKey(pluginMemoria, "nexo_enchant_" + idEnchant);
        meta.getPersistentDataContainer().set(keyEnchant, PersistentDataType.INTEGER, nivel);

        String nombreRomanos = "I";
        switch (nivel) {
            case 2: nombreRomanos = "II"; break;
            case 3: nombreRomanos = "III"; break;
            case 4: nombreRomanos = "IV"; break;
            case 5: nombreRomanos = "V"; break;
        }

        String lineaEncantamiento = serialize(enchant.nombre() + " " + nombreRomanos);

        // Convertimos a string sin colores hex para poder buscar la coincidencia
        String nombrePuro = org.bukkit.ChatColor.stripColor(LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(enchant.nombre())));

        List<String> lore = meta.getLore();
        if (lore != null) {
            boolean encontrado = false;
            for (int i = 0; i < lore.size(); i++) {
                if (org.bukkit.ChatColor.stripColor(lore.get(i)).startsWith(nombrePuro)) {
                    lore.set(i, lineaEncantamiento);
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                lore.add(lineaEncantamiento);
            }
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    // STUBS TEMPORALES PARA EVITAR ERRORES DE COMPILACIÓN (A falta de su YAML DTO)
    public static ItemStack generarArmadura(String id) {
        return generarArmaduraProfesion(id, "CHESTPLATE");
    }

    public static ItemStack generarHerramienta(String id) {
        return generarHerramientaProfesion(id);
    }
}