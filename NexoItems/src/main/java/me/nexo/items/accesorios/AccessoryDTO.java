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
        COMUN(3, "§7"), RARO(8, "§9"), EPICO(12, "§5"),
        LEGENDARIO(16, "§6"), MITICO(22, "§c"), COSMICO(30, "§b");

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