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

    // --- Ambiente ---
    private boolean estrellas = true;
    private int densidadEstrellas = 6;

    // --- Tormentas solares ---
    private boolean tormentasSolares = false;
    private int tormentaIntervaloMin = 20;
    private int tormentaDuracionSeg = 40;
    private double tormentaDanio = 3.0;
    private int tormentaTierProteccion = 3;

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

        s.set("ambiente.estrellas", estrellas);
        s.set("ambiente.densidad-estrellas", densidadEstrellas);

        s.set("tormentas-solares.activas", tormentasSolares);
        s.set("tormentas-solares.intervalo-minutos", tormentaIntervaloMin);
        s.set("tormentas-solares.duracion-segundos", tormentaDuracionSeg);
        s.set("tormentas-solares.danio", tormentaDanio);
        s.set("tormentas-solares.tier-proteccion", tormentaTierProteccion);
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
}
