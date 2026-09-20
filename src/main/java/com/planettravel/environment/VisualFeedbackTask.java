package com.planettravel.environment;

import com.planettravel.api.CoreNavesAPI;
import com.planettravel.api.CoreNavesHook;
import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Todo el feedback visual que ayuda al jugador a orientarse:
 *
 *  1) BOSSBAR DE ALTITUD: cuando pilotas en un mundo-planeta, una barra muestra
 *     lo cerca que estas de la altura de escape. Sin esto, subir a ciegas
 *     esperando a que "pase algo" es muy poco satisfactorio.
 *
 *  2) ACTIONBAR DE PROXIMIDAD: en el mundo espacio, muestra el planeta mas
 *     cercano y a cuantos bloques esta. Volar por el vacio buscando una esfera
 *     sin ninguna referencia es lo mas frustrante que hay.
 *
 *  3) BORDE DE ESFERA: dibuja con particulas el limite de la zona de aterrizaje
 *     cuando te acercas, para que veas exactamente donde empieza la atmosfera.
 */
public class VisualFeedbackTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final PlanetConfigManager configManager;
    private final CoreNavesHook coreNavesHook;

    /** Una bossbar por jugador, creada bajo demanda y reutilizada. */
    private final Map<UUID, BossBar> barras = new HashMap<>();

    /**
     * Contador de ticks propio: el borde de la esfera se dibuja mas despacio
     * que el resto (las particulas son caras y no hace falta refrescarlas
     * tantas veces por segundo).
     */
    private int contador = 0;

    public VisualFeedbackTask(JavaPlugin plugin, PlanetConfigManager configManager, CoreNavesHook coreNavesHook) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.coreNavesHook = coreNavesHook;
    }

    @Override
    public void run() {
        contador++;
        CoreNavesAPI api = coreNavesHook.getApi();

        for (Player jugador : Bukkit.getOnlinePlayers()) {
            World mundo = jugador.getWorld();

            if (mundo.getName().equals(configManager.getMundoEspacio())) {
                ocultarBarra(jugador);
                if (configManager.isActionbarProximidad()) {
                    mostrarProximidad(jugador);
                }
                if (configManager.isDibujarBordeEsfera() && contador % 4 == 0) {
                    dibujarBordesCercanos(jugador);
                }
                continue;
            }

            PlanetData planeta = configManager.getPlanetaPorMundo(mundo.getName());
            if (planeta == null) {
                ocultarBarra(jugador);
                continue;
            }

            // Solo mostramos la barra de altitud si esta pilotando de verdad.
            boolean pilotando = (api != null) && api.estaPilotandoNave(jugador);
            if (configManager.isBossbarAltitud() && pilotando) {
                mostrarAltitud(jugador, planeta);
            } else {
                ocultarBarra(jugador);
            }
        }
    }

    // ------------------------------------------------------------
    //  1) BOSSBAR DE ALTITUD
    // ------------------------------------------------------------

    private void mostrarAltitud(Player jugador, PlanetData planeta) {
        double alturaActual = jugador.getLocation().getY();
        double alturaEscape = planeta.getAlturaDespegueY();
        double alturaSuelo = planeta.getSpawnPlanetaY() - 100; // referencia baja razonable

        // Progreso 0..1 desde el "suelo" de referencia hasta la altura de escape.
        double progreso = (alturaActual - alturaSuelo) / (alturaEscape - alturaSuelo);
        progreso = Math.max(0.0, Math.min(1.0, progreso));

        BossBar barra = barras.computeIfAbsent(jugador.getUniqueId(), uuid -> {
            BossBar nueva = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SEGMENTED_10);
            nueva.addPlayer(jugador);
            return nueva;
        });

        if (!barra.getPlayers().contains(jugador)) {
            barra.addPlayer(jugador);
        }

        barra.setProgress(progreso);

        // El color y el texto cambian conforme te acercas al escape, para que
        // se note que estas a punto de salir de la atmosfera.
        if (progreso >= 0.98) {
            barra.setColor(BarColor.GREEN);
            barra.setTitle("§a§lALTITUD DE ESCAPE ALCANZADA §7— §fsaliendo al espacio");
        } else if (progreso >= 0.85) {
            barra.setColor(BarColor.YELLOW);
            barra.setTitle(String.format("§e⬆ Altitud §f%.0f §7/ §f%.0f §7— §ecasi en el limite",
                    alturaActual, alturaEscape));
        } else {
            barra.setColor(BarColor.BLUE);
            barra.setTitle(String.format("§b⬆ Altitud §f%.0f §7/ §f%.0f §7— §b%s",
                    alturaActual, alturaEscape, planeta.getNombreVisible()));
        }

        barra.setVisible(true);
    }

    private void ocultarBarra(Player jugador) {
        BossBar barra = barras.get(jugador.getUniqueId());
        if (barra != null) {
            barra.setVisible(false);
        }
    }

    // ------------------------------------------------------------
    //  2) ACTIONBAR DE PROXIMIDAD EN EL ESPACIO
    // ------------------------------------------------------------

    private void mostrarProximidad(Player jugador) {
        Location loc = jugador.getLocation();
        PlanetData masCercano = null;
        double distanciaMinima = Double.MAX_VALUE;

        // Comparamos con distancia AL CUADRADO para no gastar raices en el bucle;
        // solo sacamos la raiz del ganador, una vez.
        for (PlanetData planeta : configManager.getPlanetas().values()) {
            double d2 = planeta.distanciaCuadradaAlCentro(loc.getX(), loc.getY(), loc.getZ());
            if (d2 < distanciaMinima) {
                distanciaMinima = d2;
                masCercano = planeta;
            }
        }

        if (masCercano == null) {
            return;
        }

        double distancia = Math.sqrt(distanciaMinima);
        double distanciaAlBorde = distancia - masCercano.getRadio();

        if (distancia > configManager.getDistanciaAvisoProximidad()) {
            ChatUtil.actionBar(jugador, "§8✦ §7Espacio profundo §8— §7sin planetas cerca");
            return;
        }

        // El color indica lo cerca que estas de entrar en la atmosfera.
        String color;
        String icono;
        if (distanciaAlBorde <= 0) {
            color = "§a";
            icono = "◉";
        } else if (distanciaAlBorde < 100) {
            color = "§e";
            icono = "◎";
        } else {
            color = "§b";
            icono = "○";
        }

        ChatUtil.actionBar(jugador, String.format("%s%s §f%s §7— §f%.0f §7bloques al borde atmosferico",
                color, icono, masCercano.getNombreVisible(), Math.max(0, distanciaAlBorde)));
    }

    // ------------------------------------------------------------
    //  3) BORDE VISIBLE DE LA ESFERA
    // ------------------------------------------------------------

    /**
     * Dibuja un anillo de particulas en la superficie de la esfera de deteccion
     * de los planetas cercanos, en el lado que mira al jugador.
     *
     * No dibujamos la esfera entera (serian miles de particulas): calculamos el
     * punto de la esfera mas cercano al jugador y trazamos un disco alrededor
     * de ese punto, perpendicular a la linea jugador-planeta. Visualmente da
     * la sensacion de un "portal" o anillo de entrada flotando ante ti.
     */
    private void dibujarBordesCercanos(Player jugador) {
        Location loc = jugador.getLocation();
        double maxDist = configManager.getDistanciaDibujarBorde();

        for (PlanetData planeta : configManager.getPlanetas().values()) {
            double distancia = planeta.distanciaAlCentro(loc.getX(), loc.getY(), loc.getZ());

            // Solo si estamos cerca del borde (fuera, pero no demasiado lejos).
            double distanciaAlBorde = distancia - planeta.getRadio();
            if (distanciaAlBorde < 0 || distanciaAlBorde > maxDist) {
                continue;
            }

            dibujarAnilloDeEntrada(jugador, planeta, distancia);
        }
    }

    private void dibujarAnilloDeEntrada(Player jugador, PlanetData planeta, double distancia) {
        World mundo = jugador.getWorld();
        Location loc = jugador.getLocation();

        // Vector unitario desde el centro del planeta hacia el jugador.
        double dx = (loc.getX() - planeta.getSpaceX()) / distancia;
        double dy = (loc.getY() - planeta.getSpaceY()) / distancia;
        double dz = (loc.getZ() - planeta.getSpaceZ()) / distancia;

        // Punto de la superficie de la esfera mas cercano al jugador.
        double cx = planeta.getSpaceX() + dx * planeta.getRadio();
        double cy = planeta.getSpaceY() + dy * planeta.getRadio();
        double cz = planeta.getSpaceZ() + dz * planeta.getRadio();

        // Dos vectores perpendiculares a (dx,dy,dz) para trazar el disco.
        // Elegimos un vector auxiliar que no sea paralelo al normal.
        double[] aux = (Math.abs(dy) < 0.9) ? new double[]{0, 1, 0} : new double[]{1, 0, 0};

        // u = normal x aux (producto vectorial), normalizado
        double ux = dy * aux[2] - dz * aux[1];
        double uy = dz * aux[0] - dx * aux[2];
        double uz = dx * aux[1] - dy * aux[0];
        double lenU = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= lenU; uy /= lenU; uz /= lenU;

        // v = normal x u
        double vx = dy * uz - dz * uy;
        double vy = dz * ux - dx * uz;
        double vz = dx * uy - dy * ux;

        // Radio del anillo dibujado: proporcional al del planeta, con tope
        // para que no se convierta en miles de particulas.
        double radioAnillo = Math.min(planeta.getRadio() * 0.5, 30);
        int puntos = 40;

        Particle.DustOptions color = new Particle.DustOptions(Color.fromRGB(80, 200, 255), 2.0f);

        for (int i = 0; i < puntos; i++) {
            double angulo = (2 * Math.PI * i) / puntos;
            double cos = Math.cos(angulo) * radioAnillo;
            double sin = Math.sin(angulo) * radioAnillo;

            double px = cx + ux * cos + vx * sin;
            double py = cy + uy * cos + vy * sin;
            double pz = cz + uz * cos + vz * sin;

            mundo.spawnParticle(Particle.DUST, px, py, pz, 1, 0, 0, 0, 0, color);
        }
    }

    // ------------------------------------------------------------

    public void limpiarJugador(UUID uuid) {
        BossBar barra = barras.remove(uuid);
        if (barra != null) {
            barra.removeAll();
        }
    }

    /** Elimina todas las bossbars (al apagar el plugin). */
    public void limpiarTodo() {
        for (BossBar barra : barras.values()) {
            barra.removeAll();
        }
        barras.clear();
    }
}
