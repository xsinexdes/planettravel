package com.planettravel.environment;

import com.planettravel.config.PlanetEnvironment;
import com.planettravel.config.SuitTier;
import com.planettravel.space.EnvironmentResolver.Contexto;
import com.planettravel.suit.SuitManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema de TEMPERATURA (planetas, espacio y bases).
 *
 * La temperatura ambiente en un punto se calcula asi:
 *
 *   base efectiva      = temperatura base + efecto invernadero (solo si la presion > 1 atm)
 *   + oscilacion dia/noche  (se AMPLIFICA con poca atmosfera y se amortigua con mucha)
 *   + gradiente por altura  (escala con la gravedad)
 *   + modificadores locales (lava/fuego calientan, agua/hielo enfrian)
 *   + lluvia a la intemperie
 *
 * Dentro de una base con oxigeno la temperatura es directamente la de la base.
 *
 * La temperatura del jugador NO salta: se interpola hacia la ambiente, asi
 * entrar a una cueva o a una base da un alivio progresivo.
 */
public class TemperatureManager {

    private static final double VELOCIDAD_ADAPTACION = 0.15;
    private static final double TEMPERATURA_INICIAL = 20.0;

    private final SuitManager suits;
    private final Map<UUID, Double> temperaturaJugador = new HashMap<>();

    public TemperatureManager(SuitManager suits) {
        this.suits = suits;
    }

    /** Temperatura AMBIENTE en un punto, sin contar la ropa del jugador. */
    public double calcularTemperaturaAmbiente(Contexto ctx, Location loc) {
        // Dentro de una base con oxigeno manda el termostato de la base.
        if (ctx.habitat() && ctx.base() != null) {
            return ctx.base().getTemperatura();
        }

        PlanetEnvironment env = ctx.env();
        World mundo = loc.getWorld();
        if (mundo == null) {
            return env.getBaseEfectiva();
        }

        double temperatura = env.getBaseEfectiva();

        // Ciclo dia/noche con coseno: mediodia (6000) = maximo, medianoche (18000) = minimo.
        long hora = mundo.getTime();
        double fase = (hora - 6000) / 24000.0 * 2 * Math.PI;
        temperatura += Math.cos(fase) * env.getVariacionEfectiva();

        // Altura
        temperatura += env.getVariacionAlturaEfectiva() * (loc.getY() / 100.0);

        // Entorno inmediato
        temperatura += modificadorLocal(loc);

        // Lluvia
        if (mundo.hasStorm() && loc.getBlockY() >= mundo.getHighestBlockYAt(loc)) {
            temperatura -= 5.0;
        }

        return temperatura;
    }

    /**
     * Mira una rejilla pequenia (3x3x3 con paso 2) alrededor del jugador:
     * lava/fuego calientan, agua/hielo enfrian. Barato a proposito: corre cada
     * segundo por cada jugador afectado.
     */
    private double modificadorLocal(Location loc) {
        double modificador = 0;
        World mundo = loc.getWorld();
        if (mundo == null) return 0;

        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();

        for (int x = -2; x <= 2; x += 2) {
            for (int y = -2; y <= 2; y += 2) {
                for (int z = -2; z <= 2; z += 2) {
                    Material mat = mundo.getBlockAt(bx + x, by + y, bz + z).getType();
                    switch (mat) {
                        case LAVA, MAGMA_BLOCK, FIRE, CAMPFIRE, LANTERN -> modificador += 4.0;
                        case WATER -> modificador -= 2.0;
                        case ICE, PACKED_ICE, BLUE_ICE, SNOW, SNOW_BLOCK, POWDER_SNOW -> modificador -= 3.0;
                        default -> { }
                    }
                }
            }
        }

        if (by < mundo.getHighestBlockYAt(loc) - 3) {
            modificador *= 0.5;
        }
        return modificador;
    }

    /**
     * Proteccion termica de la armadura: [frio, calor] en grados.
     *
     *  - Piezas de TRAJE ESPACIAL: aportan los valores de su tier (editables).
     *  - Cuero: abriga mucho, da algo de calor.
     *  - Netherite: resiste el calor.
     *  - Casco clasico del planeta (modo tier 0): aisla de ambos.
     *  - Cualquier otra armadura: aislamiento medio.
     */
    public double[] calcularProteccion(Player jugador, PlanetEnvironment env) {
        double frio = 0;
        double calor = 0;
        double porPieza = env.getProteccionPorPieza();
        Material matCasco = Material.matchMaterial(env.getMaterialCasco());

        for (ItemStack pieza : jugador.getInventory().getArmorContents()) {
            if (pieza == null || pieza.getType() == Material.AIR) {
                continue;
            }

            SuitTier traje = suits.getDefinicion(pieza);
            if (traje != null) {
                frio += traje.getProteccionFrio();
                calor += traje.getProteccionCalor();
                continue;
            }

            Material tipo = pieza.getType();
            String nombre = tipo.name();

            if (nombre.startsWith("LEATHER_")) {
                frio += porPieza;
                calor += porPieza * 0.25;
            } else if (nombre.startsWith("NETHERITE_")) {
                frio += porPieza * 0.75;
                calor += porPieza;
            } else if (env.getTierCasco() <= 0 && matCasco != null && tipo == matCasco) {
                frio += porPieza * 1.5;
                calor += porPieza * 1.5;
            } else {
                frio += porPieza * 0.5;
                calor += porPieza * 0.5;
            }
        }
        return new double[]{frio, calor};
    }

    /** Actualiza y devuelve la temperatura del jugador. */
    public double actualizar(Player jugador, Contexto ctx) {
        UUID uuid = jugador.getUniqueId();
        double ambiente = calcularTemperaturaAmbiente(ctx, jugador.getLocation());

        PlanetEnvironment env = ctx.env();
        double[] proteccion = calcularProteccion(jugador, env);

        // La armadura acerca a la zona segura, nunca mas alla de ella.
        double objetivo = ambiente;
        if (ambiente < env.getUmbralFrio()) {
            objetivo = Math.min(env.getUmbralFrio() + 5, ambiente + proteccion[0]);
        } else if (ambiente > env.getUmbralCalor()) {
            objetivo = Math.max(env.getUmbralCalor() - 5, ambiente - proteccion[1]);
        }

        double actual = temperaturaJugador.getOrDefault(uuid, TEMPERATURA_INICIAL);
        double nueva = actual + (objetivo - actual) * VELOCIDAD_ADAPTACION;
        temperaturaJugador.put(uuid, nueva);
        return nueva;
    }

    public double getTemperatura(UUID uuid) {
        return temperaturaJugador.getOrDefault(uuid, TEMPERATURA_INICIAL);
    }

    public void resetear(UUID uuid) {
        temperaturaJugador.put(uuid, TEMPERATURA_INICIAL);
    }

    public void limpiar(UUID uuid) {
        temperaturaJugador.remove(uuid);
    }

    public String colorPara(double temperatura, PlanetEnvironment env) {
        if (temperatura >= env.getUmbralCalor()) return "§c";
        if (temperatura >= env.getUmbralCalor() - 10) return "§6";
        if (temperatura <= env.getUmbralFrio()) return "§b";
        if (temperatura <= env.getUmbralFrio() + 10) return "§3";
        return "§a";
    }
}
