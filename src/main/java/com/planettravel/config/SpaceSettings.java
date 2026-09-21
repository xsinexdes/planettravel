package com.planettravel.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ajustes del propio ESPACIO (el mundo "espacio" donde flotan los planetas).
 *
 * El entorno del espacio es un {@link PlanetEnvironment} normal, asi que todo
 * lo que se puede hacer con un planeta (casco obligatorio, temperatura,
 * gravedad, presion...) tambien se puede hacer con el espacio, con los mismos
 * sistemas. Las bases espaciales (zonas con oxigeno) lo anulan dentro de su area.
 *
 * "activo" es un interruptor general: mientras este en false el espacio se
 * comporta como siempre (sin asfixia, sin frio...). Asi, actualizar el plugin
 * no cambia nada hasta que tu decidas activarlo desde /espacio.
 */
public class SpaceSettings {

    private boolean activo = false;
    private PlanetEnvironment entorno = crearEntornoEspacial();

    // --- Fuego ---
    private boolean prohibirFuegoSinOxigeno = true;

    // --- Mobs ---
    private boolean bloquearMobsNormales = true;
    private final List<String> mobsPermitidos = new ArrayList<>(
            List.of("ZOMBIE", "SKELETON", "STRAY", "HUSK", "ENDERMAN", "PHANTOM"));
    private boolean mobsConCasco = true;
    private String materialCascoMobs = "GLASS";
    private double probabilidadSoltarCasco = 0.02;

    /**
     * En vez de simplemente impedir que aparezcan mobs "normales" en el
     * espacio, si esto esta activo se TRANSFORMAN: cualquier mob que no este
     * en {@link #mobsPermitidos} (aparicion natural, spawner, huevo o
     * /summon) se cancela y en su lugar aparece este tipo de mob especifico
     * "adaptado al espacio". Con esto desactivado, el comportamiento clasico
     * (bloquear sin mas) sigue disponible via bloquearMobsNormales.
     */
    private boolean transformarMobsNormales = true;
    private String mobEspacial = "HUSK";

    // --- Ambiente ---
    private boolean estrellas = true;
    private int densidadEstrellas = 6;

    /** Lista de mobs razonables para transformar en algo "espacial" (rotan en la GUI). */
    public static final List<String> MOBS_ESPACIALES = List.of(
            "HUSK", "ZOMBIE", "SKELETON", "STRAY", "DROWNED", "ENDERMAN", "PHANTOM", "PIGLIN");

    // --- Tormentas solares ---
    private boolean tormentasSolares = false;
    private int tormentaIntervaloMin = 20;
    private int tormentaDuracionSeg = 40;
    private double tormentaDanio = 3.0;
    private int tormentaTierProteccion = 3;

    // --- Meteoritos en el espacio abierto (no solo en planetas) ---
    private boolean meteoritosEspacio = false;
    private int meteoritosEspacioIntervaloSeg = 45;
    private double danioMeteoritoEspacio = 5.0;

    // --- El sol: cuanto mas cerca en el espacio, mas calor; mas lejos, mas frio ---
    private boolean solActivo = false;
    private double solX = 0;
    private double solY = 200;
    private double solZ = 0;
    /** Temperatura justo al lado del sol. */
    private double solTemperaturaCercana = 120.0;
    /** Distancia (bloques) a partir de la cual ya no calienta mas y manda la temperatura base del espacio. */
    private double solDistanciaMaxima = 2000.0;

    /** Valores por defecto pensados para un espacio realmente hostil. */
    public static PlanetEnvironment crearEntornoEspacial() {
        PlanetEnvironment e = new PlanetEnvironment();
        e.setRequiereCasco(true);
        e.setTierCasco(2);
        e.setRequiereTrajeCompleto(true);
        e.setDanioSinCasco(2.0);
        e.setGravedad(0.2);
        e.setPresion(0.0);
        e.setTemperaturaActiva(true);
        e.setTemperaturaBase(-120.0);
        e.setVariacionDiaNoche(0.0);
        e.setVariacionPorAltura(0.0);
        e.setUmbralFrio(-60.0);
        e.setUmbralCalor(80.0);
        e.setDanioTemperatura(1.5);
        e.setBloquearClima(true);
        e.setBloquearHora(true);
        e.setHoraFija(18000);
        return e;
    }

    public void desdeConfig(ConfigurationSection s) {
        if (s == null) {
            return;
        }
        activo = s.getBoolean("activo", false);
        entorno = PlanetEnvironment.desdeConfig(s.getConfigurationSection("entorno"));
        if (s.getConfigurationSection("entorno") == null) {
            entorno = crearEntornoEspacial();
        }

        prohibirFuegoSinOxigeno = s.getBoolean("prohibir-fuego-sin-oxigeno", true);

        bloquearMobsNormales = s.getBoolean("mobs.bloquear-normales", true);
        if (s.isList("mobs.permitidos")) {
            mobsPermitidos.clear();
            for (String m : s.getStringList("mobs.permitidos")) {
                mobsPermitidos.add(m.toUpperCase(Locale.ROOT));
            }
        }
        mobsConCasco = s.getBoolean("mobs.con-casco", true);
        materialCascoMobs = s.getString("mobs.material-casco", "GLASS");
        probabilidadSoltarCasco = s.getDouble("mobs.probabilidad-soltar-casco", 0.02);

        estrellas = s.getBoolean("ambiente.estrellas", true);
        densidadEstrellas = Math.max(0, Math.min(40, s.getInt("ambiente.densidad-estrellas", 6)));

        tormentasSolares = s.getBoolean("tormentas-solares.activas", false);
        tormentaIntervaloMin = Math.max(1, s.getInt("tormentas-solares.intervalo-minutos", 20));
        tormentaDuracionSeg = Math.max(5, s.getInt("tormentas-solares.duracion-segundos", 40));
        tormentaDanio = s.getDouble("tormentas-solares.danio", 3.0);
        tormentaTierProteccion = Math.max(1, s.getInt("tormentas-solares.tier-proteccion", 3));

        transformarMobsNormales = s.getBoolean("mobs.transformar-normales", true);
        mobEspacial = s.getString("mobs.mob-espacial", "HUSK").toUpperCase(Locale.ROOT);

        meteoritosEspacio = s.getBoolean("meteoritos-espacio.activos", false);
        meteoritosEspacioIntervaloSeg = Math.max(5, s.getInt("meteoritos-espacio.intervalo-segundos", 45));
        danioMeteoritoEspacio = s.getDouble("meteoritos-espacio.danio", 5.0);

        solActivo = s.getBoolean("sol.activo", false);
        solX = s.getDouble("sol.x", 0);
        solY = s.getDouble("sol.y", 200);
        solZ = s.getDouble("sol.z", 0);
        solTemperaturaCercana = s.getDouble("sol.temperatura-cercana", 120.0);
        solDistanciaMaxima = Math.max(1, s.getDouble("sol.distancia-maxima", 2000.0));
    }

    public void guardarEn(ConfigurationSection s) {
        s.set("activo", activo);
        entorno.guardarEn(s.createSection("entorno"));

        s.set("prohibir-fuego-sin-oxigeno", prohibirFuegoSinOxigeno);

        s.set("mobs.bloquear-normales", bloquearMobsNormales);
        s.set("mobs.permitidos", new ArrayList<>(mobsPermitidos));
        s.set("mobs.con-casco", mobsConCasco);
        s.set("mobs.material-casco", materialCascoMobs);
        s.set("mobs.probabilidad-soltar-casco", probabilidadSoltarCasco);
        s.set("mobs.transformar-normales", transformarMobsNormales);
        s.set("mobs.mob-espacial", mobEspacial);

        s.set("ambiente.estrellas", estrellas);
        s.set("ambiente.densidad-estrellas", densidadEstrellas);

        s.set("tormentas-solares.activas", tormentasSolares);
        s.set("tormentas-solares.intervalo-minutos", tormentaIntervaloMin);
        s.set("tormentas-solares.duracion-segundos", tormentaDuracionSeg);
        s.set("tormentas-solares.danio", tormentaDanio);
        s.set("tormentas-solares.tier-proteccion", tormentaTierProteccion);

        s.set("meteoritos-espacio.activos", meteoritosEspacio);
        s.set("meteoritos-espacio.intervalo-segundos", meteoritosEspacioIntervaloSeg);
        s.set("meteoritos-espacio.danio", danioMeteoritoEspacio);

        s.set("sol.activo", solActivo);
        s.set("sol.x", solX);
        s.set("sol.y", solY);
        s.set("sol.z", solZ);
        s.set("sol.temperatura-cercana", solTemperaturaCercana);
        s.set("sol.distancia-maxima", solDistanciaMaxima);
    }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean v) { this.activo = v; }

    public PlanetEnvironment getEntorno() { return entorno; }

    public boolean isProhibirFuegoSinOxigeno() { return prohibirFuegoSinOxigeno; }
    public void setProhibirFuegoSinOxigeno(boolean v) { this.prohibirFuegoSinOxigeno = v; }

    public boolean isBloquearMobsNormales() { return bloquearMobsNormales; }
    public void setBloquearMobsNormales(boolean v) { this.bloquearMobsNormales = v; }

    public List<String> getMobsPermitidos() { return mobsPermitidos; }

    public boolean isMobsConCasco() { return mobsConCasco; }
    public void setMobsConCasco(boolean v) { this.mobsConCasco = v; }

    public String getMaterialCascoMobs() { return materialCascoMobs; }
    public double getProbabilidadSoltarCasco() { return probabilidadSoltarCasco; }

    public boolean isEstrellas() { return estrellas; }
    public void setEstrellas(boolean v) { this.estrellas = v; }
    public int getDensidadEstrellas() { return densidadEstrellas; }

    public boolean isTormentasSolares() { return tormentasSolares; }
    public void setTormentasSolares(boolean v) { this.tormentasSolares = v; }
    public int getTormentaIntervaloMin() { return tormentaIntervaloMin; }
    public int getTormentaDuracionSeg() { return tormentaDuracionSeg; }
    public double getTormentaDanio() { return tormentaDanio; }
    public int getTormentaTierProteccion() { return tormentaTierProteccion; }

    public boolean isTransformarMobsNormales() { return transformarMobsNormales; }
    public void setTransformarMobsNormales(boolean v) { this.transformarMobsNormales = v; }

    public String getMobEspacial() { return mobEspacial; }
    public void setMobEspacial(String v) { this.mobEspacial = v == null ? "HUSK" : v.toUpperCase(Locale.ROOT); }

    /** Pasa al siguiente mob de {@link #MOBS_ESPACIALES} (para el boton de la GUI). */
    public void rotarMobEspacial() {
        int i = MOBS_ESPACIALES.indexOf(mobEspacial);
        mobEspacial = MOBS_ESPACIALES.get((i + 1) % MOBS_ESPACIALES.size());
    }

    public boolean isMeteoritosEspacio() { return meteoritosEspacio; }
    public void setMeteoritosEspacio(boolean v) { this.meteoritosEspacio = v; }
    public int getMeteoritosEspacioIntervaloSeg() { return meteoritosEspacioIntervaloSeg; }
    public void setMeteoritosEspacioIntervaloSeg(int v) { this.meteoritosEspacioIntervaloSeg = Math.max(5, v); }
    public double getDanioMeteoritoEspacio() { return danioMeteoritoEspacio; }
    public void setDanioMeteoritoEspacio(double v) { this.danioMeteoritoEspacio = Math.max(0, v); }

    public boolean isSolActivo() { return solActivo; }
    public void setSolActivo(boolean v) { this.solActivo = v; }

    public double getSolX() { return solX; }
    public double getSolY() { return solY; }
    public double getSolZ() { return solZ; }

    public void setSolPosicion(double x, double y, double z) {
        this.solX = x;
        this.solY = y;
        this.solZ = z;
    }

    public double getSolTemperaturaCercana() { return solTemperaturaCercana; }
    public void setSolTemperaturaCercana(double v) { this.solTemperaturaCercana = v; }

    public double getSolDistanciaMaxima() { return solDistanciaMaxima; }
    public void setSolDistanciaMaxima(double v) { this.solDistanciaMaxima = Math.max(1, v); }

    /**
     * Temperatura ambiente en el espacio segun la distancia al sol: pegado al
     * sol, {@link #solTemperaturaCercana}; a {@link #solDistanciaMaxima} o mas,
     * la temperatura base normal del espacio (fria). Interpolacion lineal
     * entre ambas.
     */
    public double temperaturaPorDistanciaAlSol(double x, double y, double z, double temperaturaBaseEspacio) {
        double dx = x - solX;
        double dy = y - solY;
        double dz = z - solZ;
        double distancia = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distancia >= solDistanciaMaxima) {
            return temperaturaBaseEspacio;
        }
        double factor = 1.0 - (distancia / solDistanciaMaxima); // 1 = pegado al sol, 0 = al borde
        return temperaturaBaseEspacio + (solTemperaturaCercana - temperaturaBaseEspacio) * factor;
    }
}
