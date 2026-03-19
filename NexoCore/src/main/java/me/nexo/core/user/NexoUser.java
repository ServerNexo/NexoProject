package me.nexo.core.user;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class NexoUser {

    private final UUID uuid;
    private final String nombre;

    // Estadísticas seguras para múltiples hilos
    private final AtomicInteger nexoNivel;
    private final AtomicInteger nexoXp;
    private final AtomicInteger combateNivel;
    private final AtomicInteger combateXp;
    private final AtomicInteger mineriaNivel;
    private final AtomicInteger mineriaXp;
    private final AtomicInteger agriculturaNivel;
    private final AtomicInteger agriculturaXp;

    // Variables temporales de sesión (no se guardan en BD de la misma forma)
    private final AtomicInteger energiaMineria;
    private final AtomicInteger energiaExtraAccesorios;
    private String claseJugador; // Guardará el nombre de su clase RPG si tiene

    public NexoUser(UUID uuid, String nombre, int nNivel, int nXp, int cNivel, int cXp, int mNivel, int mXp, int aNivel, int aXp) {
        this.uuid = uuid;
        this.nombre = nombre;
        this.nexoNivel = new AtomicInteger(nNivel);
        this.nexoXp = new AtomicInteger(nXp);
        this.combateNivel = new AtomicInteger(cNivel);
        this.combateXp = new AtomicInteger(cXp);
        this.mineriaNivel = new AtomicInteger(mNivel);
        this.mineriaXp = new AtomicInteger(mXp);
        this.agriculturaNivel = new AtomicInteger(aNivel);
        this.agriculturaXp = new AtomicInteger(aXp);

        // Valores por defecto al iniciar sesión
        this.energiaMineria = new AtomicInteger(100 + ((nNivel - 1) * 20));
        this.energiaExtraAccesorios = new AtomicInteger(0);
        this.claseJugador = "Ninguna";
    }

    // Getters básicos
    public UUID getUuid() { return uuid; }
    public String getNombre() { return nombre; }
    public String getClaseJugador() { return claseJugador; }
    public void setClaseJugador(String clase) { this.claseJugador = clase; }

    // Getters de Niveles y XP
    public int getNexoNivel() { return nexoNivel.get(); }
    public int getNexoXp() { return nexoXp.get(); }
    public int getCombateNivel() { return combateNivel.get(); }
    public int getCombateXp() { return combateXp.get(); }
    public int getMineriaNivel() { return mineriaNivel.get(); }
    public int getMineriaXp() { return mineriaXp.get(); }
    public int getAgriculturaNivel() { return agriculturaNivel.get(); }
    public int getAgriculturaXp() { return agriculturaXp.get(); }

    public int getEnergiaMineria() { return energiaMineria.get(); }
    public int getEnergiaExtraAccesorios() { return energiaExtraAccesorios.get(); }

    // Setters Seguros
    public void setNexoNivel(int valor) { this.nexoNivel.set(valor); }
    public void addNexoXp(int cantidad) { this.nexoXp.addAndGet(cantidad); }
    public void setNexoXp(int valor) { this.nexoXp.set(valor); }

    public void setCombateNivel(int valor) { this.combateNivel.set(valor); }
    public void addCombateXp(int cantidad) { this.combateXp.addAndGet(cantidad); }
    public void setCombateXp(int valor) { this.combateXp.set(valor); }

    public void setMineriaNivel(int valor) { this.mineriaNivel.set(valor); }
    public void addMineriaXp(int cantidad) { this.mineriaXp.addAndGet(cantidad); }
    public void setMineriaXp(int valor) { this.mineriaXp.set(valor); }

    public void setAgriculturaNivel(int valor) { this.agriculturaNivel.set(valor); }
    public void addAgriculturaXp(int cantidad) { this.agriculturaXp.addAndGet(cantidad); }
    public void setAgriculturaXp(int valor) { this.agriculturaXp.set(valor); }

    public void setEnergiaMineria(int valor) { this.energiaMineria.set(valor); }
    public void setEnergiaExtraAccesorios(int valor) { this.energiaExtraAccesorios.set(valor); }
}