package com.planettravel.environment;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.config.PlanetEnvironment;
import com.planettravel.config.SpaceSettings;
import com.planettravel.space.EnvironmentResolver;
import com.planettravel.space.EnvironmentResolver.Contexto;
import com.planettravel.suit.SuitManager;
import com.planettravel.util.ChatUtil;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ambiente: lo que hace que el espacio y los planetas SE SIENTAN vivos.
 *
 *  - Espacio: polvo estelar (particulas) alrededor del jugador.
 *  - Tormentas solares: cada X minutos, radiacion que daña a quien esta fuera
 *    de una base y sin traje del tier configurado.
 *  - Planetas: particulas ambientales (polvo, ceniza, nieve, esporas, brasas).
 *  - Planetas: lluvia de meteoritos (visual + danio en el impacto, sin romper bloques).
 *
 * Corre cada 10 ticks (dos pasadas = un segundo).
 */
public class AmbientTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final PlanetConfigManager config;
    private final EnvironmentResolver resolver;
    private final SuitManager suits;

    private int pasada = 0;
    private long tormentaHasta = 0;
    private long proximaTormenta = 0;
    private final Map<UUID, Long> proximoMeteorito = new HashMap<>();
    private final Map<UUID, Long> proximoMeteoritoEspacio = new HashMap<>();

    public AmbientTask(JavaPlugin plugin, PlanetConfigManager config,
                       EnvironmentResolver resolver, SuitManager suits) {
        this.plugin = plugin;
        this.config = config;
        this.resolver = resolver;
        this.suits = suits;
    }

    @Override
    public void run() {
        pasada++;
        boolean cadaSegundo = (pasada % 2 == 0);
        long ahora = System.currentTimeMillis();
        SpaceSettings espacio = config.getSpaceSettings();

        gestionarTormentaSolar(espacio, ahora);
        boolean tormentaActiva = ahora < tormentaHasta;

        for (Player jugador : plugin.getServer().getOnlinePlayers()) {
            if (jugador.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            World mundo = jugador.getWorld();

            if (mundo.getName().equals(config.getMundoEspacio())) {
                if (!espacio.isActivo()) {
                    continue;
                }
                if (espacio.isEstrellas()) {
                    polvoEstelar(jugador, espacio.getDensidadEstrellas());
                }
                if (tormentaActiva && cadaSegundo) {
                    aplicarRadiacion(jugador, espacio);
                }
                if (espacio.isMeteoritosEspacio() && cadaSegundo) {
                    gestionarMeteoritoEspacio(jugador, espacio, ahora);
                }
                continue;
            }

            PlanetData planeta = config.getPlanetaPorMundo(mundo.getName());
            if (planeta == null) {
                continue;
            }
            PlanetEnvironment env = planeta.getEntorno();
            particulasAmbientales(jugador, env);
            if (env.isMeteoritos() && cadaSegundo) {
                gestionarMeteorito(jugador, env, ahora);
            }
        }
    }

    // ------------------------------------------------------------
    //  ESPACIO
    // ------------------------------------------------------------

    private void polvoEstelar(Player jugador, int densidad) {
        Location loc = jugador.getLocation();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < densidad; i++) {
            double x = loc.getX() + (r.nextDouble() * 2 - 1) * 14;
            double y = loc.getY() + (r.nextDouble() * 2 - 1) * 8 + 1;
            double z = loc.getZ() + (r.nextDouble() * 2 - 1) * 14;
            jugador.spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0, 0, 0.0);
        }
    }

    private void gestionarTormentaSolar(SpaceSettings espacio, long ahora) {
        if (!espacio.isActivo() || !espacio.isTormentasSolares()) {
            proximaTormenta = 0;
            return;
        }
        if (proximaTormenta == 0) {
            proximaTormenta = ahora + espacio.getTormentaIntervaloMin() * 60_000L;
            return;
        }
        if (ahora < proximaTormenta || ahora < tormentaHasta) {
            return;
        }

        tormentaHasta = ahora + espacio.getTormentaDuracionSeg() * 1000L;
        proximaTormenta = tormentaHasta + espacio.getTormentaIntervaloMin() * 60_000L;

        for (Player jugador : plugin.getServer().getOnlinePlayers()) {
            if (jugador.getWorld().getName().equals(config.getMundoEspacio())) {
                jugador.sendTitle("§6§l☀ TORMENTA SOLAR", "§7Refugiate en una base o usa un traje tier "
                        + espacio.getTormentaTierProteccion() + "+", 10, 60, 20);
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
            }
        }
    }

    private void aplicarRadiacion(Player jugador, SpaceSettings espacio) {
        if (jugador.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        Contexto ctx = resolver.resolver(jugador.getLocation());
        if (ctx == null || ctx.habitat()) {
            return; // las bases protegen de la radiacion
        }
        if (suits.getTierMinimoTraje(jugador) >= espacio.getTormentaTierProteccion()) {
            ChatUtil.actionBar(jugador, "§6☀ Tormenta solar §7- §atu traje te protege");
            return;
        }

        jugador.damage(espacio.getTormentaDanio());
        jugador.getWorld().spawnParticle(Particle.FLAME, jugador.getLocation().add(0, 1, 0), 6, 0.3, 0.5, 0.3, 0.02);
        ChatUtil.actionBar(jugador, "§4☢ Radiacion solar §7- §cbuscando refugio o un traje tier "
                + espacio.getTormentaTierProteccion() + "+");
    }

    // ------------------------------------------------------------
    //  PLANETAS
    // ------------------------------------------------------------

    private void particulasAmbientales(Player jugador, PlanetEnvironment env) {
        String tipo = env.getParticulasAmbiente();
        if (tipo == null || tipo.equals("NONE")) {
            return;
        }
        switch (tipo) {
            case "POLVO" -> alrededor(jugador, Particle.DUST, 12,
                    new Particle.DustOptions(Color.fromRGB(190, 140, 90), 1.6f));
            case "CENIZA" -> alrededor(jugador, Particle.ASH, 14, null);
            case "NIEVE" -> alrededor(jugador, Particle.SNOWFLAKE, 10, null);
            case "ESPORAS" -> alrededor(jugador, Particle.WARPED_SPORE, 10, null);
            case "BRASAS" -> alrededor(jugador, Particle.FLAME, 3, null);
            default -> { }
        }
    }

    private void alrededor(Player jugador, Particle particula, int cantidad, Object datos) {
        Location loc = jugador.getLocation().add(0, 2, 0);
        jugador.spawnParticle(particula, loc, cantidad, 8, 4, 8, 0.02, datos);
    }

    // ------------------------------------------------------------
    //  METEORITOS
    // ------------------------------------------------------------

    private void gestionarMeteorito(Player jugador, PlanetEnvironment env, long ahora) {
        Long proximo = proximoMeteorito.get(jugador.getUniqueId());
        if (proximo == null) {
            proximoMeteorito.put(jugador.getUniqueId(), ahora + intervalo(env));
            return;
        }
        if (ahora < proximo) {
            return;
        }
        proximoMeteorito.put(jugador.getUniqueId(), ahora + intervalo(env));
        lanzarMeteorito(jugador, env);
    }

    private long intervalo(PlanetEnvironment env) {
        double variacion = 0.6 + ThreadLocalRandom.current().nextDouble() * 0.8; // 0.6x a 1.4x
        return (long) (env.getMeteoritosIntervaloSeg() * 1000L * variacion);
    }

    private void lanzarMeteorito(Player jugador, PlanetEnvironment env) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        World mundo = jugador.getWorld();
        Location base = jugador.getLocation();

        double angulo = r.nextDouble() * Math.PI * 2;
        double distancia = 20 + r.nextDouble() * 35;
        int ix = (int) Math.floor(base.getX() + Math.cos(angulo) * distancia);
        int iz = (int) Math.floor(base.getZ() + Math.sin(angulo) * distancia);

        // Solo en chunks ya cargados: no queremos que un meteorito cargue terreno.
        if (!mundo.isChunkLoaded(ix >> 4, iz >> 4)) {
            return;
        }

        int iy = mundo.getHighestBlockYAt(ix, iz) + 1;
        final Location impacto = new Location(mundo, ix + 0.5, iy, iz + 0.5);
        final Location inicio = impacto.clone().add(r.nextDouble() * 50 - 25, 70, r.nextDouble() * 50 - 25);
        final Vector recorrido = impacto.toVector().subtract(inicio.toVector());
        final int ticksVuelo = 40;
        final double danio = env.getDanioMeteorito();

        ChatUtil.actionBar(jugador, "§6☄ Meteorito entrante...");
        mundo.playSound(inicio, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 3.0f, 0.5f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick++;
                double f = tick / (double) ticksVuelo;
                Location p = inicio.clone().add(recorrido.clone().multiply(f));
                mundo.spawnParticle(Particle.FLAME, p, 8, 0.4, 0.4, 0.4, 0.02);
                mundo.spawnParticle(Particle.SMOKE, p, 5, 0.3, 0.3, 0.3, 0.01);

                if (tick >= ticksVuelo) {
                    impactar(mundo, impacto, danio);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Impacto visual + danio a lo que este cerca. NO rompe bloques ni prende fuego. */
    private void impactar(World mundo, Location impacto, double danio) {
        mundo.playSound(impacto, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.7f);
        mundo.spawnParticle(Particle.FLAME, impacto, 50, 1.5, 0.8, 1.5, 0.1);
        mundo.spawnParticle(Particle.SMOKE, impacto, 40, 1.5, 1.0, 1.5, 0.05);
        mundo.spawnParticle(Particle.LAVA, impacto, 20, 1.0, 0.5, 1.0, 0.0);

        if (danio <= 0) {
            return;
        }
        for (Entity e : mundo.getNearbyEntities(impacto, 3, 3, 3)) {
            if (e instanceof LivingEntity vivo) {
                vivo.damage(danio);
            }
        }
    }

    public void limpiarJugador(UUID uuid) {
        proximoMeteorito.remove(uuid);
        proximoMeteoritoEspacio.remove(uuid);
    }

    // ------------------------------------------------------------
    //  METEORITOS EN EL ESPACIO ABIERTO
    // ------------------------------------------------------------
    // Igual que en un planeta (visual + danio en el impacto, sin romper nada),
    // pero sin "suelo": el impacto ocurre cerca del propio jugador, como una
    // roca que pasa rozando la nave/estacion en pleno vacio.

    private void gestionarMeteoritoEspacio(Player jugador, SpaceSettings espacio, long ahora) {
        UUID uuid = jugador.getUniqueId();
        Long proximo = proximoMeteoritoEspacio.get(uuid);
        if (proximo == null) {
            proximoMeteoritoEspacio.put(uuid, ahora + intervaloEspacio(espacio));
            return;
        }
        if (ahora < proximo) {
            return;
        }
        proximoMeteoritoEspacio.put(uuid, ahora + intervaloEspacio(espacio));
        lanzarMeteoritoEspacio(jugador, espacio);
    }

    private long intervaloEspacio(SpaceSettings espacio) {
        double variacion = 0.6 + ThreadLocalRandom.current().nextDouble() * 0.8; // 0.6x a 1.4x
        return (long) (espacio.getMeteoritosEspacioIntervaloSeg() * 1000L * variacion);
    }

    private void lanzarMeteoritoEspacio(Player jugador, SpaceSettings espacio) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        World mundo = jugador.getWorld();
        Location base = jugador.getLocation();

        if (!mundo.isChunkLoaded(base.getBlockX() >> 4, base.getBlockZ() >> 4)) {
            return;
        }

        // Sin suelo que apuntar: el impacto ocurre en un punto cercano al
        // jugador, en cualquier direccion (arriba, abajo, a los lados).
        double angulo = r.nextDouble() * Math.PI * 2;
        double distancia = 6 + r.nextDouble() * 14;
        double alturaExtra = (r.nextDouble() * 2 - 1) * 8;
        final Location impacto = base.clone().add(Math.cos(angulo) * distancia, alturaExtra, Math.sin(angulo) * distancia);
        final Location inicio = impacto.clone().add(r.nextDouble() * 60 - 30, r.nextDouble() * 60 - 30, r.nextDouble() * 60 - 30);
        final Vector recorrido = impacto.toVector().subtract(inicio.toVector());
        final int ticksVuelo = 30;
        final double danio = espacio.getDanioMeteoritoEspacio();

        ChatUtil.actionBar(jugador, "§6☄ Meteorito cruzando el espacio...");
        mundo.playSound(inicio, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 0.4f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick++;
                double f = tick / (double) ticksVuelo;
                Location p = inicio.clone().add(recorrido.clone().multiply(f));
                mundo.spawnParticle(Particle.FLAME, p, 6, 0.3, 0.3, 0.3, 0.01);
                mundo.spawnParticle(Particle.END_ROD, p, 3, 0.2, 0.2, 0.2, 0.01);

                if (tick >= ticksVuelo) {
                    impactar(mundo, impacto, danio);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
