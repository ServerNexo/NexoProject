package me.nexo.items.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

/**
 * 🎒 NexoItems - Configurate Type-Safe Node (Arquitectura Enterprise)
 */
@ConfigSerializable
public class ItemsMessagesConfig {

    @Setting private MensajesNode mensajes = new MensajesNode();
    @Setting private ComandosNode comandos = new ComandosNode();
    @Setting private EstacionesNode estaciones = new EstacionesNode();
    @Setting private MochilasNode mochilas = new MochilasNode();
    @Setting private MenusNode menus = new MenusNode(); // 🌟 NODO DE MENÚS AÑADIDO

    public MensajesNode mensajes() { return mensajes; }
    public ComandosNode comandos() { return comandos; }
    public EstacionesNode estaciones() { return estaciones; }
    public MochilasNode mochilas() { return mochilas; }
    public MenusNode menus() { return menus; } // 🌟 GETTER AÑADIDO

    // =========================================
    // 🗂️ NODO: Menus (Inventarios Custom)
    // =========================================
    @ConfigSerializable
    public static class MenusNode {
        @Setting private DesguaceMenuNode desguace = new DesguaceMenuNode();
        public DesguaceMenuNode desguace() { return desguace; }
    }

    @ConfigSerializable
    public static class DesguaceMenuNode {
        @Setting private String titulo = "&#ff00ff♻ <bold>MESA DE DESGUACE</bold>";
        @Setting private BotonNode boton = new BotonNode();

        public String titulo() { return titulo; }
        public BotonNode boton() { return boton; }
    }

    @ConfigSerializable
    public static class BotonNode {
        @Setting private String titulo = "&#8b0000🔥 <bold>DESGUACAR ARTEFACTOS</bold>";
        @Setting private List<String> lore = List.of(
                "&#E6CCFFColoca aquí los artefactos que",
                "&#E6CCFFdesees reducir a materia prima.",
                "",
                "&#FF3366[!] Los ítems se perderán para siempre.",
                "&#00f5ff► Clic para desguacar"
        );

        public String titulo() { return titulo; }
        public List<String> lore() { return lore; }
    }

    // =========================================
    // 🗂️ NODO: Mensajes Generales
    // =========================================
    @ConfigSerializable
    public static class MensajesNode {
        @Setting private ErroresNode errores = new ErroresNode();
        @Setting private ExitoNode exito = new ExitoNode();

        public ErroresNode errores() { return errores; }
        public ExitoNode exito() { return exito; }
    }

    @ConfigSerializable
    public static class ErroresNode {
        @Setting("sin-permiso") private String sinPermiso = "&#FF3366[!] El Vacío rechaza tu petición (Sin Permisos).";
        @Setting("jugador-offline") private String jugadorOffline = "&#FF3366[!] Ese jugador no se encuentra en este plano.";
        @Setting("item-invalido") private String itemInvalido = "&#FF3366[!] Este artefacto no es compatible o no existe.";
        @Setting("inventario-lleno") private String inventarioLleno = "&#FF3366[!] Tu inventario está lleno. Artefacto caído al suelo.";

        public String sinPermiso() { return sinPermiso; }
        public String jugadorOffline() { return jugadorOffline; }
        public String itemInvalido() { return itemInvalido; }
        public String inventarioLleno() { return inventarioLleno; }
    }

    @ConfigSerializable
    public static class ExitoNode {
        @Setting("recarga-exitosa") private String recargaExitosa = "&#9933FF[✓] <bold>TEXTOS SAGRADOS RENOVADOS:</bold> &#E6CCFFMecánicas de ítems recargadas.";
        public String recargaExitosa() { return recargaExitosa; }
    }

    // =========================================
    // 🗂️ NODO: Comandos
    // =========================================
    @ConfigSerializable
    public static class ComandosNode {
        @Setting("item-otorgado") private String itemOtorgado = "&#00f5ff[📦] Artefacto conjurado y entregado a %player%.";
        @Setting("item-recibido") private String itemRecibido = "&#00f5ff[📦] Has recibido un nuevo Artefacto: &#E6CCFF%item%";

        public String itemOtorgado() { return itemOtorgado; }
        public String itemRecibido() { return itemRecibido; }
    }

    // =========================================
    // 🗂️ NODO: Estaciones (Forja, Desguace, etc)
    // =========================================
    @ConfigSerializable
    public static class EstacionesNode {
        @Setting("mejora-exitosa") private String mejoraExitosa = "&#00f5ff✨ <bold>FORJA EXITOSA:</bold> &#E6CCFFTu equipamiento ha ascendido de nivel.";
        @Setting("mejora-fallida") private String mejoraFallida = "&#8b0000[!] La forja ha fallado. Los materiales se han perdido.";
        @Setting("desguace-exitoso") private String desguaceExitoso = "&#ff00ff[♻] <bold>DESGUACE COMPLETADO:</bold> &#E6CCFFMateria prima recuperada.";

        public String mejoraExitosa() { return mejoraExitosa; }
        public String mejoraFallida() { return mejoraFallida; }
        public String desguaceExitoso() { return desguaceExitoso; }
    }

    // =========================================
    // 🗂️ NODO: Mochilas y Guardarropa
    // =========================================
    @ConfigSerializable
    public static class MochilasNode {
        @Setting("abriendo-mochila") private String abriendoMochila = "&#9933FFAbriendo Bóveda del Vacío #%id%...";
        @Setting("mochila-bloqueada") private String mochilaBloqueada = "&#FF3366[!] Aún no has desbloqueado esta bóveda.";

        public String abriendoMochila() { return abriendoMochila; }
        public String mochilaBloqueada() { return mochilaBloqueada; }
    }
}