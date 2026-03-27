package me.nexo.colecciones.data;

import org.bukkit.Material;
import java.util.Map;

public class CollectionItem {
    private final String id;
    private final String categoriaId;
    private final String nombre;
    private final Material icono;
    private final String nexoId;
    private final int slotMenu;
    private final Map<Integer, Tier> tiers; // Mapa de Nivel -> Datos del Tier

    public CollectionItem(String id, String categoriaId, String nombre, Material icono, String nexoId, int slotMenu, Map<Integer, Tier> tiers) {
        this.id = id;
        this.categoriaId = categoriaId;
        this.nombre = nombre;
        this.icono = icono;
        this.nexoId = nexoId;
        this.slotMenu = slotMenu;
        this.tiers = tiers;
    }

    public String getId() { return id; }
    public String getCategoriaId() { return categoriaId; }
    public String getNombre() { return nombre; }
    public Material getIcono() { return icono; }
    public String getNexoId() { return nexoId; }
    public int getSlotMenu() { return slotMenu; }
    public Map<Integer, Tier> getTiers() { return tiers; }

    public Tier getTier(int nivel) {
        return tiers.get(nivel);
    }

    public int getMaxTier() {
        return tiers.keySet().stream().max(Integer::compareTo).orElse(0);
    }
}