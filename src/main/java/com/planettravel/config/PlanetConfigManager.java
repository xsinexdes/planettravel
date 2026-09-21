package com.planettravel.config;

import org.bukkit.Bukkit;
import org.bukkit.World;
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

    // Ajustes nuevos (1.2.0)
    private double presionPeligrosa;
    private double desgasteMultiplicador;
    private double margenDeteccion;
    private final SpaceSettings spaceSettings = new SpaceSettings();
    private final Map<Integer, SuitTier> trajes = new LinkedHashMap<>();

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
        this.presionPeligrosa = Math.max(0.5, cfg.getDouble("entorno.presion-peligrosa", 5.0));
        this.desgasteMultiplicador = Math.max(0.0, cfg.getDouble("entorno.desgaste-armadura-multiplicador", 1.0));
        this.margenDeteccion = Math.max(1.0, cfg.getDouble("seleccion.margen-deteccion", 30.0));

        // --- Trajes espaciales (si no hay ninguno definido se crean los de por defecto) ---
        trajes.clear();
        ConfigurationSection secTrajes = cfg.getConfigurationSection("trajes");
        if (secTrajes != null) {
            for (String clave : secTrajes.getKeys(false)) {
                try {
                    int tier = Integer.parseInt(clave);
                    if (tier >= 1 && tier <= SuitTier.MAX_TIERS) {
                        trajes.put(tier, SuitTier.desdeConfig(tier, secTrajes.getConfigurationSection(clave)));
                    }
                } catch (NumberFormatException ignorado) {
                    // clave que no es un numero: la ignoramos
                }
            }
        }
        if (trajes.isEmpty()) {
            trajes.putAll(SuitTier.porDefecto());
        }

        // --- Ajustes del espacio ---
        spaceSettings.desdeConfig(cfg.getConfigurationSection("ajustes-espacio"));

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
            // La descripcion puede ser un texto (formato antiguo) o una lista de lineas.
            if (s.isList("descripcion")) {
                datos.setDescripcionLineas(s.getStringList("descripcion"));
            } else {
                datos.setDescripcion(s.getString("descripcion", ""));
            }
            datos.setClasificacion(s.getString("clasificacion", ""));

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
                datos.setSpawnPlanetaEsquinaB(
                        spawnPlaneta.getDouble("x2", spawnPlaneta.getDouble("x")),
                        spawnPlaneta.getDouble("y2", spawnPlaneta.getDouble("y", datos.getEntradaAtmosfericaY())),
                        spawnPlaneta.getDouble("z2", spawnPlaneta.getDouble("z"))
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
                datos.setSalidaEspacioEsquinaB(
                        salidaEspacio.getDouble("x2", salidaEspacio.getDouble("x")),
                        salidaEspacio.getDouble("y2", salidaEspacio.getDouble("y")),
                        salidaEspacio.getDouble("z2", salidaEspacio.getDouble("z"))
                );
            }

            // Los flags "lista" solo son true si YA se marcaron con el hacha
            // (o vienen de una partida guardada antes de este sistema Y ya
            // tenian coordenadas distintas de cero, para no romper mundos ya
            // configurados a mano).
            boolean entradaPorDefecto = spawnPlaneta != null
                    && (spawnPlaneta.getDouble("x") != 0 || spawnPlaneta.getDouble("z") != 0 || spawnPlaneta.getDouble("y") != 0);
            boolean salidaPorDefecto = salidaEspacio != null
                    && (salidaEspacio.getDouble("x") != 0 || salidaEspacio.getDouble("z") != 0 || salidaEspacio.getDouble("y") != 0);
            datos.setEntradaLista(s.getBoolean("entrada-lista", entradaPorDefecto));
            datos.setSalidaLista(s.getBoolean("salida-lista", salidaPorDefecto));
            datos.setEntradaUsaWorldBorder(s.getBoolean("entrada-usa-worldborder", false));

            ConfigurationSection deteccionCaja = s.getConfigurationSection("deteccion-caja");
            if (deteccionCaja != null) {
                datos.setDeteccionCaja(
                        deteccionCaja.getDouble("x"),
                        deteccionCaja.getDouble("y"),
                        deteccionCaja.getDouble("z")
                );
                datos.setDeteccionCajaEsquinaB(
                        deteccionCaja.getDouble("x2", deteccionCaja.getDouble("x")),
                        deteccionCaja.getDouble("y2", deteccionCaja.getDouble("y")),
                        deteccionCaja.getDouble("z2", deteccionCaja.getDouble("z"))
                );
            }
            datos.setDeteccionUsaCaja(s.getBoolean("deteccion-usa-caja", false));
            datos.setDeteccionCajaLista(s.getBoolean("deteccion-caja-lista", false));

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
        cfg.set("entorno.presion-peligrosa", presionPeligrosa);
        cfg.set("entorno.desgaste-armadura-multiplicador", desgasteMultiplicador);
        cfg.set("seleccion.margen-deteccion", margenDeteccion);

        cfg.set("trajes", null);
        for (SuitTier t : trajes.values()) {
            t.guardarEn(cfg.createSection("trajes." + t.getId()));
        }

        cfg.set("ajustes-espacio", null);
        spaceSettings.guardarEn(cfg.createSection("ajustes-espacio"));

        // Reescribimos toda la seccion "planetas" desde el mapa en memoria
        cfg.set("planetas", null);
        for (PlanetData p : planetas.values()) {
            String base = "planetas." + p.getId();
            cfg.set(base + ".mundo", p.getWorldName());
            cfg.set(base + ".nombre-visible", p.getNombreVisible());
            cfg.set(base + ".icono", p.getIconoMaterial());
            cfg.set(base + ".descripcion", new ArrayList<>(p.getDescripcionLineas()));
            cfg.set(base + ".clasificacion", p.getClasificacion());

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
            cfg.set(base + ".spawn-planeta.x2", p.getSpawnPlanetaX2());
            cfg.set(base + ".spawn-planeta.y2", p.getSpawnPlanetaY2());
            cfg.set(base + ".spawn-planeta.z2", p.getSpawnPlanetaZ2());

            cfg.set(base + ".salida-espacio.x", p.getSalidaEspacioX());
            cfg.set(base + ".salida-espacio.y", p.getSalidaEspacioY());
            cfg.set(base + ".salida-espacio.z", p.getSalidaEspacioZ());
            cfg.set(base + ".salida-espacio.yaw", p.getSalidaEspacioYaw());
            cfg.set(base + ".salida-espacio.pitch", p.getSalidaEspacioPitch());
            cfg.set(base + ".salida-espacio.x2", p.getSalidaEspacioX2());
            cfg.set(base + ".salida-espacio.y2", p.getSalidaEspacioY2());
            cfg.set(base + ".salida-espacio.z2", p.getSalidaEspacioZ2());
            cfg.set(base + ".entrada-lista", p.isEntradaLista());
            cfg.set(base + ".salida-lista", p.isSalidaLista());
            cfg.set(base + ".entrada-usa-worldborder", p.isEntradaUsaWorldBorder());
            cfg.set(base + ".deteccion-usa-caja", p.isDeteccionUsaCaja());
            cfg.set(base + ".deteccion-caja-lista", p.isDeteccionCajaLista());
            cfg.set(base + ".deteccion-caja.x", p.getDeteccionCajaX());
            cfg.set(base + ".deteccion-caja.y", p.getDeteccionCajaY());
            cfg.set(base + ".deteccion-caja.z", p.getDeteccionCajaZ());
            cfg.set(base + ".deteccion-caja.x2", p.getDeteccionCajaX2());
            cfg.set(base + ".deteccion-caja.y2", p.getDeteccionCajaY2());
            cfg.set(base + ".deteccion-caja.z2", p.getDeteccionCajaZ2());

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
            // PUNTOS OBLIGATORIOS: sin entrada/salida marcadas con el hacha,
            // el planeta no admite viajes (ver /planeta hacha entrada|salida).
            if (!p.entradaConfigurada()) {
                avisos.add("'" + p.getId() + "': falta marcar la zona de ENTRADA con el hacha "
                        + "(/planeta hacha entrada " + p.getId() + "), o activa /planeta editar " + p.getId()
                        + " entradaworldborder. No se podra aterrizar aqui.");
            }
            if (!p.isSalidaLista()) {
                avisos.add("'" + p.getId() + "': falta marcar la zona de SALIDA con el hacha "
                        + "(/planeta hacha salida " + p.getId() + "). No se podra despegar de aqui.");
            }

            // Modo "entrada aleatoria por WorldBorder" activado pero el mundo
            // sigue con el WorldBorder gigante por defecto de Bukkit (~6E7):
            // las naves podrian aparecer a millones de bloques del schematic.
            if (p.isEntradaUsaWorldBorder()) {
                World mundoPlaneta = Bukkit.getWorld(p.getWorldName());
                if (mundoPlaneta != null && mundoPlaneta.getWorldBorder().getSize() > 200_000) {
                    avisos.add("'" + p.getId() + "': usa entrada por WorldBorder, pero el mundo '" + p.getWorldName()
                            + "' no tiene un /worldborder configurado (sigue con el tamanio por defecto). "
                            + "Ponle uno razonable en ese mundo o las naves podrian aparecer muy lejos del planeta.");
                }
            }

            // Modo "deteccion por caja" activado pero nunca se marco con el hacha.
            if (p.isDeteccionUsaCaja() && !p.isDeteccionCajaLista()) {
                avisos.add("'" + p.getId() + "': tiene activada la deteccion por caja pero no se ha marcado "
                        + "con el hacha (/planeta hacha deteccion " + p.getId() + "). Mientras tanto se usa "
                        + "la esfera normal.");
            }

            // BUCLE CLASICO: la salida al espacio cae dentro de la propia esfera.
            // Despegas -> apareces dentro -> el detector te vuelve a aterrizar.
            if (p.salidaEstaDentroDeSuPropiaEsfera()) {
                avisos.add("'" + p.getId() + "': la zona de salida al espacio esta DENTRO de su propia " +
                        "esfera (radio " + p.getRadio() + "). Esto causara un bucle de teletransporte. " +
                        "Vuelve a marcarla mas lejos con /planeta hacha salida " + p.getId() + ".");
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
                if (otro.estaEnZonaDeDeteccion(p.getSalidaCentroX(), p.getSalidaCentroY(), p.getSalidaCentroZ())) {
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

    public double getPresionPeligrosa() { return presionPeligrosa; }
    public double getDesgasteMultiplicador() { return desgasteMultiplicador; }
    public double getMargenDeteccion() { return margenDeteccion; }

    public SpaceSettings getSpaceSettings() { return spaceSettings; }

    // ---------- Trajes espaciales ----------

    public Map<Integer, SuitTier> getTrajes() { return trajes; }

    /** Definicion del tier indicado, o null si no existe. */
    public SuitTier getTraje(int tier) { return trajes.get(tier); }

    public int getMaxTierDefinido() {
        int max = 0;
        for (int t : trajes.keySet()) {
            max = Math.max(max, t);
        }
        return max;
    }

    /** Anade un tier nuevo (el siguiente numero libre) copiando los valores del ultimo. Devuelve null si ya hay el maximo. */
    public SuitTier anadirTraje() {
        int siguiente = getMaxTierDefinido() + 1;
        if (siguiente > SuitTier.MAX_TIERS) {
            return null;
        }
        SuitTier nuevo = new SuitTier(siguiente);
        SuitTier previo = trajes.get(siguiente - 1);
        if (previo != null) {
            nuevo.setMaterial(previo.getMaterial());
            nuevo.setProteccionFrio(previo.getProteccionFrio() * 1.3);
            nuevo.setProteccionCalor(previo.getProteccionCalor() * 1.3);
            nuevo.setResistenciaPresion(previo.getResistenciaPresion() * 2);
        }
        trajes.put(siguiente, nuevo);
        return nuevo;
    }

    /** Elimina el ultimo tier (siempre queda al menos uno). */
    public boolean quitarUltimoTraje() {
        if (trajes.size() <= 1) {
            return false;
        }
        trajes.remove(getMaxTierDefinido());
        return true;
    }

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
