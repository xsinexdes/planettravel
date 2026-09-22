package com.planettravel.listeners;

import com.planettravel.api.CoreNavesAPI;
import com.planettravel.api.CoreNavesHook;
import com.planettravel.api.Ship;
import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.environment.EnvironmentManager;
import com.planettravel.environment.VisualFeedbackTask;
import com.planettravel.teleport.TeleportManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detecta, en tiempo real:
 *   1) ATERRIZAJE: un jugador pilotando una nave en el mundo "espacio" entra
 *      en la esfera de un planeta -> se manda la nave a ese planeta.
 *   2) DESPEGUE: un jugador pilotando una nave en un mundo-planeta, ASCENDIENDO,
 *      supera la altura de despegue de ese planeta -> se manda la nave al espacio.
 *
 * POR QUE UNA TAREA Y NO PlayerMoveEvent:
 * El evento de movimiento solo se dispara cuando el JUGADOR se mueve. Si CoreNaves
 * mueve la nave mientras el piloto esta sentado y quieto (piloto automatico, o
 * simplemente no toca el raton), el evento no salta y el aterrizaje no se detecta.
 * Una tarea programada que recorre solo a los pilotos activos es mas fiable y,
 * encima, mas barata: el evento se dispara decenas de veces por segundo por
 * jugador, mientras que la tarea corre a un ritmo fijo que controlamos.
 */
public class ShipMovementListener extends BukkitRunnable implements Listener {

    private final JavaPlugin plugin;
    private final PlanetConfigManager configManager;
    private final CoreNavesHook coreNavesHook;
    private final TeleportManager teleportManager;
    private final EnvironmentManager environmentManager;
    private final VisualFeedbackTask visualFeedbackTask;

    /**
     * Para saber si el jugador esta ASCENDIENDO (necesario para el despegue):
     * guardamos su ultima altura Y conocida en el mundo-planeta.
     */
    private final Map<UUID, Double> ultimaAlturaY = new HashMap<>();

    public ShipMovementListener(JavaPlugin plugin, PlanetConfigManager configManager,
                                CoreNavesHook coreNavesHook, TeleportManager teleportManager,
                                EnvironmentManager environmentManager,
                                VisualFeedbackTask visualFeedbackTask) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.coreNavesHook = coreNavesHook;
        this.teleportManager = teleportManager;
        this.environmentManager = environmentManager;
        this.visualFeedbackTask = visualFeedbackTask;
    }

    @Override
    public void run() {
        CoreNavesAPI api = coreNavesHook.getApi();
        if (api == null) {
            return; // ni CoreNaves ni modo de respaldo: no hay nada que detectar
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // Descarte rapido: si el jugador no pilota nada, no seguimos.
            if (!api.estaPilotandoNave(player)) {
                ultimaAlturaY.remove(player.getUniqueId());
                continue;
            }

            Ship nave = api.getNaveDelJugador(player);
            if (nave == null || !nave.isPilotada()) {
                continue;
            }

            // Cooldown anti-bucle: si esta nave se acaba de teletransportar,
            // no comprobamos nada hasta que pase el cooldown configurado.
            if (teleportManager.estaEnCooldown(nave.getId())) {
                continue;
            }

            Location posicion = nave.getLocation();
            World mundoActual = posicion.getWorld();
            if (mundoActual == null) {
                continue;
            }

            if (mundoActual.getName().equals(configManager.getMundoEspacio())) {
                comprobarAterrizaje(player, nave, posicion);
            } else {
                comprobarDespegue(player, nave, posicion, mundoActual);
            }
        }
    }

    /**
     * Recorre los planetas configurados y comprueba si la posicion actual (en el
     * mundo "espacio") cae dentro del radio de deteccion de alguno de ellos.
     *
     * Se usa distancia AL CUADRADO comparada con radio^2: evita una raiz cuadrada
     * por planeta y por jugador en cada pasada, que es justo el tipo de coste
     * que se acumula cuando esto corre continuamente.
     */
    private void comprobarAterrizaje(Player piloto, Ship nave, Location posicionEspacio) {
        for (PlanetData planeta : configManager.getPlanetas().values()) {
            if (planeta.estaEnZonaDeDeteccion(posicionEspacio.getX(), posicionEspacio.getY(), posicionEspacio.getZ())) {
                teleportManager.aterrizarEnPlaneta(piloto, nave, planeta);
                return; // una nave solo puede aterrizar en un planeta a la vez
            }
        }
    }

    /**
     * Comprueba si el jugador, volando en un mundo-planeta y ASCENDIENDO, ha
     * superado la altura de despegue configurada para ese planeta.
     *
     * La condicion de "ascendiendo" importa: sin ella, una nave que entrara al
     * planeta por encima de la altura de escape (o que la cruzara cayendo)
     * dispararia un despegue inmediato no deseado.
     */
    private void comprobarDespegue(Player piloto, Ship nave, Location posicionPlaneta, World mundoActual) {
        UUID uuid = piloto.getUniqueId();
        double alturaActual = posicionPlaneta.getY();
        Double alturaAnterior = ultimaAlturaY.put(uuid, alturaActual);

        // Solo nos interesa el despegue si el jugador esta subiendo, no bajando.
        // En la primera pasada (sin altura anterior) no asumimos ascenso: esperamos
        // a tener dos medidas, para no despegar justo al aterrizar.
        if (alturaAnterior == null || alturaActual <= alturaAnterior) {
            return;
        }

        PlanetData planeta = configManager.getPlanetaPorMundo(mundoActual.getName());
        if (planeta == null) {
            return;
        }

        if (alturaActual >= planeta.getAlturaDespegueY()) {
            teleportManager.despegarDePlaneta(piloto, nave, planeta);
            ultimaAlturaY.remove(uuid);
        }
    }

    /** Limpieza de memoria: evita acumular entradas de jugadores desconectados. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        ultimaAlturaY.remove(uuid);
        environmentManager.limpiarJugador(uuid);
        visualFeedbackTask.limpiarJugador(uuid);
    }
}
