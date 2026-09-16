package com.planettravel.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Carga y guarda la seccion "planetas" del config.yml en un mapa en memoria
 * (id -> PlanetData). Todos los comandos, GUIs y listeners trabajan siempre
 * sobre este mapa; solo se toca el archivo al cargar o al llamar a
 * {@link #guardarTodo()}.
 */
public class PlanetConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, PlanetData> planetas = new LinkedHashMap<>();

    private String mundoEspacio;
    private boolean modoSinCoreNaves;
    private long intervaloComprobacionTicks;
    private long cooldownTeletransporteSegundos;
    private double alturaEntradaPorDefecto;
    private double alturaDespeguePorDefecto;

    // Ajustes de las funciones visuales
    private boolean bossbarAltitud;
    private boolean actionbarProximidad;
    private double distanciaAvisoProximidad;
    private boolean dibujarBordeEsfera;
    private double distanciaDibujarBorde;
    private long intervaloEntornoTicks;

    public PlanetConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Lee config.yml completo (opciones globales + todos los planetas) hacia memoria. */
    public void cargar() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        this.mundoEspacio = cfg.getString("mundo-espacio", "espacio");
        this.modoSinCoreNaves = cfg.getBoolean("modo-sin-corenaves", true);
        this.intervaloComprobacionTicks = cfg.getLong("intervalo-comprobacion-ticks", 10);
        this.cooldownTeletransporteSegundos = cfg.getLong("cooldown-teletransporte-segundos", 5);
        this.alturaEntradaPorDefecto = cfg.getDouble("altura-entrada-atmosferica-por-defecto", 310);
        this.alturaDespeguePorDefecto = cfg.getDouble("altura-despegue-por-defecto", 315);

        this.bossbarAltitud = cfg.getBoolean("visual.bossbar-altitud", true);
        this.actionbarProximidad = cfg.getBoolean("visual.actionbar-proximidad", true);
        this.distanciaAvisoProximidad = cfg.getDouble("visual.distancia-aviso-proximidad", 600);
        this.dibujarBordeEsfera = cfg.getBoolean("visual.dibujar-borde-esfera", true);
        this.distanciaDibujarBorde = cfg.getDouble("visual.distancia-dibujar-borde", 250);
        this.intervaloEntornoTicks = cfg.getLong("entorno.intervalo-ticks", 20);

        planetas.clear();
        ConfigurationSection seccionPlanetas = cfg.getConfigurationSection("planetas");
        if (seccionPlanetas == null) {
            return;
        }

        for (String id : seccionPlanetas.getKeys(false)) {
            ConfigurationSection s = seccionPlanetas.getConfigurationSection(id);
            if (s == null) continue;

            PlanetData datos = new PlanetData(id);
            datos.setWorldName(s.getString("mundo", id));
            datos.setNombreVisible(s.getString("nombre-visible", id));
            datos.setIconoMaterial(s.getString("icono", "RED_CONCRETE"));
            datos.setDescripcion(s.getString("descripcion", ""));

            ConfigurationSection espacio = s.getConfigurationSection("espacio");
            if (espacio != null) {
                datos.setEsferaEspacio(
                        espacio.getDouble("x"),
                        espacio.getDouble("y"),
                        espacio.getDouble("z"),
                        espacio.getDouble("radio", 100)
                );
                datos.setRadioVisual(espacio.getDouble("radio-visual", 0));
            }

            datos.setEntradaAtmosfericaY(s.getDouble("entrada-atmosferica-y", alturaEntradaPorDefecto));
            datos.setAlturaDespegueY(s.getDouble("altura-despegue-y", alturaDespeguePorDefecto));

            ConfigurationSection spawnPlaneta = s.getConfigurationSection("spawn-planeta");
            if (spawnPlaneta != null) {
                datos.setSpawnPlaneta(
                        spawnPlaneta.getDouble("x"),
                        spawnPlaneta.getDouble("y", datos.getEntradaAtmosfericaY()),
                        spawnPlaneta.getDouble("z"),
                        (float) spawnPlaneta.getDouble("yaw", 0),
                        (float) spawnPlaneta.getDouble("pitch", 0)
                );
            }

            ConfigurationSection salidaEspacio = s.getConfigurationSection("salida-espacio");
            if (salidaEspacio != null) {
                datos.setSalidaEspacio(
                        salidaEspacio.getDouble("x"),
                        salidaEspacio.getDouble("y"),
                        salidaEspacio.getDouble("z"),
                        (float) salidaEspacio.getDouble("yaw", 0),
                        (float) salidaEspacio.getDouble("pitch", 0)
                );
            }

            datos.setEntorno(PlanetEnvironment.desdeConfig(s.getConfigurationSection("entorno")));

            planetas.put(id.toLowerCase(), datos);
        }
    }

    /** Vuelca el mapa en memoria de nuevo al config.yml y lo guarda a disco. */
    public void guardarTodo() {
        FileConfiguration cfg = plugin.getConfig();

        cfg.set("mundo-espacio", mundoEspacio);
        cfg.set("modo-sin-corenaves", modoSinCoreNaves);
        cfg.set("intervalo-comprobacion-ticks", intervaloComprobacionTicks);
        cfg.set("cooldown-teletransporte-segundos", cooldownTeletransporteSegundos);
        cfg.set("altura-entrada-atmosferica-por-defecto", alturaEntradaPorDefecto);
        cfg.set("altura-despegue-por-defecto", alturaDespeguePorDefecto);

        cfg.set("visual.bossbar-altitud", bossbarAltitud);
        cfg.set("visual.actionbar-proximidad", actionbarProximidad);
        cfg.set("visual.distancia-aviso-proximidad", distanciaAvisoProximidad);
        cfg.set("visual.dibujar-borde-esfera", dibujarBordeEsfera);
        cfg.set("visual.distancia-dibujar-borde", distanciaDibujarBorde);
        cfg.set("entorno.intervalo-ticks", intervaloEntornoTicks);

        // Reescribimos toda la seccion "planetas" desde el mapa en memoria
        cfg.set("planetas", null);
        for (PlanetData p : planetas.values()) {
            String base = "planetas." + p.getId();
            cfg.set(base + ".mundo", p.getWorldName());
            cfg.set(base + ".nombre-visible", p.getNombreVisible());
            cfg.set(base + ".icono", p.getIconoMaterial());
            cfg.set(base + ".descripcion", p.getDescripcion());

            cfg.set(base + ".espacio.x", p.getSpaceX());
            cfg.set(base + ".espacio.y", p.getSpaceY());
            cfg.set(base + ".espacio.z", p.getSpaceZ());
            cfg.set(base + ".espacio.radio", p.getRadio());
            cfg.set(base + ".espacio.radio-visual", p.getRadioVisual());

            cfg.set(base + ".entrada-atmosferica-y", p.getEntradaAtmosfericaY());
            cfg.set(base + ".altura-despegue-y", p.getAlturaDespegueY());

            cfg.set(base + ".spawn-planeta.x", p.getSpawnPlanetaX());
            cfg.set(base + ".spawn-planeta.y", p.getSpawnPlanetaY());
            cfg.set(base + ".spawn-planeta.z", p.getSpawnPlanetaZ());
            cfg.set(base + ".spawn-planeta.yaw", p.getSpawnPlanetaYaw());
            cfg.set(base + ".spawn-planeta.pitch", p.getSpawnPlanetaPitch());

            cfg.set(base + ".salida-espacio.x", p.getSalidaEspacioX());
            cfg.set(base + ".salida-espacio.y", p.getSalidaEspacioY());
            cfg.set(base + ".salida-espacio.z", p.getSalidaEspacioZ());
            cfg.set(base + ".salida-espacio.yaw", p.getSalidaEspacioYaw());
            cfg.set(base + ".salida-espacio.pitch", p.getSalidaEspacioPitch());

            ConfigurationSection secEntorno = cfg.createSection(base + ".entorno");
            p.getEntorno().guardarEn(secEntorno);
        }

        plugin.saveConfig();
    }

    /**
     * Revisa todos los planetas buscando configuraciones que provocarian
     * bucles de teletransporte o colisiones, y devuelve los avisos en texto.
     * Se llama al arrancar y tras cada edicion.
     */
    public List<String> validarPlanetas() {
        List<String> avisos = new ArrayList<>();

        for (PlanetData p : planetas.values()) {
            // BUCLE CLASICO: la salida al espacio cae dentro de la propia esfera.
            // Despegas -> apareces dentro -> el detector te vuelve a aterrizar.
            if (p.salidaEstaDentroDeSuPropiaEsfera()) {
                avisos.add("'" + p.getId() + "': el punto de salida al espacio esta DENTRO de su propia " +
                        "esfera (radio " + p.getRadio() + "). Esto causara un bucle de teletransporte. " +
                        "Mueve la salida mas lejos con /planeta editar " + p.getId() + " salidaaqui.");
            }

            // El radio de deteccion debe ser MAYOR que la esfera de bloques,
            // o la nave chocara contra los bloques antes de que salte el TP.
            if (p.radioDeteccionEsDemasiadoPequenio()) {
                avisos.add("'" + p.getId() + "': el radio de deteccion (" + p.getRadio() + ") no es mayor " +
                        "que la esfera de bloques (" + p.getRadioVisual() + "). Las naves chocaran antes " +
                        "de aterrizar. Sube el radio de deteccion.");
            }

            // La salida de un planeta no debe caer dentro de la esfera de OTRO planeta.
            for (PlanetData otro : planetas.values()) {
                if (otro == p) continue;
                if (otro.estaDentroDeEsfera(p.getSalidaEspacioX(), p.getSalidaEspacioY(), p.getSalidaEspacioZ())) {
                    avisos.add("'" + p.getId() + "': su salida al espacio cae dentro de la esfera de '" +
                            otro.getId() + "'. Al despegar aterrizarias inmediatamente en el otro planeta.");
                }
            }

            // Esferas que se solapan: la deteccion seria ambigua.
            for (PlanetData otro : planetas.values()) {
                if (otro == p || otro.getId().compareTo(p.getId()) < 0) continue;
                double distancia = p.distanciaAlCentro(otro.getSpaceX(), otro.getSpaceY(), otro.getSpaceZ());
                if (distancia < (p.getRadio() + otro.getRadio())) {
                    avisos.add("Las esferas de '" + p.getId() + "' y '" + otro.getId() + "' se solapan. " +
                            "La deteccion de aterrizaje sera impredecible entre ambas.");
                }
            }
        }

        return avisos;
    }

    // ---------- Acceso a los datos en memoria ----------

    public Optional<PlanetData> getPlaneta(String id) {
        return Optional.ofNullable(planetas.get(id.toLowerCase()));
    }

    public Map<String, PlanetData> getPlanetas() { return planetas; }

    public void registrarPlaneta(PlanetData datos) {
        planetas.put(datos.getId().toLowerCase(), datos);
    }

    public void eliminarPlaneta(String id) {
        planetas.remove(id.toLowerCase());
    }

    public String getMundoEspacio() { return mundoEspacio; }
    public boolean isModoSinCoreNaves() { return modoSinCoreNaves; }
    public long getIntervaloComprobacionTicks() { return intervaloComprobacionTicks; }
    public long getCooldownTeletransporteSegundos() { return cooldownTeletransporteSegundos; }
    public double getAlturaEntradaPorDefecto() { return alturaEntradaPorDefecto; }
    public double getAlturaDespeguePorDefecto() { return alturaDespeguePorDefecto; }

    public boolean isBossbarAltitud() { return bossbarAltitud; }
    public boolean isActionbarProximidad() { return actionbarProximidad; }
    public double getDistanciaAvisoProximidad() { return distanciaAvisoProximidad; }
    public boolean isDibujarBordeEsfera() { return dibujarBordeEsfera; }
    public double getDistanciaDibujarBorde() { return distanciaDibujarBorde; }
    public long getIntervaloEntornoTicks() { return intervaloEntornoTicks; }

    /** Busca el planeta cuyo mundo coincide con el nombre dado, o null. */
    public PlanetData getPlanetaPorMundo(String nombreMundo) {
        for (PlanetData p : planetas.values()) {
            if (p.getWorldName().equalsIgnoreCase(nombreMundo)) {
                return p;
            }
        }
        return null;
    }
}
