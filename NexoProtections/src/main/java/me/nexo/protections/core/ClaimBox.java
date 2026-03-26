package me.nexo.protections.core;

import org.bukkit.Location;

public record ClaimBox(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    // Método ultra-rápido para saber si un bloque exacto está dentro de este cubo
    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(world)) return false;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    // 🌟 NUEVO: Fórmula Matemática AABB para detectar choques entre dos terrenos
    public boolean intersects(ClaimBox other) {
        if (!this.world.equals(other.world())) return false;

        // Si los cubos se solapan en los 3 ejes (X, Y, Z), entonces hay colisión.
        return this.minX <= other.maxX() && this.maxX >= other.minX() &&
                this.minY <= other.maxY() && this.maxY >= other.minY() &&
                this.minZ <= other.maxZ() && this.maxZ >= other.minZ();
    }
}