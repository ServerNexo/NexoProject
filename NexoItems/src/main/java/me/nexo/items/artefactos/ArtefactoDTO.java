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
    // Definimos las rarezas con sus respectivos colores Ciberpunk
    public enum Rareza {
        COMUN("&#AAAAAA"),       // Gris Plomo
        RARO("&#5555FF"),        // Azul Neón
        EPICO("&#AA00AA"),       // Púrpura Profundo
        LEGENDARIO("&#FFAA00"),  // Naranja/Dorado Corporativo
        MITICO("&#FF5555"),      // Rojo Alerta
        COSMICO("&#00E5FF");     // Cian Ciberpunk

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