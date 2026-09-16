package com.planettravel.environment;

import com.planettravel.config.PlanetData;
import com.planettravel.config.PlanetEnvironment;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema de TEMPERATURA por planeta.
 *
 * La temperatura que siente un jugador se calcula asi:
 *
 *   temperatura_ambiente = base
 *                        + variacion_dia_noche (segun la hora del mundo)
 *                        + variacion_por_altura * (altura / 100)
 *                        + modificadores locales (lava cerca, agua, lluvia, cueva)
 *
 *   temperatura_sentida = temperatura_ambiente ajustada por la armadura del jugador,
 *                         que "tira" del valor hacia la zona segura.
 *
 * Ademas, la temperatura del jugador NO salta de golpe: se interpola poco a poco
 * hacia la temperatura sentida (ver {@link #actualizar}). Asi, entrar en una cueva
 * fresca en un planeta ardiente da un alivio progresivo, no instantaneo — y salir
 * al exterior no te mata en el primer tick.
 */
public class TemperatureManager {

    /**
     * Cuanto se acerca la temperatura del jugador a la del ambiente en cada
     * actualizacion (0..1). 0.15 = transicion suave de unos pocos segundos.
     */
    private static final double VELOCIDAD_ADAPTACION = 0.15;

    /** Temperatura corporal "neutra" con la que arranca un jugador. */
    private static final double TEMPERATURA_INICIAL = 20.0;

    /** Temperatura actual sentida por cada jugador. */
    private final Map<UUID, Double> temperaturaJugador = new HashMap<>();

    /**
     * Calcula la temperatura AMBIENTE en una ubicacion concreta de un planeta,
     * sin tener en cuenta la ropa del jugador.
     */
    public double calcularTemperaturaAmbiente(PlanetData planeta, Location loc) {
        PlanetEnvironment env = planeta.getEntorno();
        World mundo = loc.getWorld();
        if (mundo == null) {
            return env.getTemperaturaBase();
        }

        double temperatura = env.getTemperaturaBase();

        // --- Ciclo dia/noche ---
        // getTime() va de 0 a 24000. Mediodia (6000) = mas calor,
        // medianoche (18000) = mas frio. Usamos un coseno para que la
        // transicion sea suave en lugar de un salto brusco al amanecer.
        long hora = mundo.getTime();
        double fase = (hora - 6000) / 24000.0 * 2 * Math.PI;
        temperatura += Math.cos(fase) * env.getVariacionDiaNoche();

        // --- Altura ---
        // Cuanto mas arriba, normalmente mas frio (variacion negativa).
        temperatura += env.getVariacionPorAltura() * (loc.getY() / 100.0);

        // --- Modificadores locales del entorno inmediato ---
        temperatura += modificadorLocal(loc);

        // --- Clima ---
        if (mundo.hasStorm() && loc.getBlockY() >= mundo.getHighestBlockYAt(loc)) {
            temperatura -= 5.0; // la lluvia enfria si estas a la intemperie
        }

        return temperatura;
    }

    /**
     * Revisa los bloques del entorno inmediato para ajustar la temperatura:
     * lava y fuego calientan, agua y hielo enfrian. Se mira una rejilla pequenia
     * (5x5x5 con paso 2) para no reventar el rendimiento: esto corre cada
     * segundo por cada jugador que este en un planeta con temperatura activa.
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

        // Estar bajo tierra estabiliza la temperatura hacia un valor templado
        if (by < mundo.getHighestBlockYAt(loc) - 3) {
            modificador *= 0.5;
        }

        return modificador;
    }

    /**
     * Calcula cuanta proteccion da la armadura del jugador.
     * Devuelve dos valores en un array: [proteccionFrio, proteccionCalor].
     *
     * - El cuero abriga: protege del frio.
     * - Cualquier armadura pesada da algo de aislamiento general.
     * - Un casco espacial (configurado en el planeta) protege de ambos extremos.
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
            Material tipo = pieza.getType();
            String nombre = tipo.name();

            if (nombre.startsWith("LEATHER_")) {
                frio += porPieza;          // el cuero abriga mucho
                calor += porPieza * 0.25;  // pero da calor, protege poco del sol
            } else if (nombre.startsWith("NETHERITE_")) {
                frio += porPieza * 0.75;
                calor += porPieza;         // el netherite resiste el fuego
            } else if (matCasco != null && tipo == matCasco) {
                frio += porPieza * 1.5;    // el casco espacial aisla de todo
                calor += porPieza * 1.5;
            } else {
                frio += porPieza * 0.5;
                calor += porPieza * 0.5;
            }
        }

        return new double[]{frio, calor};
    }

    /**
     * Actualiza la temperatura de un jugador y devuelve su valor actual.
     * La temperatura se mueve gradualmente hacia la del ambiente en vez de
     * saltar de golpe, para que los cambios se sientan progresivos.
     */
    public double actualizar(Player jugador, PlanetData planeta) {
        UUID uuid = jugador.getUniqueId();
        double ambiente = calcularTemperaturaAmbiente(planeta, jugador.getLocation());

        PlanetEnvironment env = planeta.getEntorno();
        double[] proteccion = calcularProteccion(jugador, env);

        // La armadura "tira" de la temperatura hacia la zona segura, pero nunca
        // mas alla de ella: abrigarse en un planeta helado te acerca al umbral
        // de frio, no te pone a 20 grados.
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

    /** Devuelve la temperatura actual de un jugador sin recalcularla. */
    public double getTemperatura(UUID uuid) {
        return temperaturaJugador.getOrDefault(uuid, TEMPERATURA_INICIAL);
    }

    /** Resetea al jugador a temperatura neutra (al salir de un planeta, al morir). */
    public void resetear(UUID uuid) {
        temperaturaJugador.put(uuid, TEMPERATURA_INICIAL);
    }

    public void limpiar(UUID uuid) {
        temperaturaJugador.remove(uuid);
    }

    /**
     * Devuelve un color segun lo peligrosa que sea la temperatura,
     * para pintar el actionbar/bossbar.
     */
    public String colorPara(double temperatura, PlanetEnvironment env) {
        if (temperatura >= env.getUmbralCalor()) return "§c";      // rojo: calor peligroso
        if (temperatura >= env.getUmbralCalor() - 10) return "§6"; // naranja: empieza a apretar
        if (temperatura <= env.getUmbralFrio()) return "§b";       // cian: frio peligroso
        if (temperatura <= env.getUmbralFrio() + 10) return "§3";  // azul: empieza a helar
        return "§a";                                                // verde: zona segura
    }
}
