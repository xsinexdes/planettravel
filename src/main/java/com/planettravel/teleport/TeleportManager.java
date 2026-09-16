package com.planettravel.teleport;

import com.planettravel.api.CoreNavesHook;
import com.planettravel.api.Ship;
import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.environment.TemperatureManager;
import com.planettravel.util.WorldUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Encargado de EJECUTAR los teletransportes de naves entre el mundo "espacio"
 * y los mundos-planeta, aplicando:
 *   - precarga asincrona del chunk destino (evita el tiron del servidor)
 *   - cooldown por nave (evita bucles aterrizar/despegar en el mismo instante)
 *   - efecto PROLONGADO de entrada atmosferica (ver AtmosphericEntryEffect)
 *
 * Este es el UNICO sitio del plugin que llama a CoreNavesAPI.teletransportarNave(...).
 */
public class TeleportManager {

    private final JavaPlugin plugin;
    private final PlanetConfigManager configManager;
    private final CoreNavesHook coreNavesHook;
    private final TemperatureManager temperatureManager;

    // Ultima vez (epoch millis) que CADA nave (por UUID) fue teletransportada.
    // Se usa para el cooldown anti-bucle.
    private final Map<UUID, Long> ultimoTeletransporte = new HashMap<>();

    public TeleportManager(JavaPlugin plugin, PlanetConfigManager configManager,
                           CoreNavesHook coreNavesHook, TemperatureManager temperatureManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.coreNavesHook = coreNavesHook;
        this.temperatureManager = temperatureManager;
    }

    /** true si la nave indicada todavia esta "en cooldown". */
    public boolean estaEnCooldown(UUID naveId) {
        Long ultima = ultimoTeletransporte.get(naveId);
        if (ultima == null) {
            return false;
        }
        long cooldownMs = configManager.getCooldownTeletransporteSegundos() * 1000L;
        return (System.currentTimeMillis() - ultima) < cooldownMs;
    }

    private void marcarTeletransportada(UUID naveId) {
        ultimoTeletransporte.put(naveId, System.currentTimeMillis());
    }

    /**
     * ATERRIZAJE: mueve la nave del mundo "espacio" al mundo del planeta,
     * a la altura de entrada atmosferica configurada.
     */
    public void aterrizarEnPlaneta(Player piloto, Ship nave, PlanetData planeta) {
        if (coreNavesHook.getApi() == null) {
            piloto.sendMessage("§cNo se puede aterrizar: CoreNaves no esta disponible.");
            return;
        }

        World mundoPlaneta = WorldUtil.cargarSiHaceFalta(planeta.getWorldName(), plugin.getLogger());
        if (mundoPlaneta == null) {
            piloto.sendMessage("§cError interno: no se pudo cargar el mundo del planeta.");
            return;
        }

        // Conservamos el yaw/pitch de vuelo actual de la nave; solo cambiamos la
        // posicion al spawn configurado y fijamos la Y de entrada atmosferica.
        Location origen = nave.getLocation();
        Location destino = planeta.getSpawnPlanetaLocation(mundoPlaneta);
        destino.setYaw(origen.getYaw());
        destino.setPitch(origen.getPitch());
        destino.setY(clampAltura(planeta.getEntradaAtmosfericaY(), mundoPlaneta));

        // Marcamos el cooldown YA, antes de la carga asincrona, para que ninguna
        // comprobacion que corra mientras tanto dispare un segundo teletransporte.
        marcarTeletransportada(nave.getId());

        // Precargamos el chunk destino en segundo plano: teletransportar a un
        // chunk sin cargar congela el hilo principal, y con naves grandes se nota.
        // getChunkAtAsync completa el futuro cuando el chunk esta listo. Volvemos
        // explicitamente al hilo principal antes de tocar entidades o bloques:
        // teletransportar desde un hilo asincrono corrompe el estado del servidor.
        mundoPlaneta.getChunkAtAsync(destino).thenRun(() ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!piloto.isOnline() || !nave.isPilotada()) {
                return;
            }
            coreNavesHook.getApi().teletransportarNave(nave, destino, true);
            temperatureManager.resetear(piloto.getUniqueId());

            // Efecto de reentrada que dura varios segundos siguiendo a la nave.
            AtmosphericEntryEffect.lanzar(plugin, nave, piloto, mundoPlaneta.getName());

            piloto.sendTitle("§6§lENTRADA ATMOSFERICA",
                    "§f" + planeta.getNombreVisible(), 10, 50, 20);
            avisarPeligros(piloto, planeta);
        }));
    }

    /**
     * DESPEGUE: mueve la nave del mundo-planeta de vuelta al mundo "espacio",
     * en el punto de salida configurado para ese planeta.
     */
    public void despegarDePlaneta(Player piloto, Ship nave, PlanetData planeta) {
        if (coreNavesHook.getApi() == null) {
            piloto.sendMessage("§cNo se puede despegar: CoreNaves no esta disponible.");
            return;
        }

        World mundoEspacio = WorldUtil.cargarSiHaceFalta(configManager.getMundoEspacio(), plugin.getLogger());
        if (mundoEspacio == null) {
            piloto.sendMessage("§cError interno: no se pudo cargar el mundo espacio.");
            return;
        }

        Location origen = nave.getLocation();
        Location destino = planeta.getSalidaEspacioLocation(mundoEspacio);
        destino.setYaw(origen.getYaw());
        destino.setPitch(origen.getPitch());

        marcarTeletransportada(nave.getId());

        // Efecto de despegue en el punto de origen, antes de irse.
        World mundoOrigen = origen.getWorld();
        if (mundoOrigen != null) {
            mundoOrigen.spawnParticle(Particle.CLOUD, origen, 60, 2, 1, 2, 0.15);
            mundoOrigen.spawnParticle(Particle.FLAME, origen, 40, 1.5, 1, 1.5, 0.1);
            mundoOrigen.playSound(origen, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 0.7f);
        }

        mundoEspacio.getChunkAtAsync(destino).thenRun(() ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!piloto.isOnline() || !nave.isPilotada()) {
                return;
            }
            coreNavesHook.getApi().teletransportarNave(nave, destino, true);
            temperatureManager.resetear(piloto.getUniqueId());

            mundoEspacio.spawnParticle(Particle.END_ROD, destino, 50, 2, 2, 2, 0.05);
            mundoEspacio.playSound(destino, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

            piloto.sendTitle("§b§lESPACIO ABIERTO",
                    "§7Has dejado " + planeta.getNombreVisible(), 10, 40, 20);
        }));
    }

    /**
     * Al aterrizar, avisa al piloto de los peligros del planeta para que no
     * los descubra muriendose. Solo menciona lo que esta realmente activo.
     */
    private void avisarPeligros(Player piloto, PlanetData planeta) {
        var env = planeta.getEntorno();
        boolean hayPeligro = env.isRequiereCasco() || env.isAguaToxica() || env.isTemperaturaActiva();
        if (!hayPeligro) {
            return;
        }

        piloto.sendMessage("§8§m                                        ");
        piloto.sendMessage("§6⚠ §fCondiciones de §b" + planeta.getNombreVisible());

        if (env.isRequiereCasco()) {
            piloto.sendMessage("§c ✖ §7Sin atmosfera respirable. Necesitas §f"
                    + env.getMaterialCasco().toLowerCase().replace('_', ' ') + "§7.");
        }
        if (env.isAguaToxica()) {
            piloto.sendMessage("§2 ☣ §7El agua es corrosiva. No te metas.");
        }
        if (env.isTemperaturaActiva()) {
            piloto.sendMessage(String.format("§e 🌡 §7Temperatura base §f%.0f°C§7. Zona segura: §f%.0f a %.0f°C",
                    env.getTemperaturaBase(), env.getUmbralFrio(), env.getUmbralCalor()));
        }
        if (env.getGravedad() < 0.9) {
            piloto.sendMessage("§d ⬇ §7Gravedad baja. Cuidado con los saltos.");
        } else if (env.getGravedad() > 1.1) {
            piloto.sendMessage("§4 ⬇ §7Gravedad alta. Te moveras con esfuerzo.");
        }
        piloto.sendMessage("§8§m                                        ");
    }

    /**
     * Ajusta una altura configurada al limite real del mundo. Si el admin dejo
     * Y=1400 pero el mundo solo llega a 320, aparecer ahi seria caer al vacio:
     * preferimos colocar la nave justo bajo el techo del mundo y avisar en consola.
     */
    private double clampAltura(double altura, World mundo) {
        double maxSeguro = mundo.getMaxHeight() - 5;
        if (altura > maxSeguro) {
            plugin.getLogger().warning("La altura configurada (" + altura + ") supera el limite del mundo '"
                    + mundo.getName() + "' (" + mundo.getMaxHeight() + "). Se usara Y=" + maxSeguro + ".");
            return maxSeguro;
        }
        return altura;
    }

    public void limpiarNave(UUID naveId) {
        ultimoTeletransporte.remove(naveId);
    }
}
