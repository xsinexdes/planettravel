package com.planettravel.config;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    /**
     * Descripcion en varias lineas (admite codigos de color con &). Se muestra
     * en el mapa estelar y en la ficha del planeta.
     */
    private List<String> descripcion = new ArrayList<>();

    /** Etiqueta corta del tipo de mundo: "Desertico", "Gigante gaseoso"... */
    private String clasificacion = "";

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

    // --- Aterrizaje (espacio -> planeta): ZONA definida por 2 esquinas con el hacha ---
    // "Esquina A" (clic izquierdo) y "Esquina B" (clic derecho), igual que la
    // seleccion de area normal. El aterrizaje elige un punto X/Z aleatorio
    // dentro de esa caja: asi cada nave entra en la atmosfera en un sitio
    // distinto (menos colisiones) pero siempre sobre el mismo planeta fisico.
    private double entradaAtmosfericaY;
    private double spawnPlanetaX;
    private double spawnPlanetaY;
    private double spawnPlanetaZ;
    private float spawnPlanetaYaw;
    private float spawnPlanetaPitch;
    private double spawnPlanetaX2;
    private double spawnPlanetaY2;
    private double spawnPlanetaZ2;

    // --- Despegue (planeta -> espacio): ZONA (3D) definida por 2 esquinas con el hacha ---
    // El despegue elige un punto aleatorio dentro de esta caja, siempre cerca
    // del planeta fisico (la esfera de bloques), como saldria una nave real.
    private double alturaDespegueY;
    private double salidaEspacioX;
    private double salidaEspacioY;
    private double salidaEspacioZ;
    private float salidaEspacioYaw;
    private float salidaEspacioPitch;
    private double salidaEspacioX2;
    private double salidaEspacioY2;
    private double salidaEspacioZ2;

    /**
     * true solo cuando el punto de entrada/salida se ha marcado de verdad con
     * el hacha de seleccion (o desde el editor, que usa el mismo mecanismo).
     * Mientras falte alguno de los dos, el planeta no admite aterrizajes ni
     * despegues: evita que un planeta recien creado tenga un punto de
     * aparicion "0,0,0" sin sentido.
     */
    private boolean entradaLista = false;
    private boolean salidaLista = false;

    /** Si esta activo, la entrada ignora la zona del hacha y usa el WorldBorder del mundo (ver abajo). */
    private boolean entradaUsaWorldBorder = false;

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

    public List<String> getDescripcionLineas() { return descripcion; }

    /** Descripcion completa en una sola cadena (lineas unidas por espacio). */
    public String getDescripcion() { return String.join(" ", descripcion); }

    /** Sustituye la descripcion. Un '|' en el texto separa lineas. */
    public void setDescripcion(String texto) {
        descripcion = new ArrayList<>();
        if (texto == null || texto.isBlank()) return;
        for (String linea : texto.split("\\|")) {
            if (!linea.isBlank()) descripcion.add(linea.trim());
        }
    }

    public void setDescripcionLineas(List<String> lineas) {
        descripcion = new ArrayList<>(lineas == null ? List.of() : lineas);
    }

    public void anadirLineaDescripcion(String linea) {
        if (linea != null && !linea.isBlank()) descripcion.add(linea.trim());
    }

    public void limpiarDescripcion() { descripcion.clear(); }

    public String getClasificacion() { return clasificacion == null ? "" : clasificacion; }

    public void setClasificacion(String v) { this.clasificacion = v == null ? "" : v.trim(); }

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

    // --- Zona de deteccion alternativa: CAJA marcada con el hacha (2 esquinas) ---
    // Por defecto la deteccion es la esfera de arriba (radio alrededor del
    // centro). Si se activa este modo, en vez de eso se usa una caja
    // rectangular: entras por CUALQUIER lado (arriba, abajo, izquierda,
    // derecha...) mientras estes dentro de esas 2 esquinas, como una region
    // de WorldGuard.
    private boolean deteccionUsaCaja = false;
    private boolean deteccionCajaLista = false;
    private double deteccionCajaX, deteccionCajaY, deteccionCajaZ;
    private double deteccionCajaX2, deteccionCajaY2, deteccionCajaZ2;

    public boolean isDeteccionUsaCaja() { return deteccionUsaCaja; }
    public void setDeteccionUsaCaja(boolean v) { this.deteccionUsaCaja = v; }

    public boolean isDeteccionCajaLista() { return deteccionCajaLista; }
    public void setDeteccionCajaLista(boolean v) { this.deteccionCajaLista = v; }

    public double getDeteccionCajaX() { return deteccionCajaX; }
    public double getDeteccionCajaY() { return deteccionCajaY; }
    public double getDeteccionCajaZ() { return deteccionCajaZ; }
    public double getDeteccionCajaX2() { return deteccionCajaX2; }
    public double getDeteccionCajaY2() { return deteccionCajaY2; }
    public double getDeteccionCajaZ2() { return deteccionCajaZ2; }

    /** Esquina A de la caja de deteccion (primer clic del hacha). */
    public void setDeteccionCaja(double x, double y, double z) {
        this.deteccionCajaX = x;
        this.deteccionCajaY = y;
        this.deteccionCajaZ = z;
        this.deteccionCajaX2 = x;
        this.deteccionCajaY2 = y;
        this.deteccionCajaZ2 = z;
    }

    /** Esquina B de la caja de deteccion (segundo clic del hacha). */
    public void setDeteccionCajaEsquinaB(double x, double y, double z) {
        this.deteccionCajaX2 = x;
        this.deteccionCajaY2 = y;
        this.deteccionCajaZ2 = z;
    }

    public double getDeteccionCajaAnchoX() { return Math.abs(deteccionCajaX2 - deteccionCajaX); }
    public double getDeteccionCajaAnchoY() { return Math.abs(deteccionCajaY2 - deteccionCajaY); }
    public double getDeteccionCajaAnchoZ() { return Math.abs(deteccionCajaZ2 - deteccionCajaZ); }

    /** true si el punto esta dentro de la caja de deteccion marcada con el hacha. */
    public boolean estaDentroDeCajaDeteccion(double x, double y, double z) {
        double minX = Math.min(deteccionCajaX, deteccionCajaX2);
        double maxX = Math.max(deteccionCajaX, deteccionCajaX2);
        double minY = Math.min(deteccionCajaY, deteccionCajaY2);
        double maxY = Math.max(deteccionCajaY, deteccionCajaY2);
        double minZ = Math.min(deteccionCajaZ, deteccionCajaZ2);
        double maxZ = Math.max(deteccionCajaZ, deteccionCajaZ2);
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /**
     * Punto de "esta lo bastante cerca para aterrizar" real: usa la caja
     * marcada con el hacha si {@link #isDeteccionUsaCaja()} esta activo (y ya
     * se marco algo), o si no la esfera de {@link #getRadio()} de siempre.
     * TODO el codigo que comprueba cercania (deteccion de aterrizaje,
     * validaciones anti-bucle) pasa por aqui en vez de llamar directamente a
     * estaDentroDeEsfera, para que ambos modos se comporten igual en todos lados.
     */
    public boolean estaEnZonaDeDeteccion(double x, double y, double z) {
        if (deteccionUsaCaja && deteccionCajaLista) {
            return estaDentroDeCajaDeteccion(x, y, z);
        }
        return estaDentroDeEsfera(x, y, z);
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
    public double getSpawnPlanetaX2() { return spawnPlanetaX2; }
    public double getSpawnPlanetaY2() { return spawnPlanetaY2; }
    public double getSpawnPlanetaZ2() { return spawnPlanetaZ2; }

    /** Esquina A de la zona de entrada (primer clic del hacha). */
    public void setSpawnPlaneta(double x, double y, double z, float yaw, float pitch) {
        this.spawnPlanetaX = x;
        this.spawnPlanetaY = y;
        this.spawnPlanetaZ = z;
        this.spawnPlanetaYaw = yaw;
        this.spawnPlanetaPitch = pitch;
        // Por defecto la esquina B coincide con la A (zona de tamano 0 = un
        // solo punto) hasta que se marque la segunda esquina.
        this.spawnPlanetaX2 = x;
        this.spawnPlanetaY2 = y;
        this.spawnPlanetaZ2 = z;
    }

    /** Esquina B de la zona de entrada (segundo clic del hacha). */
    public void setSpawnPlanetaEsquinaB(double x, double y, double z) {
        this.spawnPlanetaX2 = x;
        this.spawnPlanetaY2 = y;
        this.spawnPlanetaZ2 = z;
    }

    public double getSalidaEspacioX() { return salidaEspacioX; }
    public double getSalidaEspacioY() { return salidaEspacioY; }
    public double getSalidaEspacioZ() { return salidaEspacioZ; }
    public float getSalidaEspacioYaw() { return salidaEspacioYaw; }
    public float getSalidaEspacioPitch() { return salidaEspacioPitch; }
    public double getSalidaEspacioX2() { return salidaEspacioX2; }
    public double getSalidaEspacioY2() { return salidaEspacioY2; }
    public double getSalidaEspacioZ2() { return salidaEspacioZ2; }

    /** Esquina A de la zona de salida (primer clic del hacha). */
    public void setSalidaEspacio(double x, double y, double z, float yaw, float pitch) {
        this.salidaEspacioX = x;
        this.salidaEspacioY = y;
        this.salidaEspacioZ = z;
        this.salidaEspacioYaw = yaw;
        this.salidaEspacioPitch = pitch;
        this.salidaEspacioX2 = x;
        this.salidaEspacioY2 = y;
        this.salidaEspacioZ2 = z;
    }

    /** Esquina B de la zona de salida (segundo clic del hacha). */
    public void setSalidaEspacioEsquinaB(double x, double y, double z) {
        this.salidaEspacioX2 = x;
        this.salidaEspacioY2 = y;
        this.salidaEspacioZ2 = z;
    }

    public Location getSpawnPlanetaLocation(World world) {
        return new Location(world, spawnPlanetaX, spawnPlanetaY, spawnPlanetaZ, spawnPlanetaYaw, spawnPlanetaPitch);
    }

    public Location getSalidaEspacioLocation(World espacio) {
        return new Location(espacio, salidaEspacioX, salidaEspacioY, salidaEspacioZ, salidaEspacioYaw, salidaEspacioPitch);
    }

    /** Tamano (en bloques, por eje X/Z) de la zona de aterrizaje marcada con el hacha. */
    public double getEntradaZonaAnchoX() { return Math.abs(spawnPlanetaX2 - spawnPlanetaX); }
    public double getEntradaZonaAnchoZ() { return Math.abs(spawnPlanetaZ2 - spawnPlanetaZ); }

    /** Tamano (en bloques, por eje) de la zona de despegue marcada con el hacha. */
    public double getSalidaZonaAnchoX() { return Math.abs(salidaEspacioX2 - salidaEspacioX); }
    public double getSalidaZonaAnchoY() { return Math.abs(salidaEspacioY2 - salidaEspacioY); }
    public double getSalidaZonaAnchoZ() { return Math.abs(salidaEspacioZ2 - salidaEspacioZ); }

    /**
     * Punto aleatorio dentro de la zona de aterrizaje (solo varia X/Z: la
     * altura la decide siempre la entrada atmosferica configurada). Es lo que
     * hace que cada nave "aterrice" (entre en la atmosfera) en un sitio
     * distinto de la zona marcada, en vez de siempre el mismo punto exacto.
     */
    public Location puntoEntradaAleatorio(World mundo, double y) {
        double minX = Math.min(spawnPlanetaX, spawnPlanetaX2);
        double maxX = Math.max(spawnPlanetaX, spawnPlanetaX2);
        double minZ = Math.min(spawnPlanetaZ, spawnPlanetaZ2);
        double maxZ = Math.max(spawnPlanetaZ, spawnPlanetaZ2);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double x = (maxX > minX) ? r.nextDouble(minX, maxX) : minX;
        double z = (maxZ > minZ) ? r.nextDouble(minZ, maxZ) : minZ;
        return new Location(mundo, x, y, z, spawnPlanetaYaw, spawnPlanetaPitch);
    }

    /**
     * true si la entrada debe ser aleatoria dentro del WorldBorder del mundo
     * del planeta (el que pongas con /worldborder), en vez de dentro de la
     * zona marcada a mano con el hacha. Mas comodo cuando el "planeta fisico"
     * ya ocupa el mundo entero.
     */
    public boolean isEntradaUsaWorldBorder() { return entradaUsaWorldBorder; }
    public void setEntradaUsaWorldBorder(boolean v) { this.entradaUsaWorldBorder = v; }

    /**
     * Punto aleatorio X/Z dentro del WorldBorder actual del mundo (con un
     * pequenio margen de seguridad hacia dentro, para no aparecer pegado al
     * borde). Si el admin no configuro un WorldBorder, Bukkit ya trae uno por
     * defecto enorme (unos 60 millones de bloques): se avisa de esto al
     * activar el modo, para que el admin ponga uno razonable con /worldborder.
     */
    public Location puntoEntradaAleatorioWorldBorder(World mundo, double y) {
        org.bukkit.WorldBorder borde = mundo.getWorldBorder();
        org.bukkit.Location centro = borde.getCenter();
        // Margen del 8% hacia dentro: evita aparecer justo en el limite del
        // borde (donde puede haber danio de borde o chunks sin generar).
        double mitad = (borde.getSize() / 2.0) * 0.92;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double x = centro.getX() + (r.nextDouble() * 2 - 1) * mitad;
        double z = centro.getZ() + (r.nextDouble() * 2 - 1) * mitad;
        return new Location(mundo, x, y, z, spawnPlanetaYaw, spawnPlanetaPitch);
    }

    /**
     * Punto aleatorio dentro de la zona de despegue en el espacio (3D
     * completo: X, Y y Z), siempre cerca del planeta fisico, como si una nave
     * real saliera desde un punto distinto cada vez alrededor de el.
     */
    public Location puntoSalidaAleatorio(World mundo) {
        double minX = Math.min(salidaEspacioX, salidaEspacioX2);
        double maxX = Math.max(salidaEspacioX, salidaEspacioX2);
        double minY = Math.min(salidaEspacioY, salidaEspacioY2);
        double maxY = Math.max(salidaEspacioY, salidaEspacioY2);
        double minZ = Math.min(salidaEspacioZ, salidaEspacioZ2);
        double maxZ = Math.max(salidaEspacioZ, salidaEspacioZ2);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double x = (maxX > minX) ? r.nextDouble(minX, maxX) : minX;
        double y = (maxY > minY) ? r.nextDouble(minY, maxY) : minY;
        double z = (maxZ > minZ) ? r.nextDouble(minZ, maxZ) : minZ;
        return new Location(mundo, x, y, z, salidaEspacioYaw, salidaEspacioPitch);
    }

    public boolean isEntradaLista() { return entradaLista; }
    public void setEntradaLista(boolean v) { this.entradaLista = v; }

    public boolean isSalidaLista() { return salidaLista; }
    public void setSalidaLista(boolean v) { this.salidaLista = v; }

    /** true si la entrada ya esta lista para usarse, sea por zona (hacha) o por WorldBorder. */
    public boolean entradaConfigurada() { return entradaLista || entradaUsaWorldBorder; }

    /** true solo si tanto la entrada como la salida estan listas para usarse. */
    public boolean puedeViajar() { return entradaConfigurada() && salidaLista; }

    // ---------- Validaciones anti-bucle ----------

    /**
     * true si el CENTRO de la zona de salida al espacio cae DENTRO del radio
     * de deteccion de este mismo planeta. Eso provocaria un bucle: despegas,
     * apareces dentro de tu propia esfera y el detector vuelve a aterrizarte
     * en cuanto expira el cooldown. El plugin avisa de esto al arrancar y al
     * marcar la zona.
     */
    public boolean salidaEstaDentroDeSuPropiaEsfera() {
        return estaEnZonaDeDeteccion(getSalidaCentroX(), getSalidaCentroY(), getSalidaCentroZ());
    }

    public double getSalidaCentroX() { return (salidaEspacioX + salidaEspacioX2) / 2.0; }
    public double getSalidaCentroY() { return (salidaEspacioY + salidaEspacioY2) / 2.0; }
    public double getSalidaCentroZ() { return (salidaEspacioZ + salidaEspacioZ2) / 2.0; }

    /** true si el radio de deteccion es menor o igual que la esfera fisica de bloques. Solo aplica en modo esfera. */
    public boolean radioDeteccionEsDemasiadoPequenio() {
        return !deteccionUsaCaja && radioVisual > 0 && radio <= radioVisual;
    }
}
