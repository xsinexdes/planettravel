package com.planettravel.space;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * BASE ESPACIAL: una zona cuboide (marcada con el hacha de seleccion) donde HAY
 * OXIGENO. Dentro se puede respirar sin traje, la temperatura es la de la base
 * y (opcionalmente) hay gravedad artificial. Fuera de la zona rigen las reglas
 * normales del espacio o del planeta donde este.
 *
 * ------------------------------------------------------------------
 *  PREPARADO PARA EL FUTURO: bases de jugadores con combustible
 * ------------------------------------------------------------------
 * Los campos tipo / propietario / requiereFuel / fuelActual / fuelMax /
 * consumoPorMinuto ya existen y se guardan en bases.yml, pero AHORA NO HACEN
 * NADA salvo por una cosa: {@link #tieneOxigeno()} ya tiene en cuenta el fuel.
 *
 * El dia que quieras bases de jugadores, solo hay que:
 *   1) crear una "maquina" (bloque/GUI) que llame a {@link #anadirFuel(double)},
 *   2) una tarea periodica que llame a {@link #consumirFuel(double)} cada minuto
 *      para las bases con requiereFuel=true,
 *   3) un comando o item para que un jugador cree una base propia (tipo JUGADOR).
 * Cuando el fuel llegue a 0, el oxigeno de esa base se apaga solo, sin tocar
 * nada del sistema de entorno.
 */
public class SpaceBase {

    public enum Tipo { ADMIN, JUGADOR }

    private final String id;
    private String nombre;
    private String mundo;
    private int minX, minY, minZ, maxX, maxY, maxZ;

    private boolean oxigeno = true;
    private double temperatura = 21.0;
    private boolean gravedadArtificial = true;

    // --- Futuro: bases de jugadores ---
    private Tipo tipo = Tipo.ADMIN;
    private UUID propietario = null;
    private boolean requiereFuel = false;
    private double fuelActual = 0.0;
    private double fuelMax = 1000.0;
    private double consumoPorMinuto = 1.0;

    public SpaceBase(String id, String nombre, String mundo,
                     int x1, int y1, int z1, int x2, int y2, int z2) {
        this.id = id;
        this.nombre = nombre;
        this.mundo = mundo;
        setArea(x1, y1, z1, x2, y2, z2);
    }

    public void setArea(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    /** true si la posicion cae dentro del cuboide (en el mundo de la base). */
    public boolean contiene(Location loc) {
        World w = loc.getWorld();
        if (w == null || !w.getName().equalsIgnoreCase(mundo)) {
            return false;
        }
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        return x >= minX && x < maxX + 1
                && y >= minY && y < maxY + 1
                && z >= minZ && z < maxZ + 1;
    }

    /**
     * true si ahora mismo hay aire respirable. Punto unico de decision: cuando
     * exista el sistema de fuel, una base sin combustible dejara de tener oxigeno.
     */
    public boolean tieneOxigeno() {
        return oxigeno && (!requiereFuel || fuelActual > 0);
    }

    // ---- Ganchos para el sistema de fuel (aun sin usar) ----

    public void anadirFuel(double cantidad) {
        fuelActual = Math.min(fuelMax, fuelActual + Math.max(0, cantidad));
    }

    public void consumirFuel(double cantidad) {
        fuelActual = Math.max(0, fuelActual - Math.max(0, cantidad));
    }

    // ---- Geometria ----

    public double getCentroX() { return (minX + maxX + 1) / 2.0; }
    public double getCentroY() { return (minY + maxY + 1) / 2.0; }
    public double getCentroZ() { return (minZ + maxZ + 1) / 2.0; }

    public int getAnchoX() { return maxX - minX + 1; }
    public int getAltoY() { return maxY - minY + 1; }
    public int getAnchoZ() { return maxZ - minZ + 1; }

    // ---- Getters / setters ----

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }
    public String getMundo() { return mundo; }
    public void setMundo(String v) { this.mundo = v; }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public boolean isOxigeno() { return oxigeno; }
    public void setOxigeno(boolean v) { this.oxigeno = v; }

    public double getTemperatura() { return temperatura; }
    public void setTemperatura(double v) { this.temperatura = Math.max(-200, Math.min(300, v)); }

    public boolean isGravedadArtificial() { return gravedadArtificial; }
    public void setGravedadArtificial(boolean v) { this.gravedadArtificial = v; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo v) { this.tipo = v; }

    public UUID getPropietario() { return propietario; }
    public void setPropietario(UUID v) { this.propietario = v; }

    public boolean isRequiereFuel() { return requiereFuel; }
    public void setRequiereFuel(boolean v) { this.requiereFuel = v; }

    public double getFuelActual() { return fuelActual; }
    public void setFuelActual(double v) { this.fuelActual = Math.max(0, v); }

    public double getFuelMax() { return fuelMax; }
    public void setFuelMax(double v) { this.fuelMax = Math.max(1, v); }

    public double getConsumoPorMinuto() { return consumoPorMinuto; }
    public void setConsumoPorMinuto(double v) { this.consumoPorMinuto = Math.max(0, v); }
}
