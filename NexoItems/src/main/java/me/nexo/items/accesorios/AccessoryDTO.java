package me.nexo.items.accesorios;

public record AccessoryDTO(
        String id,
        Familia family,
        Rareza rarity,
        StatType statType,
        double statValue,
        String abilityDescription
) {
    public enum Familia {
        MINERIA, TALA, COSECHA, PESCA, TANQUE, MELEE, RANGO, ENERGIA, MOVILIDAD, RIQUEZA, CAZAJEFES
    }

    public enum Rareza {
        COMUN(3, "&#AAAAAA"),       // Gris Plomo
        RARO(8, "&#5555FF"),        // Azul Neón
        EPICO(12, "&#AA00AA"),      // Púrpura Profundo
        LEGENDARIO(16, "&#FFAA00"), // Naranja/Dorado Corporativo
        MITICO(22, "&#FF5555"),     // Rojo Alerta
        COSMICO(30, "&#00E5FF");    // Cian Ciberpunk

        private final int poderNexo;
        private final String color;

        Rareza(int poderNexo, String color) {
            this.poderNexo = poderNexo;
            this.color = color;
        }
        public int getPoderNexo() { return poderNexo; }
        public String getColor() { return color; }
    }

    public enum StatType {
        FUERZA, VIDA, VELOCIDAD, ENERGIA_CUSTOM, ARMADURA
    }
}