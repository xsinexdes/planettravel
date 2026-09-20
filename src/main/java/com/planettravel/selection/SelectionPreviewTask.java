package com.planettravel.selection;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Dibuja con particulas la caja seleccionada, solo para el jugador que sostiene
 * el hacha. Como un planeta puede medir cientos de bloques, solo se dibujan los
 * tramos de arista que quedan cerca del jugador (a menos de 40 bloques): asi el
 * coste es fijo, sea cual sea el tamanio de la seleccion.
 */
public class SelectionPreviewTask extends BukkitRunnable {

    private static final double ALCANCE = 40.0;
    private static final double PASO = 1.5;

    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final SelectionManager seleccion;

    public SelectionPreviewTask(org.bukkit.plugin.java.JavaPlugin plugin, SelectionManager seleccion) {
        this.plugin = plugin;
        this.seleccion = seleccion;
    }

    @Override
    public void run() {
        for (Player jugador : plugin.getServer().getOnlinePlayers()) {
            if (!seleccion.esHacha(jugador.getInventory().getItemInMainHand())) {
                continue;
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
