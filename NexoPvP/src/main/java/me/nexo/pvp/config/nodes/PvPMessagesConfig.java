package me.nexo.pvp.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

/**
 * 🏛️ Nexo Network - Configurate Type-Safe Node (PvP)
 * Esto convierte tu messages.yml de NexoPvP directamente en objetos de Java.
 */
@ConfigSerializable
public class PvPMessagesConfig {

    @Setting private MenusNode menus = new MenusNode();
    @Setting private MensajesNode mensajes = new MensajesNode();

    public MenusNode menus() { return menus; }
    public MensajesNode mensajes() { return mensajes; }

    // =========================================
    // NODO: Menus
    // =========================================
    @ConfigSerializable
    public static class MenusNode {
        @Setting private TemploNode templo = new TemploNode();
        public TemploNode templo() { return templo; }
    }

    @ConfigSerializable
    public static class TemploNode {
        @Setting private String titulo = "&#ff00ff✧ &#00f5ffTemplo del Vacío";
        @Setting private ItemsNode items = new ItemsNode();

        public String titulo() { return titulo; }
        public ItemsNode items() { return items; }
    }

    @ConfigSerializable
    public static class ItemsNode {
        @Setting("bendicion-menor")
        private ItemData bendicionMenor = new ItemData("&#00f5ffBendición Menor", List.of("&#E6CCFFProtección básica"));

        @Setting("bendicion-premium")
        private ItemData bendicionPremium = new ItemData("&#ff00ffBendición Premium", List.of("&#E6CCFFProtección total"));

        public ItemData bendicionMenor() { return bendicionMenor; }
        public ItemData bendicionPremium() { return bendicionPremium; }
    }

    @ConfigSerializable
    public static class ItemData {
        @Setting private String nombre;
        @Setting private List<String> lore;

        public ItemData() {} // Obligatorio para Configurate
        public ItemData(String nombre, List<String> lore) { this.nombre = nombre; this.lore = lore; }

        public String nombre() { return nombre; }
        public List<String> lore() { return lore; }
    }

    // =========================================
    // NODO: Mensajes
    // =========================================
    @ConfigSerializable
    public static class MensajesNode {
        @Setting private ErroresNode errores = new ErroresNode();
        @Setting private ExitoNode exito = new ExitoNode();
        @Setting private PenalizacionesNode penalizaciones = new PenalizacionesNode();

        public ErroresNode errores() { return errores; }
        public ExitoNode exito() { return exito; }
        public PenalizacionesNode penalizaciones() { return penalizaciones; }
    }

    @ConfigSerializable
    public static class ErroresNode {
        @Setting("bendicion-activa") private String bendicionActiva = "&#8b0000[!] Ya tienes una bendición activa.";
        @Setting("sin-monedas") private String sinMonedas = "&#8b0000[!] No tienes suficientes monedas.";
        @Setting("sin-gemas") private String sinGemas = "&#8b0000[!] No tienes suficientes gemas.";
        @Setting("solo-jugadores") private String soloJugadores = "&#8b0000[!] Este comando solo puede ser usado por jugadores.";

        public String bendicionActiva() { return bendicionActiva; }
        public String sinMonedas() { return sinMonedas; }
        public String sinGemas() { return sinGemas; }
        public String soloJugadores() { return soloJugadores; }
    }

    @ConfigSerializable
    public static class ExitoNode {
        @Setting("compra-menor") private String compraMenor = "&#00f5ff[✓] Has adquirido la Bendición Menor.";
        @Setting("compra-premium") private String compraPremium = "&#ff00ff[✓] Has adquirido la Bendición Premium.";

        public String compraMenor() { return compraMenor; }
        public String compraPremium() { return compraPremium; }
    }

    @ConfigSerializable
    public static class PenalizacionesNode {
        @Setting("muerte-protegida") private String muerteProtegida = "&#00f5ff✧ Tu bendición te ha protegido de la penalización de muerte.";
        @Setting("cobro-resurreccion") private String cobroResurreccion = "&#8b0000[!] Has pagado %amount% monedas al morir.";
        @Setting("perdida-progreso") private String perdidaProgreso = "&#8b0000[!] Has perdido parte de tu progreso por morir.";
        @Setting("consejo-bendicion") private String consejoBendicion = "&#E6CCFFConsejo: Usa una Esencia del Vacío en el Templo para protegerte.";

        public String muerteProtegida() { return muerteProtegida; }
        public String cobroResurreccion() { return cobroResurreccion; }
        public String perdidaProgreso() { return perdidaProgreso; }
        public String consejoBendicion() { return consejoBendicion; }
    }
}