package com.planettravel.selection;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.config.SpaceSettings;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Dibuja con particulas la caja seleccionada, solo para el jugador que sostiene
 * el hacha. Como un planeta puede medir cientos de bloques, solo se dibujan los
 * tramos de arista que quedan cerca del jugador (a menos de 40 bloques): asi el
 * coste es fijo, sea cual sea el tamanio de la seleccion.
 *
 * Ademas, mientras el jugador esta en "modo marcar punto" (ver
 * {@link SelectionManager.ModoPunto}) dibuja en vivo la zona de ENTRADA o
 * SALIDA que esta configurando (verde/naranja), o la posicion del SOL, para
 * que vea exactamente donde va a caer antes de confirmar con el hacha.
 */
public class SelectionPreviewTask extends BukkitRunnable {

    private static final double ALCANCE = 40.0;
    private static final double PASO = 1.5;

    private static final Color COLOR_ENTRADA = Color.fromRGB(80, 255, 100);
    private static final Color COLOR_SALIDA = Color.fromRGB(255, 160, 40);
    private static final Color COLOR_SOL = Color.fromRGB(255, 230, 60);
    private static final Color COLOR_DETECCION = Color.fromRGB(120, 80, 255);

    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final SelectionManager seleccion;
    private final PlanetConfigManager config;

    public SelectionPreviewTask(org.bukkit.plugin.java.JavaPlugin plugin, SelectionManager seleccion,
                                 PlanetConfigManager config) {
        this.plugin = plugin;
        this.seleccion = seleccion;
        this.config = config;
    }

    @Override
    public void run() {
        for (Player jugador : plugin.getServer().getOnlinePlayers()) {
            if (!seleccion.esHacha(jugador.getInventory().getItemInMainHand())) {
                continue;
            }

            SelectionManager.ModoPunto modo = seleccion.getModoPunto(jugador);
            if (modo != null) {
                dibujarModoPunto(jugador, modo);
                continue; // en modo punto no se dibuja tambien la seleccion pos1/pos2
            }

            Location a = seleccion.getPos1(jugador);
            Location b = seleccion.getPos2(jugador);
            Location cerca = (a != null) ? a : b;
            if (cerca == null || cerca.getWorld() == null || !cerca.getWorld().equals(jugador.getWorld())) {
                continue;
            }

            if (a != null && b != null && a.getWorld().equals(b.getWorld())) {
                Seleccion s = seleccion.getSeleccion(jugador);
                if (s != null) {
                    dibujarCaja(jugador, s.minX(), s.minY(), s.minZ(),
                            s.maxX() + 1.0, s.maxY() + 1.0, s.maxZ() + 1.0,
                            Color.fromRGB(80, 220, 255));
                }
            }
            // Cada punto marcado se dibuja como un bloque, en otro color.
            marcarBloque(jugador, a, Color.fromRGB(255, 200, 40));
            marcarBloque(jugador, b, Color.fromRGB(255, 120, 40));
        }
    }

    // ------------------------------------------------------------
    //  Vista previa de la zona de ENTRADA / SALIDA / SOL en vivo
    // ------------------------------------------------------------

    private void dibujarModoPunto(Player jugador, SelectionManager.ModoPunto modo) {
        if (modo.tipo() == SelectionManager.TipoPunto.SOL) {
            dibujarSol(jugador);
            return;
        }

        PlanetData planeta = config.getPlaneta(modo.idPlaneta()).orElse(null);
        if (planeta == null) {
            return;
        }

        if (modo.tipo() == SelectionManager.TipoPunto.DETECCION) {
            if (!jugador.getWorld().getName().equals(config.getMundoEspacio())) {
                return;
            }
            double minX = Math.min(planeta.getDeteccionCajaX(), planeta.getDeteccionCajaX2());
            double maxX = Math.max(planeta.getDeteccionCajaX(), planeta.getDeteccionCajaX2()) + 1.0;
            double minY = Math.min(planeta.getDeteccionCajaY(), planeta.getDeteccionCajaY2());
            double maxY = Math.max(planeta.getDeteccionCajaY(), planeta.getDeteccionCajaY2()) + 1.0;
            double minZ = Math.min(planeta.getDeteccionCajaZ(), planeta.getDeteccionCajaZ2());
            double maxZ = Math.max(planeta.getDeteccionCajaZ(), planeta.getDeteccionCajaZ2()) + 1.0;
            dibujarCaja(jugador, minX, minY, minZ, maxX, maxY, maxZ, COLOR_DETECCION);
            return;
        }

        boolean esEntrada = modo.tipo() == SelectionManager.TipoPunto.ENTRADA;
        String mundoEsperado = esEntrada ? planeta.getWorldName() : config.getMundoEspacio();
        if (!jugador.getWorld().getName().equals(mundoEsperado)) {
            return; // esta en el mundo equivocado: nada que dibujar aqui todavia
        }

        if (esEntrada) {
            double minX = Math.min(planeta.getSpawnPlanetaX(), planeta.getSpawnPlanetaX2());
            double maxX = Math.max(planeta.getSpawnPlanetaX(), planeta.getSpawnPlanetaX2()) + 1.0;
            double minZ = Math.min(planeta.getSpawnPlanetaZ(), planeta.getSpawnPlanetaZ2());
            double maxZ = Math.max(planeta.getSpawnPlanetaZ(), planeta.getSpawnPlanetaZ2()) + 1.0;
            double y = planeta.getEntradaAtmosfericaY();
            // Zona 2D (solo X/Z): se dibuja como una caja plana y fina a la
            // altura de entrada atmosferica, para que se vea "flotando" ahi.
            dibujarCaja(jugador, minX, y, minZ, maxX, y + 0.3, maxZ, COLOR_ENTRADA);
        } else {
            double minX = Math.min(planeta.getSalidaEspacioX(), planeta.getSalidaEspacioX2());
            double maxX = Math.max(planeta.getSalidaEspacioX(), planeta.getSalidaEspacioX2()) + 1.0;
            double minY = Math.min(planeta.getSalidaEspacioY(), planeta.getSalidaEspacioY2());
            double maxY = Math.max(planeta.getSalidaEspacioY(), planeta.getSalidaEspacioY2()) + 1.0;
            double minZ = Math.min(planeta.getSalidaEspacioZ(), planeta.getSalidaEspacioZ2());
            double maxZ = Math.max(planeta.getSalidaEspacioZ(), planeta.getSalidaEspacioZ2()) + 1.0;
            dibujarCaja(jugador, minX, minY, minZ, maxX, maxY, maxZ, COLOR_SALIDA);
        }
    }

    private void dibujarSol(Player jugador) {
        SpaceSettings espacio = config.getSpaceSettings();
        World mundoEspacio = plugin.getServer().getWorld(config.getMundoEspacio());
        if (mundoEspacio == null || !jugador.getWorld().equals(mundoEspacio)) {
            return;
        }
        double r = 1.5;
        dibujarCaja(jugador, espacio.getSolX() - r, espacio.getSolY() - r, espacio.getSolZ() - r,
                espacio.getSolX() + r, espacio.getSolY() + r, espacio.getSolZ() + r, COLOR_SOL);
    }

    private void marcarBloque(Player jugador, Location punto, Color color) {
        if (punto == null || punto.getWorld() == null || !punto.getWorld().equals(jugador.getWorld())) {
            return;
        }
        dibujarCaja(jugador, punto.getBlockX(), punto.getBlockY(), punto.getBlockZ(),
                punto.getBlockX() + 1.0, punto.getBlockY() + 1.0, punto.getBlockZ() + 1.0, color);
    }

    private void dibujarCaja(Player j, double x1, double y1, double z1,
                             double x2, double y2, double z2, Color color) {
        Particle.DustOptions polvo = new Particle.DustOptions(color, 1.2f);

        // 4 aristas en cada eje
        for (double y : new double[]{y1, y2}) {
            for (double z : new double[]{z1, z2}) {
                linea(j, x1, y, z, x2, y, z, polvo);
            }
        }
        for (double x : new double[]{x1, x2}) {
            for (double z : new double[]{z1, z2}) {
                linea(j, x, y1, z, x, y2, z, polvo);
            }
        }
        for (double x : new double[]{x1, x2}) {
            for (double y : new double[]{y1, y2}) {
                linea(j, x, y, z1, x, y, z2, polvo);
            }
        }
    }

    /** Recorre la arista y solo emite particulas en los puntos cercanos al jugador. */
    private void linea(Player j, double x1, double y1, double z1,
                       double x2, double y2, double z2, Particle.DustOptions polvo) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double largo = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (largo <= 0) {
            return;
        }

        Location ojo = j.getLocation();
        double alcance2 = ALCANCE * ALCANCE;
        int pasos = (int) Math.ceil(largo / PASO);

        for (int i = 0; i <= pasos; i++) {
            double t = (double) i / pasos;
            double px = x1 + dx * t;
            double py = y1 + dy * t;
            double pz = z1 + dz * t;

            double ex = px - ojo.getX();
            double ey = py - ojo.getY();
            double ez = pz - ojo.getZ();
            if (ex * ex + ey * ey + ez * ez > alcance2) {
                continue;
            }
            j.spawnParticle(Particle.DUST, px, py, pz, 1, 0, 0, 0, 0, polvo);
        }
    }
}
