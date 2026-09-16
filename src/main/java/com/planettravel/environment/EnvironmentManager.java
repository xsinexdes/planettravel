package com.planettravel.environment;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.config.PlanetEnvironment;
import com.planettravel.util.ChatUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tarea periodica que simula el entorno de cada planeta sobre los jugadores
 * que estan en el: casco espacial, agua toxica, gravedad, temperatura y
 * efectos permanentes.
 *
 * RENDIMIENTO: corre cada "entorno.intervalo-ticks" (20 por defecto = 1 vez
 * por segundo) y solo recorre jugadores que esten EN un mundo-planeta
 * configurado. Los jugadores en el espacio o en mundos normales se descartan
 * en la primera comprobacion.
 */
public class EnvironmentManager extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final PlanetConfigManager configManager;
    private final TemperatureManager temperatureManager;

    /** Ultimo aviso de asfixia/temperatura por jugador, para no spamear sonidos. */
    private final Map<UUID, Long> ultimoAviso = new HashMap<>();

    public EnvironmentManager(JavaPlugin plugin, PlanetConfigManager configManager,
                              TemperatureManager temperatureManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.temperatureManager = temperatureManager;
    }

    @Override
    public void run() {
        for (Player jugador : plugin.getServer().getOnlinePlayers()) {
            World mundo = jugador.getWorld();
            PlanetData planeta = configManager.getPlanetaPorMundo(mundo.getName());

            // Descarte rapido: no esta en ningun mundo-planeta configurado.
            if (planeta == null) {
                temperatureManager.limpiar(jugador.getUniqueId());
                continue;
            }

            // Los admins en creativo/espectador no sufren el entorno.
            if (jugador.getGameMode().name().equals("CREATIVE")
                    || jugador.getGameMode().name().equals("SPECTATOR")) {
                continue;
            }

            PlanetEnvironment env = planeta.getEntorno();

            aplicarCasco(jugador, env);
            aplicarAgua(jugador, env);
            aplicarGravedad(jugador, env);
            aplicarEfectosPermanentes(jugador, env);
            aplicarTemperatura(jugador, planeta, env);
            aplicarClimaYHora(mundo, env);
        }
    }

    // ------------------------------------------------------------
    //  CASCO ESPACIAL / ATMOSFERA
    // ------------------------------------------------------------

    private void aplicarCasco(Player jugador, PlanetEnvironment env) {
        if (!env.isRequiereCasco()) {
            return;
        }
        if (tieneProteccionRespiratoria(jugador, env)) {
            return;
        }

        // Sin casco en un planeta sin atmosfera respirable: asfixia progresiva.
        jugador.damage(env.getDanioSinCasco());
        jugador.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, true, false));
        jugador.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true, false));

        if (avisar(jugador, 3000)) {
            ChatUtil.actionBar(jugador, "§4✖ §cSin oxigeno §7— necesitas un casco espacial");
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1.0f, 0.6f);
        }
    }

    /** true si el jugador lleva el casco (y el traje, si el planeta lo exige). */
    private boolean tieneProteccionRespiratoria(Player jugador, PlanetEnvironment env) {
        Material requerido = Material.matchMaterial(env.getMaterialCasco());
        if (requerido == null) {
            return true; // material mal configurado: no castigamos al jugador
        }

        ItemStack casco = jugador.getInventory().getHelmet();
        if (casco == null || casco.getType() != requerido) {
            return false;
        }

        if (!env.isRequiereTrajeCompleto()) {
            return true;
        }

        // Traje completo: las cuatro piezas puestas (del tipo que sean)
        for (ItemStack pieza : jugador.getInventory().getArmorContents()) {
            if (pieza == null || pieza.getType() == Material.AIR) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------
    //  AGUA TOXICA
    // ------------------------------------------------------------

    private void aplicarAgua(Player jugador, PlanetEnvironment env) {
        if (!env.isAguaToxica()) {
            return;
        }

        Material bloque = jugador.getLocation().getBlock().getType();
        boolean enAgua = (bloque == Material.WATER) || jugador.isInWater();
        if (!enAgua) {
            return;
        }

        jugador.damage(env.getDanioAgua());

        if (env.isAguaEnvenena()) {
            // El veneno persiste unos segundos tras salir: es el "residuo" toxico.
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 140, 0, true, true));
        }

        jugador.getWorld().spawnParticle(Particle.ITEM_SLIME, jugador.getLocation().add(0, 1, 0),
                8, 0.4, 0.6, 0.4, 0.01);

        if (avisar(jugador, 2000)) {
            ChatUtil.actionBar(jugador, "§2☣ §aEl agua de este planeta es corrosiva");
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 0.8f, 1.4f);
        }
    }

    // ------------------------------------------------------------
    //  GRAVEDAD
    // ------------------------------------------------------------

    /**
     * Minecraft no deja cambiar la gravedad de un jugador directamente, asi que
     * la simulamos con efectos:
     *   - gravedad BAJA (< 1): salto potenciado + caida lenta -> sensacion lunar
     *   - gravedad ALTA (> 1): lentitud + fatiga al minar, y un empujon hacia
     *     abajo para que las caidas se sientan mas pesadas
     */
    private void aplicarGravedad(Player jugador, PlanetEnvironment env) {
        double g = env.getGravedad();

        // Zona neutra: no tocamos nada si la gravedad es practicamente terrestre.
        if (g > 0.9 && g < 1.1) {
            return;
        }

        int duracion = 60; // algo mas que el intervalo de la tarea, para que no parpadee

        if (g < 0.9) {
            // Cuanto menor la gravedad, mayor el nivel de salto.
            int nivelSalto = (int) Math.round((1.0 - g) * 4);
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,
                    duracion, Math.max(0, nivelSalto), true, false));
            if (!jugador.isOnGround() && jugador.getVelocity().getY() < 0) {
                jugador.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,
                        duracion, 0, true, false));
            }
        } else {
            // Gravedad aplastante: te mueves con esfuerzo y caes mas rapido.
            int nivel = (int) Math.round((g - 1.0) * 2);
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    duracion, Math.max(0, nivel), true, false));
            if (!jugador.isOnGround()) {
                jugador.setVelocity(jugador.getVelocity().add(
                        new org.bukkit.util.Vector(0, -0.08 * (g - 1.0), 0)));
            }
        }
    }

    // ------------------------------------------------------------
    //  EFECTOS PERMANENTES CONFIGURADOS
    // ------------------------------------------------------------

    private void aplicarEfectosPermanentes(Player jugador, PlanetEnvironment env) {
        for (Map.Entry<String, Integer> entrada : env.getEfectosPermanentes().entrySet()) {
            PotionEffectType tipo = PotionEffectType.getByName(entrada.getKey());
            if (tipo == null) {
                continue; // nombre mal escrito en el config: lo ignoramos en silencio
            }
            int amplificador = Math.max(0, entrada.getValue() - 1);
            jugador.addPotionEffect(new PotionEffect(tipo, 60, amplificador, true, false));
        }
    }

    // ------------------------------------------------------------
    //  TEMPERATURA
    // ------------------------------------------------------------

    private void aplicarTemperatura(Player jugador, PlanetData planeta, PlanetEnvironment env) {
        if (!env.isTemperaturaActiva()) {
            return;
        }

        double temperatura = temperatureManager.actualizar(jugador, planeta);
        String color = temperatureManager.colorPara(temperatura, env);

        // Mostramos la temperatura en el actionbar solo si no hay otro aviso
        // mas urgente ocupando ese espacio.
        ChatUtil.actionBar(jugador, String.format("%s%.0f°C §7| §f%s", color, temperatura, planeta.getNombreVisible()));

        if (temperatura >= env.getUmbralCalor()) {
            // Cuanto mas te pasas del umbral, mas danio. Escala suave.
            double exceso = temperatura - env.getUmbralCalor();
            double danio = env.getDanioTemperatura() * (1 + exceso / 20.0);
            jugador.damage(danio);
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 1, true, false));
            jugador.getWorld().spawnParticle(Particle.SMOKE,
                    jugador.getLocation().add(0, 1.8, 0), 4, 0.3, 0.3, 0.3, 0.01);

            if (avisar(jugador, 4000)) {
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.6f, 1.2f);
            }

        } else if (temperatura <= env.getUmbralFrio()) {
            double exceso = env.getUmbralFrio() - temperatura;
            double danio = env.getDanioTemperatura() * (1 + exceso / 20.0);
            jugador.damage(danio);
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0, true, false));
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 0, true, false));
            jugador.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    jugador.getLocation().add(0, 1.8, 0), 6, 0.3, 0.3, 0.3, 0.01);

            if (avisar(jugador, 4000)) {
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 0.8f, 0.6f);
            }
        }
    }

    // ------------------------------------------------------------
    //  CLIMA Y HORA FIJADOS
    // ------------------------------------------------------------

    private void aplicarClimaYHora(World mundo, PlanetEnvironment env) {
        if (env.isBloquearClima()) {
            if (mundo.hasStorm() != env.isLluviaPermanente()) {
                mundo.setStorm(env.isLluviaPermanente());
            }
            mundo.setWeatherDuration(Integer.MAX_VALUE);
        }

        if (env.isBloquearHora()) {
            if (mundo.getTime() != env.getHoraFija()) {
                mundo.setTime(env.getHoraFija());
            }
        }
    }

    // ------------------------------------------------------------

    /** Evita spamear sonidos/mensajes: true si ya paso el intervalo dado. */
    private boolean avisar(Player jugador, long intervaloMs) {
        long ahora = System.currentTimeMillis();
        Long anterior = ultimoAviso.get(jugador.getUniqueId());
        if (anterior != null && (ahora - anterior) < intervaloMs) {
            return false;
        }
        ultimoAviso.put(jugador.getUniqueId(), ahora);
        return true;
    }

    public void limpiarJugador(UUID uuid) {
        ultimoAviso.remove(uuid);
        temperatureManager.limpiar(uuid);
    }
}
