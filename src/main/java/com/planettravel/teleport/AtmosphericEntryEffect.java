package com.planettravel.teleport;

import com.planettravel.api.Ship;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Efecto PROLONGADO de entrada atmosferica.
 *
 * En vez de un unico estallido de particulas en el instante del teletransporte,
 * esta tarea sigue a la nave durante varios segundos mientras "cae", generando
 * una estela de fuego y humo que se va apagando. Da mucha mas sensacion de
 * reentrada real que un flash instantaneo.
 *
 * La tarea se auto-cancela cuando se acaban los ticks, cuando el piloto
 * desconecta o cuando cambia de mundo (porque entonces el efecto ya no
 * tiene sentido).
 */
public class AtmosphericEntryEffect extends BukkitRunnable {

    /** Duracion total del efecto, en ticks (20 ticks = 1 segundo). */
    private static final int DURACION_TICKS = 90; // 4,5 segundos

    /** Cada cuantos ticks corre esta tarea. */
    public static final int PERIODO_TICKS = 2;

    private final Ship nave;
    private final Player piloto;
    private final String mundoEsperado;

    private int ticksTranscurridos = 0;

    public AtmosphericEntryEffect(Ship nave, Player piloto, String mundoEsperado) {
        this.nave = nave;
        this.piloto = piloto;
        this.mundoEsperado = mundoEsperado;
    }

    /** Arranca el efecto sobre una nave recien aterrizada. */
    public static void lanzar(JavaPlugin plugin, Ship nave, Player piloto, String mundo) {
        new AtmosphericEntryEffect(nave, piloto, mundo)
                .runTaskTimer(plugin, 0L, PERIODO_TICKS);
    }

    @Override
    public void run() {
        ticksTranscurridos += PERIODO_TICKS;

        // Condiciones de parada
        if (ticksTranscurridos >= DURACION_TICKS || piloto == null || !piloto.isOnline()) {
            cancel();
            return;
        }
        if (!piloto.getWorld().getName().equals(mundoEsperado)) {
            cancel(); // cambio de mundo: el efecto ya no aplica
            return;
        }

        Location loc = nave.getLocation();
        World mundo = loc.getWorld();
        if (mundo == null) {
            cancel();
            return;
        }

        // La intensidad va de 1.0 al inicio a 0.0 al final: la estela se apaga
        // gradualmente conforme la nave "frena" en la atmosfera.
        double intensidad = 1.0 - ((double) ticksTranscurridos / DURACION_TICKS);

        generarEstela(mundo, loc, intensidad);
        generarSonido(mundo, loc, intensidad);
        sacudirPantalla(intensidad);
    }

    /**
     * Estela de fuego y humo. Las particulas se generan ligeramente POR ENCIMA
     * de la nave y con velocidad hacia arriba, para que parezca que la nave las
     * va dejando atras mientras desciende.
     */
    private void generarEstela(World mundo, Location loc, double intensidad) {
        int cantidadFuego = (int) Math.max(2, 20 * intensidad);
        int cantidadHumo = (int) Math.max(2, 14 * intensidad);

        Location estela = loc.clone().add(0, 1.0, 0);

        mundo.spawnParticle(Particle.FLAME, estela, cantidadFuego, 1.2, 0.6, 1.2, 0.04);
        mundo.spawnParticle(Particle.LARGE_SMOKE, estela, cantidadHumo, 1.4, 0.8, 1.4, 0.02);

        // Chispas naranjas que caen: el "desprendimiento" del escudo termico.
        Particle.DustOptions naranja = new Particle.DustOptions(
                Color.fromRGB(255, (int) (120 * intensidad) + 60, 20),
                (float) (2.0 * intensidad + 0.8));
        mundo.spawnParticle(Particle.DUST, estela, (int) Math.max(3, 16 * intensidad),
                1.6, 1.0, 1.6, 0, naranja);

        // Al principio del efecto, un halo de fuego mas dramatico alrededor.
        if (intensidad > 0.6) {
            mundo.spawnParticle(Particle.LAVA, estela, 3, 1.0, 0.5, 1.0, 0);
            mundo.spawnParticle(Particle.FALLING_LAVA, estela, 5, 1.5, 0.5, 1.5, 0);
        }
    }

    private void generarSonido(World mundo, Location loc, double intensidad) {
        // Rugido de friccion continuo, cada 6 ticks para no saturar de sonidos.
        if (ticksTranscurridos % 6 == 0) {
            mundo.playSound(loc, Sound.ENTITY_BLAZE_BURN, (float) (1.2 * intensidad), 0.5f);
            mundo.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, (float) (1.5 * intensidad), 0.7f);
        }

        // Golpe inicial de entrada
        if (ticksTranscurridos <= PERIODO_TICKS) {
            mundo.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
            mundo.playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.5f);
        }

        // Sonido de "estabilizacion" al terminar
        if (ticksTranscurridos >= DURACION_TICKS - PERIODO_TICKS * 2) {
            mundo.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.8f);
        }
    }

    /**
     * Sacudida de camara simulada: aplicamos un empujon minusculo y aleatorio
     * al piloto. Es suficiente para que la pantalla tiemble sin llegar a mover
     * la nave de sitio ni descontrolar el vuelo.
     */
    private void sacudirPantalla(double intensidad) {
        if (intensidad < 0.3) {
            return; // ya casi no vibra al final
        }
        double fuerza = 0.035 * intensidad;
        piloto.setVelocity(piloto.getVelocity().add(new org.bukkit.util.Vector(
                (Math.random() - 0.5) * fuerza,
                0,
                (Math.random() - 0.5) * fuerza
        )));
    }
}
