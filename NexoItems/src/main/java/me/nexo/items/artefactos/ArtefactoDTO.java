package me.nexo.items.artefactos;

public record ArtefactoDTO(
        String id,
        String name,
        Rareza rarity,
        int cost,
        int cooldown,
        HabilidadType type,
        double power
) {
    // Definimos las rarezas con sus respectivos colores según tu prompt
    public enum Rareza {
        COMUN("§7"),       // Gris
        RARO("§9"),        // Azul
        EPICO("§5"),       // Púrpura
        LEGENDARIO("§6"),  // Oro
        MITICO("§c"),      // Rojo
        COSMICO("§b");     // Aqua/Celeste

        private final String color;

        Rareza(String color) {
            this.color = color;
        }

        public String getColor() {
            return color;
        }
    }

    // Definimos el tipo de ejecución de la habilidad
    public enum HabilidadType {
        ACTIVA,       // Se usa una vez y entra en cooldown
        TOGGLE,       // Se prende y se apaga (como las Alas)
        DESPLIEGUE    // Invoca una entidad temporal (como el Orbe)
    }
}