package com.planettravel;

import com.planettravel.api.CoreNavesHook;
import com.planettravel.commands.PlanetCommand;
import com.planettravel.commands.StarMapCommand;
import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.environment.EnvironmentManager;
import com.planettravel.environment.TemperatureManager;
import com.planettravel.environment.VisualFeedbackTask;
import com.planettravel.gui.EffectsGUI;
import com.planettravel.gui.PlanetEditorGUI;
import com.planettravel.gui.StarMapGUI;
import com.planettravel.listeners.GUIListener;
import com.planettravel.listeners.ShipMovementListener;
import com.planettravel.teleport.TeleportManager;
import com.planettravel.util.WorldUtil;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Clase principal de PlanetTravel.
 *
 * Orden de arranque:
 *   1) Cargar config.yml -> PlanetConfigManager (todos los planetas en memoria)
 *   2) Enganchar con CoreNaves (real si esta presente, o modo de respaldo)
 *   3) Cargar los mundos-planeta y validar alturas contra el limite real de cada mundo
 *   4) Arrancar las tres tareas periodicas: deteccion de viaje, entorno y visuales
 *   5) Registrar listeners y comandos
 *   6) Avisar de configuraciones que causarian bucles de teletransporte
 */
public class PlanetTravel extends JavaPlugin {

    private PlanetConfigManager configManager;
    private CoreNavesHook coreNavesHook;
    private TeleportManager teleportManager;
    private TemperatureManager temperatureManager;
    private EnvironmentManager environmentManager;
    private VisualFeedbackTask visualFeedbackTask;
    private ShipMovementListener shipMovementListener;

    @Override
    public void onEnable() {
        // --- 1) Configuracion ---
        this.configManager = new PlanetConfigManager(this);
        configManager.cargar();

        // --- 2) CoreNaves ---
        this.coreNavesHook = new CoreNavesHook(this, configManager.isModoSinCoreNaves());
        coreNavesHook.enganchar();

        // --- 3) Mundos ---
        this.temperatureManager = new TemperatureManager();
        this.teleportManager = new TeleportManager(this, configManager, coreNavesHook, temperatureManager);
        cargarYValidarMundos();

        // --- 4) GUIs ---
        StarMapGUI starMapGUI = new StarMapGUI(configManager);
        PlanetEditorGUI editorGUI = new PlanetEditorGUI();
        EffectsGUI effectsGUI = new EffectsGUI();

        // --- 5) Tareas periodicas ---
        this.environmentManager = new EnvironmentManager(this, configManager, temperatureManager);
        environmentManager.runTaskTimer(this, 40L, configManager.getIntervaloEntornoTicks());

        this.visualFeedbackTask = new VisualFeedbackTask(this, configManager, coreNavesHook);
        visualFeedbackTask.runTaskTimer(this, 40L, 5L); // 4 veces por segundo: fluido sin ser caro

        this.shipMovementListener = new ShipMovementListener(
                this, configManager, coreNavesHook, teleportManager, environmentManager, visualFeedbackTask);
        shipMovementListener.runTaskTimer(this, 40L, configManager.getIntervaloComprobacionTicks());

        // --- 6) Listeners ---
        getServer().getPluginManager().registerEvents(shipMovementListener, this);
        getServer().getPluginManager().registerEvents(
                new GUIListener(configManager, starMapGUI, editorGUI, effectsGUI), this);

        // --- 7) Comandos ---
        registrarComando("planeta", new PlanetCommand(configManager, coreNavesHook, starMapGUI, editorGUI));
        registrarComando("planetas", new StarMapCommand(starMapGUI));

        // --- 8) Avisos de configuracion ---
        mostrarAvisosDeValidacion();

        getLogger().info("PlanetTravel activado con " + configManager.getPlanetas().size() + " planeta(s).");
    }

    @Override
    public void onDisable() {
        if (visualFeedbackTask != null) {
            visualFeedbackTask.limpiarTodo(); // quita las bossbars de todos
        }
        if (configManager != null) {
            configManager.guardarTodo();
        }
        getLogger().info("PlanetTravel desactivado.");
    }

    private void registrarComando(String nombre, Object ejecutor) {
        PluginCommand comando = getCommand(nombre);
        if (comando == null) {
            getLogger().severe("No se pudo registrar el comando /" + nombre + " (revisa plugin.yml).");
            return;
        }
        comando.setExecutor((org.bukkit.command.CommandExecutor) ejecutor);
        if (ejecutor instanceof org.bukkit.command.TabCompleter tc) {
            comando.setTabCompleter(tc);
        }
    }

    /**
     * Con pocos mundos-planeta es mas sencillo y fiable mantenerlos SIEMPRE
     * cargados que estar cargando/descargando segun quien aterrice. De paso
     * aprovechamos para validar que las alturas configuradas caben de verdad
     * en cada mundo: si un mundo llega solo hasta Y=320 y el config pide Y=1400,
     * la nave apareceria fuera del mundo y caeria al vacio.
     */
    private void cargarYValidarMundos() {
        for (PlanetData planeta : configManager.getPlanetas().values()) {
            World mundo = WorldUtil.cargarSiHaceFalta(planeta.getWorldName(), getLogger());
            if (mundo == null) {
                continue;
            }

            int maxAltura = mundo.getMaxHeight();

            if (planeta.getEntradaAtmosfericaY() > maxAltura - 5) {
                getLogger().warning("'" + planeta.getId() + "': la altura de entrada ("
                        + (int) planeta.getEntradaAtmosfericaY() + ") supera el limite del mundo '"
                        + mundo.getName() + "' (" + maxAltura + "). Las naves apareceran mas abajo. "
                        + "Baja el valor o genera el mundo con altura extendida.");
            }

            if (planeta.getAlturaDespegueY() > maxAltura) {
                getLogger().warning("'" + planeta.getId() + "': la altura de despegue ("
                        + (int) planeta.getAlturaDespegueY() + ") esta por encima del techo del mundo '"
                        + mundo.getName() + "' (" + maxAltura + "). NUNCA se podra despegar de ahi.");
            }

            if (planeta.getAlturaDespegueY() <= planeta.getEntradaAtmosfericaY()) {
                getLogger().warning("'" + planeta.getId() + "': la altura de despegue no es mayor que la "
                        + "de entrada. Al aterrizar despegarias inmediatamente.");
            }
        }

        // El mundo espacio tambien tiene que existir.
        WorldUtil.cargarSiHaceFalta(configManager.getMundoEspacio(), getLogger());
    }

    private void mostrarAvisosDeValidacion() {
        List<String> avisos = configManager.validarPlanetas();
        if (avisos.isEmpty()) {
            return;
        }
        getLogger().warning("=== Problemas de configuracion detectados ===");
        for (String aviso : avisos) {
            getLogger().warning(" - " + aviso);
        }
        getLogger().warning("Usa /planeta validar en el juego para revisarlos.");
    }

    public PlanetConfigManager getConfigManager() { return configManager; }
    public CoreNavesHook getCoreNavesHook() { return coreNavesHook; }
    public TeleportManager getTeleportManager() { return teleportManager; }
    public TemperatureManager getTemperatureManager() { return temperatureManager; }
}
