package com.planettravel.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Todo lo que hace que un planeta se SIENTA distinto de otro: atmosfera,
 * gravedad, agua toxica, temperatura, efectos permanentes...
 *
 * Cada campo es independiente y todos tienen un valor por defecto "neutro"
 * (como la Tierra), de modo que un planeta recien creado se comporta igual
 * que un mundo normal y el admin va activando cosas desde la GUI.
 */
public class PlanetEnvironment {

    // ---------------- ATMOSFERA / CASCO ----------------

    /** Si true, el jugador necesita casco espacial o sufrira asfixia. */
    private boolean requiereCasco = false;

    /** Danio por segundo cuando falta el casco en un planeta que lo requiere. */
    private double danioSinCasco = 1.0;

    /**
     * Material (nombre Bukkit) que cuenta como casco espacial valido.
     * Por defecto TURTLE_HELMET (el "casco de buzo" vanilla, encaja tematicamente).
     */
    private String materialCasco = "TURTLE_HELMET";

    /** Si true, ademas del casco hace falta la armadura completa del mismo tipo. */
    private boolean requiereTrajeCompleto = false;

    /**
     * Tier MINIMO de traje espacial (ver {@link SuitTier}) que exige este planeta.
     * 0 = modo clasico: se usa {@link #materialCasco}. Con tier > 0 se ignora el material.
     */
    private int tierCasco = 0;

    // ---------------- PRESION ATMOSFERICA ----------------

    /**
     * Presion atmosferica en atmosferas (1.0 = Tierra, 0 = vacio). Influye en:
     *  - cuanto oscila la temperatura entre dia y noche (poco aire = oscilaciones enormes)
     *  - el efecto invernadero (mucha presion = mas calor)
     *  - el danio por aplastamiento si supera la presion peligrosa (config.yml)
     */
    private double presion = 1.0;

    // ---------------- ARMADURA ----------------

    /** Si true, el frio/calor extremo desgasta y "congela" la armadura del jugador. */
    private boolean desgasteArmadura = true;

    // ---------------- MOBS Y AMBIENTE ----------------

    /** Si true, los mobs humanoides que aparecen aqui llevan casco de vidrio. */
    private boolean mobsConCasco = false;

    /** Lluvia de meteoritos (visual + danio en el punto de impacto, sin romper bloques). */
    private boolean meteoritos = false;
    private int meteoritosIntervaloSeg = 60;
    private double danioMeteorito = 4.0;

    /** Particulas ambientales: uno de {@link #PARTICULAS}. */
    private String particulasAmbiente = "NONE";

    public static final List<String> PARTICULAS =
            List.of("NONE", "POLVO", "CENIZA", "NIEVE", "ESPORAS", "BRASAS");

    // ---------------- AGUA ----------------

    /** Si true, el agua de este planeta hace danio al contacto. */
    private boolean aguaToxica = false;

    /** Danio por segundo dentro del agua toxica. */
    private double danioAgua = 2.0;

    /** Si true, ademas del danio aplica un efecto de veneno al salir. */
    private boolean aguaEnvenena = false;

    // ---------------- GRAVEDAD ----------------

    /**
     * Multiplicador de gravedad: 1.0 = Tierra. Valores < 1 hacen saltos
     * mas altos y caidas lentas (luna); valores > 1 aplastan al jugador.
     * Se traduce internamente a efectos de salto y caida lenta.
     */
    private double gravedad = 1.0;

    // ---------------- TEMPERATURA ----------------

    /** Si false, todo el sistema de temperatura se ignora en este planeta. */
    private boolean temperaturaActiva = false;

    /** Temperatura base del planeta en grados (puede ser negativa). */
    private double temperaturaBase = 20.0;

    /** Cuanto sube la temperatura de dia y baja de noche respecto a la base. */
    private double variacionDiaNoche = 10.0;

    /**
     * Cuanto cambia la temperatura por cada 100 bloques de altura.
     * Negativo = hace mas frio al subir (como en la Tierra).
     */
    private double variacionPorAltura = -2.0;

    /** Por encima de esta temperatura el jugador empieza a sufrir calor. */
    private double umbralCalor = 45.0;

    /** Por debajo de esta temperatura el jugador empieza a sufrir frio. */
    private double umbralFrio = -10.0;

    /** Danio por segundo cuando se superan los umbrales (escala con lo extremo que sea). */
    private double danioTemperatura = 1.0;

    /**
     * Cuantos grados de proteccion da cada pieza de armadura de cuero (frio)
     * o de cada pieza cualquiera (calor). Permite "vestirse" para sobrevivir.
     */
    private double proteccionPorPieza = 8.0;

    // ---------------- EFECTOS PERMANENTES ----------------

    /**
     * Efectos de pocion que se aplican continuamente mientras estes en el planeta.
     * Clave: nombre del PotionEffectType. Valor: nivel (amplificador + 1).
     * Ejemplo: {"SLOWNESS": 1, "NIGHT_VISION": 1}
     */
    private final Map<String, Integer> efectosPermanentes = new LinkedHashMap<>();

    // ---------------- AMBIENTE VISUAL ----------------

    /** Si true, el clima del planeta se fuerza (sin lluvia aleatoria). */
    private boolean bloquearClima = false;

    /** Si bloquearClima, si debe llover permanentemente. */
    private boolean lluviaPermanente = false;

    /** Si true, el ciclo dia/noche se detiene y se fija la hora. */
    private boolean bloquearHora = false;

    /** Hora fija del mundo (ticks, 0 = amanecer, 18000 = medianoche). */
    private long horaFija = 6000;

    // ================= Carga / guardado =================

    public static PlanetEnvironment desdeConfig(ConfigurationSection s) {
        PlanetEnvironment env = new PlanetEnvironment();
        if (s == null) {
            return env;
        }

        env.requiereCasco = s.getBoolean("requiere-casco", false);
        env.danioSinCasco = s.getDouble("danio-sin-casco", 1.0);
        env.materialCasco = s.getString("material-casco", "TURTLE_HELMET");
        env.requiereTrajeCompleto = s.getBoolean("requiere-traje-completo", false);
        env.tierCasco = Math.max(0, s.getInt("tier-casco", 0));
        env.presion = Math.max(0.0, s.getDouble("presion", 1.0));
        env.desgasteArmadura = s.getBoolean("desgaste-armadura", true);
        env.mobsConCasco = s.getBoolean("mobs-con-casco", false);
        env.meteoritos = s.getBoolean("meteoritos", false);
        env.meteoritosIntervaloSeg = Math.max(5, s.getInt("meteoritos-intervalo-seg", 60));
        env.danioMeteorito = s.getDouble("danio-meteorito", 4.0);
        env.particulasAmbiente = s.getString("particulas-ambiente", "NONE").toUpperCase();

        env.aguaToxica = s.getBoolean("agua-toxica", false);
        env.danioAgua = s.getDouble("danio-agua", 2.0);
        env.aguaEnvenena = s.getBoolean("agua-envenena", false);

        env.gravedad = s.getDouble("gravedad", 1.0);

        env.temperaturaActiva = s.getBoolean("temperatura-activa", false);
        env.temperaturaBase = s.getDouble("temperatura-base", 20.0);
        env.variacionDiaNoche = s.getDouble("variacion-dia-noche", 10.0);
        env.variacionPorAltura = s.getDouble("variacion-por-altura", -2.0);
        env.umbralCalor = s.getDouble("umbral-calor", 45.0);
        env.umbralFrio = s.getDouble("umbral-frio", -10.0);
        env.danioTemperatura = s.getDouble("danio-temperatura", 1.0);
        env.proteccionPorPieza = s.getDouble("proteccion-por-pieza", 8.0);

        ConfigurationSection efectos = s.getConfigurationSection("efectos-permanentes");
        if (efectos != null) {
            for (String clave : efectos.getKeys(false)) {
                env.efectosPermanentes.put(clave.toUpperCase(), efectos.getInt(clave, 1));
            }
        }

        env.bloquearClima = s.getBoolean("bloquear-clima", false);
        env.lluviaPermanente = s.getBoolean("lluvia-permanente", false);
        env.bloquearHora = s.getBoolean("bloquear-hora", false);
        env.horaFija = s.getLong("hora-fija", 6000);

        return env;
    }

    public void guardarEn(ConfigurationSection s) {
        s.set("requiere-casco", requiereCasco);
        s.set("danio-sin-casco", danioSinCasco);
        s.set("material-casco", materialCasco);
        s.set("requiere-traje-completo", requiereTrajeCompleto);
        s.set("tier-casco", tierCasco);
        s.set("presion", presion);
        s.set("desgaste-armadura", desgasteArmadura);
        s.set("mobs-con-casco", mobsConCasco);
        s.set("meteoritos", meteoritos);
        s.set("meteoritos-intervalo-seg", meteoritosIntervaloSeg);
        s.set("danio-meteorito", danioMeteorito);
        s.set("particulas-ambiente", particulasAmbiente);

        s.set("agua-toxica", aguaToxica);
        s.set("danio-agua", danioAgua);
        s.set("agua-envenena", aguaEnvenena);

        s.set("gravedad", gravedad);

        s.set("temperatura-activa", temperaturaActiva);
        s.set("temperatura-base", temperaturaBase);
        s.set("variacion-dia-noche", variacionDiaNoche);
        s.set("variacion-por-altura", variacionPorAltura);
        s.set("umbral-calor", umbralCalor);
        s.set("umbral-frio", umbralFrio);
        s.set("danio-temperatura", danioTemperatura);
        s.set("proteccion-por-pieza", proteccionPorPieza);

        s.set("efectos-permanentes", null);
        for (Map.Entry<String, Integer> e : efectosPermanentes.entrySet()) {
            s.set("efectos-permanentes." + e.getKey(), e.getValue());
        }

        s.set("bloquear-clima", bloquearClima);
        s.set("lluvia-permanente", lluviaPermanente);
        s.set("bloquear-hora", bloquearHora);
        s.set("hora-fija", horaFija);
    }

    /** Lista de efectos validos (los que Bukkit reconoce) listos para aplicar. */
    public List<String> getNombresEfectosValidos() {
        List<String> validos = new ArrayList<>();
        for (String nombre : efectosPermanentes.keySet()) {
            if (PotionEffectType.getByName(nombre) != null) {
                validos.add(nombre);
            }
        }
        return validos;
    }

    // ================= Calculos derivados =================

    /**
     * Temperatura base efectiva: una atmosfera densa (>1 atm) atrapa calor
     * (efecto invernadero, max +40). Con presion <= 1 no cambia nada, asi los
     * planetas antiguos se comportan exactamente igual que antes.
     */
    public double getBaseEfectiva() {
        if (presion <= 1.0) {
            return temperaturaBase;
        }
        return temperaturaBase + Math.min(40.0, 6.0 * Math.log(presion));
    }

    /**
     * Oscilacion dia/noche efectiva: con poca atmosfera el calor se escapa de
     * noche y se acumula de dia, asi que la oscilacion se amplifica (hasta x3).
     * Con mucha atmosfera se amortigua (hasta x0.5). Con 1 atm no cambia.
     */
    public double getVariacionEfectiva() {
        double p = Math.max(presion, 0.05);
        double factor = Math.max(0.5, Math.min(3.0, 1.0 / Math.sqrt(p)));
        return variacionDiaNoche * factor;
    }

    /** Gradiente termico por altura: mas gravedad = columna de aire mas densa = gradiente mayor. */
    public double getVariacionAlturaEfectiva() {
        return variacionPorAltura * Math.max(0.5, Math.min(2.0, gravedad));
    }

    /** Etiqueta legible de la atmosfera segun su presion. */
    public String getNombreAtmosfera() {
        if (presion < 0.01) return "Vacio";
        if (presion < 0.3) return "Tenue";
        if (presion < 1.5) return "Terrestre";
        if (presion < 5.0) return "Densa";
        return "Aplastante";
    }

    // ================= Getters / setters =================

    public int getTierCasco() { return tierCasco; }
    public void setTierCasco(int v) { this.tierCasco = Math.max(0, Math.min(SuitTier.MAX_TIERS, v)); }

    public double getPresion() { return presion; }
    public void setPresion(double v) { this.presion = Math.max(0.0, Math.min(500.0, Math.round(v * 100.0) / 100.0)); }

    public boolean isDesgasteArmadura() { return desgasteArmadura; }
    public void setDesgasteArmadura(boolean v) { this.desgasteArmadura = v; }

    public boolean isMobsConCasco() { return mobsConCasco; }
    public void setMobsConCasco(boolean v) { this.mobsConCasco = v; }

    public boolean isMeteoritos() { return meteoritos; }
    public void setMeteoritos(boolean v) { this.meteoritos = v; }

    public int getMeteoritosIntervaloSeg() { return meteoritosIntervaloSeg; }
    public void setMeteoritosIntervaloSeg(int v) { this.meteoritosIntervaloSeg = Math.max(5, v); }

    public double getDanioMeteorito() { return danioMeteorito; }
    public void setDanioMeteorito(double v) { this.danioMeteorito = Math.max(0, v); }

    public String getParticulasAmbiente() { return particulasAmbiente; }
    public void setParticulasAmbiente(String v) { this.particulasAmbiente = PARTICULAS.contains(v) ? v : "NONE"; }


    public boolean isRequiereCasco() { return requiereCasco; }
    public void setRequiereCasco(boolean v) { this.requiereCasco = v; }

    public double getDanioSinCasco() { return danioSinCasco; }
    public void setDanioSinCasco(double v) { this.danioSinCasco = v; }

    public String getMaterialCasco() { return materialCasco; }
    public void setMaterialCasco(String v) { this.materialCasco = v; }

    public boolean isRequiereTrajeCompleto() { return requiereTrajeCompleto; }
    public void setRequiereTrajeCompleto(boolean v) { this.requiereTrajeCompleto = v; }

    public boolean isAguaToxica() { return aguaToxica; }
    public void setAguaToxica(boolean v) { this.aguaToxica = v; }

    public double getDanioAgua() { return danioAgua; }
    public void setDanioAgua(double v) { this.danioAgua = v; }

    public boolean isAguaEnvenena() { return aguaEnvenena; }
    public void setAguaEnvenena(boolean v) { this.aguaEnvenena = v; }

    public double getGravedad() { return gravedad; }
    public void setGravedad(double v) { this.gravedad = Math.max(0.1, Math.min(3.0, v)); }

    public boolean isTemperaturaActiva() { return temperaturaActiva; }
    public void setTemperaturaActiva(boolean v) { this.temperaturaActiva = v; }

    public double getTemperaturaBase() { return temperaturaBase; }
    public void setTemperaturaBase(double v) { this.temperaturaBase = v; }

    public double getVariacionDiaNoche() { return variacionDiaNoche; }
    public void setVariacionDiaNoche(double v) { this.variacionDiaNoche = v; }

    public double getVariacionPorAltura() { return variacionPorAltura; }
    public void setVariacionPorAltura(double v) { this.variacionPorAltura = v; }

    public double getUmbralCalor() { return umbralCalor; }
    public void setUmbralCalor(double v) { this.umbralCalor = v; }

    public double getUmbralFrio() { return umbralFrio; }
    public void setUmbralFrio(double v) { this.umbralFrio = v; }

    public double getDanioTemperatura() { return danioTemperatura; }
    public void setDanioTemperatura(double v) { this.danioTemperatura = v; }

    public double getProteccionPorPieza() { return proteccionPorPieza; }
    public void setProteccionPorPieza(double v) { this.proteccionPorPieza = v; }

    public Map<String, Integer> getEfectosPermanentes() { return efectosPermanentes; }

    public boolean isBloquearClima() { return bloquearClima; }
    public void setBloquearClima(boolean v) { this.bloquearClima = v; }

    public boolean isLluviaPermanente() { return lluviaPermanente; }
    public void setLluviaPermanente(boolean v) { this.lluviaPermanente = v; }

    public boolean isBloquearHora() { return bloquearHora; }
    public void setBloquearHora(boolean v) { this.bloquearHora = v; }

    public long getHoraFija() { return horaFija; }
    public void setHoraFija(long v) { this.horaFija = Math.max(0, Math.min(24000, v)); }
}
