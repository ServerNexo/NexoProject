package me.nexo.colecciones.data;

import org.bukkit.Material;
import java.util.HashMap;
import java.util.Map;

public class CollectionCategory {
    private final String id;
    private final String nombre;
    private final Material icono;
    private final int slot;
    private final Map<String, CollectionItem> items = new HashMap<>();

    public CollectionCategory(String id, String nombre, Material icono, int slot) {
        this.id = id;
        this.nombre = nombre;
        this.icono = icono;
        this.slot = slot;
    }

    public void addItem(CollectionItem item) {
        items.put(item.getId(), item);
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public Material getIcono() { return icono; }
    public int getSlot() { return slot; }
    public Map<String, CollectionItem> getItems() { return items; }
}