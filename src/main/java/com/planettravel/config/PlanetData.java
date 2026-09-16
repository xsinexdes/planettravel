package com.planettravel.config;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * Representa toda la configuracion de UN planeta:
 *  - donde esta su esfera dentro del mundo "espacio" (centro + radio)
 *  - a que mundo-planeta corresponde
 *  - las alturas de entrada atmosferica / despegue
 *  - los puntos de aparicion en cada lado del viaje
 *  - su {@link PlanetEnvironment} (atmosfera, gravedad, temperatura, agua...)
 *
 * Es un contenedor de datos. La carga/guardado vive en {@link PlanetConfigManager},
 * la deteccion en listeners/, el teletransporte en teleport/ y la simulacion
 * ambiental en environment/.
 */
public class PlanetData {

    private final String id;
    private String worldName;

    /** Nombre bonito mostrado en la GUI y los mensajes (por defecto, el id). */
    private String nombreVisible;

    /** Material usado como icono en el mapa estelar. */
    private String iconoMaterial = "RED_CONCRETE";

    /** Descripcion corta mostrada en el mapa estelar. */
    private String descripcion = "";

    // --- Esfera en el mundo "espacio" ---
    private double spaceX;
    private double spaceY;
    private double spaceZ;
    private double radio;

    /**
     * Radio VISUAL de la esfera de bloques construida con WorldEdit.
     * Es distinto del radio de deteccion: el de deteccion debe ser MAYOR
     * para que el teletransporte salte antes de que la nave choque contra
     * los bloques. Solo se usa para dibujar el borde y para avisar al admin
     * si configura un radio de deteccion menor que la esfera fisica.
     */
    private double radioVisual;

    // --- Aterrizaje (espacio -> planeta) ---
    private double entradaAtmosfericaY;
    private double spawnPlanetaX;
    private double spawnPlanetaY;
    private double spawnPlanetaZ;
    private float spawnPlanetaYaw;
    private float spawnPlanetaPitch;

    // --- Despegue (planeta -> espacio) ---
    private double alturaDespegueY;
    private double salidaEspacioX;
    private double salidaEspacioY;
    private double salidaEspacioZ;
    private float salidaEspacioYaw;
    private float salidaEspacioPitch;

    // --- Entorno ---
    private PlanetEnvironment entorno = new PlanetEnvironment();

    public PlanetData(String id) {
        this.id = id;
        this.nombreVisible = id;
    }

    // ---------- Identidad ----------

    public String getId() { return id; }

    public String getNombreVisible() {
        return (nombreVisible == null || nombreVisible.isEmpty()) ? id : nombreVisible;
    }

    public void setNombreVisible(String v) { this.nombreVisible = v; }

    public String getIconoMaterial() { return iconoMaterial; }

    public void setIconoMaterial(String v) { this.iconoMaterial = v; }

    /** Devuelve el Material del icono, o un fallback seguro si el nombre no es valido. */
    public Material getIcono() {
        Material m = Material.matchMaterial(iconoMaterial);
        return (m == null) ? Material.RED_CONCRETE : m;
    }

    public String getDescripcion() { return descripcion; }

    public void setDescripcion(String v) { this.descripcion = v; }

    public String getWorldName() { return worldName; }

    public void setWorldName(String worldName) { this.worldName = worldName; }

    public PlanetEnvironment getEntorno() { return entorno; }

    public void setEntorno(PlanetEnvironment entorno) { this.entorno = entorno; }

    // ---------- Esfera en el espacio ----------

    public double getSpaceX() { return spaceX; }
    public double getSpaceY() { return spaceY; }
    public double getSpaceZ() { return spaceZ; }
    public double getRadio() { return radio; }

    public double getRadioVisual() { return radioVisual; }
    public void setRadioVisual(double v) { this.radioVisual = v; }

    public void setEsferaEspacio(double x, double y, double z, double radio) {
        this.spaceX = x;
        this.spaceY = y;
        this.spaceZ = z;
        this.radio = radio;
    }

    /**
     * Distancia al cuadrado (sin raiz, mas barato) entre un punto y el CENTRO
     * de la esfera. Se compara con radio^2 para saber si el punto esta dentro
     * sin gastar una raiz cuadrada en cada comprobacion — esto corre muy a
     * menudo, sobre jugadores en movimiento.
     */
    public double distanciaCuadradaAlCentro(double x, double y, double z) {
        double dx = x - spaceX;
        double dy = y - spaceY;
        double dz = z - spaceZ;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Distancia real (con raiz). Solo para mostrarla al jugador, no para detectar. */
    public double distanciaAlCentro(double x, double y, double z) {
        return Math.sqrt(distanciaCuadradaAlCentro(x, y, z));
    }

    /** true si el punto esta dentro del radio de DETECCION de la esfera. */
    public boolean estaDentroDeEsfera(double x, double y, double z) {
        return distanciaCuadradaAlCentro(x, y, z) <= (radio * radio);
    }

    // ---------- Puntos de viaje ----------

    public double getEntradaAtmosfericaY() { return entradaAtmosfericaY; }
    public void setEntradaAtmosfericaY(double v) { this.entradaAtmosfericaY = v; }

    public double getAlturaDespegueY() { return alturaDespegueY; }
    public void setAlturaDespegueY(double v) { this.alturaDespegueY = v; }

    public double getSpawnPlanetaX() { return spawnPlanetaX; }
    public double getSpawnPlanetaY() { return spawnPlanetaY; }
    public double getSpawnPlanetaZ() { return spawnPlanetaZ; }
    public float getSpawnPlanetaYaw() { return spawnPlanetaYaw; }
    public float getSpawnPlanetaPitch() { return spawnPlanetaPitch; }

    public void setSpawnPlaneta(double x, double y, double z, float yaw, float pitch) {
        this.spawnPlanetaX = x;
        this.spawnPlanetaY = y;
        this.spawnPlanetaZ = z;
        this.spawnPlanetaYaw = yaw;
        this.spawnPlanetaPitch = pitch;
    }

    public double getSalidaEspacioX() { return salidaEspacioX; }
    public double getSalidaEspacioY() { return salidaEspacioY; }
    public double getSalidaEspacioZ() { return salidaEspacioZ; }
    public float getSalidaEspacioYaw() { return salidaEspacioYaw; }
    public float getSalidaEspacioPitch() { return salidaEspacioPitch; }

    public void setSalidaEspacio(double x, double y, double z, float yaw, float pitch) {
        this.salidaEspacioX = x;
        this.salidaEspacioY = y;
        this.salidaEspacioZ = z;
        this.salidaEspacioYaw = yaw;
        this.salidaEspacioPitch = pitch;
    }

    public Location getSpawnPlanetaLocation(World world) {
        return new Location(world, spawnPlanetaX, spawnPlanetaY, spawnPlanetaZ, spawnPlanetaYaw, spawnPlanetaPitch);
    }

    public Location getSalidaEspacioLocation(World espacio) {
        return new Location(espacio, salidaEspacioX, salidaEspacioY, salidaEspacioZ, salidaEspacioYaw, salidaEspacioPitch);
    }

    // ---------- Validaciones anti-bucle ----------

    /**
     * true si el punto de salida al espacio cae DENTRO del radio de deteccion
     * de este mismo planeta. Eso provocaria un bucle: despegas, apareces dentro
     * de tu propia esfera y el detector vuelve a aterrizarte en cuanto expira
     * el cooldown. El plugin avisa de esto al arrancar y al editar.
     */
    public boolean salidaEstaDentroDeSuPropiaEsfera() {
        return estaDentroDeEsfera(salidaEspacioX, salidaEspacioY, salidaEspacioZ);
    }

    /** true si el radio de deteccion es menor o igual que la esfera fisica de bloques. */
    public boolean radioDeteccionEsDemasiadoPequenio() {
        return radioVisual > 0 && radio <= radioVisual;
    }
}
