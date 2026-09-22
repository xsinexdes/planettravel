package com.planettravel.selection;

import com.planettravel.PlanetTravel;
import com.planettravel.config.PlanetData;
import org.bukkit.entity.Player;

/**
 * Convierte una seleccion en el AREA de un planeta.
 *
 * El planeta es una esfera, asi que la seleccion se toma como la caja que la
 * contiene (la misma que marcarias en WorldEdit alrededor de tu esfera):
 *
 *   centro          = centro de la caja
 *   radio visual    = la mitad de la dimension mas grande (radio de la esfera de bloques)
 *   radio deteccion = radio visual + margen (config: seleccion.margen-deteccion)
 *
 * Asi la nave salta al planeta un poco ANTES de chocar contra los bloques.
 */
public final class AreaPlaneta {

    private AreaPlaneta() {
    }

    public static void aplicar(PlanetData planeta, Seleccion s, double margen) {
        double centroX = (s.minX() + s.maxX() + 1) / 2.0;
        double centroY = (s.minY() + s.maxY() + 1) / 2.0;
        double centroZ = (s.minZ() + s.maxZ() + 1) / 2.0;

        double radioVisual = Math.max(s.anchoX(), Math.max(s.altoY(), s.anchoZ())) / 2.0;

        planeta.setEsferaEspacio(centroX, centroY, centroZ, radioVisual + margen);
        planeta.setRadioVisual(radioVisual);
    }

    /**
     * Toma la seleccion del jugador (hacha propia o WorldEdit), la aplica como area
     * del planeta y guarda. Devuelve el mensaje de exito o lanza IllegalStateException
     * con un mensaje listo para mostrar.
     */
    public static String aplicarDesde(PlanetTravel plugin, Player jugador, PlanetData planeta, boolean worldEdit) {
        Seleccion s = plugin.getSelectionManager().obtener(jugador, worldEdit);

        String mundoEspacio = plugin.getConfigManager().getMundoEspacio();
        if (!s.mundo().getName().equals(mundoEspacio)) {
            throw new IllegalStateException("La seleccion esta en el mundo '" + s.mundo().getName()
                    + "', pero el area de un planeta se marca en el mundo del espacio ('" + mundoEspacio + "').");
        }

        aplicar(planeta, s, plugin.getConfigManager().getMargenDeteccion());
        plugin.getConfigManager().guardarTodo();

        return String.format("§a✔ Area de '%s' actualizada (%s). Centro: §f%.0f, %.0f, %.0f§a | radio visual: §f%.0f§a | deteccion: §f%.0f",
                planeta.getId(), s.resumen(), planeta.getSpaceX(), planeta.getSpaceY(), planeta.getSpaceZ(),
                planeta.getRadioVisual(), planeta.getRadio());
    }
}
